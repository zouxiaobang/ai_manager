import { spawn } from 'node-pty';
import { WebSocketServer } from 'ws';
import { createServer } from 'node:http';
import { homedir } from 'node:os';

// ─── Configuration ───────────────────────────────────────────────────────────
const PORT = parseInt(process.env.PORT || '3001', 10);
const BIND_HOST = process.env.BIND_HOST || '127.0.0.1';
const CLAUDE_CMD = process.env.CLAUDE_CLI_PATH || 'claude';

// Heartbeat: per-client ping every 15s, drop client if no pong within 10s
const PING_INTERVAL_MS = 15000;
const PONG_TIMEOUT_MS = 10000;

// Grace period: keep PTY alive for this long after last client disconnects
const SESSION_IDLE_TIMEOUT_MS = 5 * 60 * 1000; // 5 minutes

// Ring buffer: replay recent PTY output to clients that join mid-session.
// Without this, a refresh shows a blank screen because the PTY is idle.
const MAX_BUFFER_SIZE = 128 * 1024; // 128 KB

const isWindows = process.platform === 'win32';
const defaultWorkDir = isWindows ? 'G:/projects' : homedir();
const WORK_DIR = process.env.CLAUDE_WORK_DIR || defaultWorkDir;

// ─── State ───────────────────────────────────────────────────────────────────
// session = { pty, cols, rows, clients: Map<ws, heartbeatTimers>, idleTimer, buffer, bufferSize }
let session = null;
let clientIdCounter = 0;

// ─── Helpers ─────────────────────────────────────────────────────────────────

// Generous default — PTY only ever grows, never shrinks (see resize handler)
const DEFAULT_COLS = 100;
const DEFAULT_ROWS = 30;

function createPty(cols = DEFAULT_COLS, rows = DEFAULT_ROWS) {
  const env = { ...process.env, TERM: 'xterm-256color', COLORTERM: 'truecolor' };
  if (isWindows) {
    return spawn('powershell.exe', ['-NoLogo', '-NoProfile'], {
      name: 'xterm-256color', cols, rows, cwd: WORK_DIR, env,
    });
  }
  return spawn('bash', ['-c', CLAUDE_CMD], {
    name: 'xterm-256color', cols, rows, cwd: WORK_DIR, env,
  });
}

function broadcast(data) {
  if (!session) return;
  const dead = [];
  for (const [ws] of session.clients) {
    try {
      if (ws.readyState === ws.OPEN) ws.send(data);
    } catch { dead.push(ws); }
  }
  for (const ws of dead) session.clients.delete(ws);
}

function sendJson(ws, obj) {
  try {
    if (ws.readyState === ws.OPEN) ws.send(JSON.stringify(obj));
  } catch { /* ignore */ }
}

// ─── Per-Client Heartbeat ────────────────────────────────────────────────────

function addHeartbeat(ws) {
  const timers = { ping: null, pong: null };
  timers.ping = setInterval(() => {
    if (ws.readyState !== ws.OPEN) { removeClient(ws); return; }
    let ok = false;
    ws.once('pong', () => { ok = true; });
    ws.ping();
    timers.pong = setTimeout(() => {
      if (!ok) {
        console.log('[relay] Heartbeat timeout — removing client');
        removeClient(ws);
        try { ws.terminate(); } catch { /* ignore */ }
      }
    }, PONG_TIMEOUT_MS);
  }, PING_INTERVAL_MS);
  session.clients.get(ws)._hb = timers;
}

function clearHeartbeat(ws) {
  const hb = session?.clients.get(ws)?._hb;
  if (!hb) return;
  if (hb.ping) { clearInterval(hb.ping); hb.ping = null; }
  if (hb.pong) { clearTimeout(hb.pong); hb.pong = null; }
}

// ─── Session Management ──────────────────────────────────────────────────────

function startPty(cols, rows) {
  const pty = createPty(cols, rows);
  session = { pty, cols, rows, clients: new Map(), idleTimer: null, buffer: [], bufferSize: 0 };
  console.log(`[relay] PTY started (PID: ${pty.pid}, cols=${cols}, rows=${rows})`);

  // PTY output → buffer + broadcast to all clients
  pty.onData((data) => {
    // Append to ring buffer for replay to late-joining clients
    session.buffer.push(data);
    session.bufferSize += data.length;
    // Trim old data when buffer exceeds max size
    while (session.bufferSize > MAX_BUFFER_SIZE && session.buffer.length > 0) {
      const removed = session.buffer.shift();
      session.bufferSize -= removed.length;
    }
    broadcast(data);
  });

  // PTY exit → notify all clients
  pty.onExit(({ exitCode, signal }) => {
    console.log(`[relay] PTY exited (code=${exitCode}, signal=${signal})`);
    for (const [ws] of session.clients) {
      sendJson(ws, { type: 'exit', exitCode, signal });
      try { ws.close(); } catch { /* ignore */ }
    }
    destroySession();
  });

  if (isWindows) {
    console.log(`[relay] Launching Claude via PowerShell (CWD: ${WORK_DIR})`);
    setTimeout(() => {
      // Clear buffer so sync() returns only Claude output, not PowerShell startup
      session.buffer = [];
      session.bufferSize = 0;
      try { pty.write(`${CLAUDE_CMD}\r\n`); } catch { /* ignore */ }
    }, 300);
  }
}

function destroySession() {
  if (!session) return;
  for (const [ws] of session.clients) {
    clearHeartbeat(ws);
    try { ws.close(); } catch { /* ignore */ }
  }
  session.clients.clear();
  if (session.idleTimer) { clearTimeout(session.idleTimer); session.idleTimer = null; }
  try { session.pty.kill(); } catch { /* ignore */ }
  session = null;
  console.log('[relay] Session destroyed');
}

function addClient(ws) {
  const id = ++clientIdCounter;
  session.clients.set(ws, { id });
  addHeartbeat(ws);
  // Cancel idle kill timer
  if (session.idleTimer) { clearTimeout(session.idleTimer); session.idleTimer = null; }
  console.log(`[relay] Client #${id} joined (total: ${session.clients.size})`);

  // Notify all clients of new count
  const msg = JSON.stringify({
    type: 'clients',
    count: session.clients.size,
    action: id === 1 ? 'created' : 'joined',
  });
  broadcast(msg);
}

function removeClient(ws) {
  if (!session) return;
  clearHeartbeat(ws);
  const hadCols = session.clients.get(ws)?.cols ?? 80;
  const hadRows = session.clients.get(ws)?.rows ?? 24;
  session.clients.delete(ws);
  console.log(`[relay] Client left (remaining: ${session.clients.size})`);

  if (session.clients.size === 0) {
    // Start grace period — keep PTY alive for reconnection
    console.log(`[relay] No clients, PTY will be killed after ${SESSION_IDLE_TIMEOUT_MS / 1000}s idle`);
    session.idleTimer = setTimeout(() => {
      console.log('[relay] Idle timeout — destroying PTY');
      destroySession();
    }, SESSION_IDLE_TIMEOUT_MS);
  } else {
    // A large client left — recalculate max and shrink PTY if necessary
    let maxCols = 0, maxRows = 0;
    for (const [, other] of session.clients) {
      if (other.cols > maxCols) maxCols = other.cols;
      if (other.rows > maxRows) maxRows = other.rows;
    }
    maxCols = Math.max(20, maxCols);
    maxRows = Math.max(5, maxRows);
    if (maxCols < session.cols || maxRows < session.rows) {
      session.cols = maxCols;
      session.rows = maxRows;
      console.log(`[relay] PTY resized to ${maxCols}x${maxRows} (client left)`);
      try { session.pty.resize(maxCols, maxRows); } catch { /* ignore */ }
    }

    broadcast(JSON.stringify({ type: 'clients', count: session.clients.size, action: 'left' }));
  }
}

// ─── HTTP + WebSocket Server ─────────────────────────────────────────────────

const httpServer = createServer((req, res) => {
  const url = new URL(req.url, `http://${req.headers.host || 'localhost'}`);

  if (url.pathname === '/health' || url.pathname === '/') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({
      status: 'ok',
      clients: session ? session.clients.size : 0,
      platform: process.platform,
      cli: CLAUDE_CMD,
      cwd: WORK_DIR,
      pid: session?.pty?.pid ?? null,
    }));
    return;
  }

  if (url.pathname === '/reset' && req.method === 'POST') {
    destroySession();
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ status: 'ok', message: 'Session destroyed' }));
    return;
  }

  res.writeHead(404);
  res.end();
});

const wss = new WebSocketServer({ server: httpServer, maxPayload: 1024 * 1024 });

wss.on('connection', (ws, req) => {
  console.log(`[relay] WebSocket connected from ${req.socket.remoteAddress}`);

  let clientCols = 80;
  let clientRows = 24;

  ws.on('message', (raw) => {
    let data;
    try {
      data = typeof raw === 'string' ? raw : raw.toString();
    } catch { return; }

    // JSON control messages
    try {
      const msg = JSON.parse(data);
      if (msg.type === 'resize' && typeof msg.cols === 'number' && typeof msg.rows === 'number') {
        clientCols = Math.max(20, Math.min(msg.cols, 500));
        clientRows = Math.max(5, Math.min(msg.rows, 200));
        // Store this client's dimensions (so "largest client" calc uses fresh data)
        const ci = session?.clients?.get(ws);
        if (ci) { ci.cols = clientCols; ci.rows = clientRows; }
        // Resize PTY to match the largest client. Unlike the old "grow-only"
        // policy, we also allow shrinking so a mobile client can pull the
        // PTY width down to its screen size when PC isn't connected.
        if (session) {
          let maxCols = 0;
          let maxRows = 0;
          for (const [, other] of session.clients) {
            if (other.cols > maxCols) maxCols = other.cols;
            if (other.rows > maxRows) maxRows = other.rows;
          }
          // Floor at reasonable minimums
          maxCols = Math.max(20, maxCols);
          maxRows = Math.max(5, maxRows);
          if (maxCols !== session.cols || maxRows !== session.rows) {
            session.cols = maxCols;
            session.rows = maxRows;
            console.log(`[relay] PTY resized to ${maxCols}x${maxRows}`);
            try { session.pty.resize(maxCols, maxRows); } catch { /* ignore */ }
          }
        }
        return;
      }
      if (msg.type === 'sync') {
        // Client requests buffered content replay (terminal is now fitted).
        // Only reply to the requesting client, not broadcast.
        if (session && session.buffer.length > 0) {
          for (const chunk of session.buffer) {
            try {
              if (ws.readyState === ws.OPEN) ws.send(chunk);
            } catch { break; }
          }
        }
        return;
      }
      return; // unknown JSON — ignore
    } catch {
      // Not JSON — terminal input
    }

    // Forward input to PTY (any client can type)
    if (session?.pty) {
      try { session.pty.write(data); } catch { /* ignore */ }
    }
  });

  ws.on('close', () => {
    console.log('[relay] WebSocket disconnected');
    if (session?.clients.has(ws)) removeClient(ws);
  });

  ws.on('error', (err) => {
    console.error('[relay] WebSocket error:', err.message);
    if (session?.clients.has(ws)) removeClient(ws);
  });

  // Join or create session
  const isNewSession = !session;
  if (isNewSession) {
    startPty(clientCols, clientRows);
  }
  addClient(ws);
  // Store client's preferred size
  session.clients.get(ws).cols = clientCols;
  session.clients.get(ws).rows = clientRows;
  sendJson(ws, { type: 'ready', cols: session.cols, rows: session.rows });
});

httpServer.listen(PORT, BIND_HOST, () => {
  console.log(`[relay] Claude Code relay listening on http://${BIND_HOST}:${PORT}`);
  console.log(`[relay] Platform: ${process.platform}, CLI: ${CLAUDE_CMD}, CWD: ${WORK_DIR}`);
});

// ─── Graceful shutdown ───────────────────────────────────────────────────────

process.on('SIGINT', shutdown);
process.on('SIGTERM', shutdown);

function shutdown() {
  console.log('[relay] Shutting down...');
  destroySession();
  wss.close(() => httpServer.close(() => process.exit(0)));
}
