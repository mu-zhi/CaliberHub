import { Component } from "react";

const FALLBACK_MESSAGE = "页面渲染出现异常，请刷新后重试。";

export class AppErrorBoundary extends Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false };
  }

  static getDerivedStateFromError() {
    return { hasError: true };
  }

  componentDidCatch(error, info) {
    if (typeof window === "undefined") {
      return;
    }
    window.dispatchEvent(
      new CustomEvent("dd-ui-error", {
        detail: {
          message: error?.message || FALLBACK_MESSAGE,
          componentStack: info?.componentStack || "",
          errorStack: error?.stack || "",
        },
      }),
    );
  }

  render() {
    if (this.state.hasError) {
      return (
        <section className="panel fatal-panel" role="alert">
          <h2>页面暂时不可用</h2>
          <p className="subtle-note">{FALLBACK_MESSAGE}</p>
          <button
            type="button"
            className="btn btn-primary"
            onClick={() => {
              if (typeof window !== "undefined") {
                window.location.reload();
              }
            }}
          >
            刷新页面
          </button>
        </section>
      );
    }
    return this.props.children;
  }
}
