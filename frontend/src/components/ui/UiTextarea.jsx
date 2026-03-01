function classNames(...values) {
  return values.filter(Boolean).join(" ");
}

export function UiTextarea({
  invalid = false,
  hint = "",
  prefix = null,
  suffix = null,
  className = "",
  textareaClassName = "",
  ...rest
}) {
  return (
    <div className={classNames("ui-field", invalid ? "is-invalid" : "", className)}>
      <div className={classNames("ui-input-wrap ui-textarea-wrap", prefix ? "has-prefix" : "", suffix ? "has-suffix" : "")}>
        {prefix ? <span className="ui-input-addon ui-input-prefix">{prefix}</span> : null}
        <textarea className={classNames("ui-textarea", textareaClassName)} {...rest} />
        {suffix ? <span className="ui-input-addon ui-input-suffix">{suffix}</span> : null}
      </div>
      {hint ? <p className={classNames("ui-field-hint", invalid ? "is-invalid" : "")}>{hint}</p> : null}
    </div>
  );
}
