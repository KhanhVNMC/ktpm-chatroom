# HỆ THỐNG PHÒNG CHAT ONLINE

## 1. Bài toán
 
## 2. Nghiệp vụ

## 3. Tài liệu API (API Documentation)

### Kiểm tra trạng thái hệ thống
| Method | Endpoint | Mô tả | Input | Thành công |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/health` | Kiểm tra hệ thống có đang hoạt động hay không |   | `200`, nội dung text `OK` |

### Xác thực và Người dùng (Authentication & User)
| Method | Endpoint | Mô tả | Input | Phản hồi thành công |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/register` | Đăng ký tài khoản mới | Username, password, displayname | `201`, thông tin người dùng |
| `POST` | `/api/auth/login` | Đăng nhập vào hệ thống | Username, password | `200`, trả về session id |
| `POST` | `/api/auth/logout` | Đăng xuất khỏi hệ thống |   | `200`, `{}` |
| `GET` | `/api/auth/me` | Lấy thông tin cá nhân của người dùng hiện tại |   | Thông tin user |
| `GET` | `/api/user/{id}` | Lấy thông tin của một người dùng bất kỳ |   | `200`, thông tin người dùng |
| `PATCH` | `/api/user/me` | Cập nhật thông tin cá nhân | Tên hiển thị, ảnh đại diện, trạng thái hoạt động, dòng trạng thái | `200`, thông tin người dùng |

### Tệp tải lên (Assets)
| Method | Endpoint | Mô tả | Input | Phản hồi thành công |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/assets` | Tải một tệp lên server | Nội dung tệp, tên tệp | `201`, id và đường dẫn của tệp |
| `GET` | `/api/assets/{id}` | Tải tệp đã tải lên |   | `200`, nội dung tệp |

### Quản lý Phòng chat (Chatrooms)
| Method | Endpoint | Mô tả | Input | Phản hồi thành công |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/chatrooms` | Tạo phòng chat mới | Tên phòng, lời chào, thiết lập công khai, mã mời | `201`, trả về ID phòng, mã mời và link mời |
| `GET` | `/api/chatrooms/listing?limit={n}`| Lấy danh sách các phòng chat công khai |   | `200`, danh sách phòng công khai |
| `GET` | `/api/chatrooms/mine?limit={n}`| Lấy danh sách các phòng mình đang tham gia |   | `200`, danh sách phòng |
| `GET` | `/api/chatrooms/{id}/overview` | Thông tin tổng quan của phòng chat |   | `200`, thông tin tổng quan |
| `PATCH` | `/api/chatrooms/{id}` | Đổi tên, lời chào của phòng chat | Tên mới, lời chào mới | `200`, thông tin phòng |
| `DELETE` | `/api/chatrooms/{id}` | Xóa phòng chat |   | `200`, `{}` |
| `GET` | `/api/chatrooms/{id}/avatar` | Lấy ảnh đại diện của phòng chat |   | `200`, ảnh đại diện |
| `PUT` | `/api/chatrooms/{id}/avatar` | Đặt ảnh đại diện của phòng chat | Nội dung ảnh, tên tệp | `200`, thông tin phòng |
| `DELETE` | `/api/chatrooms/{id}/avatar` | Gỡ ảnh đại diện của phòng chat |   | `200`, thông tin phòng |
| `POST` | `/api/chatrooms/{id}/invite-code` | Đổi mã mời của phòng chat | Mã mời mới | `200`, mã mời và link mời |
| `GET` | `/api/chatrooms/invite/{code}` | Xem trước phòng chat bằng mã mời |   | `200`, thông tin tổng quan |
| `POST` | `/api/chatrooms/invite/{code}/join` | Tham gia phòng chat bằng mã mời |   | `200`, ID phòng |
| `POST` | `/api/chatrooms/{id}/join` | Tham gia vào phòng chat | Mã mời phòng | `200`, `{}` |
| `POST` | `/api/chatrooms/{id}/leave` | Rời khỏi phòng chat | Rời ẩn danh, lý do | `200`, `{}` |

### Quản lý Thành viên (Room members)
| Method | Endpoint | Mô tả | Input | Phản hồi thành công |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/chatrooms/{id}/members` | Lấy danh sách thành viên của phòng chat |   | `200`, danh sách thành viên |
| `POST` | `/api/chatrooms/{id}/members/{userId}/{action}` | Kick, ban, unban, mute hoặc unmute một thành viên | Lý do | `200`, `{}` |
| `PUT` | `/api/chatrooms/{id}/members/{userId}/permissions` | Đặt quyền cho một thành viên | Permission bits | `200`, `{}` |
| `POST` | `/api/chatrooms/{id}/transfer-ownership` | Chuyển quyền chủ phòng | ID chủ phòng mới | `200`, `{}` |

Permission bits:

| Giá trị | Quyền |
| :--- | :--- |
| `1` | `SEND_MESSAGES` |
| `2` | `EDIT_MESSAGES` |
| `4` | `DELETE_MESSAGES` |
| `8` | `KICK_MEMBERS` |
| `16` | `BAN_MEMBERS` |
| `32` | `MANAGE_ROOM` |
| `64` | `MUTE_MEMBERS` |
| `128` | `GRANT_PERMISSIONS` |

### Hệ thống Tin nhắn (Messages)
| Method | Endpoint | Mô tả | Input | Phản hồi thành công |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/chatrooms/{id}/history?limit={n}&before_message={uuid}` | Tải lịch sử tin nhắn của phòng chat |   | `200`, danh sách tin nhắn |
| `GET` | `/api/chatrooms/{id}/search?q={text}&limit={n}&before_message={uuid}` | Tìm kiếm tin nhắn trong phòng chat |   | `200`, danh sách tin nhắn |
| `POST` | `/api/chatrooms/{id}/messages` | Gửi tin nhắn mới vào phòng chat | Nội dung tin nhắn, danh sách file tải lên | `201`, tin nhắn vừa gửi |
| `PATCH`| `/api/chatrooms/{id}/messages/{messageId}`| Chỉnh sửa nội dung tin nhắn đã gửi | Nội dung mới, giữ hay bỏ file đính kèm | `200`, tin nhắn sau khi sửa |
| `DELETE`| `/api/chatrooms/{id}/messages/{messageId}`| Xóa một tin nhắn |   | `200`, `{}` |

### Sự kiện thời gian thực (Server-Sent Events)
| Method | Endpoint | Mô tả | Input | Phản hồi thành công |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/events` | Nhận sự kiện của các phòng chat đang tham gia | Header `Accept: text/event-stream` | `200`, luồng `text/event-stream` |

Mỗi data segment có dạng `{"type": <loại sự kiện>, "payload": "<chuỗi JSON>"}`.

| Loại sự kiện | Mô tả |
| :--- | :--- |
| `CONNECTED` | Kết nối thành công |
| `MESSAGE_CREATED` | Có tin nhắn mới |
| `MESSAGE_EDITED` | Tin nhắn được chỉnh sửa |
| `MESSAGE_DELETED` | Tin nhắn bị xóa |
| `MEMBER_JOINED` | Có thành viên vào phòng |
| `MEMBER_LEFT` | Có thành viên rời phòng |
| `MEMBER_KICKED` | Thành viên bị kick |
| `MEMBER_BANNED` | Thành viên bị ban |
| `MEMBER_UNBANNED` | Thành viên được unban |
| `MEMBER_MUTED` | Thành viên bị mute |
| `MEMBER_UNMUTED` | Thành viên được unmute |
| `MEMBER_PERMISSIONS_CHANGED` | Quyền của thành viên thay đổi |
| `OWNER_TRANSFERRED` | Chủ phòng thay đổi |
| `ROOM_UPDATED` | Thông tin phòng thay đổi |
| `ROOM_DELETED` | Phòng chat bị xóa |
| `USER_UPDATED` | Thông tin người dùng thay đổi |



## 4. Technology Used

| Layer | Technology | Purpose |
|---|---|---|
| Runtime | Java | Application runtime and language level |
| Web | Javalin | HTTP API, routing, and static client serving |
| Serialization | Jackson Databind | JSON request and response handling |
| Authentication | jBCrypt | Password hashing and verification |
| ORM | Hibernate ORM | JPA-based persistence |
| Database | PostgreSQL | Persistent users, credentials, sessions, rooms, memberships, messages, and asset metadata |
| Logging | SLF4J Simple | Application logging |
| Frontend | HTML, CSS, JavaScript  | Static browser-based chat client |
| Testing | JUnit Jupiter | Java tests |

## 5. Project Structure

```text
ktpm-chatroom/


├── src/
│   └── main/
│       ├── java/
│       │   └── dev/ktpm/
│       │       ├── api/           HTTP controllers, auth middleware, SSE
│       │       │   └── dto/       API request and response data types
│       │       ├── consts/        Enums and permission constants
│       │       ├── impl/          Implementation for interfaces
│       │       │   ├── jpa/
│       │       │   │   ├── entities/     JPA persistence entities
│       │       │   │   └── repos/        JPA repository implementations
│       │       │   └── service/       Business logic implementations
│       │       ├── model/         Domain models
│       │       ├── repository/    Repository interfaces
│       │       ├── service/       Service interfaces
│       │       └── App.java       Application startup and dependency wiring
│       └── resources/
│           ├── public/            Browser client: HTML, CSS, JavaScript
│           ├── hibernate.cfg.xml  Hibernate and database configuration
│           └── openapi.yaml       OpenAPI API specification
├── plan.adml                      Project plan
└── README.md                      Project overview and API documentation
