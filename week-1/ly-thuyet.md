# TUẦN 1: DATABASE VÀ OOP

## Phần 1: Database (Cơ sở dữ liệu quan hệ)

### 1. Cài đặt DBMS

#### DBMS là gì?

DBMS (Database Management System) là phần mềm dùng để tạo, lưu trữ, truy vấn và quản lý cơ sở dữ liệu. Một số DBMS phổ biến là MySQL, PostgreSQL, SQL Server và Oracle.

Project này sử dụng **MySQL** chạy trong Docker.

#### MySQL, Docker và DBeaver

- **MySQL** là database server, nơi dữ liệu thực sự được lưu trữ và xử lý.
- **Docker** tạo môi trường độc lập để chạy MySQL mà không cần cài trực tiếp vào máy.
- **DBeaver** là công cụ giao diện giúp kết nối MySQL, viết SQL và xem dữ liệu.

Luồng kết nối:

```text
DBeaver hoặc ứng dụng Java
          ↓
localhost và port Docker đã map
          ↓
MySQL trong Docker container
```

Database dùng chung cho project là `ticket_booking`.

---

### 2. SQL cơ bản

#### Cơ sở dữ liệu quan hệ

Cơ sở dữ liệu quan hệ lưu dữ liệu dưới dạng bảng:

- **Table**: bảng dữ liệu, ví dụ `users`, `events`.
- **Column**: thuộc tính, ví dụ `email`, `start_at`.
- **Row**: một bản ghi cụ thể.
- **Primary key**: xác định duy nhất một dòng.
- **Foreign key**: liên kết dữ liệu giữa hai bảng.

Ví dụ:

```text
users 1 ─── N bookings
```

Một user có thể có nhiều booking, nhưng một booking chỉ thuộc một user.

#### Các bảng trong project

| Bảng | Ý nghĩa |
|---|---|
| `users` | Người dùng và quản trị viên |
| `venues` | Địa điểm tổ chức |
| `events` | Sự kiện |
| `seats` | Ghế vật lý tại địa điểm |
| `event_seats` | Giá và trạng thái ghế trong từng sự kiện |
| `bookings` | Đơn đặt vé |
| `booking_items` | Các ghế nằm trong đơn đặt vé |
| `payments` | Các lần thanh toán |

#### DDL

DDL (Data Definition Language) dùng để tạo hoặc thay đổi cấu trúc database.

Các lệnh chính:

- `CREATE`: tạo database, table hoặc index.
- `ALTER`: thay đổi cấu trúc bảng.
- `DROP`: xóa database hoặc bảng.

Ví dụ:

```sql
CREATE TABLE categories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL
);

ALTER TABLE categories
ADD COLUMN description TEXT;

DROP TABLE categories;
```

`DROP` là thao tác xóa cấu trúc và dữ liệu nên cần sử dụng cẩn thận.

#### DML

DML (Data Manipulation Language) dùng để thêm, sửa và xóa dữ liệu.

```sql
INSERT INTO users (email, password_hash, full_name)
VALUES ('user@example.com', 'HASH_VALUE', 'Nguyễn Văn A');

UPDATE users
SET full_name = 'Nguyễn Văn B'
WHERE id = 1;

DELETE FROM users
WHERE id = 1;
```

Khi dùng `UPDATE` hoặc `DELETE`, cần kiểm tra kỹ điều kiện `WHERE`. Nếu thiếu `WHERE`, câu lệnh có thể ảnh hưởng toàn bộ bảng.

#### SELECT và WHERE

`SELECT` dùng để đọc dữ liệu. `WHERE` lọc những dòng thỏa mãn điều kiện.

```sql
SELECT id, name, start_at
FROM events
WHERE status = 'PUBLISHED';
```

Một số toán tử thường gặp:

- So sánh: `=`, `<>`, `>`, `>=`, `<`, `<=`.
- Logic: `AND`, `OR`, `NOT`.
- Tập hợp: `IN`, `NOT IN`.
- Khoảng: `BETWEEN`.
- Chuỗi: `LIKE`.
- Giá trị rỗng: `IS NULL`, `IS NOT NULL`.

`NULL` không được so sánh bằng `= NULL`. Cách đúng là:

```sql
WHERE paid_at IS NULL
```

#### JOIN

`JOIN` kết hợp dữ liệu từ nhiều bảng thông qua điều kiện liên kết.

##### INNER JOIN

Chỉ trả về những dòng có dữ liệu khớp ở cả hai bảng.

```sql
SELECT b.booking_code, u.full_name
FROM bookings AS b
INNER JOIN users AS u ON u.id = b.user_id;
```

##### LEFT JOIN

Giữ toàn bộ dòng của bảng bên trái. Nếu không có dữ liệu tương ứng ở bảng bên phải, các cột bên phải nhận `NULL`.

```sql
SELECT e.name, b.booking_code
FROM events AS e
LEFT JOIN bookings AS b ON b.event_id = e.id;
```

Cách này có thể lấy cả những event chưa có booking.

##### RIGHT JOIN

Giữ toàn bộ dòng của bảng bên phải. `RIGHT JOIN` có thể viết lại thành `LEFT JOIN` bằng cách đổi thứ tự hai bảng.

```sql
SELECT e.name, b.booking_code
FROM bookings AS b
RIGHT JOIN events AS e ON e.id = b.event_id;
```

#### Aggregate function

Aggregate function tổng hợp nhiều dòng thành một giá trị.

| Hàm | Ý nghĩa |
|---|---|
| `COUNT` | Đếm số lượng |
| `SUM` | Tính tổng |
| `AVG` | Tính trung bình |
| `MIN` | Giá trị nhỏ nhất |
| `MAX` | Giá trị lớn nhất |

Ví dụ:

```sql
SELECT COUNT(*) AS total_bookings
FROM bookings;
```

`COUNT(*)` đếm số dòng. `COUNT(column)` chỉ đếm các giá trị khác `NULL` của cột đó.

#### GROUP BY

`GROUP BY` gom các dòng có cùng giá trị thành từng nhóm.

```sql
SELECT
    event_id,
    COUNT(*) AS total_bookings,
    SUM(total_amount) AS revenue
FROM bookings
GROUP BY event_id;
```

Mỗi dòng kết quả đại diện cho một event.

#### HAVING

`WHERE` lọc từng dòng trước khi grouping. `HAVING` lọc kết quả sau khi `GROUP BY`.

```sql
SELECT event_id, SUM(total_amount) AS revenue
FROM bookings
WHERE status = 'CONFIRMED'
GROUP BY event_id
HAVING SUM(total_amount) >= 1000000;
```

#### ORDER BY

`ORDER BY` sắp xếp kết quả.

```sql
SELECT id, name, start_at
FROM events
ORDER BY start_at ASC;
```

- `ASC`: tăng dần.
- `DESC`: giảm dần.

Nếu không có `ORDER BY`, database không bảo đảm thứ tự trả về.

---

### 3. Index

#### Index là gì?

**Index (chỉ mục)** trong cơ sở dữ liệu là một cấu trúc dữ liệu đặc biệt, thường được tổ chức dưới dạng **B-Tree** hoặc **B+Tree**, giúp tăng tốc độ truy xuất dữ liệu.

Nếu không có index phù hợp, database có thể phải đọc lần lượt nhiều hoặc toàn bộ dòng của bảng. Quá trình này gọi là **Full Table Scan**. Khi có index phù hợp, database có thể tìm nhanh vị trí của những dòng cần thiết thay vì quét toàn bộ bảng.

Có thể hình dung index giống mục lục của một cuốn sách: mục lục giúp tìm đến đúng trang mà không cần đọc lần lượt từ đầu đến cuối.

Index là cấu trúc tách biệt với dữ liệu chính của bảng. Nó thường lưu:

- Giá trị của cột được đánh index.
- Thông tin giúp database xác định vị trí của dòng tương ứng.

Database có bộ tối ưu truy vấn (**Query Optimizer**) để quyết định có sử dụng index hay không. Việc một index tồn tại không có nghĩa mọi truy vấn liên quan đều sử dụng index đó.

#### Cách tạo index

Cú pháp có thể khác nhau giữa các DBMS. Ví dụ dưới đây sử dụng MySQL:

```sql
CREATE INDEX idx_bookings_user_id
ON bookings(user_id);
```

Index trên có thể hỗ trợ truy vấn tìm booking theo người dùng:

```sql
SELECT *
FROM bookings
WHERE user_id = 2;
```

#### Tại sao cần index?

Khi bảng có lượng dữ liệu lớn, quét toàn bộ bảng cho mỗi truy vấn sẽ tốn thời gian và tài nguyên. Index giúp giảm số lượng dòng database phải kiểm tra, từ đó giảm thời gian phản hồi của truy vấn.

Index đặc biệt hữu ích với các cột thường xuất hiện trong:

- `WHERE`.
- `JOIN`.
- `ORDER BY`.
- `GROUP BY`.

Trong hầu hết DBMS quan hệ, primary key và unique constraint được hỗ trợ bằng index để bảo đảm tính duy nhất và tăng tốc tìm kiếm. Cách triển khai cụ thể phụ thuộc từng DBMS và storage engine.

#### Các loại index thường gặp

##### Single-column index

Index được tạo trên một cột:

```sql
CREATE INDEX idx_events_start_at
ON events(start_at);
```

##### Composite index

Composite index được tạo trên nhiều cột:

```sql
CREATE INDEX idx_events_status_start_at
ON events(status, start_at);
```

Thứ tự các cột trong composite index rất quan trọng. Với index `(status, start_at)`, database thường có thể sử dụng hiệu quả cho:

```sql
WHERE status = 'PUBLISHED'
```

hoặc:

```sql
WHERE status = 'PUBLISHED'
  AND start_at >= '2026-10-01'
```

Nhưng index này thường không tối ưu cho điều kiện chỉ có `start_at`. Đây là nguyên tắc **leftmost prefix** của các index dạng B-Tree/B+Tree.

##### Unique index

Unique index vừa hỗ trợ tìm kiếm, vừa không cho phép các giá trị bị trùng:

```sql
CREATE UNIQUE INDEX idx_users_email
ON users(email);
```

Ngoài ra còn có các loại index khác như hash index, full-text index hoặc spatial index. Khả năng hỗ trợ và cách hoạt động phụ thuộc vào từng DBMS.

#### Khi nào nên đánh index?

Nên cân nhắc index khi:

- Cột thường xuyên xuất hiện trong `WHERE`.
- Cột thường được dùng trong điều kiện `JOIN`.
- Cột thường được dùng trong `ORDER BY` hoặc `GROUP BY`.
- Cột có nhiều giá trị khác nhau và truy vấn chỉ trả về một phần nhỏ dữ liệu (cột có tính phân biệt cao).
- Các bảng có kích thước lớn (hàng triệu bản ghi) và thiên về đọc dữ liệu (Read-heavy).

Mức độ một cột giúp thu hẹp kết quả được gọi là **selectivity**. Ví dụ, `email` thường có selectivity cao vì gần như mỗi dòng có một email khác nhau. Cột `status` chỉ có vài giá trị nên thường có selectivity thấp khi đứng một mình.

#### Khi nào không nên đánh index?

Index giúp đọc nhanh hơn nhưng làm tăng chi phí ghi. Mỗi lần `INSERT`, `UPDATE` hoặc `DELETE`, database có thể phải cập nhật cả dữ liệu bảng và các index liên quan.

Không nên tạo index tùy tiện khi:

- Bảng nhỏ và full table scan đã đủ nhanh.
- Cột rất ít được dùng để tìm kiếm hoặc kết hợp dữ liệu.
- Cột có tính phân biệt thấp: dữ liệu bị lặp lại quá nhiều (ví dụ: cột gender chỉ có nam/nữ, is_active chỉ có 0/1).
- Bảng có tần suất ghi rất cao nhưng ít truy vấn đọc.

Tạo quá nhiều index gây ra:

- Tốn thêm dung lượng lưu trữ.
- Làm chậm `INSERT`, `UPDATE` và `DELETE`.
- Tăng chi phí bảo trì index.
- Tạo ra các index dư thừa nhưng không được sử dụng.

#### Khi nào index có thể không được sử dụng?

Query Optimizer có thể chọn Full Table Scan dù bảng đã có index nếu:

- Truy vấn trả về phần lớn số dòng trong bảng.
- Bảng quá nhỏ nên quét toàn bảng nhanh hơn.
- Truy vấn không sử dụng cột đầu tiên của composite index.
- Cột được bọc trong một hàm hoặc phép biến đổi không phù hợp.
- Tìm chuỗi bằng wildcard ở đầu, ví dụ `LIKE '%music'`.
- Kiểu dữ liệu trong phép so sánh không tương thích.

Full Table Scan không phải lúc nào cũng xấu. Nếu cần đọc phần lớn dữ liệu của bảng, quét toàn bảng có thể hiệu quả hơn việc đi qua index rồi quay lại lấy từng dòng.

#### Kiểm tra index

Không nên đánh giá index chỉ bằng việc query “có vẻ nhanh”. Cần xem **execution plan** do DBMS cung cấp.

Thông thường:

- `EXPLAIN` hiển thị kế hoạch thực thi dự kiến.
- `EXPLAIN ANALYZE` thực thi truy vấn và bổ sung số liệu thực tế.

Tên lệnh, nội dung kết quả và mức độ hỗ trợ phụ thuộc vào từng DBMS. Ví dụ với MySQL:

```sql
EXPLAIN ANALYZE
SELECT *
FROM bookings
WHERE user_id = 2;
```

Cần quan sát:

- Database đang quét toàn bảng hay sử dụng index.
- Index nào được chọn.
- Số dòng dự kiến và số dòng thực tế phải đọc.
- Thời gian thực thi.

Khi so sánh trước và sau khi tạo index, cần dùng cùng một truy vấn và tập dữ liệu đủ lớn. Kết quả trên một bảng chỉ có vài dòng không phản ánh rõ lợi ích của index.

---

### 4. Phân trang (Pagination)

#### Tại sao cần phân trang?

Không nên trả toàn bộ dữ liệu khi bảng có nhiều dòng vì:

- Query tốn thời gian và tài nguyên.
- Ứng dụng tốn bộ nhớ.
- Response lớn và truyền qua mạng chậm.
- Client mất nhiều thời gian hiển thị.

Pagination chia kết quả thành các phần nhỏ.

#### Offset-based pagination

```sql
SELECT id, name, start_at
FROM events
ORDER BY id
LIMIT 20 OFFSET 40;
```

Câu lệnh trên bỏ qua 40 dòng đầu và lấy 20 dòng tiếp theo.

Ưu điểm:

- Dễ hiểu và dễ triển khai.
- Có thể chuyển trực tiếp đến một trang cụ thể.
- Phù hợp với màn hình quản trị và dữ liệu không quá lớn.

Nhược điểm:

- Offset càng lớn thì query có thể càng chậm.
- Dữ liệu thêm hoặc xóa giữa hai lần đọc có thể làm trùng hoặc bỏ sót dòng.

#### Cursor-based pagination

```sql
SELECT id, name, start_at
FROM events
WHERE id > 100
ORDER BY id
LIMIT 20;
```

`100` là ID cuối cùng của trang trước.

Ưu điểm:

- Hiệu quả hơn với dữ liệu lớn nếu cursor có index.
- Không phải bỏ qua nhiều dòng.
- Phù hợp với infinite scroll hoặc dữ liệu thay đổi liên tục.

Nhược điểm:

- Không dễ nhảy trực tiếp đến một trang bất kỳ.
- Client phải lưu cursor.
- Khó hiển thị chính xác tổng số trang.

| Trường hợp | Nên dùng |
|---|---|
| Màn hình quản trị cần số trang | Offset |
| Dữ liệu nhỏ hoặc vừa | Offset |
| Infinite scroll | Cursor |
| Dữ liệu rất lớn, thay đổi liên tục | Cursor |

---

### 5. Transaction

#### Transaction là gì?

Transaction là một nhóm thao tác được database xử lý như một đơn vị công việc duy nhất. Các thao tác phải cùng thành công; nếu một thao tác thất bại thì toàn bộ thay đổi có thể được hoàn tác để dữ liệu không rơi vào trạng thái dở dang.

Transaction cần được sử dụng khi một nghiệp vụ gồm nhiều thao tác có quan hệ chặt chẽ và không được phép chỉ hoàn thành một phần.

Ví dụ đặt vé gồm:

1. Kiểm tra ghế còn trống.
2. Cập nhật trạng thái ghế.
3. Tạo booking.
4. Thêm booking item.
5. Tạo payment.

Nếu tạo booking thành công nhưng thêm ghế thất bại, hệ thống phải rollback để tránh tồn tại một booking không có ghế.

#### Các tính chất ACID

- **Atomicity**: tất cả cùng thành công hoặc cùng thất bại.
- **Consistency**: transaction đưa database từ một trạng thái hợp lệ sang một trạng thái hợp lệ khác.
- **Isolation**: các transaction chạy đồng thời không được làm sai kết quả của nhau.
- **Durability**: sau khi commit, dữ liệu được lưu bền vững.

#### START TRANSACTION, COMMIT và ROLLBACK

`START TRANSACTION` bắt đầu một transaction. `COMMIT` xác nhận toàn bộ thay đổi:

```sql
START TRANSACTION;

-- Các câu lệnh xử lý nghiệp vụ

COMMIT;
```

Nếu có lỗi, `ROLLBACK` hủy toàn bộ thay đổi chưa được commit:

```sql
START TRANSACTION;

-- Các câu lệnh xử lý nghiệp vụ

ROLLBACK;
```

Sau khi `COMMIT` thành công, không thể dùng `ROLLBACK` để hủy lại transaction đó.

MySQL thường bật chế độ autocommit, nghĩa là mỗi câu lệnh độc lập sẽ tự động commit. Khi cần gom nhiều câu lệnh thành một nghiệp vụ nguyên tử, phải bắt đầu transaction rõ ràng.

Transaction nên được giữ ngắn. Transaction kéo dài có thể giữ lock lâu và làm các request khác phải chờ.

---

### 6. Locking trong Database

#### Tại sao cần lock?

Locking là cơ chế kiểm soát việc nhiều transaction cùng truy cập và thay đổi dữ liệu. Lock giúp bảo vệ tính nhất quán khi các thao tác đồng thời có thể xung đột.

Ví dụ: hai khách cùng nhìn thấy ghế A1 đang `AVAILABLE`. Nếu cả hai cùng đặt mà không kiểm soát concurrency, hệ thống có thể bán một ghế hai lần.

Lock thường được giữ trong phạm vi transaction và được giải phóng khi transaction `COMMIT` hoặc `ROLLBACK`. Vì vậy cần hiểu transaction trước khi tìm hiểu locking.

#### Pessimistic locking

Pessimistic locking giả định xung đột dễ xảy ra nên khóa dữ liệu trước khi cập nhật.

```sql
START TRANSACTION;

SELECT *
FROM event_seats
WHERE id = 10
FOR UPDATE;

-- Kiểm tra và cập nhật ghế

COMMIT;
```

`FOR UPDATE` khóa dòng được chọn để chuẩn bị cập nhật. Một transaction khác muốn khóa hoặc cập nhật cùng dòng thường phải chờ transaction hiện tại `COMMIT` hoặc `ROLLBACK`.

Ưu điểm:

- Ngăn xung đột ngay từ đầu.
- Phù hợp với đặt ghế, tồn kho và chuyển tiền.

Nhược điểm:

- Transaction khác có thể phải chờ.
- Có thể giảm khả năng xử lý đồng thời.
- Có nguy cơ deadlock nếu các transaction khóa tài nguyên theo thứ tự khác nhau.

#### Optimistic locking

Optimistic locking giả định xung đột ít xảy ra. Thay vì giữ lock từ lúc đọc, ứng dụng kiểm tra phiên bản của dữ liệu tại thời điểm cập nhật.

Project sử dụng cột `version` trong `event_seats`:

```sql
UPDATE event_seats
SET status = 'BOOKED',
    version = version + 1
WHERE id = 10
  AND status = 'AVAILABLE'
  AND version = 0;
```

Nếu transaction khác đã cập nhật dòng, `version` không còn bằng `0`. Câu lệnh trên cập nhật `0` dòng và ứng dụng phát hiện xung đột. Ứng dụng phải kiểm tra số dòng bị ảnh hưởng để quyết định báo lỗi hoặc thử lại.

Ưu điểm:

- Không giữ lock trong thời gian dài giữa lúc đọc và cập nhật.
- Hiệu quả khi xung đột hiếm.

Nhược điểm:

- Ứng dụng phải chủ động phát hiện xung đột.
- Khi có xung đột, request có thể phải báo lỗi hoặc thử lại.

#### So sánh Optimistic locking và Pessimistic locking

| Tiêu chí | Optimistic locking | Pessimistic locking |
|---|---|---|
| Giả định | Xung đột hiếm | Xung đột thường xuyên |
| Cách xử lý | Phát hiện khi cập nhật bằng `version` hoặc điều kiện trạng thái | Khóa bản ghi trước bằng `SELECT ... FOR UPDATE` |
| Khi có xung đột | Request thất bại và phải báo lỗi hoặc retry | Request đến sau phải chờ |
| Hiệu năng | Tốt khi ít xung đột | Phù hợp khi nhiều request cạnh tranh |
| Rủi ro | Retry nhiều nếu xung đột cao | Chờ lock, timeout hoặc deadlock |

##### Khi nào dùng Optimistic locking?

Dùng khi:

- Khả năng nhiều request cùng sửa một bản ghi thấp.
- Hệ thống đọc nhiều hơn ghi.
- Có thể chấp nhận báo conflict hoặc retry.
- Không muốn giữ transaction và database connection trong lúc chờ.

Ví dụ: cập nhật hồ sơ người dùng, sửa thông tin event hoặc chỉnh sửa nội dung.

Không nên dùng khi một bản ghi liên tục bị nhiều request cập nhật, vì nhiều request sẽ thất bại và phải retry.

##### Khi nào dùng Pessimistic locking?

Dùng khi:

- Nhiều request thường xuyên tranh chấp cùng dữ liệu.
- Dữ liệu là tài nguyên quan trọng như ghế, tồn kho hoặc số dư.
- Cần ngăn request khác thay đổi dữ liệu ngay từ đầu.
- Transaction có thể hoàn thành nhanh.

Ví dụ: đặt cùng một ghế, trừ tồn kho hoặc chuyển tiền.

Không nên dùng cho transaction kéo dài vì request khác phải chờ, đồng thời làm tăng nguy cơ timeout và deadlock.

##### Quy tắc lựa chọn nhanh

```text
Xung đột hiếm + có thể retry
→ Optimistic locking

Xung đột cao + tài nguyên quan trọng + transaction ngắn
→ Pessimistic locking
```

<!-- Trong project đặt vé:

- Có thể dùng Optimistic locking bằng cột `version`. Request cập nhật được một dòng sẽ thành công; request nhận `affected rows = 0` sẽ thất bại.
- Có thể dùng Pessimistic locking với `SELECT ... FOR UPDATE` nếu muốn request đến sau chờ request đang xử lý ghế.
- Không giữ lock trong lúc người dùng thanh toán. Chỉ chuyển ghế sang `HELD`, lưu `held_until`, sau đó `COMMIT` ngay. -->

---

## Phần 2: OOP

### 1. OOP trong Java

#### OOP là gì?

OOP (Object-Oriented Programming) là cách tổ chức chương trình bằng các object. Một object bao gồm:

- **State**: trạng thái, được biểu diễn bằng field.
- **Behavior**: hành vi, được biểu diễn bằng method.

Class là bản thiết kế; object là một instance được tạo từ class.

```java
public class Booking {
    private String bookingCode;
    private String status;

    public void confirm() {
        status = "CONFIRMED";
    }
}
```

#### Encapsulation — Tính đóng gói

Encapsulation che giấu trạng thái nội bộ và chỉ cho phép thay đổi thông qua các method hợp lệ.

```java
public class Booking {
    private BookingStatus status = BookingStatus.PENDING;

    public void confirm() {
        if (status != BookingStatus.PENDING) {
            throw new IllegalStateException("Booking is not pending");
        }

        status = BookingStatus.CONFIRMED;
    }

    public BookingStatus getStatus() {
        return status;
    }
}
```

Đóng gói không chỉ là đặt field thành `private` rồi tạo setter cho mọi field. Object cần tự bảo vệ các quy tắc hợp lệ của nó.

#### Inheritance — Tính kế thừa

Inheritance cho phép class con kế thừa state và behavior của class cha. Abstract class phù hợp khi các class con cùng một họ cần dùng chung dữ liệu hoặc logic.

```java
public abstract class Notification {
    protected final String recipient;

    protected Notification(String recipient) {
        this.recipient = recipient;
    }

    protected void log() {
        System.out.println("Sending notification to " + recipient);
    }

    public abstract void send(String message);
}

public class EmailNotification extends Notification {
    public EmailNotification(String recipient) {
        super(recipient);
    }

    @Override
    public void send(String message) {
        log();
        System.out.println("Send email: " + message);
    }
}

public class SmsNotification extends Notification {
    public SmsNotification(String recipient) {
        super(recipient);
    }

    @Override
    public void send(String message) {
        log();
        System.out.println("Send SMS: " + message);
    }
}
```

`EmailNotification` và `SmsNotification` dùng chung field `recipient` và method `log()` từ class cha. Kế thừa phù hợp khi class con thực sự là một dạng của class cha, tức quan hệ **is-a**. Không nên lạm dụng vì class con sẽ phụ thuộc chặt vào class cha.

#### Polymorphism — Tính đa hình

Polymorphism cho phép sử dụng nhiều implementation thông qua một kiểu chung.

```java
Notification email = new EmailNotification("user@example.com");
Notification sms = new SmsNotification("0900000001");

email.send("Booking confirmed");
sms.send("Booking confirmed");
```

Hai biến cùng có kiểu `Notification`, nhưng Java gọi `send()` của object thực tế là `EmailNotification` hoặc `SmsNotification`. Code sử dụng kiểu cha mà không cần phụ thuộc vào từng class con cụ thể.

#### Abstraction — Tính trừu tượng

Abstraction chỉ công khai những gì bên sử dụng cần biết và che giấu chi tiết triển khai.

```java
public interface PaymentGateway {
    void pay(long bookingId, BigDecimal amount);
}
```

Code sử dụng chỉ cần biết `pay()` tồn tại, không cần biết cổng thanh toán gọi API bên ngoài như thế nào.

#### Class, abstract class và interface

| Loại | Khi sử dụng |
|---|---|
| Class | Đối tượng có implementation đầy đủ và có thể khởi tạo |
| Abstract class | Các class cùng một họ cần chia sẻ state hoặc logic chung |
| Interface | Cần định nghĩa hợp đồng và có nhiều implementation thay thế |

Abstract class:

- Không thể khởi tạo trực tiếp.
- Có thể chứa field, constructor và method đã có implementation.
- Có thể chứa abstract method để class con triển khai.
- Java chỉ cho phép một class kế thừa một class khác.

Interface:

- Mô tả một hợp đồng hoặc khả năng.
- Không dùng để giữ state như class thông thường.
- Một class có thể implement nhiều interface.
- Phù hợp để giảm phụ thuộc giữa các thành phần.

---

### 2. Dependency Injection (DI) và Inversion of Control (IoC)

#### Dependency là gì?

Dependency là một object mà class khác cần để hoàn thành công việc. Ví dụ `BookingService` cần `BookingRepository` để lưu booking và `PaymentGateway` để thanh toán.

Ta định nghĩa dependency bằng interface và tạo các implementation khác nhau:

```java
public interface BookingRepository {
    void save(String bookingCode);
}

public class MySqlBookingRepository implements BookingRepository {
    @Override
    public void save(String bookingCode) {
        System.out.println("Save " + bookingCode + " to MySQL");
    }
}

public class InMemoryBookingRepository implements BookingRepository {
    @Override
    public void save(String bookingCode) {
        System.out.println("Save " + bookingCode + " to memory");
    }
}
```

#### Code phụ thuộc chặt

```java
public class BookingService {
    private final MySqlBookingRepository repository =
            new MySqlBookingRepository();

    public void createBooking(String bookingCode) {
        repository.save(bookingCode);
    }
}
```

`BookingService` tự tạo implementation cụ thể nên:

- Khó thay MySQL bằng cách lưu khác.
- Khó unit test mà không kết nối database.
- Business logic bị gắn chặt với chi tiết kỹ thuật.

#### Dependency Injection

Dependency Injection là truyền dependency từ bên ngoài thay vì để class tự tạo.

```java
public class BookingService {
    private final BookingRepository repository;

    public BookingService(BookingRepository repository) {
        this.repository = repository;
    }

    public void createBooking(String bookingCode) {
        repository.save(bookingCode);
    }
}
```

Nơi khởi tạo ứng dụng chọn implementation và truyền nó vào service. Đây là Java thuần, chưa sử dụng framework:

```java
BookingRepository repository = new MySqlBookingRepository();
BookingService service = new BookingService(repository);

service.createBooking("BKG-2026-0001");
```

Muốn thay cách lưu dữ liệu, chỉ cần truyền implementation khác; không phải sửa `BookingService`.

Constructor injection thường được ưu tiên vì:

- Dependency bắt buộc được thể hiện rõ.
- Field có thể khai báo `final`.
- Object không thể được tạo khi thiếu dependency.
- Dễ truyền fake implementation trong unit test.

Ví dụ khi test:

```java
BookingRepository repository = new InMemoryBookingRepository();
BookingService service = new BookingService(repository);

service.createBooking("BKG-TEST-0001");
```

Test không cần kết nối MySQL thật. Có thể thay repository bằng fake hoặc in-memory implementation và chỉ kiểm tra business logic của `BookingService`.

#### Inversion of Control

Inversion of Control là nguyên lý chuyển quyền tạo và quản lý object ra khỏi business class.

```text
Không có IoC:
BookingService tự tạo repository.

Có IoC:
Code bên ngoài hoặc framework tạo repository
và truyền repository vào BookingService.
```

DI là một cách phổ biến để thực hiện IoC:

- **IoC** là nguyên lý đảo quyền điều khiển.
- **DI** là kỹ thuật cung cấp dependency từ bên ngoài.

#### DI và IoC trong Spring

Spring có IoC Container chịu trách nhiệm:

- Tạo các object do Spring quản lý, gọi là bean.
- Tìm dependency phù hợp.
- Inject dependency vào constructor.
- Quản lý vòng đời của bean.

```java
@Repository
public class MySqlBookingRepository implements BookingRepository {
    @Override
    public void save(String bookingCode) {
        // Lưu booking vào MySQL
    }
}

@Service
public class BookingService {
    private final BookingRepository repository;

    public BookingService(BookingRepository repository) {
        this.repository = repository;
    }
}
```

`@Repository` đăng ký `MySqlBookingRepository` thành một bean. `@Service` đăng ký `BookingService` thành một bean khác. Spring tìm bean triển khai `BookingRepository` và truyền nó vào constructor của `BookingService`.

Ý tưởng vẫn giống Java thuần: dependency được tạo bên ngoài rồi truyền vào service. Spring chỉ tự động hóa việc tạo, kết nối và quản lý vòng đời của các object.
