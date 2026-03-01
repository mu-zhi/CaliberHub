function classNames(...values) {
  return values.filter(Boolean).join(" ");
}

export function UiBadge({ tone = "neutral", className = "", children, ...rest }) {
  return (
    <span className={classNames("ui-badge", `ui-badge-${tone}`, className)} {...rest}>
      {children}
    </span>
  );
}
