function classNames(...values) {
  return values.filter(Boolean).join(" ");
}

export function UiCard({
  as: Component = "section",
  elevation = "card",
  interactive = false,
  className = "",
  children,
  ...rest
}) {
  const cls = classNames(
    "ui-card",
    `ui-card-${elevation}`,
    interactive ? "is-interactive" : "",
    className,
  );
  return (
    <Component className={cls} {...rest}>
      {children}
    </Component>
  );
}
