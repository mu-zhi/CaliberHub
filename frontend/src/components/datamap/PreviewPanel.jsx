import { useMemo, useRef, useState } from "react";
import Prism from "prismjs";
import "prismjs/components/prism-sql";
import { parseJsonText } from "../../api/client";
import { Copy, Workflow } from "lucide-react";
import { UiButton, UiInlineError } from "../ui";

function firstSqlText(sceneDetail) {
  const variants = parseJsonText(sceneDetail?.sqlVariantsJson, parseJsonText(sceneDetail?.sqlBlocksJson, []));
  if (!Array.isArray(variants) || variants.length === 0) {
    return "";
  }
  const sqlText = `${variants[0]?.sql_text || ""}`.trim();
  return sqlText;
}

function outputFields(sceneDetail) {
  const rows = parseJsonText(sceneDetail?.outputsJson, []);
  if (!Array.isArray(rows)) {
    return [];
  }
  return rows.slice(0, 12).map((item) => ({
    name: `${item?.field_name || item?.name || ""}`.trim(),
    description: `${item?.description || item?.field_comment || ""}`.trim(),
  })).filter((item) => item.name || item.description);
}

function sourceTables(sceneDetail) {
  const raw = parseJsonText(sceneDetail?.sourceTablesJson, []);
  if (!Array.isArray(raw)) {
    return [];
  }
  return raw.map((item) => `${item || ""}`.trim()).filter(Boolean);
}

export function PreviewPanel({
  sceneNode,
  sceneDetail,
  loading,
  error,
  width,
  onWidthChange,
  onRetry,
  onSwitchToLineage,
}) {
  const startXRef = useRef(0);
  const startWidthRef = useRef(0);
  const [copyDone, setCopyDone] = useState(false);

  const sqlText = useMemo(() => firstSqlText(sceneDetail), [sceneDetail]);
  const sqlHtml = useMemo(() => {
    if (!sqlText) {
      return "";
    }
    return Prism.highlight(sqlText, Prism.languages.sql, "sql");
  }, [sqlText]);
  const fields = useMemo(() => outputFields(sceneDetail), [sceneDetail]);
  const tables = useMemo(() => sourceTables(sceneDetail), [sceneDetail]);

  function onResizeStart(event) {
    event.preventDefault();
    startXRef.current = event.clientX;
    startWidthRef.current = width;
    const onMove = (moveEvent) => {
      const delta = moveEvent.clientX - startXRef.current;
      const next = Math.max(400, Math.min(800, startWidthRef.current - delta));
      onWidthChange(next);
    };
    const onEnd = () => {
      window.removeEventListener("mousemove", onMove);
      window.removeEventListener("mouseup", onEnd);
    };
    window.addEventListener("mousemove", onMove);
    window.addEventListener("mouseup", onEnd);
  }

  async function copySql() {
    if (!sqlText) {
      return;
    }
    try {
      await navigator.clipboard.writeText(sqlText);
      setCopyDone(true);
      window.setTimeout(() => setCopyDone(false), 1500);
    } catch (_) {
      setCopyDone(false);
    }
  }

  return (
    <aside className="datamap-preview-panel" style={{ width: `${width}px` }} aria-label="场景预览面板">
      <button
        type="button"
        className="datamap-resize-handle"
        onMouseDown={onResizeStart}
        aria-label="拖拽调整预览面板宽度"
      />
      <header className="datamap-preview-head">
        <div>
          <h3>{sceneNode?.label || "场景预览"}</h3>
          <p>可追溯预览：SQL、来源表、输出字段</p>
        </div>
        <div className="datamap-preview-actions">
          <UiButton type="button" variant="secondary" size="sm" onClick={onSwitchToLineage} icon={<Workflow size={14} />}>
            切换为血缘视图
          </UiButton>
        </div>
      </header>

      {loading ? <p className="subtle-note">正在加载场景详情…</p> : null}
      {error ? (
        <UiInlineError className="datamap-preview-error" message={error} actionText="重试" onAction={onRetry} />
      ) : null}

      {!loading && !error && sceneDetail ? (
        <div className="datamap-preview-body">
          <section>
            <h4>来源表</h4>
            <p>{tables.length > 0 ? tables.join("，") : "未识别来源表"}</p>
          </section>
          <section>
            <div className="datamap-sql-head">
              <h4>SQL 代码</h4>
              <UiButton type="button" variant="ghost" size="sm" onClick={copySql} icon={<Copy size={14} />}>
                {copyDone ? "已复制" : "复制 SQL"}
              </UiButton>
            </div>
            {sqlHtml ? (
              <pre className="datamap-sql-code"><code dangerouslySetInnerHTML={{ __html: sqlHtml }} /></pre>
            ) : (
              <p className="subtle-note">暂无 SQL 片段</p>
            )}
          </section>
          <section>
            <h4>输出字段</h4>
            {fields.length > 0 ? (
              <table className="datamap-field-table">
                <thead>
                  <tr>
                    <th>字段名</th>
                    <th>说明</th>
                  </tr>
                </thead>
                <tbody>
                  {fields.map((item, index) => (
                    <tr key={`${item.name}-${index}`}>
                      <td>{item.name || "-"}</td>
                      <td>{item.description || "-"}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            ) : (
              <p className="subtle-note">暂无字段定义</p>
            )}
          </section>
        </div>
      ) : null}
    </aside>
  );
}
