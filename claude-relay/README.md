# Claude Code Terminal Relay

A lightweight WebSocket relay server that spawns Claude Code CLI in a PTY
(pseudo-terminal) and bridges it to xterm.js in the browser.

## Architecture

```
Browser (xterm.js) ←→ WebSocket ←→ Node.js Relay ←→ PTY (Claude Code CLI)
```

## Setup

### Prerequisites

- Node.js 18+
- Claude Code CLI installed (`npm install -g @anthropic-ai/claude-code`)
- Build tools for `node-pty`:
  - **Linux (Raspberry Pi)**: `sudo apt install build-essential python3`
  - **Windows**: Visual Studio Build Tools with C++ workload, or `npm install --global windows-build-tools`

### Install

```bash
cd claude-relay
npm install
```

### Run (development)

```bash
npm run dev
# WebSocket server on ws://127.0.0.1:3001
# Health check: http://127.0.0.1:3001/health
```

### Run (production)

```bash
npm start
```

## Configuration

Environment variables:

| Variable | Default | Description |
|----------|---------|-------------|
| `PORT` | `3001` | WebSocket listen port |
| `BIND_HOST` | `127.0.0.1` | Bind address (use `0.0.0.0` for LAN access without Nginx) |
| `CLAUDE_CLI_PATH` | `claude` | Path to Claude Code CLI executable |
| `CLAUDE_WORK_DIR` | `$HOME` | Working directory for Claude Code |

## Deployment (Raspberry Pi)

```bash
sudo cp deploy/systemd/claude-relay.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now claude-relay
sudo systemctl status claude-relay
```

## Testing

```bash
# Test WebSocket connection
npx wscat -c ws://127.0.0.1:3001

# Test health endpoint
curl http://127.0.0.1:3001/health
```

## Notes

- Only **one session** is allowed at a time (Claude Code is inherently single-user)
- The relay kills the PTY when the WebSocket disconnects
- Terminal resize is supported — the browser sends `{"type":"resize","cols":80,"rows":24}`
