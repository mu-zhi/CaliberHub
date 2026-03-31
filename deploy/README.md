# Docker 部署说明（前后端合包）

本目录提供“`Spring Boot（后端框架）` + `Vite（前端构建）` 合包”的容器化部署基线。

## 1. 准备环境变量

```bash
cd /Users/rlc/LingChao_Ren/1.2、数据直通车/deploy
cp .env.docker.example .env.docker
```

按实际环境修改 `.env.docker` 中的密码和密钥，至少要改：

- `MYSQL_ROOT_PASSWORD`
- `CALIBER_JWT_SECRET`
- 六个角色密码（`CALIBER_*_PASSWORD`）

## 2. 启动（默认：App + MySQL）

```bash
cd /Users/rlc/LingChao_Ren/1.2、数据直通车/deploy
docker compose up -d --build
```

访问：

- 应用首页：`http://127.0.0.1:8082/`
- 接口文档：`http://127.0.0.1:8082/v3/api-docs`
- 健康检查：`http://127.0.0.1:8082/actuator/health`

## 3. 可选组件

启用 Redis：

```bash
docker compose --profile redis up -d
```

启用 Neo4j：

```bash
docker compose --profile neo4j up -d
```

同时启用 Redis + Neo4j：

```bash
docker compose --profile redis --profile neo4j up -d
```

## 4. 常用运维命令

查看容器状态：

```bash
docker compose ps
```

查看应用日志：

```bash
docker compose logs -f app
```

停止并保留数据卷：

```bash
docker compose down
```

停止并删除数据卷（会清空 MySQL / Neo4j 数据）：

```bash
docker compose down -v
```

## 5. GitHub Actions 推送 GHCR 镜像

仓库已新增工作流：

- [build-and-push-ghcr.yml](/Users/rlc/LingChao_Ren/1.2、数据直通车/.github/workflows/build-and-push-ghcr.yml)

触发方式：

- 推送到 `main` 分支自动触发
- 在 `Actions` 页面手动触发（`workflow_dispatch`）

镜像命名：

- `ghcr.io/<owner>/<repo>:latest`
- `ghcr.io/<owner>/<repo>:sha-<commit>`

首次启用前请确认：

1. 仓库 `Settings -> Actions -> General -> Workflow permissions` 允许 `Read and write permissions`。
2. 组织策略允许 `GitHub Actions` 写 `Packages`。
3. 如仓库为私有且部署机需要拉取镜像，部署机需使用具有 `read:packages` 权限的 `PAT（个人访问令牌）` 登录 `GHCR`。
