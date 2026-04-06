# 顶部最小登录入口测试报告

- 需求：在前端顶部全局工具区提供常驻的角色密码登录入口，补齐最小会话鉴权闭环，避免数据地图等受保护接口依赖隐含登录前提。
- 范围：未登录态密码输入与登录提交、已登录态摘要展示、退出登录、切换角色时立即清空旧会话。

## 执行命令

- `cd frontend && npm test -- src/App.access-control.test.jsx`
- `cd frontend && npm run build`
- `curl -s http://localhost:8082/actuator/health`
- `curl -s -H 'Content-Type: application/json' -d '{"username":"admin","password":"admin123"}' http://localhost:8082/api/system/auth/token | jq '{username, roles}'`

## 结果

- 通过：`src/App.access-control.test.jsx` 共 `5` 项测试全部通过，覆盖顶栏登录提交、已登录摘要、退出动作入口与切角色清会话。
- 通过：前端生产构建成功。
- 通过：后端健康检查返回 `{"status":"UP"}`。
- 通过：认证接口可成功为 `admin` 角色签发令牌，并返回 `ADMIN / SUPPORT` 角色集。

## 备注

- 本轮只收口“顶部最小登录闭环”，未扩展全局 `401（未认证）` 统一提示策略。
- 前端构建仍存在既有的大包体告警，但不构成本次登录入口功能回归。
