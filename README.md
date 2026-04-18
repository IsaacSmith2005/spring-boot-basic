# 📚 Hệ thống Authentication & Authorization với Java Spring Boot

## 📋 Mục lục
- [Tổng quan](#tổng-quan)
- [Cấu trúc dự án](#cấu-trúc-dự-án)
- [Kiến trúc hệ thống](#kiến-trúc-hệ-thống)
- [Luồng xử lý](#luồng-xử-lý)
- [Chi tiết các thành phần](#chi-tiết-các-thành-phần)
- [API Documentation](#api-documentation)
- [Cài đặt và chạy](#cài-đặt-và-chạy)

---

## 🎯 Tổng quan

Hệ thống Authentication & Authorization được xây dựng với Spring Boot 3.x, sử dụng JWT token và cơ chế token blacklist để đảm bảo bảo mật. Hệ thống hỗ trợ:
- ✅ Đăng nhập/Đăng xuất an toàn
- ✅ Quản lý token với cơ chế blacklist
- ✅ Phân quyền dựa trên Role
- ✅ Introspect token để kiểm tra validity
- ✅ Bảo mật đa lớp

---

## 🏗️ Cấu trúc dự án

```
src/main/java/com/example/identity/
├── 📁 config/                    # Cấu hình hệ thống
│   ├── 📄 SecurityConfig.java    # Cấu hình Spring Security
│   ├── 📄 CustomJwtDecoder.java  # Custom JWT decoder với blacklist
│   └── 📄 ApplicationInitConfig.java # Khởi tạo dữ liệu mặc định
├── 📁 controller/                # Layer điều khiển API
│   └── 📄 AuthenticationController.java
├── 📁 service/                   # Layer xử lý business logic
│   └── 📄 AuthenticationService.java
├── 📁 repository/                # Layer truy cập dữ liệu
│   ├── 📄 UserRepository.java
│   └── 📄 InvalidatedTokenRepository.java
├── 📁 entity/                    # Đối tượng database
│   ├── 📄 User.java
│   └── 📄 InvalidatedToken.java
├── 📁 dto/                       # Data Transfer Objects
│   ├── 📁 request/
│   └── 📁 response/
├── 📁 exception/                 # Xử lý exception tập trung
│   └── 📄 GlobalExceptionHandle.java
└── 📁 enums/                     # Enum constants
    └── 📄 Role.java
```

---

## 🔄 Luồng xử lý Authentication

### 1. Đăng nhập (Login)
```
Client → POST /auth/login → AuthenticationController → AuthenticationService → 
verifyPassword → generateToken → return JWT
```

### 2. Đăng xuất (Logout)
```
Client → POST /auth/logout → AuthenticationController → AuthenticationService → 
verifyToken → saveToBlacklist → return success
```

### 3. Xác thực Token (Authentication)
```
Client → API + Bearer Token → Spring Security Filter → CustomJwtDecoder → 
AuthenticationService.introspect → checkBlacklist → proceed/deny
```

---

## 📦 Chi tiết các thành phần

### 🔐 SecurityConfig.java
**Chức năng:** Cấu hình Spring Security và OAuth2 Resource Server

**Các method chính:**
```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity httpSecurity)
// - Cấu hình các endpoint công khai (/auth/login, /auth/logout, etc.)
// - Thiết lập OAuth2 Resource Server với JWT
// - Config CustomJwtDecoder và authentication converter
// - Tắt CSRF cho stateless JWT authentication

@Bean
JwtAuthenticationConverter jwtAuthenticationConverter()
// - Chuyển đổi JWT claims sang Spring Security authorities
// - Thêm prefix "ROLE_" vào các quyền từ token
// - Map scope claim sang authorities

@Bean
JwtDecoder jwtDecoder()
// - Tạo JWT decoder với secret key từ application.properties
// - Sử dụng thuật toán HS256
// - Dùng làm fallback decoder

@Bean
PasswordEncoder passwordEncoder()
// - Tạo BCrypt password encoder với strength 10
// - Dùng để hash và verify password
```

### 🔑 CustomJwtDecoder.java
**Chức năng:** Custom JWT decoder với tích hợp blacklist checking

**Flow hoạt động:**
```java
public Jwt decode(String token) throws JwtException
{
    // 1. Gọi AuthenticationService.introspect() để kiểm tra token
    var response = authenticationService.introspect(
        IntrospectRequest.builder().token(token).build());
    
    // 2. Nếu token invalid → ném JwtException
    if (!response.isValid()) {
        throw new JwtException("Token invalid");
    }
    
    // 3. Tạo NimbusJwtDecoder nếu chưa có (thử HS256 trước, fallback HS512)
    // 4. Decode và trả về Jwt object cho Spring Security
}
```

### 🎮 AuthenticationController.java
**Chức năng:** Điều khiển các API authentication endpoints

**Endpoints:**
```java
@PostMapping("/login")
// - Xử lý đăng nhập người dùng
// - Nhận AuthenticationRequest (username, password)
// - Trả về AuthenticationResponse (token, authenticated status)

@PostMapping("/introspect") 
// - Kiểm tra token validity
// - Nhận IntrospectRequest (token)
// - Trả về IntrospectResponse (valid: true/false)

@PostMapping("/logout")
// - Xử lý đăng xuất người dùng
// - Nhận LogoutRequest (token)
// - Lưu token vào blacklist để vô hiệu hóa
// - Trả về message "Đã logout thành công"
```

### ⚙️ AuthenticationService.java
**Chức năng:** Core logic xử lý authentication business

**Các method chính:**
```java
public AuthenticationResponse authenticate(AuthenticationRequest request)
// 1. Tìm user trong database bằng username
// 2. Verify password với BCrypt encoder
// 3. Nếu password đúng → generate JWT token
// 4. Trả về token cho client để sử dụng trong các request sau

public void logout(LogoutRequest request)
// 1. Verify token signature và expiration time
// 2. Extract JWT ID (jti claim) từ token
// 3. Lưu token vào invalidated_tokens table (blacklist)
// 4. Token không thể tái sử dụng sau khi logout

private String generateToken(User user)
// 1. Tạo JWT header với thuật toán HS256
// 2. Tạo claims set:
//    - subject: username
//    - issuer: "Haya"
//    - issueTime: thời gian tạo
//    - expirationTime: hiện tại + 1 giờ
//    - jwtID: UUID duy nhất cho token
//    - scope: các role của user
// 3. Sign token với secret key
// 4. Serialize và trả về JWT string

private SignedJWT verifyToken(String token)
// 1. Parse JWT token string
// 2. Verify signature với MACVerifier và secret key
// 3. Check expiration time (không được hết hạn)
// 4. KIỂM TRA BLACKLIST - quan trọng nhất!
//    - Lấy JWT ID (jti) từ token
//    - Query trong invalidated_tokens table
//    - Nếu tồn tại → ném exception "Token already invalidated"
// 5. Trả về SignedJWT nếu tất cả kiểm tra đều hợp lệ

private String buildScope(User user)
// - Tạo scope string từ user roles
// - Format: "ROLE_ADMIN ROLE_USER" (nếu có nhiều roles)
// - Dùng để set trong JWT claims
```

### 👤 User.java (Entity)
**Chức năng:** Đối tượng người dùng trong database

**Các trường và annotations:**
```java
@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = PRIVATE)
public class User {
    @GeneratedValue(strategy = GenerationType.UUID)
    @Id
    String id;                    // UUID primary key
    
    String username;              // Tên đăng nhập (unique)
    
    String email;                 // Email người dùng
    
    @JsonIgnore
    @Column(name = "birth_year")
    Integer birthYear;            // Năm sinh (nullable, không trả về trong JSON)
    
    String password;              // Password đã hash với BCrypt
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", 
        joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    Set<String> roles;            // Set of roles (ADMIN, USER)
}
```

### 🚫 InvalidatedToken.java (Entity)
**Chức năng:** Lưu trữ các token đã bị hủy (blacklist mechanism)

**Các trường:**
```java
@Entity
@Table(name = "invalidated_tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = PRIVATE)
public class InvalidatedToken {
    @Id
    String id;            // JWT ID (jti claim) - primary key
    
    Date expiryTime;      // Thời gian hết hạn token (để cleanup)
}
```

### 🛡️ GlobalExceptionHandle.java
**Chức năng:** Xử lý centralized exception handling với @RestControllerAdvice

**Các handler method:**
```java
@ExceptionHandler(RuntimeException.class)
// Xử lý runtime exceptions chung
// Trả về 400 Bad Request với message

@ExceptionHandler(MethodArgumentNotValidException.class)
// Xử lý validation errors từ @Valid
// Trả về 400 với field validation errors

@ExceptionHandler(AuthenticateException.class)
// Xử lý authentication failures (sai username/password)
// Trả về 401 Unauthorized

@ExceptionHandler(AccessDeniedException.class)
// Xử lý authorization failures (không có quyền)
// Trả về 403 Forbidden

@ExceptionHandler(JwtException.class)
// Xử lý JWT token errors (token invalid/expired)
// Trả về 401 Unauthorized với custom message
```

### 🚀 ApplicationInitConfig.java
**Chức năng:** Khởi tạo dữ liệu mặc định khi ứng dụng start lên

**Method:**
```java
@Bean
ApplicationRunner applicationRunner(UserRepository userRepository)
// - Chạy khi Spring context khởi tạo xong
// - Kiểm tra xem user "admin" đã tồn tại chưa
// - Nếu chưa → tạo user admin mặc định:
//   * Username: "admin"
//   * Password: "admin" (đã hash với BCrypt)
//   * Email: "admin@example.com"
//   * Role: ADMIN
// - Log warning để nhắc admin thay đổi password
```

---

## 📡 API Documentation

### Authentication Endpoints

#### 1. Đăng nhập
```http
POST /auth/login
Content-Type: application/json

{
    "username": "admin",
    "password": "admin"
}
```

**Response thành công:**
```json
{
    "code": 1000,
    "result": {
        "token": "eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJIYXlhIiwic3ViIjoiYWRtaW4i...",
        "authenticated": true
    }
}
```

#### 2. Kiểm tra token
```http
POST /auth/introspect
Content-Type: application/json

{
    "token": "eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJIYXlhIiwic3ViIjoiYWRtaW4i..."
}
```

**Response token hợp lệ:**
```json
{
    "code": 1000,
    "result": {
        "valid": true
    }
}
```

**Response token đã logout/hết hạn:**
```json
{
    "code": 1000,
    "result": {
        "valid": false
    }
}
```

#### 3. Đăng xuất
```http
POST /auth/logout
Content-Type: application/json
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...

{
    "token": "eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJIYXlhIiwic3ViIjoiYWRtaW4i..."
}
```

**Response thành công:**
```json
{
    "code": 1000,
    "result": "Đã logout thành công"
}
```

**Response với token đã logout:**
```json
{
    "code": 1005,
    "message": "Token invalid"
}
```

---

## 🔧 Cấu hình và chạy

### Environment Variables
```properties
# application.properties
jwt.secret=your-secret-key-minimum-256-bits-long
spring.datasource.url=jdbc:sqlserver://localhost:1433;database=identity;encrypt=false;trustServerCertificate=true
spring.datasource.username=sa
spring.datasource.password=your-password
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

### Default Admin Account
- **Username:** admin
- **Password:** admin
- **Role:** ADMIN

⚠️ **Quan trọng:** Thay đổi password admin mặc định sau lần đăng nhập đầu tiên!

### Các bước chạy ứng dụng:
1. Clone repository
2. Cấu hình database trong `application.properties`
3. Chạy với Maven: `mvn spring-boot:run`
4. Ứng dụng sẽ start tại `http://localhost:8080`

---

## 🔄 Data Flow Diagram

```
1. LOGIN FLOW:
   Client → POST /auth/login → AuthenticationController
   → AuthenticationService.authenticate()
   → UserRepository.findByUsername()
   → BCrypt.matches() password
   → generateToken() → Return JWT

2. API CALL FLOW:
   Client → API + Bearer Token → Spring Security Filter Chain
   → CustomJwtDecoder.decode()
   → AuthenticationService.introspect()
   → verifyToken() → checkBlacklist()
   → Valid: Proceed to Controller | Invalid: 401 Error

3. LOGOUT FLOW:
   Client → POST /auth/logout → AuthenticationController
   → AuthenticationService.logout()
   → verifyToken() → Extract JWT ID
   → InvalidatedTokenRepository.save()
   → Return success message
```

---

## 🛠️ Công nghệ sử dụng

- **Spring Boot 3.x** - Main framework
- **Spring Security 6.x** - Authentication & Authorization
- **Spring Data JPA** - Database access layer
- **JWT (Nimbus JOSE + JWT)** - Token generation và validation
- **BCrypt** - Password hashing algorithm
- **SQL Server** - Database system
- **Lombok** - Code generation (getter, setter, builder, etc.)
- **Maven** - Build và dependency management

---

## 🔒 Tính năng bảo mật

### 1. JWT Token Authentication
- Stateless authentication - không cần server-side session
- Token expiration tự động sau 1 giờ
- Unique JWT ID (jti claim) để tracking

### 2. Token Blacklist Mechanism
- Tokens được lưu trong database khi logout
- Tự động cleanup expired tokens
- Ngăn chặn việc tái sử dụng token sau logout

### 3. Role-Based Access Control (RBAC)
- Hỗ trợ ADMIN và USER roles
- Method-level security với @PreAuthorize
- Custom authority mapping từ JWT scope

### 4. Password Security
- BCrypt hashing với strength 10
- Secure password verification
- Password không được trả về trong JSON responses

### 5. Spring Security Configuration
- OAuth2 Resource Server setup
- Custom JWT decoder integration
- CORS và CSRF protection

---

## 📝 Ghi chú quan trọng

1. **Secret Key**: JWT secret phải dài ít nhất 256 bits cho HS256
2. **Token Storage**: Client nên lưu token ở secure storage (httpOnly cookies hoặc secure local storage)
3. **Password Policy**: Nên implement password policy cho production
4. **Rate Limiting**: Nên thêm rate limiting cho authentication endpoints
5. **Logging**: Logging có thể được cải thiện cho security monitoring
6. **Token Refresh**: Có thể implement refresh token mechanism cho better UX

---

## 📞 Hỗ trợ

Nếu có câu hỏi hoặc vấn đề, vui lòng liên hệ:
- Email: support@example.com
- Documentation: [Wiki](https://github.com/your-repo/wiki)

---

**© 2026 - Haya Identity Service. All rights reserved.**
