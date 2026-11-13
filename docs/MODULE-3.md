# Module 3 — Tìm kiếm và bộ lọc chuyến xe nâng cao

Người thực hiện: Đỗ Hữu Vũ
Nhánh: feature/search-trip

Module 3 là phía KHÁCH HÀNG (tìm chuyến để đặt vé). Tìm kiếm của Module 1 (`BusTripSpecification`, `/admin/trips`)
là phía QUẢN TRỊ nên hai phần độc lập, không dùng chung code.

## Phạm vi

- Trang chủ có khung tìm kiếm: điểm đi, điểm đến, ngày đi, loại xe, khung giờ; nút đổi chiều; chọn nhanh Hôm nay/Ngày mai; gợi ý địa điểm lấy từ dữ liệu.
- Trang kết quả (`/search`) có sidebar lọc: khoảng giá, loại xe (Giường nằm, Limousine, Ghế ngồi), khung giờ (Sáng, Trưa, Tối), đánh giá nhà xe.
- Lọc, sắp xếp, phân trang bằng AJAX (`/search/filter`), không tải lại trang; URL tự cập nhật nên F5 vẫn giữ bộ lọc.
- Sắp xếp: giá thấp đến cao, giờ đi sớm nhất, đánh giá cao nhất (đánh giá của nhà xe).
- Số ghế còn trống thật ("Còn 25/45 ghế", cảnh báo sắp hết, nút "Hết chỗ") lấy từ bảng `bus_seats` của Module 4.
- Nút "Chọn chuyến" dẫn tới trang chọn ghế của Module 4: `/booking/trips/{id}/seats`.
- Dải giá thấp nhất theo ngày (Native SQL), số lượng cạnh từng ô lọc, nhãn "Giá tốt nhất", "Khởi hành sớm nhất".
- Tuyến phổ biến (`/routes`) và trang chi tiết tuyến (`/routes/detail`).

## Endpoint

| Method | URL | Kết quả |
| --- | --- | --- |
| GET | `/` | Trang chủ |
| GET | `/search` | Trang kết quả, render sẵn ở server |
| GET | `/search/filter` | JSON cho AJAX; lỗi dữ liệu trả 400 kèm `{"error": "..."}` |
| GET | `/search/locations` | JSON gợi ý điểm đi, điểm đến |
| GET | `/search/destinations?origin=` | JSON điểm đến theo điểm đi |
| GET | `/routes?origin=&limit=` | JSON tuyến phổ biến |
| GET | `/routes/detail?origin=&destination=` | Trang chi tiết tuyến |

Tham số của `/search` và `/search/filter`: `origin`, `destination`, `travelDate` (yyyy-MM-dd), `minPrice`, `maxPrice`,
`busTypes` (GIUONG_NAM, LIMOUSINE, GHE_NGOI; lặp được), `timeSlots` (MORNING, NOON, EVENING; lặp được),
`minRating`, `sortBy` (PRICE_ASC, DEPARTURE_ASC, RATING_DESC), `page` (từ 1), `size`.

Tất cả endpoint đều công khai (khách chưa đăng nhập xem được) theo `SecurityConfig` hiện tại.

## Thay đổi trên file của module khác

| File | Thay đổi | Ghi chú |
| --- | --- | --- |
| `repository/BusTripRepository.java` | Chỉ THÊM import và một khối phương thức ghi rõ "PHẦN CỦA MODULE 3" | Hai phương thức của Module 1 (`searchTrips`, `findByBusCompanyId`) giữ nguyên từng dòng. Phương thức tìm kiếm của Module 3 tên `searchByFilters` để không nhầm với `searchTrips` |
| `templates/index.html` | Thay trang giữ chỗ bằng trang chủ có khung tìm kiếm | Theo đề: mở rộng index.html |

`BusTrip`, `BusCompany`, `Seat` và mọi file khác KHÔNG bị sửa.
Module 3 chỉ ĐỌC dữ liệu: `bus_trips`, `bus_companies`, `bus_seats`.

## Quy ước dữ liệu để bộ lọc chạy đúng

- `bus_type` là ô nhập tự do trong form thêm chuyến. Bộ lọc nhận diện không phân biệt hoa/thường và dấu:
  `Giường nằm`, `giuong nam`, `GIUONG_NAM` đều là Giường nằm; tương tự `Limousine` và `Ghế ngồi`.
  Giá trị lạ vẫn hiển thị nguyên văn nhưng không lọc được theo loại xe. Nên thống nhất dùng đúng 3 tên trên.
- `departure_location`, `arrival_location` nên viết thống nhất (ví dụ luôn là `Hà Nội`, không lúc `HN`). Tìm kiếm không phân biệt hoa/thường nhưng phân biệt dấu.
- Thời gian khởi hành là giờ địa phương Việt Nam. Tìm kiếm chỉ trả chuyến chưa khởi hành.
- Đánh giá lấy từ `BusCompany.rating`. Nhà xe chưa có điểm (null) được tính là 0 và vẫn xuất hiện trong kết quả.
- Ghế còn trống = `total_seats` trừ số ghế ở trạng thái HOLD hoặc BOOKED. Module 4 tạo sơ đồ ghế khi có người mở trang chọn ghế,
  nên chuyến chưa có dòng ghế nào được coi là còn đủ chỗ.

## Chạy nhanh trên máy (có dữ liệu mẫu)

1. Mở project, Reload Maven.
2. Run `BusTicketManagementApplication` với Active profiles là `local`.
3. Thêm biến môi trường `APP_SEED_ENABLED=true` trong Run Configuration (hoặc Program arguments `--app.seed.enabled=true`).
4. Console in `[SampleTripDataLoader] Đã nạp ... nhà xe và ... chuyến xe mẫu.`
5. Mở http://localhost:8080/

Dữ liệu mẫu chỉ nạp khi bảng `bus_trips` đang rỗng và mặc định TẮT.
Nếu `work/local-db/` đã có chuyến do bạn nhập thủ công thì loader không nạp thêm; xoá thư mục đó để nạp lại từ đầu.

## Kiểm thử

- Chạy toàn bộ: `./mvnw test` (Windows: `mvnw.cmd test`).
- `FilterServiceTest`, `SearchServiceTest`, `BusTypeTest`, `DateUtilsTest`, `CurrencyFormatterTest`: logic thuần, không cần CSDL.
- `BusTripRepositoryIntegrationTests`: chạy các câu JPQL và Native SQL trên H2 (profile test), gồm cả đếm ghế và nhà xe chưa có rating.
- `SearchPagesIntegrationTests`: từ HTTP qua Spring Security, controller, service, repository, đến Thymeleaf.

## Giới hạn hiện tại

- `BusTrip` chưa có giờ đến và số lượt đánh giá, nên giao diện chưa hiện giờ đến, thời lượng, số lượt đánh giá.
- Chưa lọc theo số khách: số ghế trống thay đổi theo từng lần giữ/đặt ghế nên chỉ hiển thị, không lọc ở câu truy vấn.
