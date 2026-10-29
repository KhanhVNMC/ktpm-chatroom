# HỆ THỐNG PHÒNG CHAT ONLINE

## 1. Bài toán

Hệ thống CHATROOM: Hệ thống phòng chat trực tuyến hỗ trợ nhiều người dùng trao đổi thông tin qua tin nhắn hoặc gửi file theo thời gian thực.


## 2. Nghiệp vụ

### 2.1. Tài khoản và xác thực

1. Người dùng có đăng ký, đăng nhập bằng tài khoản & mật khẩu và đăng xuất.
2. Người dùng có thể xem hồ sơ của mình, cập nhật tên hiển thị, ảnh đại diện.


### 2.2. Quản lý phòng chat

1. Người dùng tạo phòng với tên & chế độ truy cập phòng. Phòng private sử dụng mã mời/link.
2. Người dùng xem thông tin tổng quan, danh sách phòng công khai hoặc danh sách phòng mình đang tham gia.
4. Người dùng tham gia một phòng mà bị ban, hệ thống sẽ từ chối người dùng đó.
5. Thành viên có thể rời phòng.
6. Người có quyền quản lý phòng có thể cập nhật thông tin phòng, đặt hoặc gỡ ảnh đại diện và đổi mã mời.
7. Chủ phòng có thể xóa phòng hoặc chuyển quyền sở hữu cho thành viên khác.


### 2.3. Quản lý thành viên và phân quyền

1. Thành viên có các quyền riêng trong phòng, bao gồm gửi, sửa và xóa tin nhắn; quản lý phòng; kick, ban, mute thành viên; và cấp quyền.
2. Chủ phòng có thể chuyển quyền sở hữu.
3. Trạng thái muted hạn chế khả năng gửi tin nhắn. Thành viên bị ban không thể tham gia lại cho đến khi được unban.
4. Danh sách thành viên bao gồm thông tin về quyền, thông tin của thành viên bị ban chỉ hiển thị cho người có quyền quản lý ban.

### 2.4. Tin nhắn và file

1. Thành viên được cấp quyền gửi tin nhắn có thể gửi nội dung văn bản, file.
2. Người dùng có quyền xem lịch sử tin nhắn theo từng trang và tìm kiếm nội dung trong phòng. Quyền xem phụ thuộc vào chế độ công khai và trạng thái tham gia phòng.
3. Người gửi có thể chỉnh sửa tin nhắn của mình theo quy tắc phân quyền của phòng.
4. Người có quyền phù hợp có thể xóa tin nhắn người khác.


### 2.5. Cập nhật thời gian thực

1. Người dùng đã xác thực mở kết nối `GET /api/events` với header `Accept: text/event-stream`.
2. Các sự kiện gồm kết nối, tin nhắn mới, chỉnh sửa hoặc xóa tin nhắn; thành viên tham gia, rời phòng hoặc bị quản trị; thay đổi quyền, chủ phòng, thông tin phòng và hồ sơ người dùng.
3. Khi người dùng đăng xuất, các luồng SSE thuộc phiên đó được đóng. Theo thiết kế hiện tại, sự kiện phát sinh trong lúc mất kết nối không được phát lại khi kết nối lại.

## 3. API Documentation (In [OpenAPI](src/main/resources/openapi.yaml) )

### Tệp tải lên (Assets)
| Method | Endpoint | Mô tả | Input | Phản hồi thành công |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/assets/{id}` | Tải tệp đã tải lên |   | `200`, nội dung tệp |

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
| `GET` | `/api/auth/me` | Lấy thông tin cá nhân của người dùng hiện tại |   | `200`, thông tin người dùng |
| `PATCH` | `/api/user/me` | Đổi tên hiển thị, trạng thái, status text | Tên hiển thị, trạng thái, status text | `200`, thông tin người dùng |
| `GET` | `/api/user/{id}` | Lấy thông tin của một người dùng bất kỳ |   | `200`, thông tin người dùng |
| `GET` | `/api/user/{id}/avatar` | Lấy ảnh đại diện của người dùng |   | `200`, tệp ảnh |
| `PUT` | `/api/user/me/avatar` | Đặt ảnh đại diện (chỉ nhận ảnh) | Nội dung tệp, tên tệp | `200`, thông tin người dùng |
| `DELETE` | `/api/user/me/avatar` | Xóa ảnh đại diện |   | `200`, thông tin người dùng |


### Quản lý Thành viên (Room members)
| Method | Endpoint | Mô tả | Input | Phản hồi thành công |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/chatrooms/{id}/members` | Lấy danh sách thành viên của phòng chat |   | `200`, danh sách thành viên |
| `POST` | `/api/chatrooms/{id}/members/{userId}/{action}` | Kick, ban, unban, mute hoặc unmute một thành viên | Lý do | `200`, `{}` |
| `PUT` | `/api/chatrooms/{id}/members/{userId}/permissions` | Đặt quyền cho một thành viên | Permission bits | `200`, `{}` |
| `POST` | `/api/chatrooms/{id}/transfer-ownership` | Chuyển quyền chủ phòng | ID chủ phòng mới | `200`, `{}` |

### Quản lý Phòng chat (Chatrooms)
| Method | Endpoint | Mô tả | Input | Phản hồi thành công |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/chatrooms` | Tạo phòng chat mới | Tên phòng, lời chào, thiết lập công khai, mã mời | `201`, trả về ID phòng, mã mời và link mời |
| `GET` | `/api/chatrooms/listing?limit={n}`| Lấy danh sách các phòng chat công khai |   | `200`, danh sách phòng công khai |
| `GET` | `/api/chatrooms/mine?limit={n}`| Lấy danh sách các phòng mình đang tham gia |   | `200`, danh sách phòng |
| `GET` | `/api/chatrooms/{id}/overview` | Thông tin tổng quan của phòng chat |   | `200`, thông tin tổng quan |
| `PATCH` | `/api/chatrooms/{id}` | Đổi tên, lời chào của phòng chat | Tên mới, lời chào mới | `200`, thông tin phòng |
| `DELETE` | `/api/chatrooms/{id}` | Xóa phòng chat |   | `200`, `{}` |
| `GET` `PUT` `DELETE`| `/api/chatrooms/{id}/avatar` | Lấy/Đổi/Xóa ảnh đại diện của phòng chat |   | `200`, ảnh đại diện |
| `POST` | `/api/chatrooms/{id}/invite-code` | Đổi mã mời của phòng chat | Mã mời mới | `200`, mã mời và link mời |
| `GET` | `/api/chatrooms/invite/{code}` | Xem trước phòng chat bằng mã mời |   | `200`, thông tin tổng quan |
| `POST` | `/api/chatrooms/invite/{code}/join` | Tham gia phòng chat bằng mã mời |   | `200`, ID phòng |
| `POST` | `/api/chatrooms/{id}/join` | Tham gia vào phòng chat | Mã mời phòng | `200`, `{}` |
| `POST` | `/api/chatrooms/{id}/leave` | Rời khỏi phòng chat | Rời ẩn danh, lý do | `200`, `{}` |

### Hệ thống Tin nhắn (Messages)
| Method | Endpoint | Mô tả | Input | Phản hồi thành công |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/chatrooms/{id}/history` | Tải lịch sử tin nhắn của phòng chat |   | `200`, danh sách tin nhắn |
| `GET` | `/api/chatrooms/{id}/search?q={text}` | Tìm kiếm tin nhắn trong phòng chat |   | `200`, danh sách tin nhắn |
| `POST` | `/api/chatrooms/{id}/messages` | Gửi tin nhắn mới vào phòng chat | Nội dung tin nhắn, danh sách file tải lên | `201`, tin nhắn vừa gửi |
| `PATCH` `DELETE`| `/api/chatrooms/{id}/messages/{messageId}`| Chỉnh sửa/Xóa nội dung tin nhắn đã gửi | Nội dung mới, giữ hay bỏ file đính kèm | `200`, tin nhắn sau khi sửa |

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

### Sự kiện thời gian thực (Server-Sent Events)
| Method | Endpoint | Mô tả | Input | Phản hồi thành công |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/events` | Nhận sự kiện của các phòng chat đang tham gia | Header `Accept: text/event-stream` | `200`, luồng `text/event-stream` |

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


## 4. Project Structure

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
```
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
| Deployment | Docker |  Package & Deploy |
|       |            |         |
## 6. How to run
