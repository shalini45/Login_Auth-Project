# 🔐 Auth Service — JWT + Redis Authentication Microservice

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.0-brightgreen)
![Java](https://img.shields.io/badge/Java-22-orange)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue)
![Redis](https://img.shields.io/badge/Redis-7.0-red)
![JWT](https://img.shields.io/badge/JWT-0.12.5-purple)
![License](https://img.shields.io/badge/License-MIT-yellow)

A **production-style Authentication Microservice** built with Spring Boot,
implementing industry-standard security practices used in modern applications.

---

## 📌 Table of Contents

- [About the Project](#about-the-project)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Getting Started](#getting-started)
- [API Endpoints](#api-endpoints)
- [Security Features](#security-features)
- [Project Structure](#project-structure)
- [Environment Variables](#environment-variables)
- [Author](#author)

---

## 📖 About the Project

This project is a backend authentication microservice that handles:
- Secure user registration and login
- JWT-based stateless authentication
- Token management using Redis
- Role-based access control
- Protection against brute force attacks

It demonstrates real-world authentication patterns used in
microservice architectures at companies like Swiggy, Zomato, and Razorpay.

---

## ✨ Features

| Feature | Description |
|---|---|
| 🔐 JWT Authentication | Stateless auth with Access + Refresh tokens |
| 📧 Email Verification | OTP-based email verification on register |
| 🔄 Refresh Tokens | Auto token renewal without re-login |
| 🚪 Secure Logout | Token blacklisting via Redis |
| 🛡️ Rate Limiting | Max 5 login attempts per minute per IP |
| 🔒 Account Lockout | Account locked after 5 failed attempts |
| 👥 Role Based Access | ADMIN and USER role separation |
| 📩 Password Reset | OTP-based password reset via email |
| 📝 Swagger Docs | Interactive API documentation |
| 📊 Logging | Structured logging with SLF4J |
| ⚠️ Exception Handling | Global exception handler with clean responses |

---

## 🛠️ Tech Stack

| Technology | Version | Purpose |
|---|---|---|
| Java | 22 | Programming Language |
| Spring Boot | 3.3.0 | Backend Framework |
| Spring Security | 6.x | Security & Authentication |
| Spring Data JPA | 3.3.0 | Database ORM |
| Spring Data Redis | 3.3.0 | Redis Integration |
| MySQL | 8.0 | Primary Database |
| Redis | 7.0 | Token Storage & Caching |
| JWT (jjwt) | 0.12.5 | Token Generation |
| Lombok | Latest | Boilerplate Reduction |
| Swagger/OpenAPI | 2.5.0 | API Documentation |
| Maven | 3.9.x | Build Tool |

---

## 🏗️ Architecture
```
Client Request
      ↓
JwtAuthFilter (intercepts every request)
      ↓
Check token blacklisted in Redis?
      ↓
Extract username from JWT
      ↓
Spring Security validates token
      ↓
Controller → Service → Repository
      ↓
MySQL Database
```

### Token Flow:
```
Login → Access Token (15 min) + Refresh Token (7 days)
     ↓
Access Token expires
     ↓
Call /refresh → New Access Token
     ↓
Logout → Token blacklisted in Redis
```

---

## 🚀 Getting Started

### Prerequisites

Make sure you have these installed:
- Java 22
- Maven 3.9+
- MySQL 8.0
- Redis 7.0

### Installation

**1. Clone the repository**
```bash
git clone https://github.com/shalini45/Login_Auth-Project.git
cd Login_Auth-Project
```

**2. Create MySQL Database**
```sql
CREATE DATABASE auth_service_db;
```

**3. Configure environment**

Open `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/auth_service_db
spring.datasource.username=your_mysql_username
spring.datasource.password=your_mysql_password
spring.mail.username=your_gmail@gmail.com
spring.mail.password=your_gmail_app_password
jwt.secret=your_secret_key_minimum_32_characters
```

**4. Start Redis**
```bash
# Windows
Start-Service Redis

# Linux/Mac
redis-server
```

**5. Run the application**
```bash
mvn spring-boot:run
```

**6. Access Swagger UI**
```
http://localhost:8080/swagger-ui.html
```

---

## 📡 API Endpoints

### 🔓 Public APIs (No token required)

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/auth/register` | Register new user |
| POST | `/api/auth/login` | Login and get tokens |
| POST | `/api/auth/verify-email` | Verify email with OTP |
| POST | `/api/auth/resend-verification` | Resend verification OTP |
| POST | `/api/auth/forgot-password` | Request password reset OTP |
| POST | `/api/auth/reset-password` | Reset password with OTP |
| GET | `/api/auth/health` | Service health check |

### 🔐 User APIs (USER or ADMIN token required)

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/auth/logout` | Logout and invalidate token |
| POST | `/api/auth/refresh` | Get new access token |
| GET | `/api/user/profile` | Get own profile |
| PUT | `/api/user/change-password` | Change own password |

### 👑 Admin APIs (ADMIN token only)

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/admin/users` | Get all users |
| DELETE | `/api/admin/users/{id}` | Delete a user |
| PUT | `/api/admin/users/{id}/role` | Change user role |
| PUT | `/api/admin/users/{id}/unlock` | Unlock locked account |

---

## 🔒 Security Features

### 1. JWT Token Security
```
Access Token  → Expires in 15 minutes
Refresh Token → Expires in 7 days
Algorithm     → HS256
```

### 2. Rate Limiting
```
Login endpoint    → Max 5 attempts per minute per IP
Register endpoint → Max 3 attempts per minute per IP
Response          → 429 Too Many Requests
```

### 3. Account Lockout
```
Trigger  → 5 consecutive wrong passwords
Duration → 15 minutes
Storage  → Redis (auto expires)
Response → 423 Locked
```

### 4. Password Security
```
Algorithm → BCrypt
Strength  → Default (10 rounds)
```

### 5. Token Blacklisting
```
On logout → Access token added to Redis blacklist
           → Refresh token deleted from Redis
           → Any request with old token → 401
```

---

## 📁 Project Structure
```
src/
└── main/
    ├── java/
    │   └── com/authservice/
    │       ├── Config/
    │       │   ├── SecurityConfig.java
    │       │   ├── RedisConfig.java
    │       │   └── SwaggerConfig.java
    │       ├── Controller/
    │       │   ├── AuthController.java
    │       │   ├── UserProfileController.java
    │       │   └── AdminController.java
    │       ├── Service/
    │       │   ├── AuthService.java
    │       │   ├── JwtService.java
    │       │   ├── RedisTokenService.java
    │       │   ├── EmailService.java
    │       │   ├── EmailVerificationService.java
    │       │   ├── PasswordResetService.java
    │       │   ├── RateLimitService.java
    │       │   └── AccountLockoutService.java
    │       ├── Repository/
    │       │   └── UserRepository.java
    │       ├── entity/
    │       │   ├── User.java
    │       │   └── Role.java
    │       ├── dto/
    │       │   ├── RegisterRequest.java
    │       │   ├── LoginRequest.java
    │       │   ├── AuthResponse.java
    │       │   ├── ForgotPasswordRequest.java
    │       │   └── ResetPasswordRequest.java
    │       ├── Filter/
    │       │   └── JwtAuthFilter.java
    │       └── Exception/
    │           ├── CustomException.java
    │           └── GlobalExceptionHandler.java
    └── resources/
        └── application.properties
```

---

## ⚙️ Environment Variables

| Property | Description |
|---|---|
| `spring.datasource.url` | MySQL connection URL |
| `spring.datasource.username` | MySQL username |
| `spring.datasource.password` | MySQL password |
| `spring.data.redis.host` | Redis host |
| `spring.data.redis.port` | Redis port |
| `jwt.secret` | JWT signing secret (min 32 chars) |
| `jwt.expiration` | Access token expiry in ms (900000 = 15 min) |
| `jwt.refresh.expiration` | Refresh token expiry in ms (604800000 = 7 days) |
| `spring.mail.username` | Gmail address for sending emails |
| `spring.mail.password` | Gmail app password |

---

## 👩‍💻 Author

**Shalini**
- GitHub: [@shalini45](https://github.com/shalini45)

---

## 📄 License

This project is licensed under the MIT License.

---

⭐ If you found this project helpful, please give it a star!