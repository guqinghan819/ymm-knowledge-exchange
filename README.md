# 雨绵绵知识交流平台

本仓库包含后端、用户端和管理端的独立公开副本，使用新的 Git 历史。

## 目录

- `backend/`：Java / Spring Boot 微服务、SQL 表结构及基础业务字典。
- `user/`：Vue 3 用户端。
- `admin/`：Vue 3 管理端。

## 配置

数据库、Redis、Nacos、RocketMQ、对象存储、AI 和短信服务均需使用自己的实例。
`backend/.env.example` 列出环境变量，密码和密钥留空。

本机通过 IDE 或终端设置相应环境变量。Spring Boot 不会自动加载 `.env` 文件。
分布式 ID 服务使用 `LEAF_JDBC_*` 和 `LEAF_SNOWFLAKE_ZK_ADDRESS` 环境变量。

使用 Docker Compose 时，将 `backend/.env.example` 复制为 `backend/.env` 并填写。
Compose 已将该文件注入所有应用容器；容器访问外部依赖时，需把 `localhost` 改为可达的服务名或主机地址。
现有 Compose 只定义应用服务，数据库等基础设施需要单独部署。
Elasticsearch 热更新词典可通过 `ES_DICTIONARY_PATH` 指定。

## 启动

后端需要 JDK 17 和 Maven。先配置基础设施、按需执行 `backend/sql/` 中的表结构及迁移脚本，再构建：

```sh
cd backend
mvn clean package -DskipTests
```

然后运行所需模块的 Spring Boot 主类，或在配置完成后执行 `docker compose up --build`。
网关默认端口为 `8081`。Nacos 默认组为 `DEFAULT_GROUP`，命名空间为空；如使用远程配置，请自行提供经过配置的 Nacos 数据。

两个前端分别安装和运行（Node.js 20.19+ 或 22.12+）：

```sh
cd user
npm ci
npm run dev
```

管理端在 `admin/` 中执行相同命令。用户端默认端口 `5173`，管理端默认端口 `5174`，管理端路径 `/admin/`。开发代理默认指向 `http://localhost:8081`。

## 公开副本说明

- 不包含原仓库提交历史、本地依赖、构建结果、运行日志和编辑器缓存。
- 已将配置中的凭据、服务地址、对象存储标识和个人联系方式替换为环境变量或示例值。
- 展示页中的实际账号截图已替换为示意图。
- 原始 PPT 模板及其私有存储初始化记录未包含在此副本。使用 PPT 生成功能时，需要自行提供模板，并在 `ai_ppt_template` 中配置模板信息。
- 保留权限、角色、频道和话题等基础业务种子数据；不包含运行数据库备份。
- 此次处理用于公开发布源码，不代表已完成生产部署或完整安全审计。

各子项目保留原有 Apache-2.0 许可证。
