# V1 Validation Notes

## Đã kiểm tra trong môi trường tạo project

- TypeScript/TSX parse thành công bằng `tsc --noCheck` trên toàn bộ `frontend/src`.
- `package.json` và các `tsconfig` đọc được hợp lệ.
- `pom.xml` đọc được hợp lệ.
- `application.yml`, `docker-compose.yml` và GitHub Actions workflow đọc được hợp lệ.
- Java source + test source không có lỗi cú pháp/parser; lỗi `javac` còn lại trong môi trường tạo project là do không có dependency Spring/JPA/JUnit trên classpath.
- Không còn wildcard import Java.
- Không còn tab trong Java source và các control statement quan trọng đều dùng braces.
- Các luồng API frontend V1 đã được đối chiếu với controller backend tương ứng.
- Đã bổ sung test cho pricing, seat hold, showtime conflict và mock payment state transition.
- Đã loại file build cache `*.tsbuildinfo` khỏi bản đóng gói.

## Chưa thể chạy end-to-end trong môi trường tạo project

Môi trường hiện tại có Java và Node nhưng không có Maven/Docker, đồng thời không thể hoàn tất tải dependency npm/Maven từ registry. Vì vậy chưa thể tuyên bố các lệnh dưới đây đã PASS trong môi trường tạo project:

```bash
mvn verify
npm install && npm run check
docker compose up --build
```

## Bắt buộc chạy trên máy cá nhân trước khi gắn tag `v1.0.0`

Linux/macOS/Git Bash:

```bash
./scripts/verify-local.sh
```

Windows PowerShell:

```powershell
./scripts/verify-local.ps1
```

Sau đó:

```bash
docker compose up --build
```

Test tay theo `docs/TEST_PLAN.md`. Nếu có lỗi, sửa V1 trước khi thêm feature V2.

## RC4 - lỗi compile đã sửa

Lần chạy `mvn verify` thực tế trên Windows của RC3 đã xác nhận Maven/JDK hoạt động và phát hiện 2 lỗi compile trong source:

- `BookingService`: biến local bị gán lại nhưng được tham chiếu trong lambda.
- `UserController`: gọi nhầm `userService.update(...)` thay vì contract `updateProfile(...)`.

RC4 sửa đúng hai lỗi này. Cần chạy lại `mvn verify` trên máy local để xác nhận compile/test/checkstyle tiếp theo.


RC5.1: fixed DataInitializer repository method mismatch reported by local Maven compile.
