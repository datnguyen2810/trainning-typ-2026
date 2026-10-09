# Tuần 2 — Phần 1: Framework và RESTful API

## 1. Spring Boot và cấu trúc project

Bài tập sử dụng **Java 21, Spring Boot và Maven**, xây dựng API CRUD cho Category với dữ liệu lưu trong bộ nhớ.

Spring Boot hỗ trợ phát triển ứng dụng Spring thông qua auto-configuration, dependency starter và web server nhúng. Spring quản lý các đối tượng (bean) và truyền dependency qua constructor, ví dụ truyền `CategoryService` vào controller.

| Thành phần | Trách nhiệm |
|---|---|
| `controller` | Nhận HTTP request và trả response |
| `service` | Xử lý nghiệp vụ và thao tác với danh sách Category |
| `entity` | Biểu diễn dữ liệu Category |
| `dto/request`, `dto/response` | Định nghĩa dữ liệu đầu vào và đầu ra |
| `mapper` | Chuyển entity sang response DTO |
| `exception` | Định nghĩa lỗi và xử lý exception tập trung |

Luồng xử lý: **Client → Controller → Service → dữ liệu in-memory → Mapper → Response**. Repository và migration được dành cho phần 2 khi tích hợp DB.

Cấu hình chính:

- `pom.xml`: dependency, phiên bản Java và plugin build.
- `application.yml`: tên ứng dụng, port và profile mặc định.
- `application-demo.yml`: tắt tự cấu hình DB trong demo in-memory.

## 2. RESTful API và HTTP methods

REST tổ chức API theo tài nguyên, dùng URL định danh tài nguyên và HTTP method biểu thị thao tác. API trao đổi dữ liệu bằng JSON. Tính stateless nghĩa là mỗi request mang đủ thông tin để xử lý, không phụ thuộc ngữ cảnh hội thoại của request trước.

| Method | Ý nghĩa | Endpoint trong bài |
|---|---|---|
| GET | Đọc danh sách | `/api/v1/categories?page=1&size=10` |
| GET | Đọc chi tiết | `/api/v1/categories/{id}` |
| POST | Tạo mới | `/api/v1/admin/categories` |
| PUT | Cập nhật đầy đủ theo DTO | `/api/v1/admin/categories/{id}` |
| DELETE | Xóa | `/api/v1/admin/categories/{id}` |

PATCH dùng để cập nhật một phần tài nguyên; bài sử dụng PUT. GET không nên thay đổi dữ liệu nghiệp vụ. PUT và DELETE có tính idempotent: lặp lại cùng request có cùng tác động cuối cùng lên tài nguyên, dù status trả về có thể khác nhau.

Trong Spring MVC, `@RestController` đánh dấu controller trả dữ liệu; `@RequestMapping` khai báo đường dẫn chung, các annotation `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping` ánh xạ HTTP method đến hàm xử lý.

## 3. Request và validation

| Cách nhận dữ liệu | Annotation | Ví dụ |
|---|---|---|
| Giá trị trong đường dẫn | `@PathVariable` | UUID trong `/categories/{id}` |
| Tham số truy vấn | `@RequestParam` | `page=1&size=10` |
| JSON body | `@RequestBody` | DTO tạo hoặc cập nhật Category |

Client gửi JSON với header `Content-Type: application/json`. Ví dụ body tạo mới:

```json
{
  "name": "Books",
  "slug": "books",
  "description": "Book category"
}
```

Body cập nhật còn yêu cầu `active` kiểu Boolean. `@Valid` kích hoạt validation trên DTO:

- `@NotBlank`: chuỗi không được null, rỗng hoặc chỉ chứa khoảng trắng.
- `@NotNull`: giá trị không được null.
- `@Size`: giới hạn độ dài.
- `@Pattern`: kiểm tra định dạng slug.

Validation kiểm tra dữ liệu đầu vào; service kiểm tra nghiệp vụ như slug trùng và ID không tồn tại.

## 4. Response và xử lý lỗi

API trả JSON qua DTO. `ResponseEntity` cho phép chỉ định HTTP status, header và body. Response thành công dùng `ApiResponse<T>` với các field `status`, `message`, `data`.

| HTTP status | Trường hợp |
|---|---|
| 200 OK | Đọc, cập nhật hoặc xóa thành công có body |
| 201 Created | Tạo mới thành công |
| 400 Bad Request | JSON hoặc dữ liệu đầu vào không hợp lệ |
| 404 Not Found | Không tìm thấy Category |
| 409 Conflict | Slug đã tồn tại |
| 500 Internal Server Error | Lỗi bất ngờ phía server |

Lỗi **4xx** liên quan đến request của client; lỗi **5xx** liên quan đến server. `@RestControllerAdvice` và `@ExceptionHandler` xử lý exception tập trung để trả cùng cấu trúc lỗi.

`ErrorResponse` gồm `status`, `message`, `errorCode`, `path`, `timestamp` và `fieldErrors`. Mã lỗi phân biệt lỗi nghiệp vụ; `fieldErrors` chỉ rõ field sai khi validation và được bỏ khỏi JSON khi rỗng. HTTP status thực tế phải tương ứng với lỗi, không chỉ ghi status trong body.

## 5. Phân trang in-memory

Phân trang giới hạn số phần tử trả về mỗi request. Bài quy ước **page bắt đầu từ 1**, mặc định `page=1`, `size=10`.

`PageResponse<CategoryResponse>` chứa:

- `data`: danh sách của trang hiện tại.
- `page`, `size`: trang được yêu cầu và số phần tử tối đa mỗi trang.
- `totalElements`: tổng phần tử trước khi cắt trang.
- `totalPages`: tổng số trang.

Với `page >= 1`, `size >= 1`:

```text
start = (page - 1) × size
end = min(start + size, totalElements)
totalPages = ceil(totalElements / size)
```

Lấy phần tử từ index `start` đến trước `end`. Ví dụ có 25 Category, `size=10` thì có 3 trang; trang 3 trả 5 phần tử. Theo quy ước trả trang rỗng, trang vượt giới hạn vẫn giữ tổng phần tử và tổng trang thực tế.

`PageResponse<T>` đã chứa `List<T>`, nên T là `CategoryResponse`. Project bọc `PageResponse` trong `ApiResponse`, vì vậy metadata phân trang nằm trong field `data` của response ngoài.

Ví dụ minh họa response của `GET /api/v1/categories?page=1&size=10` khi danh sách có một Category:

```json
{
  "status": 200,
  "message": "Categories retrieved successfully",
  "data": {
    "data": [
      {
        "id": "87b51c67-4770-41da-a2f4-9f78a722ebcb",
        "name": "Books",
        "slug": "books",
        "description": "Book category",
        "active": true,
        "createdAt": "2026-10-09T03:00:00Z",
        "updatedAt": "2026-10-09T03:00:00Z"
      }
    ],
    "page": 1,
    "size": 10,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

`data` ngoài là đối tượng phân trang; `data` bên trong là danh sách Category của trang hiện tại. `size=10` là giới hạn mỗi trang, nên danh sách vẫn có thể chỉ chứa một phần tử.

## 6. Lưu dữ liệu và kiểm thử

Phần 1 dùng `ArrayList<Category>`: thêm khi tạo, tìm theo UUID khi đọc/cập nhật và loại phần tử khi xóa. ID và timestamp được tự khởi tạo; thêm vào List không kích hoạt cơ chế tự sinh ID của JPA.

Dữ liệu nằm trong RAM, dùng chung giữa các request trong cùng lần chạy và mất khi restart. `ArrayList` không bảo đảm an toàn khi nhiều luồng cùng đọc/ghi; demo thực hiện request tuần tự.

Chạy từ thư mục `week-2/backend`:

```powershell
.\mvnw.cmd spring-boot:run
```

Dùng Postman với base URL `http://localhost:8080`, kiểm tra 5 API CRUD, phân trang và các trường hợp lỗi 400/404/409. Lấy UUID từ response tạo mới để gọi chi tiết, cập nhật và xóa. Link demo: [LinkDemo.md](./LinkDemo.md).

---

# Phần 2: Tích hợp Database (ORM)

## 1. ORM, JPA, Hibernate và Spring Data JPA

**ORM (Object-Relational Mapping)** ánh xạ đối tượng trong ứng dụng với dữ liệu trong database quan hệ: class tương ứng với bảng, field tương ứng với cột và đối tượng tương ứng với bản ghi. ORM giúp giảm SQL lặp lại cho CRUD, hỗ trợ quản lý quan hệ và theo dõi thay đổi của đối tượng. Khi dùng ORM vẫn cần hiểu SQL, transaction và cách truy vấn để tránh tải dữ liệu không cần thiết.

| Thành phần | Vai trò trong project |
|---|---|
| JPA (Jakarta Persistence) | Đặc tả chuẩn cho mapping và thao tác dữ liệu; cung cấp các annotation `jakarta.persistence` |
| Hibernate | Triển khai JPA, chuyển thao tác trên entity thành SQL |
| Spring Data JPA | Cung cấp repository và sinh truy vấn từ tên method |
| PostgreSQL | Lưu dữ liệu thực tế |
| Flyway | Quản lý phiên bản schema bằng migration SQL |

## 2. Kết nối PostgreSQL và quản lý schema

PostgreSQL chạy trong Docker qua `compose.yaml`, thuộc Compose project `training-typ`. Database và user đều là `backend-typ`; dữ liệu được lưu trong named volume nên vẫn còn khi restart ứng dụng hoặc tạo lại container với cùng volume.

Spring Boot dùng dependency `spring-boot-docker-compose` để đọc Compose và tự lấy thông tin kết nối PostgreSQL, vì vậy cấu hình hiện tại không cần khai báo `spring.datasource`. Docker Engine cần hoạt động trước khi chạy ứng dụng.

Các cấu hình trong `application-db.yml`:

| Cấu hình | Ý nghĩa |
|---|---|
| `ddl-auto: validate` | Hibernate kiểm tra schema khớp entity, không tự tạo bảng |
| `open-in-view: false` | Không giữ persistence context mở suốt quá trình xử lý web request |
| `show-sql: true` | Hiển thị SQL để theo dõi truy vấn |
| `lifecycle-management: start-only` | Khởi động Compose nhưng không dừng container khi ứng dụng tắt |
| `flyway.enabled: true` | Chạy migration khi khởi động |

Flyway áp dụng `db/migration/V1__create_catalog_tables.sql` để tạo các bảng `categories`, `products`, `product_images` và ghi nhận phiên bản trong `flyway_schema_history`. Migration đã áp dụng không chạy lại mỗi lần restart. Thay đổi schema tiếp theo được thêm bằng migration mới, ví dụ `V2__...sql`.

## 3. Entity mapping và quan hệ

Entity được đánh dấu bằng `@Entity`, ánh xạ tên bảng qua `@Table`. `@Id` khai báo khóa chính; `@Column` cấu hình tên cột, độ dài và nullability. `BaseEntity` dùng `@MappedSuperclass` để các entity kế thừa ID và timestamp.

Trong project:

- ID dùng UUID, sinh qua `@GeneratedValue(strategy = GenerationType.UUID)`.
- `@PrePersist` khởi tạo `createdAt`, `updatedAt` trước khi insert.
- `@PreUpdate` cập nhật `updatedAt` trước khi update entity.
- `Product.price` dùng `BigDecimal`, ánh xạ sang `NUMERIC(19, 2)`.
- `Product.status` dùng `@Enumerated(EnumType.STRING)` để lưu tên enum.

| Quan hệ | Mapping |
|---|---|
| Một Category có nhiều Product | `Category.products`: `@OneToMany(mappedBy = "category")` |
| Một Product thuộc một Category | `Product.category`: `@ManyToOne`, khóa ngoại `category_id` |
| Một Product có nhiều ProductImage | `Product.productImages`: `@OneToMany(mappedBy = "product")` |
| Một ProductImage thuộc một Product | `ProductImage.product`: `@ManyToOne`, khóa ngoại `product_id` |

Phía `@ManyToOne` giữ khóa ngoại; `mappedBy` chỉ thuộc tính quản lý quan hệ ở phía đối diện. Quan hệ Product–Category và ProductImage–Product dùng lazy loading để tải dữ liệu liên quan khi cần. Collection ảnh có `cascade = ALL`, `orphanRemoval = true`; các hàm thêm/xóa ảnh cập nhật cả hai phía quan hệ.

Migration còn khai báo unique constraint cho slug, foreign key và check constraint để bảo vệ dữ liệu tại DB. Phần API hiện thực hành CRUD Category; Product và ProductImage minh họa mapping quan hệ.

## 4. Chuyển CRUD từ List sang repository

Luồng xử lý mới: **Controller → Service → Repository → Hibernate → PostgreSQL**, sau đó map entity sang DTO response.

`CategoryRepository` kế thừa `JpaRepository<Category, UUID>`. Spring Data JPA cung cấp các thao tác cơ bản và tạo truy vấn từ tên method:

| Thao tác | Cách thực hiện hiện tại |
|---|---|
| Tạo | Kiểm tra `existsBySlug`, tạo entity rồi `saveAndFlush` |
| Đọc chi tiết | `findByIdAndActiveTrue`; không tìm thấy thì trả 404 |
| Cập nhật | `findById`, kiểm tra `existsBySlugAndIdNot`, cập nhật entity rồi `saveAndFlush` |
| Xóa | Query update `active=false`, đồng thời cập nhật `updatedAt` |
| Danh sách | Query category active với limit/offset và đếm tổng bằng `countByActiveTrue` |

DELETE hiện là **xóa mềm**: bản ghi vẫn tồn tại trong DB nhưng không xuất hiện trong GET danh sách/chi tiết. Unique slug áp dụng cho toàn bảng, bao gồm bản ghi inactive, nên kiểm tra trùng slug không lọc `active=true`.

Service DB không gọi các hàm khởi tạo in-memory; Hibernate sinh ID và thực hiện callback của entity. Với query update trực tiếp, callback `@PreUpdate` không chạy nên query xóa mềm tự gán `updatedAt`.

## 5. Transaction và flush

`@Transactional` xác định phạm vi transaction cho nghiệp vụ cập nhật/xóa; lỗi runtime mặc định dẫn đến rollback. Các hàm đọc dùng `@Transactional(readOnly = true)` để biểu thị mục đích chỉ đọc. Trong transaction, entity được quản lý và Hibernate theo dõi thay đổi bằng dirty checking.

`saveAndFlush()` lưu entity và đẩy thay đổi xuống DB ngay, giúp phát hiện lỗi unique trong lời gọi được bọc bởi `try/catch`. Flush chưa phải commit; transaction vẫn có thể rollback. Create hiện sử dụng transaction của method repository `saveAndFlush`, còn update/xóa có transaction ở service.

Kiểm tra slug trước khi lưu giúp trả thông báo nghiệp vụ rõ ràng; unique constraint ở DB vẫn cần thiết để bảo vệ khi nhiều request cùng tạo một slug. Vi phạm unique slug được trả về dưới dạng 409.

## 6. Phân trang từ DB và kiểm thử

API giữ quy ước trang bắt đầu từ 1. Service tính:

```java
long offset = (page - 1L) * size;
```

Repository dùng HQL với `where c.active = true`, `order by c.slug asc`, `limit :size offset :offset`. Hibernate chuyển thành truy vấn phân trang ở DB, thay vì tải toàn bộ dữ liệu rồi cắt List. Query tổng dùng cùng điều kiện active; `totalPages = ceil(totalElements / size)`.

Ví dụ `page=2`, `size=10` thì bỏ qua 10 bản ghi đầu và lấy tối đa 10 bản ghi tiếp theo. Controller kiểm tra `page >= 1`, `1 <= size <= 100`, trả lỗi 400 qua handler validation khi vi phạm.

Chạy từ thư mục `week-2/backend`, sau khi mở Docker Desktop:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=db"
```

Kiểm thử bằng Postman và đối chiếu trong DBeaver: CRUD Category, slug trùng, phân trang và xóa mềm. Với DELETE, bản ghi trong DB có `active=false`; API đọc không còn trả bản ghi đó. Link demo: [LinkDemo.md](./LinkDemo.md).
