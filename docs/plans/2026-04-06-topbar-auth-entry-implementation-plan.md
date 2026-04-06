# 顶部最小登录入口收口 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为前端顶部全局工具区补一个最小可用的角色密码登录入口，让受保护接口可以拿到当前会话 `JWT（JSON Web Token，身份令牌）`，从而恢复数据地图等页面的正常加载。

**Architecture:** 继续复用现有 `frontend/src/store/authStore.js` 中的 `loginByRole / logout / setRole` 与 `frontend/src/api/client.js` 的 `sessionStorage` 令牌注入逻辑；实现只收口到 `App.jsx` 顶部壳层和对应渲染测试，不新增独立登录页，也不扩全局 `401` 提示策略。

**Tech Stack:** React、Zustand、Vitest、Testing Library、Vite

---

## 设计输入

- `docs/architecture/frontend-workbench-design.md` §2.2 全局工具区
- `docs/architecture/features/iteration-02-runtime-and-governance/10-数据地图浏览与覆盖追踪.md`
- `docs/engineering/current-delivery-status.md`

## File Map

- Modify: `frontend/src/App.jsx`
- Modify: `frontend/src/App.access-control.test.jsx`
- Modify: `frontend/src/styles.css`
- Modify: `docs/engineering/current-delivery-status.md`
- Create: `docs/testing/features/engineering/2026-04-06-topbar-auth-entry-test-report.md`

## 前置依赖

- `frontend/src/store/authStore.js` 已提供 `role / roles / token / tokenExpireAt / username / setRole / loginByRole / logout`
- `frontend/src/api/client.js` 已能自动从 `sessionStorage` 读取 `dd_auth_token`
- 顶部壳层当前已有角色选择器与全局工具区样式

### Task 1: 先补顶部登录入口失败测试

**Files:**

- Modify: `frontend/src/App.access-control.test.jsx`

- [ ] **Step 1: 扩展 auth store mock，暴露登录态与动作桩**

在 `App.access-control.test.jsx` 顶部把 mock 状态扩展为：

```jsx
let currentRole = "support";
let currentRoles = ["SUPPORT"];
let currentToken = "";
let currentTokenExpireAt = "";
let currentUsername = "";
const setRoleMock = vi.fn();
const loginByRoleMock = vi.fn();
const logoutMock = vi.fn();
```

并让 `useAuthStore` 返回这些字段：

```jsx
vi.mock("./store/authStore", () => ({
  useAuthStore: (selector) => selector({
    role: currentRole,
    roles: currentRoles,
    token: currentToken,
    tokenExpireAt: currentTokenExpireAt,
    username: currentUsername,
    setRole: setRoleMock,
    loginByRole: loginByRoleMock,
    logout: logoutMock,
  }),
}));
```

- [ ] **Step 2: 新增“未登录态可提交当前角色密码”的交互测试**

在同文件增加：

```jsx
it("submits current role password from topbar auth entry", async () => {
  loginByRoleMock.mockResolvedValue(true);
  renderAppAt("/map");

  const passwordInput = await screen.findByLabelText("当前角色密码");
  await userEvent.type(passwordInput, "support123");
  await userEvent.click(screen.getByRole("button", { name: "登录" }));

  expect(loginByRoleMock).toHaveBeenCalledWith("support", "support123");
});
```

- [ ] **Step 3: 新增“已登录态显示用户名并可退出”的渲染测试**

追加：

```jsx
it("shows logged-in summary and logout action", async () => {
  currentToken = "jwt-token";
  currentTokenExpireAt = "2026-04-06T12:00:00.000Z";
  currentUsername = "admin";
  currentRoles = ["ADMIN", "SUPPORT"];

  renderAppAt("/map");

  expect(await screen.findByText(/admin/)).toBeTruthy();
  expect(screen.getByRole("button", { name: "退出" })).toBeTruthy();
});
```

- [ ] **Step 4: 新增“切角色即清会话”的交互测试**

追加：

```jsx
it("clears session immediately when switching role", async () => {
  currentToken = "jwt-token";
  currentTokenExpireAt = "2026-04-06T12:00:00.000Z";
  currentUsername = "support";

  renderAppAt("/map");

  await userEvent.selectOptions(await screen.findByLabelText("当前角色"), "admin");

  expect(setRoleMock).toHaveBeenCalledWith("admin");
});
```

- [ ] **Step 5: 运行测试验证 Red**

Run:

- `cd frontend && npm test -- src/App.access-control.test.jsx`

Expected:

- FAIL，说明顶部壳层尚未渲染最小登录入口或已登录摘要

### Task 2: 在顶部壳层接入最小登录闭环

**Files:**

- Modify: `frontend/src/App.jsx`

- [ ] **Step 1: 读取 auth store 所需字段**

在 `AppShell` 中补齐：

```jsx
  const token = useAuthStore((state) => state.token);
  const tokenExpireAt = useAuthStore((state) => state.tokenExpireAt);
  const username = useAuthStore((state) => state.username);
  const loginByRole = useAuthStore((state) => state.loginByRole);
  const logout = useAuthStore((state) => state.logout);
```

- [ ] **Step 2: 增加顶部密码输入局部状态和提交函数**

在 `AppShell` 中补：

```jsx
  const [password, setPassword] = useState("");

  async function handleLoginSubmit(event) {
    event.preventDefault();
    const success = await loginByRole(role || "admin", password);
    if (success) {
      setPassword("");
    }
  }

  function handleRoleChange(event) {
    setPassword("");
    setRole(event.target.value);
  }

  function handleLogout() {
    setPassword("");
    logout();
  }
```

- [ ] **Step 3: 在顶部工具区渲染未登录 / 已登录两态**

把 `role-switch` 区块改为“角色选择 + 认证入口”的组合：

```jsx
          <div className="role-switch">
            <label htmlFor="topRoleSwitch">当前角色</label>
            <select
              id="topRoleSwitch"
              name="topRoleSwitch"
              autoComplete="off"
              value={role || "admin"}
              onChange={handleRoleChange}
            >
              {ROLE_OPTIONS.map((item) => (
                <option key={item.value} value={item.value}>{item.label}</option>
              ))}
            </select>
            {!token ? (
              <form className="mast-auth-entry" onSubmit={handleLoginSubmit}>
                <label className="sr-only" htmlFor="topRolePassword">当前角色密码</label>
                <input
                  id="topRolePassword"
                  name="topRolePassword"
                  type="password"
                  autoComplete="current-password"
                  placeholder="输入当前角色密码"
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                />
                <button className="mini-link mast-login-btn" type="submit">登录</button>
              </form>
            ) : (
              <div className="mast-auth-summary">
                <span className="mast-auth-user">{username || "已登录"}</span>
                <span className="mast-auth-meta">{roles.join(" / ")}</span>
                <span className="mast-auth-meta">{tokenExpireAt || "会话有效"}</span>
                <button className="mini-link mast-login-btn" type="button" onClick={handleLogout}>退出</button>
              </div>
            )}
          </div>
```

- [ ] **Step 4: 运行单测验证 Green**

Run:

- `cd frontend && npm test -- src/App.access-control.test.jsx`

Expected:

- PASS

### Task 3: 收口顶部登录入口样式

**Files:**

- Modify: `frontend/src/styles.css`

- [ ] **Step 1: 为顶部登录表单补布局样式**

补充或调整：

```css
.mast-auth-entry {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.mast-auth-summary {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.mast-auth-user {
  font-size: 12px;
  font-weight: 600;
  color: #244257;
}

.mast-auth-meta {
  font-size: 11px;
  color: #6b7280;
}
```

并保持登录输入框与按钮继续复用现有圆角、浅底、顶部轨道风格。

- [ ] **Step 2: 运行前端测试和构建**

Run:

- `cd frontend && npm test -- src/App.access-control.test.jsx`
- `cd frontend && npm run build`

Expected:

- 测试通过
- 构建成功

### Task 4: 同步测试报告与交付状态

**Files:**

- Create: `docs/testing/features/engineering/2026-04-06-topbar-auth-entry-test-report.md`
- Modify: `docs/engineering/current-delivery-status.md`

- [ ] **Step 1: 生成测试报告骨架**

在测试报告中记录：

```md
# 顶部最小登录入口测试报告

- 需求：顶部常驻角色密码登录入口
- 范围：登录、登录态展示、退出、切角色清会话
- 执行命令：
  - `cd frontend && npm test -- src/App.access-control.test.jsx`
  - `cd frontend && npm run build`
- 结果：待执行后回填
```

- [ ] **Step 2: 更新交付状态**

在 `docs/engineering/current-delivery-status.md` 的“进行中工作项”或“近期待验收”中补一条工程稳定性事项，注明：

```md
- 顶部最小登录入口已补到前端全局壳层，数据地图不再依赖隐含会话前提
```

- [ ] **Step 3: 运行最终验证**

Run:

- `curl -s http://localhost:8082/actuator/health`
- `curl -s -H 'Content-Type: application/json' -d '{"username":"admin","password":"admin123"}' http://localhost:8082/api/system/auth/token | jq '{username, roles}'`

Expected:

- 后端健康检查返回 `{"status":"UP"}`
- 可获取 `admin` 令牌
