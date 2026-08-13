
## RC5.1
- Fix DataInitializer compile error by using UserRepository.findByEmailIgnoreCase for demo account seeding.
# Changelog

## RC4
- Fix backend compilation in `BookingService` by avoiding a reassigned local variable inside pricing lambdas.
- Align `UserController` with the `UserService.updateProfile(...)` contract.
- Keep RC4 scope limited to compile fixes discovered by the first real `mvn verify` run on Windows.

## V1.0.0-rc.2 - hardening candidate

### Added
- Unit tests for mock payment success/failure state transitions.
- Local verification scripts for Bash and PowerShell.
- Pull Request checklist.
- `docs/TEST_PLAN.md` for V1 acceptance testing.
- Docker ignore files and repository `.gitattributes`.
- Stronger frontend status/role/seat type types and Vietnamese display labels.

### Improved
- API client now distinguishes network failures and preserves validation field errors.
- Customer footer uses real internal links.
- Customer header logout behavior is explicit and consistent.
- My Bookings and Booking Detail now include loading/error/empty states.
- Booking result pages handle missing booking id safely.
- Core controllers were reformatted and renamed dependencies for clearer production-style code.
- Generated TypeScript build cache removed from repository package.

## V1.0.0-rc.1 - foundation candidate

### Added
- Full customer and admin route structure.
- Java/Spring Boot REST backend and PostgreSQL model.
- React + TypeScript customer/admin UI based on CineVerse Stitch references.
- JWT authentication and role-based admin authorization.
- Showtime conflict validation.
- Seat hold with pessimistic database locking and expiration.
- Booking + mock payment + ticket flow.
- Docker Compose, CI workflow, Swagger and setup documentation.

### Hardened
- Seat map price is supplied by backend instead of guessed by frontend.
- Booking creation is idempotent per hold token.
- Payment uses a booking database lock and supports idempotent success response.
- Checkout and payment display the real seat-hold countdown.
- Booking progress follows five clear steps.

## RC3
- Pin frontend dependencies to compatible versions (TypeScript 5.9.3, React Router DOM 7.18.2).
- Refactor React effects to satisfy React Hooks lint rules without disabling safety checks.
- Remove derived-state effect from Admin Showtimes and update room selection in the cinema change handler.

## RC5 — UX polish + booking recovery
- Nâng cấp header/footer và bố cục trang chủ theo hướng cinema premium.
- Chuẩn hóa chiều cao vùng tên/meta/action của MovieCard để thẻ phim không lệch nút.
- Bổ sung Top đánh giá, phim đang chiếu, sắp chiếu, dữ liệu demo phong phú hơn.
- Trang Phim có tìm kiếm, thể loại, độ tuổi và sắp xếp.
- Trang chi tiết phim có thông tin mở rộng, rating, trailer YouTube nhúng trực tiếp và nút trở về.
- BookingSteps có đường nối trạng thái hoàn thành.
- Sơ đồ ghế có lối đi giữa ghế 5/6, khu VIP riêng và cột tóm tắt cố định.
- Login/Register dùng chung header/footer khách hàng.
- Booking PENDING có thể tiếp tục thanh toán hoặc hủy để trả ghế.
- Thêm poster demo và trailer YouTube dùng chung để kiểm thử.

## RC5.2
- Fixed PaymentServiceTest fixture to include Booking -> Showtime -> Movie relation required by ticket sold counter logic.
- Keeps RC5.1 DataInitializer repository method fix.
