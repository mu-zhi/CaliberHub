import { useMemo } from "react";
import { Link } from "react-router-dom";
import { useAppStore } from "../store/appStore";
import { UiBadge, UiCard } from "../components/ui";

const DATETIME_FORMATTER = new Intl.DateTimeFormat("zh-CN", {
  year: "numeric",
  month: "2-digit",
  day: "2-digit",
  hour: "2-digit",
  minute: "2-digit",
  hour12: false,
});

function fmtTime(value) {
  if (!value) {
    return "-";
  }
  const dt = new Date(value);
  if (Number.isNaN(dt.getTime())) {
    return value;
  }
  return DATETIME_FORMATTER.format(dt).replace(/\//g, "-");
}

export function WorkspacePage({ view = "todo" }) {
  const recents = useAppStore((state) => state.recents);
  const favorites = useAppStore((state) => state.favorites);

  const body = useMemo(() => {
    if (view === "recent") {
      return (
        <ul className="plain-list">
          {recents.length === 0 ? <li>暂无最近浏览</li> : recents.map((item) => <li key={`${item.path}-${item.at}`}>{item.path} ({fmtTime(item.at)})</li>)}
        </ul>
      );
    }
    if (view === "favorites") {
      return (
        <ul className="plain-list">
          {favorites.length === 0 ? <li>暂无收藏</li> : favorites.map((id) => <li key={id}>场景 #{id}</li>)}
        </ul>
      );
    }
    if (view === "notice") {
      return (
        <ul className="plain-list">
          <li>系统公告：知识生产台与数据地图已切换至 React 版。</li>
          <li>发布提醒：建议发布前补充变更摘要与验证时间。</li>
        </ul>
      );
    }
    return (
      <ul className="reminder-list" aria-label="我的待办">
        <li>
          <Link className="reminder-item reminder-link" to="/knowledge/import">
            <span className="reminder-circle" aria-hidden="true" />
            <span className="reminder-title">待确认导入任务</span>
            <UiBadge tone="neutral" className="reminder-badge">0</UiBadge>
          </Link>
        </li>
        <li>
          <Link className="reminder-item reminder-link" to="/knowledge/import">
            <span className="reminder-circle" aria-hidden="true" />
            <span className="reminder-title">待发布草稿</span>
            <UiBadge tone="warn" className="reminder-badge">3</UiBadge>
          </Link>
        </li>
      </ul>
    );
  }, [view, recents, favorites]);

  return (
    <UiCard as="section" className="panel">
      <div className="panel-head">
        <h2>个人协作</h2>
        <p>待办、通知、收藏与最近浏览</p>
      </div>
      {body}
    </UiCard>
  );
}
