# Kiến trúc CineVerse

## Kiểu kiến trúc

**Modular Monolith + Layered Architecture**.

Một backend duy nhất, chia module theo nghiệp vụ. Trong mỗi module:

```text
Controller -> Service -> Repository -> PostgreSQL
```

- **Controller**: nhận/trả HTTP.
- **Service**: business rules và transaction.
- **Repository**: truy cập dữ liệu.
- **Entity**: ánh xạ database.
- **DTO**: request/response của API.
- **Mapper**: chuyển Entity <-> DTO.

## Module chính

- auth
- user
- movie
- cinema
- showtime
- booking
- payment
- ticket
- admin
- common

## Nguyên tắc

- Không viết SQL trong Controller.
- Không để Repository quyết định business rule.
- Không trả Entity trực tiếp cho frontend.
- Transaction nằm ở Service.
- Các rule có rủi ro race condition phải được bảo vệ ở database/transaction, không chỉ ở frontend.
