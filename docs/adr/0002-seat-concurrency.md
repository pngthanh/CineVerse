# ADR-0002: Chống đặt trùng ghế

## Vấn đề
Hai request có thể chọn cùng một ghế gần như đồng thời. Nếu chỉ kiểm tra trạng thái ở frontend, cả hai đều có thể tưởng rằng ghế còn trống.

## Quyết định
- Mỗi cặp `showtime + seat` có một bản ghi `ShowtimeSeat` duy nhất.
- Khi giữ/đặt ghế, Service chạy trong transaction.
- Repository lấy các `ShowtimeSeat` bằng **PESSIMISTIC_WRITE lock**.
- Sau khi lock mới kiểm tra AVAILABLE/HELD/BOOKED.
- Hold có `holdToken`, `heldByUserId`, `holdExpiresAt`.
- Hết hạn thì scheduler hoặc request kế tiếp giải phóng ghế.

## Vì sao
Database lock là nguồn bảo vệ cuối cùng, không phụ thuộc tốc độ frontend hay hai request tới cùng lúc.
