# AGENTS

## Scope
- Repository is documentation-first and includes runnable frontend/backend build and test commands.

## Known workflows
- "更新项目文档" trigger: follow the 12-item doc sync rules and the 6-step update flow in README.md.

## Skills
- `update-project-docs`: synchronize project docs after plan changes.
- `frontend-design-governance`: keep frontend design decisions consistent, enforce Chinese UX wording, and write decision traces into project docs.
- `web-design-guidelines`: UI 评审与可访问性审查（项目级安装于 `.agents/skills/web-design-guidelines`）。
- `vercel-react-best-practices`: React/Next 性能与工程最佳实践。
- `vercel-composition-patterns`: React 组件组合设计与可维护性规范。

## Auto trigger rules
- If user request mentions `前端`、`页面`、`界面`、`UI`、`交互`、`导航`、`样式`、`图谱`、`组件`, load and execute `skills/frontend-design-governance/SKILL.md`.
- If user request mentions `评审UI`、`可访问性`、`UX审计`、`设计规范`、`best practices`, load and execute `.agents/skills/web-design-guidelines/SKILL.md`.
- If request is frontend-review related, run `web-design-guidelines` first, then `frontend-design-governance`.
- If request is frontend-review related and doc-sync related, run `web-design-guidelines` first, then `frontend-design-governance`, then `update-project-docs`.
- If request is both frontend-related and doc-sync related, run `frontend-design-governance` first, then `update-project-docs`.

## Commands
- Frontend dev: `cd frontend && npm run dev`
- Frontend build: `cd frontend && npm run build`
- Backend test: `cd backend && mvn -q test`
- Backend package: `cd backend && mvn -q package`
- Frontend static sync to backend: `bash scripts/sync_frontend_dist.sh`

## Runtime policy (required)
- After any code change, restart affected service(s) and keep the project running for verification.
- Default local collaboration mode keeps both backend (`8080`) and frontend (`5173`) online unless task scope explicitly says backend-only or frontend-only.
- Before reporting completion, verify service availability with lightweight checks (`curl`/health endpoint).
