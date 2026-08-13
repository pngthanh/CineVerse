# CineVerse

CineVerse là hệ thống đặt vé và quản lý rạp chiếu phim full-stack dùng làm project portfolio cho vị trí Software Developer Intern.

## Tech stack

- Java 21 + Spring Boot 4.1
- React 19 + TypeScript + Vite 8
- PostgreSQL 18
- Spring Data JPA / Hibernate
- Spring Security + JWT
- REST API + OpenAPI/Swagger
- JUnit + Mockito
- Docker Compose
- GitHub Actions

## Kiến trúc

Backend dùng **Modular Monolith + Layered Architecture**:

```text
React
  ↓ REST API
Controller
  ↓
Service
  ↓
Repository
  ↓
PostgreSQL
```

Các module chính:

```text
auth
user
movie
cinema
showtime
booking
payment
ticket
admin
```

## Điểm kỹ thuật nổi bật

### Seat hold và chống đặt trùng

Khi khách chọn ghế, backend khóa các bản ghi `ShowtimeSeat` bằng `PESSIMISTIC_WRITE` trong transaction. Một ghế của một suất chỉ ở một trong ba trạng thái:

```text
AVAILABLE -> HELD -> BOOKED
               ↘ hết hạn -> AVAILABLE
```

Giá vé cũng được tính ở backend; frontend chỉ hiển thị giá server trả về.

### Showtime conflict

Admin không thể tạo hai suất chiếu chồng thời gian trong cùng một phòng.

### Payment V1

V1 dùng Mock Payment để chứng minh đúng business flow mà không phụ thuộc cổng thanh toán thật.

## Chạy project

Đọc **[SETUP.md](SETUP.md)** theo từng bước.

## Tài liệu nên đọc

- [Business Rules](docs/BUSINESS_RULES.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Code Style](docs/CODE_STYLE.md)
- [V1 Status](docs/V1_STATUS.md)
- [Validation Notes](docs/VALIDATION.md)
- [V1 Test Plan](docs/TEST_PLAN.md)
- [ADR - Modular Monolith](docs/adr/0001-modular-monolith.md)
- [ADR - Seat Concurrency](docs/adr/0002-seat-concurrency.md)

## Demo accounts

```text
Admin
admin@cineverse.vn
Admin@123

Customer
customer@cineverse.vn
Customer@123
```

> Tài khoản trên chỉ dùng cho local/demo. Không dùng password demo khi deploy production.
