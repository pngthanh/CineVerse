# CineVerse V1 - trạng thái

## Scope V1

### Customer
- Đăng ký / đăng nhập JWT.
- Xem phim, rạp, suất chiếu.
- Chọn ghế.
- Giữ ghế tạm thời.
- Chống hai người giữ cùng ghế bằng pessimistic database lock.
- Checkout.
- Mock Payment.
- Ticket code.
- Lịch sử booking và chi tiết vé.
- Trang tài khoản.

### Admin
- Dashboard cơ bản.
- Quản lý phim.
- Quản lý rạp / phòng / ghế.
- Quản lý suất chiếu và kiểm tra trùng lịch.
- Xem booking.
- Quản lý trạng thái user.

### Engineering
- Modular Monolith + Layered Architecture.
- PostgreSQL + JPA/Hibernate.
- Spring Security + JWT.
- Global exception format.
- Swagger/OpenAPI.
- JUnit + Mockito cho business rules quan trọng.
- Docker Compose.
- GitHub Actions.
- ESLint / Prettier / Checkstyle configuration.
- Pull Request template và local verification scripts.

## Những phần cố tình để V2+

- Google Login.
- Voucher/Promotion.
- Combo bắp nước.
- QR check-in / Staff role.
- Payment sandbox thật.
- Email notification.
- Recommendation.
- Audit log nâng cao.

## Quy tắc khóa scope

Không thêm Tier 2 trước khi V1 chạy end-to-end ổn định:

`Login -> Movie -> Showtime -> Seat Hold -> Checkout -> Mock Payment -> Ticket -> My Bookings`

## Hardening đã bổ sung

- Seat map trả giá từ backend; frontend không tự hard-code giá cuối.
- Booking tạo theo `holdToken` có kiểm tra idempotency và lock dữ liệu.
- Mock payment khóa booking trước khi đổi trạng thái.
- Có test cho pricing, seat hold, showtime conflict và payment state transition.
- Admin controller không truy cập repository trực tiếp cho bookings/users.
- UI status/role/seat type được hiển thị tiếng Việt trong khi code identifier vẫn dùng tiếng Anh.
- API client chuẩn hóa lỗi network và field validation metadata.
- Header/footer và các trang booking quan trọng dùng component/format helper thống nhất.
- Đã bổ sung `docs/TEST_PLAN.md`, PR checklist và script verify local.

## Điều kiện để gọi là V1 Stable

Chỉ đổi nhãn từ **Stable Candidate** sang **Stable** khi:

1. `mvn verify` PASS.
2. `npm run check` PASS.
3. `docker compose up --build` chạy được.
4. Happy path payment SUCCESS chạy end-to-end.
5. Payment FAILED trả ghế về AVAILABLE.
6. Test hai request giữ cùng ghế xác nhận chỉ một request thắng.
7. Admin CRUD/showtime conflict hoạt động đúng.

## RC4 validation checkpoint

- Frontend RC3 đã `npm run check` thành công với 0 lint errors và production build thành công.
- Backend RC3 đã chạy được Maven tới bước compile 78 source files và phát hiện 2 compile errors.
- RC4 sửa hai compile errors đó; trạng thái backend vẫn là **chờ chạy lại `mvn verify`** trước khi gọi V1 Stable.
