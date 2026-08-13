# Business Rules V1

- **BR-01:** Một phòng không được có hai suất chiếu chồng thời gian.
- **BR-02:** Một ghế của một suất chiếu chỉ có một trạng thái thực tế tại một thời điểm: AVAILABLE, HELD hoặc BOOKED.
- **BR-03:** Chỉ một user được giữ một ghế tại một thời điểm.
- **BR-04:** Seat hold mặc định hết hạn sau 5 phút.
- **BR-05:** Seat hold hết hạn phải được trả về AVAILABLE nếu chưa thanh toán thành công.
- **BR-06:** Chỉ các ghế đang HELD bởi chính user và đúng hold token mới được tạo booking.
- **BR-07:** Booking mới tạo có trạng thái PENDING.
- **BR-08:** Payment SUCCESS chuyển booking thành CONFIRMED, ghế thành BOOKED và sinh Ticket.
- **BR-09:** Payment FAILED hoặc hold hết hạn hủy booking PENDING và giải phóng ghế.
- **BR-10:** Admin không được xem password/password hash.
- **BR-11:** Movie đã liên quan dữ liệu lịch sử nên ưu tiên chuyển INACTIVE thay vì hard delete.
- **BR-12:** Giá vé được tính ở backend. Frontend không được tự quyết định tổng tiền.
