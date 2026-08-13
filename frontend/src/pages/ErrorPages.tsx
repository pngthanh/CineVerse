import { Link } from 'react-router-dom';
function ErrorPage({ code, title, text }: { code: string; title: string; text: string }) {
  return (
    <div className="page-center">
      <div className="error-card">
        <strong>{code}</strong>
        <h1>{title}</h1>
        <p>{text}</p>
        <Link className="btn" to="/">
          Về trang chủ
        </Link>
      </div>
    </div>
  );
}
export const AccessDeniedPage = () => (
  <ErrorPage
    code="403"
    title="Không có quyền truy cập"
    text="Tài khoản của bạn không được phép mở trang này."
  />
);
export const NotFoundPage = () => (
  <ErrorPage
    code="404"
    title="Không tìm thấy trang"
    text="Trang bạn đang tìm không tồn tại hoặc đã được chuyển."
  />
);
export const SystemErrorPage = () => (
  <ErrorPage
    code="500"
    title="Hệ thống gặp lỗi"
    text="Không thể hoàn tất yêu cầu. Vui lòng thử lại."
  />
);
