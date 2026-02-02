# CaliberHub 快速上手与规范补充

## 运行
- 后端：`cd backend && ./gradlew bootRun`（默认使用本地 `caliberhub.db`）。如需示例数据：`./gradlew bootRun --args='--spring.profiles.active=seed'`。
- 前端：`cd frontend && npm ci && npm run dev`。切换本地 Mock：`VITE_USE_MOCK=true npm run dev`。
- 配置示例：`backend/src/main/resources/application-example.yml`。

## 环境与配置
- Logging 默认 INFO，可用环境变量覆盖：`LOG_LEVEL_COM_CALIBERHUB`、`LOG_LEVEL_HIBERNATE_SQL`。
- 种子数据仅在 `seed` Profile 下加载，避免生产环境污染。
- 根级 `.gitignore` 已排除构建/缓存/DB/编辑器产物。

## 持续集成
- GitHub Actions 工作流：`.github/workflows/ci.yml`，执行后端测试与前端 lint/build。

## 导出
- 发布或手动导出接口 `/api/scenes/{code}/export/{doc|chunks}`，结果同时写入表 `scene_version_export` 便于重复下载。

## 依赖锁定
- Gradle 开启 dependency locking（见 `backend/build.gradle`）。如需生成/刷新锁文件：`cd backend && ./gradlew dependencies --write-locks`。
