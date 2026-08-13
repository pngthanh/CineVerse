# SETUP - Cài và chạy CineVerse

Tài liệu này dành cho trường hợp lấy source về một máy mới và muốn chạy từ đầu.

## 1. Cần cài gì?

Khuyến nghị:

- JDK 21
- Apache Maven 3.9+
- Node.js 22+ hoặc một bản LTS mới hơn tương thích Vite
- Git
- PostgreSQL 18 + pgAdmin **hoặc** Docker Desktop
- IntelliJ IDEA Community / VS Code
- Postman (không bắt buộc)

Kiểm tra bằng terminal:

```bash
java -version
mvn -version
node -v
npm -v
git --version
```

Nếu dùng Docker:

```bash
docker --version
docker compose version
```

---

## 2. Clone source

```bash
git clone <YOUR_GITHUB_REPOSITORY_URL>
cd cineverse
```

Cấu trúc chính:

```text
cineverse/
├── backend/
├── frontend/
├── docs/
├── docker-compose.yml
└── SETUP.md
```

---

## 3. Chạy theo cách dễ học nhất

Khi mới học project, nên chạy từng phần riêng:

1. PostgreSQL.
2. Backend Spring Boot.
3. Frontend React.

Cách này giúp biết lỗi nằm ở database, backend hay frontend.

### 3.1 Tạo PostgreSQL local

Mở pgAdmin hoặc psql:

```sql
CREATE USER cineverse WITH PASSWORD 'cineverse_dev_password';
CREATE DATABASE cineverse OWNER cineverse;
```

Backend mặc định dùng:

```text
DB_URL=jdbc:postgresql://localhost:5432/cineverse
DB_USERNAME=cineverse
DB_PASSWORD=cineverse_dev_password
```

Nếu máy đã có user/database khác, chỉnh biến môi trường thay vì sửa cứng source.

---

## 4. Chạy backend

```bash
cd backend
mvn spring-boot:run
```

Khi startup thành công:

```text
API:     http://localhost:8080
Swagger: http://localhost:8080/swagger-ui.html
OpenAPI: http://localhost:8080/v3/api-docs
```

Chạy test + Checkstyle:

```bash
mvn verify
```

### Demo data

Lần đầu database trống, app seed:

```text
Admin
admin@cineverse.vn
Admin@123

Customer
customer@cineverse.vn
Customer@123
```

và một số phim/rạp/phòng/ghế/suất chiếu mẫu.

---

## 5. Chạy frontend

Mở terminal khác ở thư mục gốc:

```bash
cd frontend
npm install
npm run dev
```

Mặc định:

```text
http://localhost:5173
```

Frontend gọi API qua:

```text
VITE_API_URL=http://localhost:8080/api
```

Có thể copy:

```text
frontend/.env.example -> frontend/.env
```

Kiểm tra frontend:

```bash
npm run lint
npm run build
```

---

## 6. Chạy PostgreSQL bằng Docker nhưng code backend/frontend trên máy

Từ thư mục gốc:

```bash
docker compose up -d postgres
```

Sau đó chạy backend/frontend như mục 4 và 5.

Đây là cách khá tiện khi không muốn cài PostgreSQL trực tiếp.

---

## 7. Chạy toàn bộ bằng Docker Compose

```bash
docker compose up --build
```

Dịch vụ:

```text
Frontend: http://localhost:5173
Backend:  http://localhost:8080
Swagger:  http://localhost:8080/swagger-ui.html
Postgres: localhost:5432
```

Tắt:

```bash
docker compose down
```

Xóa luôn volume database demo:

```bash
docker compose down -v
```

---

## 8. Flow test V1

### Customer

1. Login `customer@cineverse.vn`.
2. Vào danh sách phim.
3. Chọn phim và suất chiếu.
4. Chọn ghế.
5. Backend giữ ghế trong 5 phút và trả `holdToken`.
6. Checkout tạo booking `PENDING`.
7. Mock Payment -> SUCCESS.
8. Booking -> `CONFIRMED`.
9. Ghế -> `BOOKED`.
10. Ticket được sinh.
11. Vào **Vé của tôi** để xem booking/ticket.

Thử thêm flow thất bại:

1. Chọn ghế lại.
2. Tạo booking.
3. Mock Payment -> FAILED.
4. Booking -> `CANCELLED`.
5. Ghế phải trở lại `AVAILABLE`.

### Admin

Login `admin@cineverse.vn`, sau đó kiểm tra:

- Movies.
- Cinemas / Rooms / Seats.
- Showtimes.
- Bookings.
- Users.
- Dashboard.

---

## 9. Khi lỗi thì kiểm tra theo thứ tự

### Backend không kết nối DB

Kiểm tra:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
PostgreSQL có đang chạy không
Port 5432 có bị app khác dùng không
```

### Frontend báo Network Error

Kiểm tra:

```text
Backend có chạy port 8080 không
VITE_API_URL có đúng không
FRONTEND_URL của backend có đúng http://localhost:5173 không
```

### Login được nhưng API trả 401/403

Kiểm tra:

- token `cineverse_token` trong Local Storage;
- role CUSTOMER/ADMIN;
- JWT secret không bị đổi giữa lúc server đang chạy.

### Database demo muốn làm lại từ đầu

Local PostgreSQL: drop/recreate database.

Docker:

```bash
docker compose down -v
docker compose up --build
```

---

## 10. File nên đọc trước khi sửa core logic

```text
docs/BUSINESS_RULES.md
docs/ARCHITECTURE.md
docs/CODE_STYLE.md
docs/adr/0001-modular-monolith.md
docs/adr/0002-seat-concurrency.md
```

Đặc biệt không sửa Seat Hold theo kiểu bỏ database lock chỉ để code ngắn hơn.

---

## 11. Quy tắc dùng AI

- Tên code bằng tiếng Anh.
- Nội dung UI bằng tiếng Việt.
- Đọc và hiểu code trước khi commit.
- Trước feature mới phải biết actor, input, output, business rule.
- Không đổi kiến trúc chỉ để “chạy được”.
- Comment tiếng Việt được nhưng ngắn, chỉ note lý do quan trọng.
- Không đưa secret thật vào GitHub.
---

## 12. Kiểm tra toàn bộ project trước khi commit/tag

Linux/macOS/Git Bash:

```bash
./scripts/verify-local.sh
```

Windows PowerShell:

```powershell
./scripts/verify-local.ps1
```

Script chạy lần lượt backend verify, frontend check và kiểm tra cấu hình Docker Compose.

> Repo mẫu chưa kèm `package-lock.json` vì môi trường tạo project không tải được npm registry. Lần đầu chạy `npm install` trên máy cá nhân sẽ sinh lock file; nên commit `package-lock.json` vào GitHub để các lần build sau tái lập chính xác dependency. Khi đã có lock file, có thể đổi CI/Docker từ `npm install` sang `npm ci`.

