# HỆ THỐNG PHÒNG CHAT ONLINE

## 1. Bài toán
 
## 2. Nghiệp vụ

## 3. Tài liệu API (API Documentation)

### Kiểm tra trạng thái hệ thống
| Method | Endpoint | Mô tả | Input |   |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/health` | Kiểm tra hệ thống có đang hoạt động hay không |   | `200`, nội dung text `OK` |

### Xác thực và Người dùng (Authentication & User)
| Method | Endpoint | Mô tả | Input | Phản hồi thành công |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/register` | Đăng ký tài khoản người dùng mới | Tài khoản, mật khẩu, tên hiển thị | `201`, thông tin người dùng |
| `POST` | `/api/auth/login` | Đăng nhập vào hệ thống | Tài khoản, mật khẩu | `200`, trả về session id |
| `POST` | `/api/auth/logout` | Đăng xuất khỏi hệ thống |   | `200`, `{}` |
| `GET` | `/api/auth/me` | Lấy thông tin cá nhân của người dùng hiện tại |   | (Thành công trả về thông tin user) |
| `GET` | `/api/user/{id}` | Lấy thông tin chi tiết của một người dùng bất kỳ |   | `200`, thông tin người dùng |

### Quản lý Phòng chat (Chatrooms)
| Method | Endpoint | Mô tả | Input | Phản hồi thành công |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/chatrooms` | Tạo phòng chat mới | Lời chào, thiết lập công khai, mã mời | `201`, trả về ID phòng và mã mời |
| `GET` | `/api/chatrooms/listing?limit={n}`| Lấy danh sách các phòng chat công khai |   | `200`, danh sách phòng công khai |
| `GET` | `/api/chatrooms/{id}/overview` | Xem thông tin tổng quan của một phòng chat |   | `200`, thông tin tổng quan |
| `POST` | `/api/chatrooms/{id}/join` | Tham gia vào phòng chat | Mã mời của phòng | `200`, `{}` |
| `POST` | `/api/chatrooms/{id}/leave` | Rời khỏi phòng chat | Lý do rời, có rời im lặng hay không | (Thành công rời khỏi phòng) |

### Hệ thống Tin nhắn (Messages)
| Method | Endpoint | Mô tả | Input | Phản hồi thành công |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/chatrooms/{id}/history?limit={n}&before_message={uuid}` | Tải lịch sử tin nhắn của phòng chat |   | `200`, danh sách tin nhắn |
| `POST` | `/api/chatrooms/{id}/messages` | Gửi tin nhắn mới vào phòng chat | Nội dung tin nhắn, danh sách file tải lên | `200`, `{}` |
| `PATCH`| `/api/chatrooms/{id}/messages/{messageId}`| Chỉnh sửa nội dung một tin nhắn đã gửi | Nội dung mới, giữ hay bỏ file đính kèm | `200`, `{}` |
| `DELETE`| `/api/chatrooms/{id}/messages/{messageId}`| Xóa hoặc thu hồi một tin nhắn |   | (Thành công xóa/thu hồi tin) |
