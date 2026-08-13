# RC5 Acceptance Checklist

## UI/UX
- Header/footer đồng bộ ở Home, Movies, Detail, Auth và booking flow.
- MovieCard giữ cùng baseline cho tên, metadata và nút hành động.
- Home hiển thị hero nổi bật, Top đánh giá, đang chiếu, sắp chiếu, rạp và trust strip.
- Movie Detail nhúng trailer YouTube trực tiếp.
- Seat map có lối đi trung tâm và vùng VIP rõ ràng.

## Booking recovery
1. Tạo booking và đi tới Payment.
2. Điều hướng về Home.
3. Mở `Vé của tôi`.
4. Booking PENDING phải có `Tiếp tục thanh toán` và `Hủy & trả ghế`.
5. Hủy booking phải trả ghế HELD về AVAILABLE.

## Local verification
```powershell
cd backend
mvn verify

cd ..\frontend
npm install
npm run check
```
