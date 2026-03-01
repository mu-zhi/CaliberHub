import { Link } from "react-router-dom";

export function NotFoundPage() {
  return (
    <section className="panel">
      <div className="panel-head">
        <h2>页面不存在</h2>
        <p>请返回首页继续操作</p>
      </div>
      <Link className="btn btn-primary" to="/home">回到首页</Link>
    </section>
  );
}
