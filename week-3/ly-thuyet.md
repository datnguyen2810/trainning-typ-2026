# Tuần 3: Authentication & Authorization

## 1. Authentication và Authorization

### 1.1. Authentication — Xác thực

Authentication là quá trình kiểm tra danh tính của người truy cập: **người dùng là ai?** Ví dụ, khi đăng nhập bằng username và password, server kiểm tra thông tin này trước khi cấp thông tin xác thực cho các request tiếp theo.

Trong dự án, server tìm tài khoản theo username, kiểm tra mật khẩu bằng BCrypt và kiểm tra trạng thái tài khoản. Nếu hợp lệ, server cấp access token và refresh token.

### 1.2. Authorization — Phân quyền

Authorization là quá trình kiểm tra quyền thực hiện hành động trên tài nguyên: **người dùng được phép làm gì?** Người dùng đã đăng nhập vẫn có thể không được phép truy cập chức năng quản trị.

Ví dụ: khách hàng được xem thông tin cá nhân, còn quản trị viên được tạo, sửa và xóa danh mục.

### 1.3. Phân biệt

| Tiêu chí | Authentication | Authorization |
|---|---|---|
| Mục đích | Xác định danh tính | Xác định quyền truy cập |
| Căn cứ | Mật khẩu, token hoặc thông tin xác thực khác | Role, permission hoặc chính sách truy cập |
| Thứ tự với API protected | Xác thực trước | Kiểm tra quyền sau khi xác thực |
| Lỗi thường gặp | `401 Unauthorized`: thiếu hoặc sai thông tin xác thực | `403 Forbidden`: đã xác thực nhưng không đủ quyền |

Việc ẩn nút trên giao diện chỉ hỗ trợ trải nghiệm; server vẫn phải kiểm tra quyền ở từng API cần bảo vệ.

## 2. Session-based và Token-based Authentication

### 2.1. Session-based Authentication

Server lưu trạng thái đăng nhập trong session. Client thường giữ session ID trong cookie để tham chiếu đến session đó.

Luồng hoạt động:

1. Client gửi thông tin đăng nhập.
2. Server kiểm tra thông tin, tạo session và lưu dữ liệu như user ID, thời hạn đăng nhập.
3. Server gửi session ID qua header `Set-Cookie`.
4. Trình duyệt tự gửi cookie trong các request phù hợp tiếp theo.
5. Server tra cứu session để xác thực, sau đó kiểm tra quyền.
6. Khi đăng xuất, server vô hiệu hóa session và yêu cầu xóa cookie.

Session có thể được lưu trong bộ nhớ, database hoặc Redis. Khi chạy nhiều instance, cần có cơ chế chia sẻ session hoặc điều phối request phù hợp. Cookie nên được cấu hình `HttpOnly`, `Secure` và `SameSite` phù hợp; xác thực dựa trên cookie cần xử lý nguy cơ CSRF. 

### 2.2. Token-based Authentication

Server cấp token sau khi đăng nhập thành công. Client giữ token và gửi kèm request, thường qua header:

```http
Authorization: Bearer <access_token>
```

Luồng hoạt động với JWT:

1. Client gửi thông tin đăng nhập.
2. Server xác thực, tạo JWT có thời hạn và trả về client.
3. Client gửi JWT khi gọi API protected.
4. Server kiểm tra chữ ký, thời hạn và các claim cần thiết.
5. Server xác định danh tính, quyền và xử lý request nếu được phép.
6. Khi access token hết hạn, client dùng refresh token để xin access token mới hoặc đăng nhập lại.

Với JWT tự chứa thông tin, server không cần lưu session đăng nhập để xác minh mỗi access token. Đây là ý nghĩa **stateless ở bước xác thực access token**, không có nghĩa toàn bộ ứng dụng không lưu trạng thái. Dự án vẫn lưu user, role và refresh token trong database.

Token-based không bắt buộc sử dụng JWT; token cũng có thể là chuỗi ngẫu nhiên cần được server tra cứu. Cookie cũng có thể chứa token, nên cookie và token không phải hai khái niệm loại trừ nhau.

### 2.3. So sánh và lựa chọn

| Tiêu chí | Session-based | Token-based với JWT |
|---|---|---|
| Client giữ | Session ID, thường trong cookie | Access token |
| Server xác thực | Tra cứu session | Xác minh JWT |
| Trạng thái đăng nhập | Lưu tại server | Thông tin nằm trong access token |
| Thu hồi đăng nhập | Có thể xóa session ngay | Cần thêm cơ chế thu hồi hoặc chờ token hết hạn |
| Mở rộng nhiều instance | Cần chia sẻ hoặc điều phối session | Các instance có thể cùng xác minh token nếu được cấp khóa phù hợp |
| Trường hợp phù hợp | Website truyền thống, frontend và backend cùng hệ thống | REST API phục vụ web, mobile hoặc nhiều dịch vụ |

Chọn session khi cần quản lý phiên tập trung và thu hồi đơn giản. Chọn JWT khi nhiều loại client cần gọi API và muốn xác thực access token mà không tra cứu session. Cả hai vẫn cần HTTPS và bảo vệ thông tin xác thực ở client.

## 3. JWT — JSON Web Token

### 3.1. Khái niệm và cấu trúc

JWT là định dạng biểu diễn các thông tin gọi là **claim** dưới dạng JSON. Trong dự án, access token sử dụng JWT được bảo vệ bằng HS256, gồm ba phần phân cách bởi dấu chấm:

```text
Base64Url(header).Base64Url(payload).Base64Url(signature)
```

- **Header:** mô tả thuật toán, ví dụ `alg: HS256`.
- **Payload:** chứa claim như user ID, thời hạn và role.
- **Signature:** giúp kiểm tra tính toàn vẹn và xác minh token bằng khóa phù hợp.

HS256 sử dụng HMAC-SHA256 với khóa bí mật dùng chung giữa bên tạo và bên kiểm tra token. Base64Url chỉ là cách biểu diễn dữ liệu, không phải mã hóa để giữ bí mật. JWT trong dự án không che giấu nội dung payload.

### 3.2. Các claim trong dự án

| Claim | Ý nghĩa |
|---|---|
| `iss` | Bên phát hành token: `backend-typ-api` |
| `aud` | Đối tượng được chỉ định sử dụng token: `backend-typ-client` |
| `sub` | Định danh người dùng, sử dụng UUID của user |
| `iat` | Thời điểm phát hành |
| `nbf` | Thời điểm token bắt đầu có hiệu lực |
| `exp` | Thời điểm hết hạn |
| `jti` | Định danh token |
| `roles` | Danh sách vai trò, ví dụ `CUSTOMER`, `ADMIN` |
| `permissions` | Danh sách quyền chi tiết của các vai trò |

Server phải xác minh token trước khi tin các claim. Chỉ giải mã payload để đọc dữ liệu không đủ để xác thực.

### 3.3. Lợi ích

- Truyền thông tin xác thực bằng định dạng gọn, thuận tiện cho HTTP API.
- Xác minh access token mà không cần lưu session tương ứng tại server.
- Chứa định danh và role để hỗ trợ kiểm tra quyền.
- Có thời hạn rõ ràng, phù hợp với access token ngắn hạn.

### 3.4. Hạn chế và rủi ro bảo mật

| Rủi ro | Cách xử lý |
|---|---|
| Token bị đánh cắp có thể bị sử dụng lại | Dùng HTTPS, bảo vệ nơi lưu token, không đưa token vào URL hoặc log |
| Payload có thể đọc được | Không đưa mật khẩu, secret hoặc dữ liệu nhạy cảm vào payload |
| Khóa ký bị lộ cho phép giả mạo token | Giữ khóa ở biến môi trường hoặc hệ thống quản lý secret; thay khóa khi bị lộ |
| Token còn hạn sau khi đăng xuất hoặc đổi role | Dùng access token ngắn hạn; thêm kiểm tra trạng thái hoặc cơ chế thu hồi nếu cần hiệu lực tức thì |
| Chấp nhận token không đúng mục đích | Cố định thuật toán được phép, kiểm tra chữ ký, thời hạn, issuer và audience |
| XSS hoặc CSRF tùy cách lưu và gửi token | Chống XSS; nếu gửi bằng cookie thì cấu hình cookie và chống CSRF phù hợp |

Trong dự án, `JwtConfig` giới hạn thuật toán HS256, kiểm tra issuer, audience và hiệu lực thời gian. Access token có thời hạn 15 phút. Role trong token là thông tin tại thời điểm cấp token, không tự cập nhật theo database.

## 4. Password Hashing

### 4.1. Vì sao không lưu mật khẩu dạng plain text?

Nếu database hoặc bản sao lưu bị lộ, mật khẩu plain text bị đọc trực tiếp. Kẻ tấn công có thể đăng nhập vào hệ thống và thử mật khẩu đó trên các dịch vụ khác của người dùng.

Server chỉ cần kiểm tra mật khẩu đúng hay sai, không cần khôi phục mật khẩu gốc. Vì vậy, cần lưu password hash bằng thuật toán chuyên dụng thay vì lưu plain text hoặc mã hóa có thể giải mã.

### 4.2. Vì sao không dùng MD5 hoặc SHA trực tiếp?

MD5 và SHA được thiết kế để tính toán nhanh, nên không phù hợp khi dùng trực tiếp để lưu mật khẩu:

- **Brute force:** thử nhiều tổ hợp mật khẩu, băm từng tổ hợp rồi so sánh. Hàm băm nhanh làm tăng số lần thử mỗi giây.
- **Dictionary attack:** thử các mật khẩu phổ biến hoặc danh sách mật khẩu đã lộ.
- **Rainbow table:** dùng dữ liệu băm được tính trước để tìm mật khẩu, đặc biệt hiệu quả khi không có salt.

Thêm salt vào SHA giúp chống việc dùng bảng tính trước chung, nhưng không giải quyết được vấn đề tốc độ thử mật khẩu quá cao.

### 4.3. Salt, cost factor, BCrypt và Argon2

**Salt** là dữ liệu ngẫu nhiên riêng cho mỗi mật khẩu, giúp hai người dùng có cùng mật khẩu vẫn có hash khác nhau. Salt không cần giữ bí mật và được lưu cùng thông tin hash.

**Cost factor / work factor** điều chỉnh tài nguyên cần để tính hash. Tăng cost làm mỗi lần đoán mật khẩu tốn hơn, nhưng cũng tăng thời gian đăng nhập hợp lệ; cần chọn theo năng lực server.

| Thuật toán | Đặc điểm |
|---|---|
| BCrypt | Tự quản lý salt; cost dạng lũy thừa; giới hạn đầu vào 72 byte |
| Argon2id | Có thể điều chỉnh bộ nhớ, số vòng và độ song song; tăng chi phí tấn công bằng phần cứng |

OWASP ưu tiên Argon2id cho hệ thống mới. Khi dùng BCrypt, cần chú ý work factor và giới hạn 72 byte.

### 4.4. Cách áp dụng trong dự án

1. Đăng ký: gọi `passwordEncoder.encode(rawPassword)` và lưu kết quả vào `users.password_hash`.
2. Đăng nhập: gọi `passwordEncoder.matches(rawPassword, storedHash)` để kiểm tra.
3. Không trả password hash trong response và không ghi mật khẩu vào log.

`BCryptPasswordEncoder` hiện dùng strength mặc định là 10. Request đăng ký giới hạn mật khẩu 6–72 ký tự ASCII, nên không vượt giới hạn 72 byte của BCrypt. Không băm lại rồi so sánh hai chuỗi hash vì salt ngẫu nhiên có thể làm kết quả khác nhau.

## 5. Áp dụng vào phần thực hành

### 5.1. Đăng ký và đăng nhập

**Đăng ký — `POST /api/v1/auth/register`:** kiểm tra dữ liệu, chuẩn hóa username/email, kiểm tra trùng, hash mật khẩu bằng BCrypt, gán role mặc định `CUSTOMER`, lưu user và trả thông tin tài khoản với HTTP 201.

**Đăng nhập — `POST /api/v1/auth/login`:** tìm user, kiểm tra mật khẩu và trạng thái `ACTIVE`, sau đó trả JWT access token, refresh token, loại token `Bearer`, thời hạn access token và thông tin user. Sai thông tin đăng nhập trả HTTP 401.

Luồng hiện tại tách hai bước: đăng ký tạo tài khoản, đăng nhập cấp token. Client không được tự chọn role `ADMIN` khi đăng ký.

### 5.2. Filter kiểm tra JWT cho API protected

Dự án sử dụng filter có sẵn của Spring Security OAuth2 Resource Server thông qua `SecurityFilterChain`:

1. Nhận bearer token từ header `Authorization`.
2. `JwtDecoder` kiểm tra token.
3. `JwtAuthenticationConverter` chuyển role và permission thành authority.
4. Nếu hợp lệ, Spring Security thiết lập thông tin xác thực trong security context.
5. Kiểm tra quy tắc truy cập trước khi chuyển request tới controller.

Ví dụ, `GET /api/v1/users/me` lấy user ID từ claim `sub` của JWT đã được xác minh. Thiếu token, token sai hoặc hết hạn trả HTTP 401.

### 5.3. Role-based Access Control — RBAC

RBAC gán quyền thông qua vai trò thay vì cấu hình riêng từng người dùng. Dự án có các bảng `users`, `roles`, `permissions`, `user_roles` và `role_permissions`.

| Nhóm API | Quyền truy cập hiện tại |
|---|---|
| Đăng ký, đăng nhập, refresh | Không yêu cầu access token |
| GET danh mục | Công khai |
| `/api/v1/users/me`, `/api/v1/auth/logout` | Yêu cầu xác thực |
| `/api/v1/admin/**` | Yêu cầu role `ADMIN` |

Converter chuyển claim `ADMIN` thành authority `ROLE_ADMIN`, tương ứng với `hasRole("ADMIN")`. User có role `CUSTOMER` gọi API admin sẽ nhận HTTP 403. Các permission đã được đưa vào token; quy tắc bảo vệ nhóm API admin hiện dựa trên role.

### 5.4. Refresh token: mục đích và cách triển khai

Access token ngắn hạn giảm thời gian sử dụng nếu bị lộ. Refresh token cho phép duy trì đăng nhập mà không yêu cầu người dùng nhập lại mật khẩu sau mỗi lần access token hết hạn.

| Tiêu chí | Access token | Refresh token trong dự án |
|---|---|---|
| Mục đích | Gọi API protected | Xin access token mới |
| Định dạng | JWT | Chuỗi ngẫu nhiên, không phải JWT |
| Thời hạn | 15 phút | 7 ngày tính từ lúc đăng nhập |
| Server lưu | Không lưu từng access token | Lưu SHA-256 hash và trạng thái trong database |
| Thu hồi | Chưa có cơ chế thu hồi trực tiếp | Thu hồi theo token family |

Luồng refresh tại `POST /api/v1/auth/refresh`:

1. Client gửi refresh token.
2. Server băm token bằng SHA-256 và tìm bản ghi tương ứng.
3. Khóa bản ghi user để tuần tự hóa thao tác refresh/logout của cùng user.
4. Kiểm tra token chưa hết hạn, chưa bị thu hồi và user còn `ACTIVE`.
5. Tạo refresh token mới, lưu hash, đánh dấu token cũ đã bị thay thế.
6. Cấp access token mới và trả cả hai token; client thay refresh token đang giữ bằng token mới.

Đây là **refresh token rotation**. Các token kế tiếp cùng lần đăng nhập thuộc một `family_id`; token mới giữ nguyên hạn hết hiệu lực ban đầu của family. Nếu token đã rotate bị dùng lại, server thu hồi cả family và trả HTTP 401. `noRollbackFor` giúp thao tác thu hồi vẫn được commit khi ném `InvalidRefreshTokenException`.

SHA-256 phù hợp ở đây vì refresh token được tạo từ 32 byte ngẫu nhiên bằng `SecureRandom`, có độ khó đoán cao; khác với mật khẩu do con người đặt.

Khi đăng xuất, client gửi access token và refresh token tới `POST /api/v1/auth/logout`. Server kiểm tra token thuộc user hiện tại rồi thu hồi family tương ứng. Access token đã cấp vẫn có thể dùng tới khi hết hạn; đăng xuất hiện chưa thu hồi access token ngay lập tức.

Tài khoản demo admin cần được gán role `ADMIN` bằng bước chuẩn bị dữ liệu riêng, sau đó đăng nhập để lấy token. Liên kết demo được lưu tại [LinkDemo.md](./LinkDemo.md).
