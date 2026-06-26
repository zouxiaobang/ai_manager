export type DeployCenterTab = 'overview' | 'steps' | 'daily' | 'troubleshooting'

export interface DeployNodeCard {
  key: string
  title: string
  value: string
  subtitle: string
  tone: 'blue' | 'green' | 'purple' | 'orange'
  icon: 'server' | 'database' | 'laptop' | 'globe'
  tags: string[]
  tagTone?: 'theme' | 'success'
}

export interface DeployCredentialRow {
  label: string
  value: string
  copyable?: boolean
}

export interface DeployCredentialGroup {
  key: 'nodes' | 'ssh' | 'mysql' | 'app'
  fields: DeployCredentialRow[]
}

export interface DeployCommandBlock {
  title: string
  description?: string
  commands: string[]
  platform?: 'windows' | 'linux'
}

export interface DeployStepSection {
  id: string
  title: string
  summary: string
  blocks: DeployCommandBlock[]
}

export interface DeployTroubleRow {
  symptom: string
  action: string
}

export interface DeployStepsChecklistItem {
  id: string
  title: string
  detailDesc: string
  inspectSteps: string[]
  commands: string[]
  /** 健康检查通过后自动标记完成 */
  autoComplete?: 'health' | 'redis' | 'mysql'
}

export const deployAppNodeId = '114'
export const deployDataNodeId = '118'

export const DEPLOY_CENTER_DOC_ID = 'raspberry-pi-deploy'

export type DeployNodeHoverTipKey = 'app' | 'data' | 'storage'

export function hasDeployNodeHoverTip(key: string): key is DeployNodeHoverTipKey {
  return key === 'app' || key === 'data' || key === 'storage'
}

export function hoverTipWidth(key: DeployNodeHoverTipKey): number {
  if (key === 'data') return 440
  if (key === 'storage') return 460
  return 400
}

export const deployOverviewNodes: DeployNodeCard[] = [
  {
    key: 'app',
    title: '应用节点',
    value: '114',
    subtitle: 'Nginx + Spring Boot',
    tone: 'blue',
    icon: 'server',
    tags: ['负载均衡'],
  },
  {
    key: 'data',
    title: '数据节点',
    value: '118',
    subtitle: 'MySQL + Redis + Docker',
    tone: 'green',
    icon: 'database',
    tags: ['持久化存储'],
  },
  {
    key: 'dev',
    title: '开发机',
    value: '119',
    subtitle: 'Windows',
    tone: 'purple',
    icon: 'laptop',
    tags: [],
  },
  {
    key: 'url',
    title: '管理后台 URL',
    value: 'http://192.168.0.114/#/home',
    subtitle: 'PC / 移动端自动识别',
    tone: 'orange',
    icon: 'globe',
    tags: [],
  },
]

export interface DeployAppNodePathItem {
  label: string
  path: string
}

/** 应用节点 114 上常用部署路径 */
export const deployAppNodePaths: DeployAppNodePathItem[] = [
  { label: 'Nginx 配置文件', path: '/etc/nginx/sites-available/ai-manager' },
  { label: '前端静态文件', path: '/var/www/ai-manager' },
  { label: '后端部署目录', path: '/opt/ai-manager/backend' },
]

/** 应用节点 114 后端日志（systemd journal） */
export const deployMonitorLogPaths: DeployAppNodePathItem[] = [
  { label: '后端运行目录', path: '/opt/ai-manager/backend' },
  { label: 'systemd 服务单元', path: '/etc/systemd/system/ai-manager-backend.service' },
  { label: '实时查看日志', path: 'journalctl -u ai-manager-backend -f' },
  { label: '最近 50 行', path: 'journalctl -u ai-manager-backend -n 50 --no-pager' },
]

export interface DeployCredentialField {
  label: string
  value: string
  copyable?: boolean
}

export interface DeployDataNodeServiceCredentials {
  key: 'mysql' | 'redis'
  title: string
  fields: DeployCredentialField[]
}

/** 数据节点 118 Docker MySQL / Redis 凭据 */
export const deployDataNodeCredentials: DeployDataNodeServiceCredentials[] = [
  {
    key: 'mysql',
    title: 'MySQL',
    fields: [
      { label: '主机（有线优先）', value: '192.168.0.118' },
      { label: '端口', value: '3306' },
      { label: 'root 密码', value: '123456' },
      { label: '应用用户', value: 'ai_manager' },
      { label: '应用密码', value: '123456' },
      { label: '数据库名', value: 'ai_manager_admin' },
      { label: 'Docker 容器', value: 'ai-manager-mysql' },
      {
        label: '连接命令',
        value: 'mysql -h 192.168.0.118 -u ai_manager -p123456 ai_manager_admin',
      },
    ],
  },
  {
    key: 'redis',
    title: 'Redis',
    fields: [
      { label: '主机（有线优先）', value: '192.168.0.118' },
      { label: '端口', value: '6379' },
      { label: '数据库索引', value: '0' },
      { label: '密码', value: '无（内网未配置）', copyable: false },
      { label: 'Docker 容器', value: 'ai-manager-redis' },
      { label: '连接命令', value: 'redis-cli -h 192.168.0.118 -p 6379 ping' },
    ],
  },
]

export interface DeployStoragePathSection {
  key: 'local-server' | 'local-dev' | 'baidu-pan'
  title: string
  fields: DeployCredentialField[]
}

/** 文件存储：本地目录与百度网盘路径 */
export const deployStorageNodePaths: DeployStoragePathSection[] = [
  {
    key: 'local-server',
    title: '本地存储 · 服务器（114）',
    fields: [
      { label: '上传根目录', value: '/opt/ai-manager/backend/uploads' },
      { label: '笔记正文', value: '/opt/ai-manager/backend/uploads/notebook-content' },
      { label: '笔记图片', value: '/opt/ai-manager/backend/uploads/notebook/images' },
      { label: '电商图片', value: '/opt/ai-manager/backend/uploads/ecommerce' },
    ],
  },
  {
    key: 'local-dev',
    title: '本地存储 · 开发机（119）',
    fields: [
      {
        label: '上传根目录',
        value: 'G:\\projects\\ai_project\\ai_manager\\admin-backend\\admin-server\\uploads',
      },
      {
        label: '笔记正文',
        value:
          'G:\\projects\\ai_project\\ai_manager\\admin-backend\\admin-server\\uploads\\notebook-content',
      },
      {
        label: '笔记图片',
        value:
          'G:\\projects\\ai_project\\ai_manager\\admin-backend\\admin-server\\uploads\\notebook\\images',
      },
      {
        label: '电商图片',
        value:
          'G:\\projects\\ai_project\\ai_manager\\admin-backend\\admin-server\\uploads\\ecommerce',
      },
    ],
  },
  {
    key: 'baidu-pan',
    title: '百度网盘目录',
    fields: [
      { label: '应用根目录', value: '/apps/ai_blog' },
      { label: '笔记正文', value: '/apps/ai_blog/notes' },
      { label: '回收站', value: '/apps/ai_blog/trash' },
      { label: '笔记图片', value: '/apps/ai_blog/images' },
    ],
  },
]

export interface DeployArchitectureNode {
  key: string
  label: string
  lines: string[]
  tone: 'gray' | 'blue' | 'green' | 'purple' | 'orange'
  icons: Array<'client' | 'nginx' | 'mysql' | 'redis' | 'docker' | 'folder' | 'admin'>
  url?: string
}

export const deployArchitectureFlow: DeployArchitectureNode[] = [
  {
    key: 'client',
    label: '客户端',
    lines: ['浏览器 / 移动端'],
    tone: 'gray',
    icons: ['client'],
  },
  {
    key: 'app',
    label: '应用节点集群',
    lines: ['114', 'Nginx + Spring Boot'],
    tone: 'blue',
    icons: ['nginx'],
  },
  {
    key: 'data',
    label: '数据节点',
    lines: ['118', 'MySQL · Redis'],
    tone: 'green',
    icons: ['mysql', 'redis', 'docker'],
  },
  {
    key: 'storage',
    label: '文件存储',
    lines: ['本地存储 · 百度云盘'],
    tone: 'purple',
    icons: ['folder'],
  },
  {
    key: 'admin',
    label: '管理后台',
    lines: [],
    tone: 'orange',
    icons: ['admin'],
    url: 'http://192.168.0.114/#/home',
  },
]

export const deployQuickVerify: DeployCommandBlock = {
  title: '快速验证',
  description: '在任意终端执行以下命令，验证服务是否正常运行：',
  commands: [
    'curl.exe -s http://192.168.0.114/api/health',
    'curl.exe -s http://192.168.0.114/api/todos/today',
    'curl -s http://127.0.0.1/api/health',
  ],
  platform: 'windows',
}

/** 部署环境 API 健康检查地址 */
export const deployApiHealthUrl = 'http://192.168.0.114/api/health'

/** 管理后台访问地址（Hash 路由） */
export const deployAdminUrl = 'http://192.168.0.114/#/home'

export const deployStepsChecklist: DeployStepsChecklistItem[] = [
  {
    id: 'node-status',
    title: '节点状态检查',
    detailDesc:
      '数据节点是存储与处理的基础组件，部署前需确认 Docker、MySQL 与 Redis 容器均已启动并可从应用节点访问。',
    inspectSteps: [
      'SSH 登录数据节点 192.168.0.118',
      '执行 docker compose ps，确认 MySQL / Redis 为 running',
      '检查数据目录磁盘空间是否充足（df -h）',
      '确认 3306 / 6379 端口在局域网可访问',
    ],
    commands: [
      'ssh kyle@192.168.0.118',
      'cd /opt/ai-manager/data-node && docker compose ps',
      'df -h /opt/ai-manager',
      'ss -tlnp | grep -E "3306|6379"',
    ],
  },
  {
    id: 'service-health',
    title: '服务健康检查',
    detailDesc: '确认 114 上 Nginx 与 Spring Boot 后端均已启动，systemd 服务无异常重启。',
    inspectSteps: [
      'SSH 登录应用节点 192.168.0.114',
      '检查 ai-manager-backend 服务状态',
      '确认 Nginx 已加载 ai-manager 站点配置',
      '本机 curl 127.0.0.1:8080/api/health 返回 UP',
    ],
    commands: [
      'ssh kyle@192.168.0.114',
      'systemctl status ai-manager-backend --no-pager',
      'sudo nginx -t && systemctl status nginx --no-pager',
      'curl -fsS http://127.0.0.1:8080/api/health',
    ],
    autoComplete: 'health',
  },
  {
    id: 'api-check',
    title: 'API 接口检查',
    detailDesc: '从开发机或任意局域网终端访问 /api/health，确认 Nginx 反代与后端响应正常。',
    inspectSteps: [
      '在 Windows 开发机执行 curl 健康检查',
      '确认响应 code 为 0 且 data.status 为 UP',
      '可选：验证 /api/todos/today 等业务接口',
    ],
    commands: [
      'curl.exe -s http://192.168.0.114/api/health',
      'curl.exe -s http://192.168.0.114/api/todos/today',
    ],
    autoComplete: 'health',
  },
  {
    id: 'frontend-access',
    title: '前端访问检查',
    detailDesc: '浏览器打开管理后台 Hash 地址，确认静态资源加载正常，勿使用 index_pc.html。',
    inspectSteps: [
      '访问 http://192.168.0.114/#/home',
      '确认页面无 404 / 白屏',
      '检查 Nginx 根目录为 /var/www/ai-manager（非双层 dist）',
    ],
    commands: [
      '# 浏览器打开',
      'http://192.168.0.114/#/home',
      'ssh kyle@192.168.0.114 "ls -la /var/www/ai-manager/index.html"',
    ],
  },
  {
    id: 'database',
    title: '数据库连接检查',
    detailDesc: '从应用节点测试连接数据节点 MySQL，确认账号与库名配置正确。',
    inspectSteps: [
      '在 114 上执行 mysql 客户端连接 118',
      '确认 ai_manager_admin 库可查询',
      '检查 backend.env 中数据源配置',
    ],
    commands: [
      'mysql -h 192.168.0.118 -u ai_manager -p123456 ai_manager_admin -e "SELECT 1;"',
      'grep -E "DATASOURCE|JDBC" /opt/ai-manager/backend/backend.env',
    ],
    autoComplete: 'mysql',
  },
  {
    id: 'redis-cache',
    title: 'Redis 缓存检查',
    detailDesc: '确认 Redis 可 ping 通，健康检查接口中 redis 字段为 UP。',
    inspectSteps: [
      'redis-cli 连接 118 执行 PING',
      '查看 /api/health 响应中的 redis 状态',
      '检查 backend.env 中 REDIS_HOST',
    ],
    commands: [
      'redis-cli -h 192.168.0.118 ping',
      'curl -fsS http://192.168.0.114/api/health',
      'grep REDIS /opt/ai-manager/backend/backend.env',
    ],
    autoComplete: 'redis',
  },
  {
    id: 'logs-alerts',
    title: '日志与告警检查',
    detailDesc: '查看 systemd 日志，确认无持续报错；必要时配置 journal 持久化与告警。',
    inspectSteps: [
      '查看最近 50 行后端日志',
      '确认无 MySQL / Redis 连接异常堆栈',
      '关注 OOM 或端口占用错误',
    ],
    commands: [
      'journalctl -u ai-manager-backend -n 50 --no-pager',
      'journalctl -u ai-manager-backend -f',
    ],
  },
]

export const deployCredentialGroups: DeployCredentialGroup[] = [
  {
    key: 'nodes',
    fields: [
      { label: '应用节点 IP', value: '192.168.0.114', copyable: true },
      { label: '数据节点 IP（优先有线）', value: '192.168.0.118', copyable: true },
      { label: '数据节点 IP（无线）', value: '192.168.0.116', copyable: true },
      { label: '开发机 IP', value: '192.168.0.119', copyable: true },
      { label: '管理后台地址', value: 'http://192.168.0.114/#/home', copyable: true },
    ],
  },
  {
    key: 'ssh',
    fields: [
      { label: '树莓派 SSH 用户', value: 'kyle', copyable: true },
      { label: '树莓派 SSH 密码', value: 'Asd123456', copyable: true },
    ],
  },
  {
    key: 'mysql',
    fields: [
      { label: 'root 密码', value: '123456', copyable: true },
      { label: '应用用户', value: 'ai_manager', copyable: true },
      { label: '应用密码', value: '123456', copyable: true },
      { label: '数据库名', value: 'ai_manager_admin', copyable: true },
    ],
  },
  {
    key: 'app',
    fields: [{ label: '后端运行用户', value: 'aimanager', copyable: true }],
  },
]

export const deployStepSections: DeployStepSection[] = [
  {
    id: 'data-node',
    title: '4.1 数据节点 — Docker MySQL + Redis',
    summary: '在数据节点树莓派上安装 Docker，启动 MySQL 与 Redis，并导入全量 SQL。',
    blocks: [
      {
        title: '安装 Docker 并启动',
        commands: [
          'cd ~/ai_manager',
          'bash deploy/scripts/setup-data-node-docker.sh',
          'nano /opt/ai-manager/data-node/.env',
          'bash deploy/scripts/setup-data-node-docker.sh',
        ],
        platform: 'linux',
      },
      {
        title: '导入全量 SQL',
        commands: [
          'cd ~/ai_manager',
          'sudo docker exec -i ai-manager-mysql mysql -uroot -p123456 < admin-backend/sql/deploy-all.sql',
        ],
        platform: 'linux',
      },
      {
        title: '从应用节点测试连通',
        commands: [
          'mysql -h 192.168.0.118 -u ai_manager -p123456 ai_manager_admin -e "SELECT 1;"',
          'redis-cli -h 192.168.0.118 ping',
        ],
        platform: 'linux',
      },
    ],
  },
  {
    id: 'app-env',
    title: '4.2 应用节点 — 运行环境',
    summary: '在 114 上安装 JDK 17、Nginx，创建 aimanager 用户与目录。',
    blocks: [
      {
        title: '安装依赖',
        commands: [
          'sudo apt update',
          'sudo apt install -y openjdk-17-jdk nginx',
          'java -version',
        ],
        platform: 'linux',
      },
      {
        title: '创建目录与用户',
        commands: [
          'sudo useradd -r -m -d /opt/ai-manager -s /bin/bash aimanager 2>/dev/null || true',
          'sudo mkdir -p /opt/ai-manager/backend /opt/ai-manager/backend/uploads',
          'sudo mkdir -p /var/www/ai-manager',
        ],
        platform: 'linux',
      },
    ],
  },
  {
    id: 'backend',
    title: '4.3 后端部署',
    summary: 'Windows 构建 JAR 上传至 114，配置 backend.env 并注册 systemd。',
    blocks: [
      {
        title: 'Windows 构建并上传',
        commands: [
          'cd G:\\projects\\ai_project\\ai_manager\\admin-backend',
          'mvn clean package -DskipTests -pl admin-server -am',
          'scp admin-backend\\admin-server\\target\\admin-server-1.0.0-SNAPSHOT.jar kyle@192.168.0.114:/tmp/admin-server.jar',
        ],
        platform: 'windows',
      },
      {
        title: '114 上启动服务',
        commands: [
          'sudo cp deploy/systemd/ai-manager-backend.service /etc/systemd/system/',
          'sudo systemctl daemon-reload',
          'sudo systemctl enable ai-manager-backend',
          'sudo systemctl start ai-manager-backend',
          'curl -s http://127.0.0.1:8080/api/health',
        ],
        platform: 'linux',
      },
    ],
  },
  {
    id: 'frontend',
    title: '4.4 前端部署',
    summary: '构建 dist 后 rsync 到 Nginx 目录，勿产生双层 dist 目录。',
    blocks: [
      {
        title: 'Windows 构建',
        commands: [
          'cd G:\\projects\\ai_project\\ai_manager\\admin-web',
          'npm install',
          'npm run build',
        ],
        platform: 'windows',
      },
      {
        title: '上传与同步',
        commands: [
          'ssh kyle@192.168.0.114 "mkdir -p /tmp/ai-manager-new"',
          'scp -r G:\\projects\\ai_project\\ai_manager\\admin-web\\dist\\* kyle@192.168.0.114:/tmp/ai-manager-new/',
          'sudo rsync -av --delete /tmp/ai-manager-new/ /var/www/ai-manager/',
        ],
        platform: 'windows',
      },
    ],
  },
  {
    id: 'checklist',
    title: '4.5 部署完成检查清单',
    summary: '确认健康检查、待办 API、前端页面与后端日志均正常。',
    blocks: [
      {
        title: '验证命令',
        commands: [
          'curl http://192.168.0.114/api/health',
          'curl http://192.168.0.114/api/todos/today',
          'journalctl -u ai-manager-backend -n 50 --no-pager',
        ],
        platform: 'linux',
      },
    ],
  },
]

export const deployDailyCommands: DeployCommandBlock[] = [
  {
    title: '仅更新后端',
    commands: [
      'cd G:\\projects\\ai_project\\ai_manager',
      'powershell -ExecutionPolicy Bypass -File deploy/scripts/deploy-backend.ps1',
    ],
    platform: 'windows',
  },
  {
    title: '仅更新前端',
    commands: [
      'cd G:\\projects\\ai_project\\ai_manager',
      'powershell -ExecutionPolicy Bypass -File deploy/scripts/deploy-frontend.ps1',
    ],
    platform: 'windows',
  },
  {
    title: '前后端都更新',
    commands: [
      'powershell -ExecutionPolicy Bypass -File deploy/scripts/deploy-backend.ps1',
      'powershell -ExecutionPolicy Bypass -File deploy/scripts/deploy-frontend.ps1',
      'powershell -ExecutionPolicy Bypass -File deploy/scripts/health-check.ps1',
    ],
    platform: 'windows',
  },
]

export const deployTroubleshooting: DeployTroubleRow[] = [
  { symptom: '502 Bad Gateway', action: '114 上 systemctl status ai-manager-backend，确认 8080 正常' },
  { symptom: 'API 失败、页面能开', action: '检查 Nginx /api/ 反代；curl http://127.0.0.1:8080/api/health' },
  { symptom: 'MySQL 连接失败', action: '114 上 mysql -h 192.168.0.118；118 上 docker compose ps' },
  { symptom: 'Redis DOWN', action: 'redis-cli -h 192.168.0.118 ping；检查 backend.env 中 REDIS_HOST' },
  {
    symptom: '前端白屏 Unexpected token',
    action: '重新 npm run build 并 rsync；使用 index.html + Hash 路由，勿用 index_pc.html',
  },
  { symptom: 'scp -r dist 后页面异常', action: '会变成双层目录；用 dist\\* 传到 /tmp/ai-manager-new/' },
  { symptom: '百度网盘授权失败', action: '回调须为 http://192.168.0.114/oauth/baidu/callback，Nginx 须反代 /oauth/' },
  {
    symptom: '服务器时间与本地不一致',
    action:
      '114 上执行：sudo timedatectl set-timezone Asia/Shanghai && sudo timedatectl set-ntp true && timedatectl status',
  },
]

export const deployImportantNotes = [
  '生产环境请使用 index.html + Hash 路由（/#/home），不要使用 index_pc.html。',
  '后端优先连接数据节点有线 IP：192.168.0.118。',
  '上传前端时勿 scp -r dist 到已有目录，应使用 rsync 同步到空目录。',
  '全量 deploy-all.sql 仅用于新环境；日常结构变更请执行增量 SQL 脚本。',
]
