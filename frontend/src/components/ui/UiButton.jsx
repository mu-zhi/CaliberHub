function classNames(...values) {
  return values.filter(Boolean).join(" ");
}

export function UiButton({
  as: Component = "button",
  variant = "primary",
  size = "md",
  icon = null,
  loading = false,
  disabled = false,
  className = "",
  children,
  ...rest
}) {
  const isDisabled = Boolean(disabled || loading);
  const cls = classNames(
    "ui-btn",
    `ui-btn-${variant}`,
    `ui-btn-${size}`,
    loading ? "is-loading" : "",
    className,
  );

  const buttonProps = {
    className: cls,
    "aria-busy": loading ? "true" : undefined,
    ...rest,
  };

  if (Component === "button") {
    buttonProps.type = rest.type || "button";
    buttonProps.disabled = isDisabled;
  } else if (isDisabled) {
    buttonProps["aria-disabled"] = "true";
    buttonProps.onClick = (event) => event.preventDefault();
  }

  return (
    <Component {...buttonProps}>
      {icon ? <span className="ui-btn-icon" aria-hidden="true">{icon}</span> : null}
      <span>{loading ? "处理中…" : children}</span>
    </Component>
  );
}
