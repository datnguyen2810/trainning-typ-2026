# Báo cáo tuần 5: Caching & Redis
## 1. Tìm hiểu Caching

### 1.1. Caching là gì?

**Caching** là kỹ thuật lưu tạm dữ liệu đã đọc hoặc tính toán vào một nơi có thể truy xuất nhanh, để tái sử dụng trong các lần yêu cầu tiếp theo. Thay vì luôn truy vấn database hoặc tính lại kết quả, ứng dụng kiểm tra cache trước.

Hai khái niệm cơ bản:

- **Cache hit:** tìm thấy dữ liệu cần dùng trong cache.
- **Cache miss:** không tìm thấy dữ liệu; ứng dụng cần lấy từ nguồn gốc hoặc tính toán lại.

Ví dụ, API lấy chi tiết sản phẩm có thể lưu kết quả theo key `product:101`. Lần đọc đầu tiên truy vấn database rồi lưu cache; các lần đọc tiếp theo có thể dùng kết quả đó trong thời gian cache còn hợp lệ.

![Luồng đọc sản phẩm với cache hit và cache miss](docs/caching_tai_lieu/image%201.png)

*Hình 1. Ví dụ Cache-Aside: Spring Boot kiểm tra Redis; khi miss, chính ứng dụng truy vấn MySQL rồi lưu kết quả vào cache. Đây là một cách triển khai caching, không phải luồng bắt buộc của mọi loại cache.*

### 1.2. Tại sao cần caching?

Với bài toán đọc lặp lại thông tin sản phẩm, caching mang lại các lợi ích:

| Lợi ích | Ý nghĩa đối với ứng dụng |
| --- | --- |
| Giảm thời gian phản hồi | Tái sử dụng kết quả thay vì thực hiện lại truy vấn hoặc phép tính tốn kém |
| Giảm tải database | Hạn chế các truy vấn đọc cùng một dữ liệu |
| Tăng khả năng phục vụ | Giải phóng tài nguyên để xử lý thêm request hoặc nghiệp vụ khác |
| Tối ưu chi phí | Có thể giảm nhu cầu tăng tài nguyên database, nhưng cần tính cả chi phí vận hành cache |

Ví dụ minh họa: với 1.000 request đọc cùng một sản phẩm, nếu request đầu tiên đã nạp cache và 999 request sau đều hit thì chỉ cần một lần đọc database. Giả định này yêu cầu dữ liệu chưa hết hạn, chưa bị loại bỏ và không có nhiều request đồng thời cùng miss ở thời điểm khởi đầu.

Cache không bảo đảm mọi API đều nhanh hơn. Truy cập Redis vẫn có chi phí mạng và chuyển đổi dữ liệu. Khi triển khai, cần so sánh thời gian phản hồi, số truy vấn DB và tỷ lệ hit trên cùng điều kiện tải.

### 1.3. Cache ở đâu? Application-level và Distributed Cache

Cache có thể xuất hiện tại trình duyệt, CDN hoặc backend. Trong phạm vi bài tập backend, cần phân biệt cache trong bộ nhớ ứng dụng và cache dùng chung qua mạng.

**Application-level cache (local cache)** lưu ngay trong tiến trình ứng dụng, ví dụ Caffeine trong Spring Boot. Mỗi instance có bộ nhớ cache riêng.

![Local cache nằm trong ứng dụng Spring Boot](docs/caching_tai_lieu/image%202.png)

*Hình 2. Local cache dùng RAM của backend. Ứng dụng kiểm tra cache nội bộ trước khi truy vấn database.*

**Distributed cache** cung cấp một hệ thống cache dùng chung cho nhiều instance, ví dụ Redis hoặc Memcached. Backend truy cập hệ thống này qua mạng.

![Nhiều instance Spring Boot sử dụng chung Redis](docs/caching_tai_lieu/image%203.png)

*Hình 3. Nhiều instance dùng chung Redis. Mũi tên từ Redis xuống MySQL thể hiện quan hệ logic; trong mô hình này, backend trực tiếp truy vấn MySQL khi miss, Redis không tự truy vấn database.*

| Tiêu chí | Application-level cache | Distributed cache |
| --- | --- | --- |
| Vị trí | RAM trong tiến trình backend | Dịch vụ cache riêng |
| Truy cập | Không cần gọi mạng | Có chi phí kết nối mạng |
| Chia sẻ giữa các instance | Mỗi instance có bản riêng | Nhiều instance có thể dùng chung |
| Backend restart | Cache trong RAM của instance thường mất | Không mất chỉ vì backend restart |
| Tài nguyên | Chiếm RAM của backend | Cần tài nguyên và vận hành riêng |
| Đồng bộ dữ liệu | Khó giữ các bản local giống nhau | Dùng chung cache, nhưng vẫn cần đồng bộ với DB |
| Ví dụ | Caffeine | Redis, Memcached |

Dùng chung một Redis server đã có thể cung cấp cache chung cho nhiều backend; không bắt buộc triển khai Redis Cluster để minh họa mô hình này.

### 1.4. Khi nào nên và không nên cache?

| Đặc điểm dữ liệu | Đánh giá | Ví dụ |
| --- | --- | --- |
| Được đọc nhiều, ít thay đổi | Phù hợp | Danh mục sản phẩm |
| Tính toán hoặc truy vấn tốn tài nguyên, được tái sử dụng | Phù hợp | Thống kê tổng hợp |
| Chấp nhận dữ liệu trễ trong giới hạn xác định | Có thể cache với TTL phù hợp | Thông tin mô tả sản phẩm |
| Ít được đọc lại | Lợi ích thường thấp | Kết quả truy vấn dùng một lần |
| Truy vấn gốc đã đủ nhanh | Cần đo trước khi thêm cache | Bảng nhỏ, truy vấn đơn giản |
| Cần dữ liệu chính xác ngay tại thời điểm ra quyết định | Cần thận trọng | Kiểm tra số dư khi chuyển tiền |
| Thay đổi liên tục, chưa có cơ chế làm mới | Dễ trả dữ liệu cũ | Tồn kho dùng để xác nhận đặt hàng |

Đối với sản phẩm, có thể cache phần mô tả nhưng cần kiểm tra lại giá hoặc tồn kho từ nguồn dữ liệu có thẩm quyền tại bước xác nhận mua hàng, tùy yêu cầu nghiệp vụ. Dữ liệu riêng của từng người dùng cần key và quyền truy cập phù hợp để tránh trả nhầm kết quả.

Khi lựa chọn dữ liệu để cache, cần xác định mức độ chấp nhận dữ liệu cũ, thời gian tồn tại và cách xử lý khi nguồn gốc thay đổi. Cache-Aside không tự bảo đảm nhất quán giữa cache và nguồn dữ liệu.

## 2. Chiến lược Caching

### 2.1. Cache-Aside (Lazy Loading)

Cache-Aside là chiến lược trong đó **ứng dụng điều phối việc đọc cache, truy vấn database và nạp cache khi cần**. Cache chỉ chứa dữ liệu đã được yêu cầu, thay vì sao chép toàn bộ database.

![Luồng đọc Cache-Aside](docs/caching_tai_lieu/image%204.png)

*Hình 4. Khi hit, ứng dụng trả dữ liệu từ Redis. Khi miss, ứng dụng đọc DB, ghi cache rồi trả kết quả.*

Ví dụ với API `GET /products/101`:

1. Backend kiểm tra key `product:101` trong Redis.
2. Nếu có, chuyển đổi dữ liệu và trả response.
3. Nếu không có, backend truy vấn sản phẩm trong database.
4. Nếu tìm thấy, backend lưu kết quả vào Redis với TTL, rồi trả response.
5. Nếu không tìm thấy, backend trả kết quả không tồn tại theo quy ước API.

Mã giả minh họa:

```text
getProduct(id):
    key = "product:" + id
    cached = cache.get(key)
    if cached exists:
        return deserialize(cached)

    product = database.findProduct(id)
    if product does not exist:
        return notFound

    cache.set(key, serialize(product), ttl = 60 seconds)
    return product
```

TTL 60 giây ở đây là ví dụ học tập; cần lựa chọn thực tế theo yêu cầu cập nhật dữ liệu và kết quả đo.

**Luồng cập nhật thường dùng:** ghi thay đổi vào database, sau khi commit thành công thì xóa key cache liên quan. Lần đọc sau sẽ nạp lại dữ liệu mới. Trong Spring Boot, cần phân biệt “phương thức chạy xong” và “transaction đã commit”; nếu xóa cache quá sớm, request khác có thể đọc dữ liệu DB cũ và nạp lại cache.

**Ưu điểm:** chỉ cache dữ liệu được dùng, tiết kiệm RAM và dễ bắt đầu triển khai. **Hạn chế:** lần miss chịu thêm chi phí kiểm tra/nạp cache; các request đồng thời có thể cùng truy vấn DB; dữ liệu có thể cũ nếu việc làm mới không đúng. Ghi DB rồi xóa cache cũng không loại bỏ mọi tình huống tranh chấp giữa đọc và ghi.

### 2.2. Write-Through

Write-Through chủ động cập nhật cache trong cùng luồng ghi dữ liệu xuống nguồn lưu trữ chính. Khác với việc chờ một request đọc bị miss mới nạp cache, dữ liệu mới được đưa vào cache ngay trong luồng cập nhật.

![Minh họa ghi đồng bộ theo Write-Through](docs/caching_tai_lieu/image%206.png)

*Hình 5. Hình minh họa luồng qua cache layer rồi ghi DB đồng bộ. Thứ tự thao tác phụ thuộc cách triển khai; không nên hiểu đây là yêu cầu Redis tự ghi xuống MySQL.*

Với backend tự quản lý Redis và DB, có thể áp dụng luồng sau:

1. Backend cập nhật database và commit.
2. Backend cập nhật giá trị mới vào cache.
3. Trả thành công khi các thao tác cần thiết theo chính sách ghi đã hoàn tất.

```text
updateProduct(id, input):
    updated = database.updateAndCommit(id, input)
    cache.set("product:" + id, serialize(updated), ttl = 60 seconds)
    return updated
```

Đây là mã giả cho luồng thành công. Khi DB đã commit nhưng ghi Redis thất bại, hệ thống phải có chính sách xử lý lỗi, chẳng hạn xóa cache cũ và thử lại việc đồng bộ; bản thân thao tác xóa cũng có thể thất bại.

**“Đồng bộ” không có nghĩa bắt buộc chạy song song hoặc có giao dịch nguyên tử xuyên Redis và DB.** Redis thông thường không tự biết bảng dữ liệu hay tự đồng bộ với database nghiệp vụ. Không nên suy ra rằng Write-Through luôn bảo đảm nhất quán tuyệt đối.

Lợi ích là tăng khả năng đọc được dữ liệu vừa ghi từ cache; đổi lại, thao tác ghi tốn thêm thời gian, có thể cache cả dữ liệu ít được đọc và cần xử lý lỗi một phần. Vẫn cần luồng xử lý cache miss khi key hết hạn hoặc chưa được nạp.

### 2.3. So sánh và lựa chọn

| Tiêu chí | Cache-Aside | Write-Through |
| --- | --- | --- |
| Thời điểm nạp cache | Khi đọc bị miss | Trong luồng ghi |
| Sau khi thay đổi dữ liệu | Thường xóa cache, đọc sau nạp lại | Chủ động ghi giá trị mới vào cache |
| Đọc lần đầu | Có thể phải truy vấn DB | Có thể hit nếu dữ liệu đã được ghi vào cache |
| Chi phí ghi | Ghi DB và xử lý invalidation | Ghi DB và cập nhật cache |
| Bộ nhớ | Chủ yếu dành cho dữ liệu đã được đọc | Có thể chứa dữ liệu đã ghi nhưng ít được đọc |
| Điểm cần xử lý | Dữ liệu cũ, nhiều request cùng miss | Lỗi một phần và thứ tự cập nhật đồng thời |
| Tình huống phù hợp | Tập dữ liệu lớn, chỉ một phần được đọc thường xuyên | Dữ liệu vừa ghi thường được đọc lại ngay |

Trong bài tập này, **Cache-Aside kết hợp TTL và xóa cache sau cập nhật** là lựa chọn khởi đầu phù hợp cho chi tiết sản phẩm hoặc danh mục. Khi nghiệp vụ cần đọc lại dữ liệu mới ngay sau ghi, có thể cân nhắc chủ động cập nhật cache trong luồng ghi.

Hai chiến lược có thể kết hợp: Write-Through để cập nhật khi ghi, Cache-Aside để xử lý đọc bị miss. Read-Through và Write-Behind là các hướng mở rộng, không phải trọng tâm phần 2 của README.

## 3. Redis cơ bản

### 3.1. Redis là gì?

**Redis (Remote Dictionary Server)** là hệ thống lưu trữ dữ liệu dạng key-value, xử lý dữ liệu chủ yếu trong bộ nhớ. Điểm nổi bật là value có thể mang nhiều cấu trúc dữ liệu và được thao tác bằng các lệnh tương ứng, thay vì chỉ lưu chuỗi đơn giản.

Trong ứng dụng backend, Redis có thể dùng cho cache, bộ đếm, session, giới hạn tần suất và bảng xếp hạng. Khi dùng làm cache sản phẩm, database nghiệp vụ vẫn giữ dữ liệu chính; Redis giữ bản sao có thể nạp lại. Tốc độ thực tế phụ thuộc lệnh, kích thước dữ liệu, mạng và tải, nên không thể mặc định mọi API dùng Redis đều đạt một mức độ trễ cố định.

Redis có cơ chế lưu dữ liệu xuống đĩa:

- **RDB:** lưu ảnh chụp dữ liệu tại các thời điểm; có thể mất thay đổi sau snapshot gần nhất.
- **AOF:** ghi lại các thao tác thay đổi để phục hồi; mức độ bền vững phụ thuộc chính sách ghi xuống đĩa.
- Có thể kết hợp hai cơ chế hoặc không dùng persistence tùy mục đích.

Persistence giúp phục hồi nhưng không đồng nghĩa với không bao giờ mất dữ liệu. Khi chạy Docker, cần chú ý cả cấu hình persistence và nơi lưu dữ liệu ngoài vòng đời container.

### 3.2. Năm kiểu dữ liệu cơ bản

| Kiểu | Đặc điểm | Ví dụ sử dụng |
| --- | --- | --- |
| String | Một chuỗi byte; có thể chứa văn bản, số hoặc JSON đã serialize | Cache response, bộ đếm |
| Hash | Tập các cặp field-value | Thuộc tính người dùng, sản phẩm |
| List | Danh sách có thứ tự, cho phép trùng phần tử | Lịch sử, hàng đợi đơn giản |
| Set | Tập hợp phần tử không trùng, không bảo đảm thứ tự | Tag, tập ID duy nhất |
| Sorted Set | Member không trùng, mỗi member có score | Bảng xếp hạng |

Các ví dụ dùng key bắt đầu bằng `w5:` để tách dữ liệu thực hành khỏi các key khác. Dấu `:` là quy ước đặt tên, không tạo ra bảng hay thư mục thật trong Redis.

#### 3.2.1. String

String phù hợp khi cần đọc/ghi cả một giá trị. Một đối tượng được chuyển thành JSON rồi lưu bằng `SET` vẫn là Redis String. Lệnh `INCR` tăng số nguyên theo cách atomic, tránh việc nhiều client tự đọc rồi ghi đè bộ đếm.

```text
SET w5:product:101 '{"id":101,"name":"Ban phim","price":750000}' EX 60
GET w5:product:101
# Trả về chuỗi JSON đã lưu, nếu key chưa hết hạn.

SET w5:views:101 0
INCR w5:views:101
# (integer) 1
```

#### 3.2.2. Hash

Hash gom nhiều field-value vào một key, thuận tiện khi chỉ cần đọc hoặc cập nhật một thuộc tính. Field trong ví dụ là `name`, `price`; key là `w5:product:102`.

```text
HSET w5:product:102 name "Chuot" price 300000
HGET w5:product:102 name
# "Chuot"
HSET w5:product:102 price 280000
HGETALL w5:product:102
# Trả về các cặp field-value hiện có.
```

String chứa JSON thường tiện cho cache cả response; Hash tiện khi cần thao tác riêng từng field. Việc lựa chọn phụ thuộc cách đọc/ghi dữ liệu.

#### 3.2.3. List

List giữ thứ tự chèn và cho phép giá trị trùng. `LPUSH`/`RPUSH` thêm ở đầu/cuối; `LPOP`/`RPOP` lấy và xóa ở đầu/cuối. `LRANGE` đọc một khoảng, trong đó `-1` chỉ phần tử cuối.

```text
RPUSH w5:tasks "gui-email" "tao-bao-cao" "gui-email"
LRANGE w5:tasks 0 -1
# "gui-email", "tao-bao-cao", "gui-email"
LPOP w5:tasks
# "gui-email"
```

Ví dụ trên mô phỏng FIFO bằng cách thêm cuối và lấy đầu. Một hàng đợi thực tế cần cơ chế xác nhận, retry và phục hồi khi consumer lỗi; chỉ dùng thao tác pop chưa đáp ứng đầy đủ các yêu cầu đó.

#### 3.2.4. Set

Set lưu phần tử duy nhất và không bảo đảm thứ tự trả về. Thêm lại một member đã tồn tại không tạo bản sao.

```text
SADD w5:tags "java" "redis" "java"
# (integer) 2, nếu key ban đầu chưa tồn tại.
SMEMBERS w5:tags
# Gồm "java" và "redis", thứ tự có thể khác nhau.
SISMEMBER w5:tags "redis"
# (integer) 1
SREM w5:tags "java"
```

#### 3.2.5. Sorted Set

Sorted Set không trùng member, nhưng nhiều member có thể cùng score. Thứ tự mặc định theo score tăng dần; có thể đọc ngược để lấy điểm cao trước.

![Ví dụ bảng xếp hạng bằng Sorted Set](docs/redis_tai_lieu/image%208.png)

*Hình 6. Bảng xếp hạng đọc theo score giảm dần: player:A, player:B, player:C.*

```text
ZADD w5:leaderboard 100 player:C 300 player:A 200 player:B
ZRANGE w5:leaderboard 0 -1 REV WITHSCORES
# player:A 300, player:B 200, player:C 100
ZINCRBY w5:leaderboard 50 player:C
# Score mới của player:C là 150.
```

`ZREVRANGE` trong một số tài liệu cũ đã được đánh dấu deprecated từ Redis 6.2; ví dụ mới dùng `ZRANGE ... REV`.

### 3.3. Các lệnh cơ bản: GET, SET, DEL, EXPIRE, TTL

| Lệnh | Công dụng | Điểm cần nhớ |
| --- | --- | --- |
| `SET key value` | Tạo hoặc ghi đè String | Mặc định không đặt TTL; ghi đè xóa TTL cũ |
| `GET key` | Đọc String | Key không tồn tại trả nil; key không phải String gây lỗi kiểu dữ liệu |
| `DEL key [key ...]` | Xóa một hoặc nhiều key | Trả số key thực sự đã xóa |
| `EXPIRE key seconds` | Đặt thời gian hết hạn tính bằng giây | Thường trả 1 nếu đặt thành công, 0 nếu key không tồn tại |
| `TTL key` | Xem thời gian sống còn lại theo giây | Phân biệt giá trị không âm, -1 và -2 |

Ví dụ nhập lần lượt trong Redis CLI:

```text
SET w5:demo:name "Redis"
# OK
GET w5:demo:name
# "Redis"
TTL w5:demo:name
# (integer) -1: key tồn tại nhưng chưa có hạn hết hiệu lực.

EXPIRE w5:demo:name 60
# (integer) 1
TTL w5:demo:name
# Một số nguyên từ 0 đến 60 nếu key còn tồn tại.

DEL w5:demo:name
# (integer) 1 nếu chưa hết hạn trước khi xóa.
GET w5:demo:name
# (nil)
TTL w5:demo:name
# (integer) -2: key không tồn tại.
```

**TTL không phải thời gian tính từ lần đọc gần nhất.** Lệnh `GET` thông thường không tự gia hạn key. Key hết hạn hoặc bị xóa sẽ không còn dữ liệu để trả về.

![Vòng đời một key có TTL 60 giây](docs/redis_tai_lieu/image%2011.png)

*Hình 7. Key được đặt TTL 60 giây tại thời điểm tạo. Hình giả định không có thao tác đổi TTL, xóa hoặc loại bỏ key trước hạn.*

Nên dùng một lệnh để vừa lưu String vừa đặt hạn:

```text
SET w5:demo:ttl "Laptop" EX 60
```

Cách này tránh khoảng trống giữa hai lệnh `SET` và `EXPIRE`: nếu client gặp lỗi sau `SET`, key có thể chưa được đặt TTL. Khi cần giữ TTL hiện có lúc ghi đè, có thể dùng `SET key value KEEPTTL`; `PERSIST key` bỏ hạn hết hiệu lực.

### 3.4. Cài đặt Redis bằng Docker Compose

#### 3.4.1. Thêm Redis vào project `training-typ`

Dự án sử dụng file `backend/compose.yaml` để quản lý PostgreSQL, RabbitMQ và Mailpit. Thêm Redis vào cùng file để Docker Desktop hiển thị các service trong nhóm `training-typ`, thay vì tạo container độc lập bằng `docker run`.

Cấu hình dưới đây là phần Redis trong file Compose hiện tại; các service và volume khác được giữ lại:

```yaml
name: training-typ

services:
  # Các service postgres, rabbitmq và mailpit có sẵn.
  redis:
    image: redis:7.4-alpine
    command: ["redis-server", "--appendonly", "yes"]
    ports:
      - "127.0.0.1:6379:6379"
    volumes:
      - backend-typ-redis-data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s
      timeout: 3s
      retries: 5

volumes:
  # Các volume khác có sẵn.
  backend-typ-redis-data:
```

| Thành phần | Ý nghĩa |
| --- | --- |
| `name: training-typ` | Tên project Compose, tương ứng nhóm hiển thị trong Docker Desktop |
| `redis` | Tên service, dùng trong các lệnh `docker compose` |
| `redis:7.4-alpine` | Image Redis 7.4 dùng cho môi trường thực hành |
| `command` | Chạy Redis với AOF được bật để phục hồi dữ liệu |
| `127.0.0.1:6379:6379` | Cho phép kết nối từ máy host qua cổng 6379 trên địa chỉ loopback |
| `backend-typ-redis-data:/data` | Lưu file dữ liệu trong named volume |
| `healthcheck` | Dùng `redis-cli ping` để kiểm tra Redis có phản hồi |

Không cần đặt `container_name`: Compose thường tự đặt tên container là `training-typ-redis-1`; Docker Desktop có thể hiển thị tên rút gọn `redis-1` dưới nhóm project. Dùng đúng tên project `training-typ`, tránh ghi nhầm thành `trainnig-typ` khi chạy lệnh có tùy chọn `-p`.

#### 3.4.2. Khởi động và kiểm tra kết nối

Cần cài Docker và bảo đảm Docker Engine đang chạy. Trên Windows, Docker Desktop cần sử dụng Linux containers. Mở PowerShell **tại thư mục gốc repository** rồi chạy:

```powershell
cd week-5/backend
docker compose config --quiet
docker compose up -d redis
docker compose ps redis
docker compose exec redis redis-cli PING
```

`docker compose config --quiet` kiểm tra cấu hình. `up -d redis` tạo hoặc cập nhật riêng service Redis và chạy ở nền. Kết quả mong đợi của lệnh cuối là `PONG`; trạng thái healthcheck sẽ chuyển sang `healthy` khi kiểm tra thành công.

Các lệnh tiếp theo trong mục này đều chạy tại thư mục `week-5/backend`. Muốn khởi động toàn bộ các service trong file, dùng `docker compose up -d`.

Nếu cổng host 6379 đang được container Redis cũ sử dụng, có thể dừng đúng container đó hoặc đổi ánh xạ trong Compose thành `127.0.0.1:6380:6379`, rồi chạy lại `docker compose up -d redis`. Cổng bên trong container vẫn là 6379.

#### 3.4.3. Thực hành Redis CLI

Mở CLI theo tên service:

```powershell
docker compose exec redis redis-cli
```

Sau đó nhập các lệnh Redis sau trong CLI:

```text
PING
# PONG
SET w5:docker:product "Ban phim" EX 60
# OK
GET w5:docker:product
# "Ban phim"
TTL w5:docker:product
# Số giây còn lại nếu key chưa hết hạn.
DEL w5:docker:product
# (integer) 1 nếu key chưa hết hạn.
TTL w5:docker:product
# (integer) -2
```

Dùng `exit` để rời CLI. Từ PowerShell, quản lý riêng Redis bằng:

```powershell
docker compose stop redis
docker compose start redis
docker compose logs --tail 50 redis
```

Khi ứng dụng Spring Boot chạy từ IDE trên máy host, địa chỉ Redis là `localhost:6379` với ánh xạ mặc định ở trên. Nếu sau này backend cũng chạy trong cùng mạng Compose, địa chỉ kết nối là `redis:6379`; `localhost` trong container backend chỉ chính container đó. Việc thêm service này mới chuẩn bị hạ tầng; tích hợp Redis vào ứng dụng thuộc phần 4 của README.

## 4. Implement — Tích hợp Redis vào project

### 4.1. Mục tiêu và use case

Project sử dụng Redis để cache thông tin chi tiết danh mục sản phẩm, thông qua API:

```http
GET /api/v1/categories/{id}
```

Danh mục là dữ liệu có thể được đọc nhiều và ít thay đổi, phù hợp để minh họa Cache-Aside. Use case tận dụng API và nghiệp vụ có sẵn, cho phép kiểm tra cả đọc dữ liệu, thời gian hết hạn và xóa cache khi sửa/xóa danh mục.

Phạm vi hiện tại chỉ cache kết quả của `getCategoryById(UUID id)`. API danh sách phân trang `GET /api/v1/categories?page=...&size=...` vẫn đọc database, chưa được cache.

| Thành phần | Vai trò |
| --- | --- |
| Spring Boot 4.1.1 | Tiếp nhận request và xử lý nghiệp vụ |
| Spring Cache | Cung cấp các annotation quản lý cache |
| Spring Data Redis và Lettuce | Kết nối, đọc/ghi Redis |
| PostgreSQL | Lưu dữ liệu danh mục gốc |
| Redis 7.4 | Lưu bản sao chi tiết danh mục dưới dạng JSON |

### 4.2. Thư viện và cấu hình kết nối

Hai dependency được bổ sung vào `backend/pom.xml`; phiên bản được Spring Boot quản lý:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

Lettuce là client Redis được sử dụng mặc định qua starter, không cần khai báo thêm Jedis. Redis chạy trong project Compose `training-typ` như mục 3.4, với cổng host `127.0.0.1:6379` ánh xạ vào cổng container 6379.

Project đã có dependency `spring-boot-docker-compose`; profile mặc định là `db`, trong đó Compose được bật và dùng `lifecycle-management: start-only`. Khi chạy backend từ thư mục `week-5/backend`, Spring Boot phát hiện service Redis trong file Compose và cung cấp thông tin kết nối. Vì vậy cấu hình hiện tại không cần khai báo lại host/port Redis trong YAML.

Trong phạm vi demo danh mục, `app.messaging.welcome-consumer-enabled: false` tạm tắt consumer gửi email. Cấu hình này không vô hiệu hóa toàn bộ nghiệp vụ RabbitMQ; luồng đăng ký tài khoản vẫn có thể phát sự kiện nên không thuộc kịch bản demo Redis.

### 4.3. Cấu hình CacheManager và định dạng dữ liệu

Class `CacheConfig` sử dụng `@Configuration` và `@EnableCaching`, đồng thời tạo bean `RedisCacheManager`.

| Cấu hình | Giá trị và ý nghĩa |
| --- | --- |
| Cache name | `category` |
| Key thực tế | `category::<UUID>`; prefix mặc định gồm tên cache và hai dấu `:` |
| TTL | 60 giây tính từ lúc ghi cache |
| Key serializer | `StringRedisSerializer` |
| Value serializer | `JacksonJsonRedisSerializer<CategoryResponse>` dùng Jackson 3 |
| Cache null | Tắt bằng `disableCachingNullValues()` |
| Cache ngoài danh sách khai báo | Không tự tạo, do `disableCreateOnMissingCache()` |
| Phối hợp transaction | Bật `transactionAware()` |

Đoạn cấu hình chính trong code:

```java
var serializer = new JacksonJsonRedisSerializer<>(
        jsonMapper,
        CategoryResponse.class
);

var categoryConfig = RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofSeconds(60))
        .disableCachingNullValues()
        .serializeKeysWith(
                RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer())
        )
        .serializeValuesWith(
                RedisSerializationContext.SerializationPair
                        .fromSerializer(serializer)
        );

return RedisCacheManager.builder(connectionFactory)
        .cacheDefaults(categoryConfig)
        .withInitialCacheConfigurations(Map.of("category", categoryConfig))
        .disableCreateOnMissingCache()
        .transactionAware()
        .build();
```

Ứng dụng cache `CategoryResponse`, thay vì lưu trực tiếp JPA entity. DTO sử dụng `@JsonDeserialize(builder = CategoryResponse.CategoryResponseBuilder.class)` và builder có `@JsonPOJOBuilder(withPrefix = "")` để chuyển JSON từ Redis về đúng kiểu khi cache hit. Cách cấu hình serializer này dành riêng cho kiểu `CategoryResponse`; nếu bổ sung cache chứa kiểu khác cần cấu hình tương ứng.

### 4.4. Luồng đọc dữ liệu theo Cache-Aside

Annotation được đặt tại service:

```java
@Override
@Cacheable(cacheNames = "category", key = "#p0")
@Transactional(readOnly = true)
public CategoryResponse getCategoryById(UUID id) {
    Category category = categoryRepository.findByIdAndActiveTrue(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                    ErrorCode.CATEGORY_NOT_FOUND,
                    "Active category not found with id : " + id
            ));

    return categoryMapper.toResponse(category);
}
```

`#p0` lấy tham số đầu tiên của phương thức, tức UUID danh mục. Controller gọi service qua Spring bean để annotation cache được xử lý.

```text
Client gọi GET /api/v1/categories/{id}
    → Spring Cache kiểm tra key category::<UUID> trong Redis
        → Hit: đọc JSON → chuyển thành CategoryResponse → trả response
        → Miss: thực thi getCategoryById
            → truy vấn danh mục active trong PostgreSQL
            → chuyển entity thành CategoryResponse
            → cache kết quả với TTL 60 giây
            → trả response
```

Khi hit, phần thân phương thức không chạy nên không phát sinh truy vấn lấy danh mục từ repository. Khi không tìm thấy danh mục active, phương thức ném exception và không cache một DTO hợp lệ.

### 4.5. Xử lý khi thay đổi danh mục

Hai phương thức `updateCategory` và `deleteCategory` đều có:

```java
@CacheEvict(cacheNames = "category", key = "#p0")
@Transactional
```

Khi cập nhật thành công, ứng dụng xóa cache của đúng ID để lần đọc tiếp theo lấy lại dữ liệu mới. Khi xóa danh mục, code thực hiện xóa mềm bằng cách chuyển trạng thái `active` thành `false`, đồng thời làm mất hiệu lực cache. Lần GET sau phải đi qua database và không tìm thấy danh mục active.

`transactionAware()` giúp thao tác ghi/xóa cache thông thường được thực hiện sau khi transaction commit thành công nếu đang có transaction. Cơ chế này tránh chủ động làm mất hiệu lực cache trước khi dữ liệu DB được xác nhận, nhưng không tạo giao dịch nguyên tử giữa PostgreSQL và Redis hoặc giải quyết mọi tranh chấp đọc/ghi đồng thời.

Thao tác tạo danh mục chưa cần invalidation vì bản triển khai hiện tại chỉ cache chi tiết theo ID và không cache kết quả không tồn tại.

### 4.6. Kịch bản demo

Liên kết video demo được cung cấp tại [demo/linkdemo.md](demo/linkdemo.md). Kịch bản demo sử dụng cùng một UUID danh mục đang active:

![PostgreSQL và Redis đang chạy trong project training-typ](demo/%E1%BA%A3nh/moi-truong.jpg)

*Hình 8. Kết quả `docker compose ps`: PostgreSQL đang chạy, Redis có trạng thái healthy và được ánh xạ tới cổng 6379 trên máy host.*

| Bước | Thao tác | Nội dung cần quan sát |
| --- | --- | --- |
| 1 | Kiểm tra PostgreSQL và Redis | Redis hoạt động, `PING` trả `PONG` |
| 2 | Xóa đúng key demo rồi gọi GET chi tiết | Cache miss, có SQL lấy danh mục, trả dữ liệu |
| 3 | Kiểm tra key Redis | Có JSON và TTL còn lại |
| 4 | Gọi lại GET trước khi hết TTL | Cache hit, không có SQL lấy danh mục |
| 5 | PUT sửa danh mục bằng tài khoản ADMIN | Sau cập nhật, key cũ bị xóa |
| 6 | Gọi GET sau cập nhật | Dữ liệu mới được đọc từ DB và nạp lại cache |
| 7 | Chờ hết TTL rồi kiểm tra và gọi GET | Key hết hạn, GET nạp lại cache |

Các lệnh hỗ trợ, chạy tại `week-5/backend`; thay `<UUID>` bằng ID thật:

```powershell
docker compose ps
docker compose exec redis redis-cli PING
docker compose exec redis redis-cli DEL "category::<UUID>"
```

Lệnh `DEL` chỉ dùng để chuẩn bị cache miss trước khi gọi API. Sau lần GET đầu, kiểm tra:

```powershell
docker compose exec redis redis-cli GET "category::<UUID>"
docker compose exec redis redis-cli TTL "category::<UUID>"
```

![JSON danh mục trong Redis và TTL còn lại](demo/%E1%BA%A3nh/data-cache.jpg)

*Hình 9. Redis lưu JSON danh mục theo key `category::cc7b80e3-0cd8-467f-a8d6-9dd86e593b5e`; TTL tại thời điểm chụp còn 39 giây.*

Ngay sau PUT, trước khi gọi lại GET, có thể dùng `EXISTS` để kiểm tra key đã bị xóa. Sau khi key hết hạn, `TTL` trả `-2`.

```powershell
docker compose exec redis redis-cli EXISTS "category::<UUID>"
```

Cấu hình `spring.jpa.show-sql: true` đang được bật để quan sát SQL. Khi demo hit/miss cần phân biệt SQL của API chi tiết với các truy vấn phát sinh từ API danh sách hoặc thao tác cập nhật.

### 4.7. So sánh thời gian phản hồi

Số liệu ghi nhận từ hai ảnh cache miss và cache hit của lượt đo demo:

![Response của API chi tiết danh mục khi cache miss, 646 ms](demo/%E1%BA%A3nh/cache-miss-1.jpg)

*Hình 10. Lượt gọi cache miss trả HTTP 200 với thời gian phản hồi 646 ms trên Postman.*

![Log Hibernate truy vấn danh mục từ database khi cache miss](demo/%E1%BA%A3nh/cache-miss-2.jpg)

*Hình 11. Log Hibernate thể hiện truy vấn bảng `categories` theo ID và điều kiện `active=true`. Log cũng ghi nhận việc khởi tạo DispatcherServlet, cho thấy lượt đầu có thể chịu thêm chi phí khởi tạo.*

![Response của API chi tiết danh mục khi cache hit, 17 ms](demo/%E1%BA%A3nh/cache-hit.jpg)

*Hình 12. Lượt gọi được ghi nhận là cache hit trả HTTP 200 với thời gian phản hồi 17 ms; dữ liệu danh mục giống lượt miss. Ảnh Postman thể hiện response và thời gian, còn việc không truy vấn DB cần đối chiếu thêm log khi demo.*

| Trạng thái | Luồng xử lý | Response time |
| --- | --- | --- |
| Cache miss — chưa có dữ liệu trong cache | Kiểm tra Redis, truy vấn PostgreSQL và ghi cache | **646 ms** |
| Cache hit — đã có dữ liệu trong cache | Đọc dữ liệu đã cache từ Redis | **17 ms** |

Từ cặp số liệu này:

```text
Thời gian giảm = 646 - 17 = 629 ms
Tỷ lệ giảm = (646 - 17) / 646 × 100 ≈ 97,37%
Tỷ số thời gian miss/hit = 646 / 17 = 38
```

Trong lượt đo minh họa, request hit có thời gian phản hồi bằng khoảng 1/38 request miss, giảm 629 ms. Kết quả phù hợp với mục tiêu tái sử dụng dữ liệu và hạn chế truy vấn database.

Đây là **so sánh cache miss với cache hit**, chưa phải phép đo ứng dụng đã tắt hoàn toàn cache so với bật cache. Request miss vẫn có chi phí kiểm tra Redis và ghi cache. Hai số liệu cũng có thể chịu ảnh hưởng từ khởi tạo kết nối, JVM, mạng và công cụ gọi API; không dùng một cặp đo để kết luận hiệu năng trung bình hoặc khả năng chịu tải. Các ảnh trên ghi nhận môi trường, dữ liệu cache và thời gian phản hồi; liên kết video demo được cung cấp tại [demo/linkdemo.md](demo/linkdemo.md).

## 5. Các vấn đề thường gặp

### 5.1. TTL: đặt bao lâu là hợp lý?

TTL xác định thời gian tối đa một entry còn hợp lệ nếu không có thao tác thay đổi hạn hoặc loại bỏ trước đó. TTL ngắn giúp dữ liệu được nạp lại thường xuyên nhưng tăng cache miss và tải DB; TTL dài tăng cơ hội hit nhưng có thể giữ dữ liệu cũ lâu hơn.

Việc lựa chọn cần xét tần suất thay đổi, mức chấp nhận dữ liệu cũ, số lần đọc lại và chi phí truy vấn. TTL không nên là biện pháp duy nhất để xử lý cập nhật nghiệp vụ.

| Loại dữ liệu | Hướng lựa chọn |
| --- | --- |
| Ít thay đổi, đọc nhiều | Có thể dùng TTL dài hơn, kết hợp invalidation khi sửa |
| Thay đổi thường xuyên | TTL ngắn hơn hoặc cơ chế làm mới chủ động |
| Cần chính xác tại thời điểm quyết định | Kiểm tra lại nguồn dữ liệu có thẩm quyền |
| Nhiều key được nạp cùng thời điểm | Cân nhắc cộng độ lệch ngẫu nhiên vào TTL |

Project chọn **60 giây** để dễ quan sát quá trình hết hạn trong demo; đây chưa phải giá trị được tối ưu bằng tải thực tế. Lệnh GET thông thường không gia hạn TTL, nên gọi đọc liên tục không giữ key tồn tại mãi.

### 5.2. Cache Invalidation: dữ liệu thay đổi thì xử lý thế nào?

Nếu danh mục trong PostgreSQL đã được sửa nhưng Redis vẫn chứa giá trị trước đó, API có thể trả dữ liệu cũ. Có hai hướng phổ biến: xóa entry để lần đọc tiếp theo nạp lại, hoặc chủ động ghi giá trị mới vào cache.

Bản triển khai lựa chọn **xóa cache theo ID sau cập nhật thành công**:

| Thao tác | Database | Cache |
| --- | --- | --- |
| GET bị miss | Đọc danh mục active | Lưu DTO với TTL 60 giây |
| GET hit | Không gọi repository lấy danh mục | Đọc entry hiện có |
| PUT danh mục | Cập nhật thông tin | Xóa key của ID đó |
| DELETE danh mục | Chuyển `active` thành `false` | Xóa key của ID đó |

Cần dùng cùng cache name và key ở cả đọc và xóa. Nếu sau này cache danh sách phân trang, phải làm mất hiệu lực cả các trang chịu ảnh hưởng; chỉ xóa cache chi tiết sẽ không đủ.

Giới hạn hiện tại: thay đổi DB từ công cụ SQL hoặc tiến trình khác không đi qua service sẽ không kích hoạt `@CacheEvict`; entry có thể cũ cho đến khi hết TTL. Ngoài ra, một request đọc dữ liệu cũ đồng thời với request cập nhật vẫn có thể nạp lại cache cũ sau invalidation. Các trường hợp cần nhất quán nghiêm ngặt hơn phải bổ sung cơ chế phù hợp, không chỉ dựa vào TTL và annotation.

### 5.3. Cache Avalanche: nhiều key hết hạn cùng lúc

Cache Avalanche xảy ra khi nhiều entry hết hạn gần như đồng thời, làm nhiều request chuyển sang đọc database trong một khoảng thời gian ngắn. Ví dụ, nạp hàng loạt danh mục với cùng TTL khiến nhiều key cùng mất hiệu lực và DB phải xử lý lượng lớn truy vấn.

Các hướng giảm thiểu:

- **TTL có độ lệch ngẫu nhiên:** ví dụ 60 giây cộng thêm ngẫu nhiên từ 0 đến 15 giây để phân tán thời điểm hết hạn.
- **Làm mới chủ động:** nạp lại dữ liệu được đọc nhiều trước khi hết hạn khi có cơ chế điều phối phù hợp.
- **Giới hạn tải xuống DB:** áp dụng giới hạn đồng thời và kiểm soát lưu lượng khi lượng miss tăng mạnh.
- **Tăng khả năng sẵn sàng của cache:** giảm nguy cơ toàn bộ request đồng loạt chuyển xuống DB do Redis gặp sự cố.

Cần phân biệt với **cache stampede**: nhiều request cùng miss một key phổ biến và cùng nạp lại dữ liệu. Trường hợp đó có thể cần phối hợp việc nạp theo key; nếu có nhiều backend instance, cơ chế phối hợp phải xét phạm vi giữa các instance.

**Trạng thái project:** đang đặt TTL cố định 60 giây, chưa triển khai jitter, làm mới chủ động hoặc cơ chế phối hợp chống stampede. Các biện pháp trên là hướng mở rộng.

### 5.4. Cache Penetration: truy vấn dữ liệu không tồn tại

Cache Penetration xảy ra khi request liên tục hỏi các ID không tồn tại trong cả cache lẫn database. Mỗi lần đều miss và tiếp tục truy vấn DB, dù kết quả vẫn là không tìm thấy.

Trong code hiện tại, một UUID hợp lệ nhưng không có danh mục active sẽ làm repository trả rỗng và service ném `ResourceNotFoundException`. Kết quả lỗi này không được cache; gọi lại ID đó sẽ tiếp tục truy vấn DB. `disableCachingNullValues()` cũng không phải biện pháp chống penetration.

Các hướng xử lý có thể bổ sung:

| Biện pháp | Cách áp dụng và điểm cần chú ý |
| --- | --- |
| Kiểm tra đầu vào | Từ chối ID sai định dạng trước khi truy vấn; không ngăn được UUID hợp lệ nhưng không tồn tại |
| Negative caching | Lưu dấu hiệu không tồn tại với TTL ngắn; cần thiết kế cách biểu diễn riêng và tránh giữ kết quả âm khi dữ liệu đã thay đổi |
| Bloom filter | Sàng lọc ID chắc chắn không có trong tập; vẫn có false positive và cần duy trì bộ lọc phù hợp |
| Rate limiting | Giảm lưu lượng lặp lại hoặc bất thường trước khi gây quá tải DB |

**Trạng thái project:** chưa có negative caching, Bloom filter hoặc rate limiting. Giữ nguyên xử lý lỗi hiện tại phù hợp phạm vi demo cơ bản, nhưng chưa giải quyết penetration khi có lưu lượng lớn.

### 5.5. Phạm vi đã thực hiện và giới hạn

| Nội dung | Trạng thái trong code |
| --- | --- |
| Redis trong Compose và client Lettuce | Đã cấu hình |
| Cache-Aside cho chi tiết danh mục | Đã triển khai |
| JSON serializer và TTL 60 giây | Đã triển khai |
| Xóa cache khi sửa/xóa danh mục | Đã triển khai |
| So sánh thời gian miss/hit | Có cặp số liệu minh họa 646 ms và 17 ms |
| Video demo | Đã bổ sung liên kết tại [demo/linkdemo.md](demo/linkdemo.md) |
| Chống avalanche và penetration | Mới trình bày hướng xử lý, chưa triển khai |
| Xử lý khi Redis không khả dụng | Chưa có cơ chế bỏ qua lỗi cache riêng; API dùng cache có thể lỗi |

Việc hoàn thành phần demo tập trung vào chứng minh luồng miss → truy vấn DB → lưu cache và hit → trả dữ liệu từ cache, đồng thời giải thích TTL, invalidation và các giới hạn còn lại của bản triển khai.
