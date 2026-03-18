import { AlertTriangle } from "lucide-react";
import { UiButton } from "./UiButton";

function classNames(...values) {
  return values.filter(Boolean).join(" ");
}

export function UiInlineError({
  icon = null,
  message = "加载失败，请稍后重试",
  actionText = "",
  onAction,
  className = "",
}) {
  const iconNode = icon || <AlertTriangle size={16} strokeWidth={2} />;
  return (
    <div className={classNames("ui-inline-error", className)} role="alert">
      <p>
        <span className="ui-inline-error-icon" aria-hidden="true">{iconNode}</span>
        <span>{message}</span>
      </p>
      {actionText && onAction ? (
        <UiButton variant="ghost" size="sm" onClick={onAction}>
          {actionText}
        </UiButton>
      ) : null}
    </div>
  );
}
