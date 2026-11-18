# 雨绵绵知识交换平台架构图

![四层架构图](./output/architecture-layered.png)

这张图采用白底、彩色分层边框和业务域双栏布局，按 **接入层 → Web/API 层 → 业务服务域 → 基础设施层** 自上而下排列。可放大、编辑的矢量版本见 [architecture-layered.svg](./output/architecture-layered.svg)。

## 图中模块

- **接入层**：用户端 Vue 3 与管理端 Vue 3，均为浏览器应用。
- **Web/API 层**：Nginx 托管两个前端站点并代理 `/api/`；Spring Cloud Gateway 负责路由，Sa-Token 负责鉴权；AI 接口使用 SSE 流式响应。
- **业务服务域**：认证、用户、笔记、评论、关注、计数、通知、搜索、AI、文件共 10 个经网关暴露的服务；正文服务与 ID 服务为内部服务。公共框架是共享代码依赖。
- **基础设施层**：MySQL、Redis、Cassandra、RocketMQ、Elasticsearch、Qdrant、COS/MinIO、Nacos、Canal，以及 DashScope/Tavily 外部能力。

**部署说明**：当前后端 `docker-compose.yml` 编排网关、10 个对外服务、正文服务和 ID 服务，共 13 个容器。Nginx 与基础设施组件不在该 Compose 内。对象存储当前配置为 COS，MinIO 是代码支持的另一种策略。

**依据**：后端根 `pom.xml`、`docker-compose.yml`、网关路由与各服务配置；两个前端的路由与 Vite 配置；管理端 `deploy/nginx-default.conf`。

[PNG 图片](./output/architecture-layered.png) · [SVG 矢量图](./output/architecture-layered.svg) · [绘图脚本](./output/render_architecture.py)
