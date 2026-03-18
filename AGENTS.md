# AGENTS

## Scope
- Repository is documentation-first and includes runnable frontend/backend build and test commands.

## Known workflows
- "更新项目文档" trigger: follow the 12-item doc sync rules and the 6-step update flow in README.md.
- When updating docs, enforce bilingual annotation for English terms, abbreviations, variables, constants, and metrics; if a new item is missing from the glossary, add it to `01-设计文档/02-统一术语表.md` first.

## Skills
- `update-project-docs`: synchronize project docs after plan changes.
- `springdoc-api-sync`: 接入 `SpringDoc（OpenAPI 文档生成框架）` / `Swagger（接口文档生态）`，生成接口文档并消除“文档与实现漂移”。
- `caliber-redteam-review`: 当前停用（历史质询技能，已移除 08/09/10/11 文档输出路径）。
- `frontend-design-governance`: keep frontend design decisions consistent, enforce Chinese UX wording, and write decision traces into project docs.
- `web-design-guidelines`: UI 评审与可访问性审查（项目级安装于 `.agents/skills/web-design-guidelines`）。
- `vercel-react-best-practices`: React/Next 性能与工程最佳实践。
- `vercel-composition-patterns`: React 组件组合设计与可维护性规范。

## Auto trigger rules
- If user request mentions `Swagger`、`SpringDoc`、`OpenAPI`、`接口文档自动生成`、`文档与实现漂移`、`/v3/api-docs`、`swagger-ui`, load and execute `.agents/skills/springdoc-api-sync/SKILL.md`.
- If user request mentions `前端`、`页面`、`界面`、`UI`、`交互`、`导航`、`样式`、`图谱`、`组件`, load and execute `skills/frontend-design-governance/SKILL.md`.
- If user request mentions `质询`、`口径文档提问`、`对抗评审`、`红队评审`、`challenge case`, do not auto-execute `skills/caliber-redteam-review/SKILL.md`（当前停用，需先明确恢复范围与新输出路径）。
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
