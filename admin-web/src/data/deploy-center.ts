export type DeployCenterTab = 'overview' | 'steps' | 'database' | 'versions' | 'logs'

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
  key: 'nodes' | 'ssh' | 'mysql' | 'pgvector' | 'app'
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
  key: 'mysql' | 'redis' | 'pgvector'
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
  {
    key: 'pgvector',
    title: 'PostgreSQL + pgvector',
    fields: [
      { label: '主机（有线优先）', value: '192.168.0.118' },
      { label: '端口', value: '5432' },
      { label: '数据库名', value: 'ai_manager_rag' },
      { label: '应用用户', value: 'ai_manager' },
      { label: '应用密码', value: '123456' },
      { label: 'pgvector 版本', value: '0.7.4' },
      { label: '连接命令', value: 'psql -h 192.168.0.118 -U ai_manager -d ai_manager_rag -W' },
      { label: '连接字符串（JDBC）', value: 'jdbc:postgresql://192.168.0.118:5432/ai_manager_rag' },
    ],
  },
]

export interface DeployStoragePathSection {
  key: 'local-server' | 'local-dev' | 'baidu-pan-prod' | 'baidu-pan-dev'
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
    key: 'baidu-pan-prod',
    title: '百度网盘 · 生产（114）',
    fields: [
      { label: '应用根目录', value: '/apps/ai_blog' },
      { label: '笔记正文', value: '/apps/ai_blog/notes' },
      { label: '回收站', value: '/apps/ai_blog/trash' },
      { label: '笔记图片', value: '/apps/ai_blog/images' },
      { label: '电商图片', value: '/apps/ai_blog/ecommerce-images' },
      { label: '销售订单导入', value: '/apps/ai_blog/imports/sales-orders' },
    ],
  },
  {
    key: 'baidu-pan-dev',
    title: '百度网盘 · 开发',
    fields: [
      { label: '应用根目录', value: '/apps/ai_blog/dev' },
      { label: '笔记正文', value: '/apps/ai_blog/dev/notes' },
      { label: '回收站', value: '/apps/ai_blog/dev/trash' },
      { label: '笔记图片', value: '/apps/ai_blog/dev/images' },
      { label: '电商图片', value: '/apps/ai_blog/dev/ecommerce-images' },
      { label: '销售订单导入', value: '/apps/ai_blog/dev/imports/sales-orders' },
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
    lines: ['118', 'MySQL · Redis · pgvector'],
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
      '数据节点是存储与处理的基础组件，部署前需确认 Docker、MySQL、Redis 与 PostgreSQL 容器均已启动并可从应用节点访问。',
    inspectSteps: [
      'SSH 登录数据节点 192.168.0.118',
      '执行 docker compose ps，确认 MySQL / Redis / PostgreSQL 为 running',
      '检查 pgvector 扩展是否已安装：psql -d ai_manager_rag -c "EXTENSION vector"',
      '检查数据目录磁盘空间是否充足（df -h）',
      '确认 3306 / 6379 / 5432 端口在局域网可访问',
    ],
    commands: [
      'ssh kyle@192.168.0.118',
      'cd /opt/ai-manager/data-node && docker compose ps',
      'psql -h 127.0.0.1 -U ai_manager -d ai_manager_rag -c "SELECT extname FROM pg_extension;"',
      'df -h /opt/ai-manager',
      'ss -tlnp | grep -E "3306|6379|5432"',
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
    detailDesc: '从应用节点测试连接数据节点 MySQL 与 PostgreSQL，确认账号与库名配置正确。',
    inspectSteps: [
      '在 114 上执行 mysql 客户端连接 118',
      '确认 ai_manager_admin 库可查询',
      'psql 连接 pgvector，确认 ai_manager_rag 库可查询',
      '检查 backend.env 中数据源配置',
    ],
    commands: [
      'mysql -h 192.168.0.118 -u ai_manager -p123456 ai_manager_admin -e "SELECT 1;"',
      'psql -h 192.168.0.118 -U ai_manager -d ai_manager_rag -c "SELECT 1;"',
      'grep -E "MYSQL_HOST|PGVECTOR_HOST" /opt/ai-manager/backend/backend.env',
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
    id: 'pgvector-check',
    title: 'pgvector 向量库检查',
    detailDesc: '确认 PostgreSQL 中 pgvector 扩展已启用、rag_vectors 表已创建、可正常读写向量数据。',
    inspectSteps: [
      '确认 pgvector 扩展可用：psql -c "SELECT * FROM pg_available_extensions WHERE name = \'vector\';"',
      '确认向量表存在：psql -c "\d+ rag_vectors"',
      '测试写入一条向量并查询',
      '确认 hnsw 索引已创建',
    ],
    commands: [
      'psql -h 192.168.0.118 -U ai_manager -d ai_manager_rag -c "SELECT extname, extversion FROM pg_extension;"',
      'psql -h 192.168.0.118 -U ai_manager -d ai_manager_rag -c "\d+ rag_vectors"',
      'psql -h 192.168.0.118 -U ai_manager -d ai_manager_rag -c "SELECT count(*) FROM rag_vectors;"',
      'psql -h 192.168.0.118 -U ai_manager -d ai_manager_rag -c "SELECT idxname FROM pg_indexes WHERE tablename = \'rag_vectors\';"',
    ],
  },
  {
    id: 'autostart-check',
    title: '开机自启检查',
    detailDesc: '确认后端 systemd 服务与 Nginx 均已设置为开机自启（enabled），树莓派重启后服务可自动恢复。',
    inspectSteps: [
      '检查 ai-manager-backend 服务是否 enabled',
      '检查 nginx 服务是否 enabled',
      '确认两个服务均为 active (running)',
    ],
    commands: [
      'sudo systemctl is-enabled ai-manager-backend nginx',
      'sudo systemctl is-active ai-manager-backend nginx',
    ],
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
    key: 'pgvector',
    fields: [
      { label: 'PostgreSQL 主机', value: '192.168.0.118', copyable: true },
      { label: '端口', value: '5432', copyable: true },
      { label: '数据库名', value: 'ai_manager_rag', copyable: true },
      { label: '应用用户', value: 'ai_manager', copyable: true },
      { label: '应用密码', value: '123456', copyable: true },
      { label: 'JDBC 连接串', value: 'jdbc:postgresql://192.168.0.118:5432/ai_manager_rag', copyable: true },
      { label: 'psql 连接命令', value: 'psql -h 192.168.0.118 -U ai_manager -d ai_manager_rag', copyable: true },
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
    title: '数据节点 — Docker MySQL + Redis + PostgreSQL(pgvector)',
    summary:
      '在数据节点树莓派（116 或 118，同一台机器双网卡）上安装 Docker，启动 MySQL、Redis 与 PostgreSQL(pgvector)，并导入全量 SQL。后端优先连接有线 IP 192.168.0.118。',
    blocks: [
      {
        title: '（可选）卸载原生 MariaDB / Redis',
        commands: [
          'cd ~/ai_manager',
          'sudo bash deploy/scripts/uninstall-native-db-on-116.sh',
        ],
        platform: 'linux',
      },
      {
        title: '安装 Docker 并启动',
        commands: [
          'cd ~/ai_manager',
          'bash deploy/scripts/setup-data-node-docker.sh',
          'nano /opt/ai-manager/data-node/.env',
          '# .env 示例：MYSQL_ROOT_PASSWORD=123456',
          '#          MYSQL_DATABASE=ai_manager_admin',
          '#          MYSQL_USER=ai_manager',
          '#          MYSQL_PASSWORD=123456',
          'bash deploy/scripts/setup-data-node-docker.sh',
        ],
        platform: 'linux',
      },
      {
        title: '验证 Docker 服务',
        commands: [
          'cd /opt/ai-manager/data-node',
          'docker compose ps',
          'docker exec ai-manager-mysql mysql -u ai_manager -p123456 ai_manager_admin -e "SELECT 1;"',
          'docker exec ai-manager-redis redis-cli ping',
        ],
        platform: 'linux',
      },
      {
        title: '导入全量 SQL（新环境一次）',
        commands: [
          'cd ~/ai_manager',
          'sudo docker exec -i ai-manager-mysql mysql -uroot -p123456 --default-character-set=utf8mb4 < admin-backend/sql/deploy-all.sql',
          '# 或（自动读取 .env 密码，已带 utf8mb4 客户端编码）：',
          'bash deploy/scripts/import-sql-to-docker-mysql.sh admin-backend/sql/deploy-all.sql',
          '# 已有库修复乱码 COMMENT：',
          'mysql -h 192.168.0.118 -u ai_manager -p123456 --default-character-set=utf8mb4 ai_manager_admin < admin-backend/sql/all_table_comment_fix.sql',
        ],
        platform: 'linux',
      },
      {
        title: '安装 PostgreSQL + pgvector',
        commands: [
          '# 安装 PostgreSQL',
          'sudo apt install -y postgresql postgresql-contrib',
          '# 安装 pgvector（从源码编译）',
          'cd /tmp && git clone --branch v0.7.4 https://github.com/pgvector/pgvector.git',
          'cd /tmp/pgvector && make && sudo make install',
          '# 创建数据库和用户',
          "sudo -u postgres psql -c \"CREATE USER ai_manager WITH PASSWORD '123456';\"",
          'sudo -u postgres psql -c "CREATE DATABASE ai_manager_rag OWNER ai_manager;"',
          'sudo -u postgres psql -d ai_manager_rag -c "CREATE EXTENSION vector;"',
          '# 创建向量表（复制整个 SQL 块到 psql 执行）',
          'sudo -u postgres psql -d ai_manager_rag <<SQL',
          'CREATE TABLE IF NOT EXISTS rag_vectors (',
          '  id BIGSERIAL PRIMARY KEY, chunk_id BIGINT NOT NULL, doc_id BIGINT NOT NULL,',
          '  embedding VECTOR(1536) NOT NULL, content TEXT NOT NULL',
          ');',
          'DROP INDEX IF EXISTS idx_rag_vectors_embedding;',
          'CREATE INDEX idx_rag_vectors_embedding ON rag_vectors USING hnsw (embedding vector_cosine_ops) WITH (m = 16, ef_construction = 64);',
          'SQL',
          '# 开放远程连接',
          'echo "host    ai_manager_rag    ai_manager    192.168.0.0/24    md5" | sudo tee -a /etc/postgresql/*/main/pg_hba.conf',
          "sudo sed -i \"s/#listen_addresses = .*/listen_addresses = '*'/\" /etc/postgresql/*/main/postgresql.conf",
          'sudo systemctl restart postgresql',
        ],
        platform: 'linux',
      },
      {
        title: '从应用节点 114 测试连通',
        commands: [
          'mysql -h 192.168.0.118 -u ai_manager -p123456 ai_manager_admin -e "SELECT 1;"',
          'redis-cli -h 192.168.0.118 ping',
          'psql -h 192.168.0.118 -U ai_manager -d ai_manager_rag -c "SELECT 1;"',
          '# 若 118 不通可试无线 192.168.0.116',
        ],
        platform: 'linux',
      },
    ],
  },
  {
    id: 'app-env',
    title: '应用节点 — 运行环境',
    summary:
      '在 114 上安装 JDK 17、Nginx、构建工具与数据库客户端，克隆仓库，创建 aimanager 用户与目录，并配置本机一键部署所需 sudo 权限。',
    blocks: [
      {
        title: '安装系统依赖',
        commands: [
          'sudo apt update',
          '# ARM 架构树莓派包名：default-jdk（非 openjdk-17-jdk）、mariadb-client（非 mysql-client）',
          '# psql 客户端用于验证 pgvector 连接',
          'sudo apt install -y default-jdk nginx maven nodejs npm git mariadb-client postgresql-client redis-tools rsync',
          'java -version',
          'node -v && npm -v',
        ],
        platform: 'linux',
      },
      {
        title: '同步时区（建议）',
        commands: [
          'sudo timedatectl set-timezone Asia/Shanghai',
          'sudo timedatectl set-ntp true',
          'timedatectl status',
        ],
        platform: 'linux',
      },
      {
        title: '克隆仓库',
        commands: [
          'git clone <你的仓库地址> ~/ai_manager',
          'cd ~/ai_manager && git pull --ff-only',
        ],
        platform: 'linux',
      },
      {
        title: '创建运行用户与目录',
        commands: [
          'sudo useradd -r -m -d /opt/ai-manager -s /bin/bash aimanager 2>/dev/null || true',
          'sudo mkdir -p /opt/ai-manager/backend /opt/ai-manager/backend/uploads',
          'sudo mkdir -p /var/www/ai-manager',
          'sudo chown -R aimanager:aimanager /opt/ai-manager',
          'sudo chown -R www-data:www-data /var/www/ai-manager',
        ],
        platform: 'linux',
      },
      {
        title: '确认能连数据节点',
        commands: [
          'mysql -h 192.168.0.118 -u ai_manager -p123456 ai_manager_admin -e "SELECT 1;"',
          'redis-cli -h 192.168.0.118 ping',
          'psql -h 192.168.0.118 -U ai_manager -d ai_manager_rag -c "SELECT 1;"',
        ],
        platform: 'linux',
      },
      {
        title: '本机一键部署 sudo 权限（Web 部署必需）',
        commands: [
          'sudo cp ~/ai_manager/deploy/sudoers/ai-manager-deploy.example /etc/sudoers.d/ai-manager-deploy',
          'sudo chmod 440 /etc/sudoers.d/ai-manager-deploy',
          'sudo visudo -c',
          'sudo -u aimanager sudo -n -u kyle test -d ~/ai_manager/deploy/scripts && echo OK',
        ],
        platform: 'linux',
      },
    ],
  },
  {
    id: 'backend',
    title: '后端部署',
    summary:
      '方式 A：Windows 构建 JAR 上传至 114。方式 B：114 Web 界面「一键部署后端」（需已完成上一节环境准备）。均需配置 backend.env 与 systemd。',
    blocks: [
      {
        title: '方式 A — Windows 构建并上传',
        commands: [
          'cd G:\\projects\\ai_project\\ai_manager',
          'mvn clean package -DskipTests -pl admin-server -am -f admin-backend\\pom.xml',
          'scp admin-backend\\admin-server\\target\\admin-server-1.0.0-SNAPSHOT.jar kyle@192.168.0.114:/tmp/admin-server.jar',
          'scp deploy\\env\\backend.env.example kyle@192.168.0.114:/tmp/backend.env',
        ],
        platform: 'windows',
      },
      {
        title: '方式 A — 114 上安装 JAR 与 systemd',
        commands: [
          'sudo mv /tmp/admin-server.jar /opt/ai-manager/backend/admin-server.jar',
          'sudo mv /tmp/backend.env /opt/ai-manager/backend/backend.env',
          'sudo chown aimanager:aimanager /opt/ai-manager/backend/admin-server.jar /opt/ai-manager/backend/backend.env',
          'sudo chmod 600 /opt/ai-manager/backend/backend.env',
          'grep -E "MYSQL_HOST|REDIS_HOST|PGVECTOR_HOST|SPRING_PROFILES" /opt/ai-manager/backend/backend.env',
          'cd ~/ai_manager',
          'sudo cp deploy/systemd/ai-manager-backend.service /etc/systemd/system/',
          'sudo systemctl daemon-reload',
          '# enable 设置开机自启，--now 同时立即启动',
          'sudo systemctl enable --now ai-manager-backend',
          '# 验证服务状态与健康检查',
          'sudo systemctl status ai-manager-backend --no-pager',
          'curl -s http://127.0.0.1:8080/api/health',
        ],
        platform: 'linux',
      },
      {
        title: '方式 B — 114 Web 一键部署后端',
        commands: [
          '# 浏览器打开 http://192.168.0.114/#/deploy → 部署步骤',
          '# 点击「一键部署后端」',
          '# 脚本：deploy/scripts/deploy-on-pi-backend.sh（git pull + mvn + 安装 JAR + 延迟重启）',
          '# 首次需先按上一节安装 Maven、sudoers，并部署含 runner 的新版 JAR',
        ],
        platform: 'linux',
      },
      {
        title: '方式 C — Windows Web 一键部署后端',
        commands: [
          '# 开发机启动后端（dev profile，runner.mode=remote）',
          '# application-dev.yml 配置 ai-manager.deploy.pi.password',
          '# 浏览器本地 http://127.0.0.1:5173 → 部署中心 → 一键部署后端',
          '# 或执行：powershell -File deploy/scripts/deploy-backend.ps1',
        ],
        platform: 'windows',
      },
    ],
  },
  {
    id: 'frontend',
    title: '前端部署与 Nginx',
    summary:
      '构建 dist 后同步到 /var/www/ai-manager，配置 Nginx 反代 /api、/oauth、/uploads 及部署 SSE 长连接。勿产生双层 dist 目录。',
    blocks: [
      {
        title: '方式 A — Windows 构建并上传',
        commands: [
          'cd G:\\projects\\ai_project\\ai_manager\\admin-web',
          'npm install',
          'npm run build',
          'ssh kyle@192.168.0.114 "mkdir -p /tmp/ai-manager-new"',
          'scp -r G:\\projects\\ai_project\\ai_manager\\admin-web\\dist\\* kyle@192.168.0.114:/tmp/ai-manager-new/',
          'ssh kyle@192.168.0.114 "sudo rsync -av --delete /tmp/ai-manager-new/ /var/www/ai-manager/ && sudo chown -R www-data:www-data /var/www/ai-manager"',
        ],
        platform: 'windows',
      },
      {
        title: '配置 Nginx（首次必做，更新配置后 reload）',
        commands: [
          'cd ~/ai_manager',
          'sudo cp deploy/nginx/ai-manager.conf /etc/nginx/sites-available/ai-manager',
          'sudo ln -sf /etc/nginx/sites-available/ai-manager /etc/nginx/sites-enabled/',
          'sudo rm -f /etc/nginx/sites-enabled/default',
          'sudo nginx -t',
          'sudo systemctl reload nginx',
        ],
        platform: 'linux',
      },
      {
        title: '方式 B — 114 Web 一键部署前端',
        commands: [
          '# 浏览器 http://192.168.0.114/#/deploy → 一键部署前端',
          '# 脚本：deploy/scripts/deploy-on-pi-frontend.sh（git pull + npm build + rsync）',
          '# Pi 上构建较慢（5～15 分钟），日志中断后可能仍在后台执行',
        ],
        platform: 'linux',
      },
      {
        title: '方式 C — Windows 脚本或 Web 一键',
        commands: [
          'cd G:\\projects\\ai_project\\ai_manager',
          'powershell -ExecutionPolicy Bypass -File deploy/scripts/deploy-frontend.ps1',
          '# 或在部署中心点击「一键部署前端」（Java SSH 密码上传）',
        ],
        platform: 'windows',
      },
    ],
  },
  {
    id: 'autostart',
    title: '开机自启动配置',
    summary:
      '配置 systemd 与 Nginx 开机自启，确保树莓派重启后后端 Java 服务、Nginx 反向代理均自动恢复，无需手动干预。',
    blocks: [
      {
        title: '确认 systemd 后端服务已启用开机自启',
        commands: [
          '# 查看是否已启用（输出 enabled 表示已开启）',
          'sudo systemctl is-enabled ai-manager-backend',
          '# 若输出 disabled，执行以下命令启用：',
          'sudo systemctl enable ai-manager-backend',
          '# 确认服务当前正在运行：',
          'sudo systemctl status ai-manager-backend --no-pager',
        ],
        platform: 'linux',
      },
      {
        title: '确认 Nginx 已启用开机自启',
        commands: [
          'sudo systemctl is-enabled nginx',
          '# 若未启用：',
          'sudo systemctl enable nginx',
          'sudo systemctl status nginx --no-pager',
        ],
        platform: 'linux',
      },
      {
        title: '验证开机自启是否生效（重启测试）',
        commands: [
          '# 重启树莓派',
          'sudo reboot',
          '# 重启完成后 SSH 重新连接，验证所有服务：',
          'sudo systemctl is-active ai-manager-backend nginx',
          'curl -s http://127.0.0.1/api/health',
          '# 响应中 status / mysql / redis 均为 UP 即表示自启动成功',
        ],
        platform: 'linux',
      },
      {
        title: '常用服务管理命令',
        commands: [
          '# 启动 / 停止 / 重启后端',
          'sudo systemctl start ai-manager-backend',
          'sudo systemctl stop ai-manager-backend',
          'sudo systemctl restart ai-manager-backend',
          '# 禁用 / 重新启用开机自启',
          'sudo systemctl disable ai-manager-backend',
          'sudo systemctl enable ai-manager-backend',
          '# 实时查看后端日志',
          'sudo journalctl -u ai-manager-backend -f',
        ],
        platform: 'linux',
      },
    ],
  },
  {
    id: 'checklist',
    title: '部署完成检查清单',
    summary:
      '确认健康检查、待办 API、前端页面与后端日志均正常。部署中心「部署步骤」页可自动按 1～8 项顺序检查。',
    blocks: [
      {
        title: '命令行验证',
        commands: [
          'curl http://192.168.0.114/api/health',
          'curl http://192.168.0.114/api/todos/today',
          'journalctl -u ai-manager-backend -n 50 --no-pager',
          '# 浏览器：http://192.168.0.114/#/home（Ctrl+F5 强制刷新）',
        ],
        platform: 'linux',
      },
    ],
  },
]

export const deployTroubleshooting: DeployTroubleRow[] = [
  {
    symptom: '502 Bad Gateway',
    action:
      '后端未运行。检查：1) systemctl status ai-manager-backend 是否 active；2) /opt/ai-manager/backend/backend.env 是否存在且配置正确；3) systemd 服务文件是否已安装到 /etc/systemd/system/',
  },
  { symptom: 'API 失败、页面能开', action: '检查 Nginx /api/ 反代；curl http://127.0.0.1:8080/api/health' },
  { symptom: 'MySQL 连接失败', action: '114 上 mysql -h 192.168.0.118；118 上 docker compose ps' },
  { symptom: 'Redis DOWN', action: 'redis-cli -h 192.168.0.118 ping；检查 backend.env 中 REDIS_HOST' },
  { symptom: 'pgvector 连接失败', action: '114 上 psql -h 192.168.0.118 -U ai_manager -d ai_manager_rag；检查 PostgreSQL 是否监听 5432 端口及 pg_hba.conf 权限' },
  { symptom: 'RAG 搜索返回空', action: '检查 rag_vectors 表是否有数据；确认 Embedding API Key 已配置；查看后端日志中的嵌入调用错误' },
  { symptom: '文档上传后状态一直是 processing', action: '检查后端日志是否有解析/嵌入异常；确认该模型提供商的 Embedding API 可正常访问；检查 /opt/ai-manager/rag-docs 目录权限' },
  {
    symptom: '前端白屏 Unexpected token',
    action: '重新 npm run build 并 rsync；使用 index.html + Hash 路由，勿用 index_pc.html',
  },
  { symptom: 'scp -r dist 后页面异常', action: '会变成双层目录；用 dist\\* 传到 /tmp/ai-manager-new/' },
  { symptom: '百度网盘授权失败', action: '回调须为 http://192.168.0.114/oauth/baidu/callback，Nginx 须反代 /oauth/' },
  {
    symptom: '检查清单第 7 项 journalctl 失败',
    action:
      '114 上：sudo usermod -aG systemd-journal aimanager && sudo systemctl restart ai-manager-backend；或更新 sudoers 加入 aimanager 的 journalctl 权限后重启后端',
  },
]

export const deployImportantNotes = [
  '生产环境请使用 index.html + Hash 路由（/#/home），不要使用 index_pc.html。',
  '后端连接数据节点优先使用有线 IP：192.168.0.118（backend.env 中 MYSQL_HOST / REDIS_HOST / PGVECTOR_HOST）。',
  '向量数据库使用 PostgreSQL + pgvector，需先安装 pgvector 扩展再建表，见数据节点部署步骤。',
  'RAG 知识库上传目录默认为 /opt/ai-manager/rag-docs，需确保 aimanager 用户有写入权限。',
  '首次部署必须配置 Nginx（deploy/nginx/ai-manager.conf）并安装 backend.env、systemd 服务。',
  'ARM 架构树莓派 apt 包名：用 default-jdk 替代 openjdk-17-jdk，mariadb-client 替代 mysql-client。',
  'MariaDB 客户端连接需加 --skip-ssl 参数（非 MySQL 的 --ssl-mode=DISABLED）。',
  '部署完成后务必执行 sudo systemctl enable ai-manager-backend nginx 设置开机自启。',
  '上传前端时勿 scp -r dist 到已有目录，应使用 dist\\* 传到 /tmp/ai-manager-new/ 再 rsync。',
  '114 本机一键部署需安装 deploy/sudoers/ai-manager-deploy.example，且仓库 clone 到 ~/ai_manager。',
  '全量 deploy-all.sql 仅用于新环境；日常结构变更请执行 admin-backend/sql/ 下增量脚本。',
  '更新 Nginx 或 systemd 后执行：sudo nginx -t && sudo systemctl reload nginx / daemon-reload。',
]
