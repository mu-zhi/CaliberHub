---
name: springdoc-api-sync
description: Add or refine SpringDoc and Swagger based API documentation generation for Spring Boot services. Use when the user says 同步接口文档, asks to expose OpenAPI docs, or wants to reduce drift between implementation and interface docs.
metadata:
  author: codex
  version: "1.0.0"
  argument-hint: <backend-module-or-interface-doc>
---

# SpringDoc API Sync

Use this skill when the user asks to:

- 同步接口文档
- 接入 `SpringDoc（OpenAPI 文档生成框架）`
- 接入 `Swagger（接口文档生态）`
- 自动生成 `OpenAPI（开放接口描述规范）` 文档
- 解决“接口文档与实现漂移”
- 暴露 `/v3/api-docs` 或 `swagger-ui`
- 用代码注解替代大段手写接口说明

## Goal

Make the backend implementation the primary truth source for interface contracts, then derive human-readable documentation from `OpenAPI（开放接口描述规范）` output instead of maintaining full parallel manual docs.

## Apply when

1. The repository uses `Spring Boot（应用框架）`.
2. The backend exposes `REST（表述性状态转移）` APIs.
3. Manual interface docs are drifting from controller and DTO changes.
4. The team wants generated `/v3/api-docs` and `Swagger UI（接口调试界面）`.

## Default workflow

1. Inspect the backend module:
   - check `pom.xml`
   - locate controller packages
   - locate security configuration
   - locate existing interface docs
2. Add `SpringDoc（OpenAPI 文档生成框架）` dependency appropriate for the stack:
   - Spring Boot 3 + MVC: prefer `org.springdoc:springdoc-openapi-starter-webmvc-ui`
   - Spring Boot 3 + WebFlux: prefer `org.springdoc:springdoc-openapi-starter-webflux-ui`
3. Expose generated docs:
   - `/v3/api-docs`
   - `/swagger-ui/index.html`
4. If security is enabled, explicitly allow the documentation endpoints.
5. Add or refine annotations on controllers and DTOs:
   - `@Tag（接口分组注解）`
   - `@Operation（接口说明注解）`
   - `@Parameter（参数说明注解）`
   - `@Schema（模型说明注解）`
   - `@ApiResponse（响应说明注解）`
6. Run the service and verify:
   - app starts
   - `/v3/api-docs` returns valid JSON
   - `swagger-ui` loads
7. Sync repository docs:
   - keep human docs focused on business rules, caveats, and usage guidance
   - avoid duplicating request/response field tables that can be generated from `OpenAPI（开放接口描述规范）`
   - if the repository has a manual interface doc, convert it to “generated source + supplementary explanation”

## Repository editing rules

1. Do not keep two competing truth sources for the same API field contract.
2. Generated docs should describe path, method, parameters, request body, response body, and status codes.
3. Manual docs should only retain:
   - business context
   - permission and approval notes
   - non-obvious caveats
   - examples that generated docs do not express well
4. If you add new annotation-driven fields or terms in docs, ensure they obey the glossary rule in `docs/glossary.md`.

## Verification checklist

1. `pom.xml` contains the correct `SpringDoc（OpenAPI 文档生成框架）` starter.
2. Security config allows `/v3/api-docs/**` and `/swagger-ui/**` if needed.
3. At least one controller is annotated and visible in generated docs.
4. At least one DTO has field-level `@Schema（模型说明注解）` where naming alone is insufficient.
5. `/v3/api-docs` is reachable locally.
6. `swagger-ui` is reachable locally.
7. Manual interface docs, if present, no longer duplicate low-level contract tables that now come from generated output.

## Recommended outputs

When using this skill, produce:

1. changed files
2. generated endpoint URLs
3. remaining manual-doc sections that still need human maintenance
4. risks or gaps, especially:
   - undocumented legacy controllers
   - auth rules missing from generated docs
   - DTO fields with ambiguous semantics
