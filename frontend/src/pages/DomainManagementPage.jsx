import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { apiRequest } from "../api/client";
import { API_CONTRACTS, buildApiPath } from "../api/contracts";
import { useAuthStore } from "../store/authStore";

const DATETIME_FORMATTER = new Intl.DateTimeFormat("zh-CN", {
  year: "numeric",
  month: "2-digit",
  day: "2-digit",
  hour: "2-digit",
  minute: "2-digit",
  hour12: false,
});

function formatTime(value) {
  if (!value) {
    return "-";
  }
  const dt = new Date(value);
  if (Number.isNaN(dt.getTime())) {
    return value;
  }
  return DATETIME_FORMATTER.format(dt).replace(/\//g, "-");
}

function mapDomainToForm(domain) {
  return {
    id: domain?.id || null,
    domainCode: `${domain?.domainCode || ""}`,
    domainName: `${domain?.domainName || ""}`,
    sortOrder: `${domain?.sortOrder ?? 0}`,
    domainOverview: `${domain?.domainOverview || ""}`,
    commonTables: `${domain?.commonTables || ""}`,
    contacts: `${domain?.contacts || ""}`,
  };
}

function createEmptyForm() {
  return {
    id: null,
    domainCode: "",
    domainName: "",
    sortOrder: "0",
    domainOverview: "",
    commonTables: "",
    contacts: "",
  };
}

function sortDomains(rows) {
  return [...rows].sort((a, b) => {
    const sortA = Number(a?.sortOrder ?? 0);
    const sortB = Number(b?.sortOrder ?? 0);
    if (sortA !== sortB) {
      return sortA - sortB;
    }
    const codeA = `${a?.domainCode || ""}`;
    const codeB = `${b?.domainCode || ""}`;
    return codeA.localeCompare(codeB, "zh-CN");
  });
}

export function DomainManagementPage() {
  const token = useAuthStore((state) => state.token);
  const [rows, setRows] = useState([]);
  const [keyword, setKeyword] = useState("");
  const [selectedId, setSelectedId] = useState(null);
  const [form, setForm] = useState(createEmptyForm());
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [meta, setMeta] = useState("");
  const [error, setError] = useState("");
  const selectedIdRef = useRef(selectedId);

  const filteredRows = useMemo(() => {
    const sorted = sortDomains(rows);
    if (!keyword.trim()) {
      return sorted;
    }
    const text = keyword.trim().toLowerCase();
    return sorted.filter((item) => {
      const target = `${item.domainCode || ""} ${item.domainName || ""} ${item.domainOverview || ""} ${item.commonTables || ""} ${item.contacts || ""}`.toLowerCase();
      return target.includes(text);
    });
  }, [rows, keyword]);

  useEffect(() => {
    selectedIdRef.current = selectedId;
  }, [selectedId]);

  const loadDomains = useCallback(async (nextSelectedId = selectedIdRef.current) => {
    setLoading(true);
    setError("");
    try {
      const result = await apiRequest(API_CONTRACTS.domains, { token });
      const nextRows = Array.isArray(result) ? result : [];
      setRows(nextRows);
      if (nextRows.length === 0) {
        setSelectedId(null);
        setForm(createEmptyForm());
        setMeta("暂无业务领域，请先创建。");
        return;
      }
      const picked = nextRows.find((item) => item.id === nextSelectedId) || sortDomains(nextRows)[0];
      setSelectedId(picked?.id || null);
      setForm(mapDomainToForm(picked));
      setMeta(`已加载 ${nextRows.length} 个业务领域。`);
    } catch (err) {
      setRows([]);
      setSelectedId(null);
      setForm(createEmptyForm());
      setError(err.message || "加载业务领域失败");
    } finally {
      setLoading(false);
    }
  }, [token]);

  useEffect(() => {
    loadDomains();
  }, [loadDomains]);

  function selectDomain(domain) {
    setSelectedId(domain.id);
    setForm(mapDomainToForm(domain));
    setError("");
    setMeta(`已切换到业务领域：${domain.domainName || domain.domainCode || "-"}`);
  }

  function createNewDomainDraft() {
    setSelectedId(null);
    setForm(createEmptyForm());
    setError("");
    setMeta("已切换到新建业务领域草稿。");
  }

  function updateField(field, value) {
    setForm((prev) => ({ ...prev, [field]: value }));
  }

  async function saveDomain() {
    const code = form.domainCode.trim();
    const name = form.domainName.trim();
    if (!code || !name) {
      setError("请先填写业务领域编码和业务领域名称。");
      return;
    }
    setSaving(true);
    setError("");
    try {
      const payload = {
        domainCode: code,
        domainName: name,
        domainOverview: form.domainOverview,
        commonTables: form.commonTables,
        contacts: form.contacts,
        sortOrder: Number.parseInt(`${form.sortOrder || "0"}`, 10) || 0,
        operator: "",
      };
      const saved = form.id
        ? await apiRequest(buildApiPath("domainById", { id: form.id }), { method: "PUT", token, body: payload })
        : await apiRequest(API_CONTRACTS.domains, { method: "POST", token, body: payload });
      const savedId = saved?.id || null;
      setMeta(`${form.id ? "更新" : "创建"}成功：${saved?.domainName || saved?.domainCode || "-"}`);
      await loadDomains(savedId);
    } catch (err) {
      setError(err.message || "保存业务领域失败");
    } finally {
      setSaving(false);
    }
  }

  return (
    <section className="panel">
      <div className="panel-head">
        <h2>业务领域管理</h2>
        <p>维护域概述、常见表与联系人，支撑场景录入与地图检索。</p>
      </div>
      <div className="domain-workspace">
        <aside className="panel-block domain-list">
          <div className="row form-row">
            <label htmlFor="domainKeyword">业务领域筛选</label>
            <input
              id="domainKeyword"
              name="domainKeyword"
              autoComplete="off"
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
              placeholder="按编码/名称/概述搜索"
            />
          </div>
          <div className="actions">
            <button className="btn btn-ghost" type="button" onClick={() => loadDomains()} disabled={loading}>
              {loading ? "刷新中…" : "刷新"}
            </button>
            <button className="btn btn-primary" type="button" onClick={createNewDomainDraft}>
              新建业务领域
            </button>
          </div>
          <div className="domain-list-scroll">
            {filteredRows.length === 0 ? (
              <p className="muted">暂无匹配业务领域。</p>
            ) : filteredRows.map((item) => (
              <button
                key={item.id}
                type="button"
                className={`domain-list-item ${selectedId === item.id ? "is-active" : ""}`}
                onClick={() => selectDomain(item)}
              >
                <strong>{item.domainName || "-"}</strong>
                <span>{item.domainCode || "-"}</span>
                <small>排序：{item.sortOrder ?? 0} · 更新：{formatTime(item.updatedAt)}</small>
              </button>
            ))}
          </div>
        </aside>

        <section className="panel-block domain-editor">
          <div className="domain-editor-head">
            <h3>{form.id ? `编辑业务领域 #${form.id}` : "新建业务领域"}</h3>
            <p className="subtle-note">业务领域编码将自动转为大写保存。</p>
          </div>
          <div className="search-grid">
            <div className="row form-row">
              <label htmlFor="domainCode">业务领域编码</label>
              <input
                id="domainCode"
                name="domainCode"
                autoComplete="off"
                value={form.domainCode}
                onChange={(event) => updateField("domainCode", event.target.value.toUpperCase())}
                placeholder="例如：RETAIL"
              />
            </div>
            <div className="row form-row">
              <label htmlFor="domainName">业务领域名称</label>
              <input
                id="domainName"
                name="domainName"
                autoComplete="off"
                value={form.domainName}
                onChange={(event) => updateField("domainName", event.target.value)}
                placeholder="例如：零售业务"
              />
            </div>
          </div>
          <div className="row form-row">
            <label htmlFor="domainSortOrder">排序</label>
            <input
              id="domainSortOrder"
              name="domainSortOrder"
              autoComplete="off"
              type="number"
              value={form.sortOrder}
              onChange={(event) => updateField("sortOrder", event.target.value)}
              placeholder="默认 0"
            />
          </div>
          <label className="textarea-label" htmlFor="domainOverview">域概述</label>
          <textarea
            id="domainOverview"
            name="domainOverview"
            autoComplete="off"
            className="mini"
            value={form.domainOverview}
            onChange={(event) => updateField("domainOverview", event.target.value)}
            placeholder="描述业务领域边界、主要目标和关键口径。"
          />
          <label className="textarea-label" htmlFor="domainCommonTables">常见表</label>
          <textarea
            id="domainCommonTables"
            name="domainCommonTables"
            autoComplete="off"
            className="mini"
            value={form.commonTables}
            onChange={(event) => updateField("commonTables", event.target.value)}
            placeholder="可按行列出常见表或以逗号分隔。"
          />
          <label className="textarea-label" htmlFor="domainContacts">联系人</label>
          <textarea
            id="domainContacts"
            name="domainContacts"
            autoComplete="off"
            className="mini"
            value={form.contacts}
            onChange={(event) => updateField("contacts", event.target.value)}
            placeholder="例如：张三（零售产品）/ 李四（数据治理）"
          />
          <div className="actions">
            <button className="btn btn-primary" type="button" onClick={saveDomain} disabled={saving}>
              {saving ? "保存中…" : (form.id ? "更新业务领域" : "创建业务领域")}
            </button>
            <button className="btn btn-ghost" type="button" onClick={() => loadDomains(form.id)} disabled={loading}>
              重新加载
            </button>
          </div>
          <p className="meta" role="status" aria-live="polite">{meta}</p>
          {error ? <p className="danger-text" role="status" aria-live="polite">{error}</p> : null}
        </section>
      </div>
    </section>
  );
}
