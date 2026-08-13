# V1 Test Plan

## 1. Luồng khách hàng bắt buộc

1. Đăng ký tài khoản mới.
2. Đăng nhập và nhận JWT.
3. Xem phim -> suất chiếu -> sơ đồ ghế.
4. Giữ một hoặc nhiều ghế còn trống.
5. Tạo booking từ `holdToken`.
6. Mock payment thành công -> booking `CONFIRMED`, ghế `BOOKED`, sinh ticket.
7. Xem booking trong **Vé của tôi** và trang chi tiết vé.
8. Mock payment thất bại -> booking `CANCELLED`, ghế trở lại `AVAILABLE`.

## 2. Luật đồng thời quan trọng

- Hai request giữ cùng một ghế: chỉ một request được thành công.
- Hold hết hạn: ghế có thể được người khác giữ lại.
- Gửi lại request tạo booking cùng `holdToken`: không sinh booking thứ hai.
- Gửi lại mock payment SUCCESS sau khi đã thành công: trả kết quả cũ, không sinh ticket thứ hai.

## 3. Showtime

- Không cho tạo hai suất chiếu chồng thời gian trong cùng phòng.
- Cho phép cùng thời gian nếu khác phòng.

## 4. Security

- Guest chỉ truy cập API GET công khai.
- Customer không truy cập `/api/admin/**`.
- Admin truy cập được API quản trị.
- API protected không có JWT -> 401.
- User không xem booking của user khác.

## 5. Frontend

- Header/footer customer nhất quán giữa các trang.
- Sidebar admin nhất quán giữa các trang admin.
- Tất cả tiền hiển thị VNĐ.
- Trạng thái/role/loại ghế hiển thị tiếng Việt.
- Empty/loading/error states không làm vỡ layout.
- Responsive kiểm tra tối thiểu 1440px, 1024px và mobile ~390px.

## 6. Trước khi gắn tag V1 stable

```bash
./scripts/verify-local.sh
# hoặc Windows PowerShell
./scripts/verify-local.ps1
```

Sau đó chạy `docker compose up --build` và test tay toàn bộ happy path + payment failed path.
