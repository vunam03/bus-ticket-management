# Module 2 — Tài khoản và phân quyền

Người thực hiện: Bùi Lê Minh
Nhánh: bui-le-minh

## Phạm vi

- Đăng ký CUSTOMER; username/email không phân biệt hoa thường, kiểm tra trùng.
- Đăng nhập Spring Security, lưu Authentication trong HTTP session; đăng xuất hủy session.
- Phân quyền ADMIN cho /admin/**; hồ sơ cá nhân yêu cầu đăng nhập.
- Xem và sửa họ tên, email, điện thoại, địa chỉ; xác nhận mật khẩu hiện tại.
- Đổi mật khẩu, kiểm tra mật khẩu cũ, mật khẩu mạnh và xác nhận.
- Quên/reset mật khẩu: token 256 bit, lưu SHA-256, hạn 15 phút, dùng một lần.
- Đổi/reset mật khẩu thu hồi các phiên cũ ở lần truy cập tiếp theo.
- Đổi email thu hồi link reset đã gửi đến email cũ.
- Khóa tài khoản hoặc đổi role khiến phiên cũ phải đăng nhập lại.
- Bootstrap 5, responsive, tiếng Việt, hiện/ẩn mật khẩu và JavaScript validation.
- CSRF, giới hạn tần suất gửi form theo IP, tạo ADMIN qua biến môi trường.

## Chạy nhanh trong IntelliJ IDEA

1. Mở project và Reload Maven.
2. Chọn Project SDK là JDK 17 trở lên. Source biên dịch với Java release 17.
3. Mở Run → Edit Configurations → BusTicketManagementApplication.
4. Đặt Active profiles là local.
   Nếu không có ô này, thêm Program arguments: --spring.profiles.active=local.
5. Run application.
6. Mở http://localhost:8080/auth/register, tạo CUSTOMER rồi đăng nhập.

Profile local dùng H2 tại work/local-db/, không cần MySQL.
Dữ liệu vẫn còn sau khi tắt app. Hộp thư demo ở work/mail-outbox/.
Local chỉ lắng nghe 127.0.0.1; H2 console tắt.
Nếu đổi port bằng --server.port=8081, đặt APP_BASE_URL=http://localhost:8081.

## Tạo ADMIN đầu tiên

Trong Environment variables của Run Configuration, khai báo đủ ba biến:

| Biến | Nội dung |
| --- | --- |
| ADMIN_USERNAME | 4–30 chữ cái, số hoặc dấu gạch dưới |
| ADMIN_EMAIL | Email hợp lệ, chưa thuộc tài khoản khác |
| ADMIN_PASSWORD | Mật khẩu mạnh theo chính sách bên dưới |

Chạy ứng dụng để tạo ADMIN, sau đó có thể gỡ ba biến khỏi cấu hình.
Khởi động lại không đổi mật khẩu của ADMIN đã tồn tại.
Nếu username thuộc CUSTOMER hoặc email thuộc tài khoản khác, ứng dụng báo lỗi
thay vì tự nâng quyền hoặc ghi đè mật khẩu.
Không đăng ký CUSTOMER bằng username dự định dùng cho ADMIN trước bước này.

## Demo quên mật khẩu trên máy

1. Run với profile local, đăng ký email hợp lệ.
2. Mở /auth/forgot-password, nhập email vừa đăng ký.
3. Mở thư mục work/mail-outbox trong IntelliJ hoặc File Explorer.
4. Đọc file .txt mới nhất và mở đường dẫn reset trong đó.
5. Đặt mật khẩu mới, đăng nhập lại.
6. Mở lại link cũ để chứng minh token chỉ dùng được một lần.

Hộp thư file chỉ phục vụ demo local, không có endpoint công khai để đọc thư.
Xin link mới thì link cũ bị thu hồi.
Email không tồn tại hoặc tài khoản bị khóa không tạo thư nhưng vẫn trả thông báo chung.
Thư demo chứa reset link, không được đưa lên GitHub.

## MySQL và email thật

Bỏ profile local và cấu hình các biến sau:

| Biến | Ý nghĩa |
| --- | --- |
| DB_URL | Mặc định jdbc:mysql://localhost:3306/bus_ticket_db?useSSL=false&serverTimezone=UTC |
| DB_USERNAME | Mặc định root; nên dùng tài khoản riêng khi triển khai |
| DB_PASSWORD | Mật khẩu MySQL trên máy |
| APP_BASE_URL | URL gốc ứng dụng dùng trong link email |
| SPRING_MAIL_HOST | Máy chủ SMTP |
| SPRING_MAIL_PORT | Cổng SMTP, thường 587 |
| SPRING_MAIL_USERNAME | Tài khoản SMTP |
| SPRING_MAIL_PASSWORD | Mật khẩu ứng dụng hoặc SMTP |
| SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH | true |
| SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE | true nếu nhà cung cấp yêu cầu |
| MAIL_FROM | Địa chỉ người gửi |
| SESSION_COOKIE_SECURE | true khi triển khai HTTPS |

Database bus_ticket_db phải được tạo trước.
Mật khẩu MySQL ghi cứng trước đây đã được thay bằng DB_PASSWORD.
Hibernate vẫn dùng ddl-auto=update theo cấu hình gốc; sao lưu dữ liệu trước khi áp dụng
schema mới lên database quan trọng.
Dữ liệu H2 và MySQL là hai bộ riêng, không tự đồng bộ.
Test tự động dùng H2; chưa kiểm tra trực tiếp MySQL hoặc SMTP thật của anh.

URL reset lấy từ cấu hình, không lấy từ Host header.
SMTP timeout 5 giây. Nếu gửi thư thất bại, transaction tạo token rollback.
Response không tiết lộ sự tồn tại của email hay thông tin SMTP.
Session và rate limit nằm trong bộ nhớ một instance.
Nếu chạy nhiều instance, cần kho session/rate limit dùng chung.

## Chính sách mật khẩu

- 8–64 ký tự, tối đa 72 byte UTF-8 để phù hợp BCrypt.
- Có chữ hoa, chữ thường, số và ký tự đặc biệt; không có khoảng trắng.
- Mật khẩu xác nhận phải khớp.
- Mật khẩu mới phải khác mật khẩu hiện tại.
- BCrypt strength 12; không lưu mật khẩu thô.
- Không hiển thị lại giá trị mật khẩu trên form bị lỗi.

## Đường dẫn

| Method | URL | Quyền | Chức năng |
| --- | --- | --- | --- |
| GET | / | Công khai | Trang chủ |
| GET, POST | /auth/register | Công khai | Đăng ký |
| GET, POST | /auth/login | Công khai | Đăng nhập |
| POST | /auth/logout | Có CSRF | Đăng xuất |
| GET, POST | /auth/forgot-password | Công khai | Xin link reset |
| GET, POST | /auth/reset-password | Công khai | Đặt mật khẩu mới |
| GET | /auth/access-denied | Công khai | Trang 403 |
| GET, POST | /user/profile | Đăng nhập | Hồ sơ |
| GET, POST | /user/change-password | Đăng nhập | Đổi mật khẩu |
| GET | /admin/account | ADMIN | Trang xác nhận quyền |

POST /auth/login do Spring Security xử lý, không phải controller.
Các POST cần CSRF; Thymeleaf tự thêm hidden token khi dùng th:action.
Các form POST /admin/** của module khác cũng cần th:action hoặc hidden CSRF token.
Các đường dẫn ngoài /user/** và /admin/** đang công khai để các module khác tích hợp;
module đặt vé phải bổ sung xác thực cho các URL nghiệp vụ riêng.

## Cấu trúc code và tích hợp

- model: User, Role, PasswordResetToken.
- dto: RegisterRequest, LoginRequest, UpdateProfileRequest, ChangePasswordRequest,
  ForgotPasswordRequest, ResetPasswordRequest, UserProfileResponse.
- repository: UserRepository, RoleRepository, PasswordResetTokenRepository.
- service: UserService, AuthService, CustomUserDetailsService, các dịch vụ gửi email.
- controller: AuthController, UserController, AccountHomeController.
- config: SecurityConfig, AccountInitializer.
- security: AccountPrincipal, CredentialVersionFilter, AuthRateLimitFilter.
- validator: StrongPassword và StrongPasswordValidator.
- exception: trùng tài khoản, sai mật khẩu, token và lỗi nghiệp vụ.
- templates/auth, templates/user, templates/fragments/account.html.
- static/css/account.css và static/js/account.js.

Các module vé có thể tham chiếu User bằng user_id.
Lấy người dùng hiện tại qua Authentication.getName(); không tin userId từ form
để xác định chủ tài khoản.
Không trả trực tiếp entity User ra JSON vì entity chứa password hash.
Dùng UserProfileResponse để hiển thị thông tin an toàn.
Đổi role hoặc enabled được kiểm tra lại ở request tiếp theo.

AccountHomeController chỉ xử lý /admin/account. Module 2 không khai báo mapping GET /.
index.html đã được trả về bản gốc để Module 3 phụ trách.
Module 2 không triển khai CRUD chuyến xe, nhà xe hoặc đặt vé.

## Kiểm thử và trình bày bài

~~~powershell
.\mvnw.cmd -B verify
~~~

Terminal cần JAVA_HOME tới JDK.
Máy hiện tại có JDK đi kèm IntelliJ tại D:\IntelliJ IDEA 2026.2.4\jbr;
có thể đặt JAVA_HOME cho riêng phiên terminal, không cần thay đổi toàn máy.

Test bao gồm:
- Spring context chạy độc lập bằng profile test.
- Chính sách mật khẩu, giới hạn ký tự và byte UTF-8.
- Đăng ký, unique username/email, chống tự cấp ADMIN qua form.
- CSRF, đăng nhập, đăng xuất, quyền, thu hồi session.
- Hồ sơ cá nhân, đổi mật khẩu, xác nhận mật khẩu hiện tại.
- Reset token hash/hết hạn/dùng lại, cấp mới và hai request đồng thời.
- Đổi email, trạng thái tài khoản, giới hạn tần suất và lỗi gửi email.
- Khởi tạo ADMIN lặp lại an toàn và ngăn nâng quyền CUSTOMER đã tồn tại.

Khi bảo vệ, trình bày DTO validation → controller → service → repository → entity.
Giải thích BCrypt dùng cho mật khẩu, còn SHA-256 dùng cho token ngẫu nhiên.
Giao diện dùng th:text để escape dữ liệu người dùng.

## Git theo luật nhóm

- Nhánh cá nhân bui-le-minh; cập nhật main rồi merge khi nhóm có thay đổi.
- Commit nhỏ, rõ nội dung; không commit .idea, target, work, .class hoặc mật khẩu.
- Sau khi anh kiểm tra, push nhánh cá nhân và tạo Pull Request.
- Trưởng nhóm review và merge; không tự merge vào main.

Tài liệu kỹ thuật:
[Spring Security — DaoAuthenticationProvider](https://www.springframework.org/spring-security/reference/servlet/authentication/passwords/dao-authentication-provider.html).


## Mở rộng riêng Module 2

Không thêm quản lý khách hàng ADMIN, dashboard, doanh thu, chuyến xe, ghế hoặc vé.
Các tính năng dưới đây chỉ quản lý xác thực của chính chủ tài khoản.

### Lịch sử bảo mật cá nhân

- Đường dẫn GET /user/security/history, truy cập từ hồ sơ cá nhân.
- Ghi nhận đăng ký, đăng nhập thành công/thất bại, tạm chặn đăng nhập, đăng xuất.
- Ghi nhận cập nhật hồ sơ, đổi/reset mật khẩu và thu hồi phiên.
- Lọc theo loại hoạt động; phân trang 15 bản ghi; sắp xếp mới nhất trước.
- Thời gian được định dạng theo Asia/Ho_Chi_Minh.
- Chủ tài khoản chỉ truy vấn được lịch sử theo ID lấy từ principal đăng nhập.
- Không nhận userId từ URL/form để chọn tài khoản.
- Không lưu mật khẩu, hash mật khẩu, token reset hoặc JSESSIONID trong nhật ký.
- Chỉ lưu loại hoạt động, thời điểm, địa chỉ IP và User-Agent rút gọn.
- Dữ liệu nhập từ trình duyệt được escape khi hiển thị.
- Hoạt động cập nhật thất bại không bị ghi thành cập nhật thành công.
- Giữ lịch sử 90 ngày; dọn hàng ngày lúc 03:15 theo giờ Việt Nam.

### Quản lý phiên đăng nhập

- GET /user/security/sessions: xem các phiên của mình và nhận biết phiên hiện tại.
- POST /user/security/sessions/{id}/revoke: thu hồi một phiên thuộc tài khoản.
- POST /user/security/sessions/revoke-others: giữ phiên hiện tại, thu hồi các phiên khác.
- POST /user/security/sessions/revoke-all: thu hồi tất cả, đăng xuất cả phiên hiện tại.
- Các thao tác POST có CSRF và hộp xác nhận trên giao diện.
- UUID dùng để quản lý phiên là mã độc lập, không phải JSESSIONID.
- Database chỉ lưu SHA-256 của JSESSIONID, không lưu cookie có thể dùng để đăng nhập.
- Truy vấn thu hồi luôn kèm userId của chủ tài khoản để chống truy cập chéo.
- Phiên bị thu hồi bị chặn ở request tiếp theo, không cần chờ hết hạn cookie.
- Mỗi request xác thực cập nhật lastSeen/expiresAt mà không ghi đè trạng thái đã thu hồi.
- Phiên hết hạn không được gia hạn lại bằng một request đến muộn.
- Khi đổi/reset mật khẩu, credentialVersion làm các phiên cũ mất hiệu lực.
- Phiên HttpSession thật vẫn thuộc bộ nhớ một instance; thông tin quản lý được lưu database.
- Khởi động lại ứng dụng yêu cầu đăng nhập lại; bản ghi cũ hết hạn được dọn tự động.

### Chống dò mật khẩu theo tài khoản

- Nhập sai 5 lần trong cửa sổ 15 phút thì tạm chặn đăng nhập 15 phút.
- Giữ nguyên enabled của User: khóa tạm đăng nhập khác với khóa tài khoản.
- Đổi IP không bỏ qua được chặn theo username.
- Chuẩn hóa tên đăng nhập để tránh tách bộ đếm bằng chữ hoa/thường.
- Tên không tồn tại cũng có cùng chính sách chặn và thông báo.
- Chỉ lưu digest tên đăng nhập trong bảng bộ đếm.
- Lưu database để khởi động lại không làm mất khóa tạm.
- Trang đăng nhập có bộ đếm thời gian thử lại; máy chủ mới là nơi kiểm tra khóa.
- Đăng nhập thành công xóa số lần sai trước đó.
- Reset mật khẩu bằng link hợp lệ gỡ khóa tạm.
- Giới hạn theo IP của bản trước vẫn hoạt động song song.
- Striped lock trong tiến trình tuần tự hóa thử đăng nhập cùng username.
  Khi chuyển sang nhiều instance, cần khóa phân tán cho bộ đếm này.

### Bảng và file bổ sung

- account_security_events: SecurityEvent, SecurityEventType, SecurityEventRepository.
- account_sessions: AccountSession, AccountSessionRepository.
- account_login_attempts: LoginAttempt, LoginAttemptRepository.
- SecurityAuditService, AccountSessionService, LoginAttemptService.
- GuardedAuthenticationProvider, AccountAuthenticationHandlers.
- AccountSecurityController, AccountMaintenanceConfig.
- SecurityEventResponse, AccountSessionResponse.
- user/security-history.html, user/sessions.html.

Bảng mới không thay đổi schema nghiệp vụ của BusTrip, BusCompany, Seat, Ticket
hoặc PaymentTransaction. index.html giữ nguyên bản gốc.
