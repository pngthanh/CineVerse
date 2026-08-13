# Quy ước phát triển

## Branch
- `main`: bản ổn định.
- `develop`: tích hợp tính năng.
- `feature/<name>`: tính năng mới.
- `fix/<name>`: sửa lỗi.

## Commit
Dùng Conventional Commits:
- `feat: add seat hold flow`
- `fix: prevent duplicate seat booking`
- `test: add pricing service tests`
- `refactor: extract booking mapper`
- `docs: update booking flow`

## Code review checklist
- Tên biến/hàm/class bằng tiếng Anh, có ý nghĩa.
- Controller mỏng, business logic nằm ở Service.
- Repository chỉ truy cập dữ liệu.
- Không trả Entity trực tiếp ra API.
- Validation và error response thống nhất.
- Comment ngắn, chủ yếu giải thích **tại sao**, không kể lại code.
- Logic quan trọng có test.
