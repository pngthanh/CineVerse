# Quy ước code CineVerse

## 1. Ngôn ngữ

- Tên class, method, biến, file, API và cột database: **tiếng Anh**.
- Nội dung hiển thị cho người dùng: **tiếng Việt**.
- Comment có thể dùng tiếng Việt nhưng phải ngắn và chỉ giải thích ý quan trọng.

## 2. Backend Java

- Package theo module nghiệp vụ: `movie`, `cinema`, `showtime`, `booking`, `payment`, `user`.
- Luồng chuẩn: `Controller -> Service -> Repository -> Database`.
- Controller chỉ nhận request, validation và trả response.
- Business rule nằm trong Service.
- Repository chỉ phụ trách truy cập dữ liệu.
- Không trả JPA Entity trực tiếp ra API; dùng DTO.
- Dùng constructor injection, không field injection.
- Dùng enum thay magic string cho trạng thái.
- Dùng `@Transactional` ở nghiệp vụ cần tính nguyên tử.
- Dùng database lock cho các đoạn có tranh chấp dữ liệu như seat hold/payment.
- Không log password, JWT secret hoặc dữ liệu nhạy cảm.

## 3. Frontend React + TypeScript

- Component và type dùng PascalCase.
- Hàm/biến dùng camelCase.
- Page đặt trong `pages`, component dùng chung đặt trong `components`.
- Logic dùng lại đặt trong `hooks` hoặc `lib`.
- Không gọi `fetch` trực tiếp rải rác; dùng `lib/api.ts`.
- Không tự tính giá vé từ dữ liệu cố định ở frontend; giá phải nhận từ backend.
- Header/Footer/Admin layout là component dùng chung.

## 4. Comment

Comment giải thích **tại sao**, không lặp lại code.

Tốt:

```java
// Khóa booking để hai request thanh toán không cùng xác nhận một đơn.
Booking booking = bookings.requireForUpdate(id);
```

Không cần:

```java
// Gán trạng thái thành công.
payment.setStatus(PaymentStatus.SUCCESS);
```

## 5. Git

Ví dụ commit:

```text
feat: implement seat hold flow
fix: prevent duplicate booking payment
refactor: move pricing logic to backend
 test: add seat pricing tests
docs: update local setup guide
```

Không dùng commit như `fix`, `update`, `final2`.
