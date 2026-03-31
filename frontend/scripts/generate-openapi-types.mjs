import fs from "node:fs/promises";
import path from "node:path";
import process from "node:process";
import openapiTS, { astToString, COMMENT_HEADER } from "openapi-typescript";

const schemaUrl = process.env.OPENAPI_SCHEMA_URL || "http://127.0.0.1:8080/v3/api-docs";
const outputPath = path.resolve(process.cwd(), "src/types/openapi.d.ts");

try {
  const ast = await openapiTS(new URL(schemaUrl));
  const contents = astToString(ast);
  const header = `/* eslint-disable */\n${COMMENT_HEADER}// source: ${schemaUrl}\n\n`;
  await fs.mkdir(path.dirname(outputPath), { recursive: true });
  await fs.writeFile(outputPath, `${header}${contents}`, "utf8");
  process.stdout.write(`generated ${path.relative(process.cwd(), outputPath)} from ${schemaUrl}\n`);
} catch (error) {
  const reason = error instanceof Error ? error.message : String(error);
  process.stderr.write(`failed to generate OpenAPI types from ${schemaUrl}: ${reason}\n`);
  process.exitCode = 1;
}
