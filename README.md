# Chat BLN — Ứng dụng Android Native (Kotlin + Jetpack Compose)

[![Build Android APKs](https://github.com/fishuvn/chat-bln-android/actions/workflows/build-apk.yml/badge.svg)](https://github.com/fishuvn/chat-bln-android/actions/workflows/build-apk.yml)

Ứng dụng di động Android chính thức của hệ thống nhắn tin thời gian thực **Chat BLN**, được xây dựng bằng **Kotlin 2.0**, **Jetpack Compose (Material 3)**, **Retrofit 2 + OkHttp 4**, **DataStore Preferences**, **Coil** và giao thức **STOMP 1.2 over Raw WebSocket (`/ws-native`)** kết nối trực tiếp tới backend Spring Boot (cổng `8088`).

---

## CI/CD Tự động Build 3 File APK (GitHub Actions)

Workflow `.github/workflows/build-apk.yml` tự động chạy trên mọi lệnh `push` / `pull_request` vào `main` và tự động tạo **GitHub Release** khi đẩy tag `v*` (ví dụ `git tag v1.0.0 && git push origin v1.0.0`) hoặc bật tuỳ chọn `create_release` trong mục **Actions -> Run workflow**:

1. `ChatBLN-arm32-v7a.apk` — Dành cho thiết bị Android **ARM 32-bit (`armeabi-v7a`)**.
2. `ChatBLN-arm64-v8a.apk` — Dành cho thiết bị Android **ARM 64-bit (`arm64-v8a`)**.
3. `ChatBLN-arm-universal.apk` — Bản **ARM Universal** chạy trên mọi thiết bị ARM 32-bit và 64-bit.

---

## 1. Yêu cầu môi trường

- **Android Studio**: Ladybug (`2024.2.1`) trở lên.
- **JDK**: Java 17 hoặc Java 21 (Android Studio đã tích hợp sẵn JetBrains Runtime 21).
- **Android SDK**: `compileSdk = 35`, hỗ trợ thiết bị từ **Android 8.0+ (`minSdk = 26`)** đến **Android 15 (`targetSdk = 35`)**.
- **Backend Chat BLN**: Đang chạy Spring Boot ở cổng `8088` (cùng MySQL cơ sở dữ liệu của dự án).

---

## 2. Hướng dẫn mở và chạy dự án trong Android Studio

1. Khởi động **Android Studio** -> chọn **Open**.
2. Trỏ trực tiếp vào thư mục **`android-app`** (thư mục chứa `settings.gradle.kts`, không mở thư mục gốc cả monorepo).
3. Đợi Android Studio đồng bộ **Gradle (`8.10.2`)** và tải dependencies tự động.
4. Chọn thiết bị ảo (**Android Emulator**) hoặc thiết bị thật đã bật **USB Debugging** / **Wireless Debugging**, sau đó nhấn **Run (`Shift + F10`)**.

Nếu chạy bằng dòng lệnh (khi máy đã cài đặt `ANDROID_HOME` / Android SDK):

```bash
cd android-app
./gradlew assembleDebug
```

---

## 3. Cấu hình địa chỉ máy chủ: Máy ảo (Emulator) vs Thiết bị thật (LAN)

Ứng dụng tích hợp sẵn hộp thoại **Cấu hình máy chủ (Server URL)** ngay trên màn hình **Đăng nhập**, **Đăng ký** và trong mục **Hồ sơ -> Địa chỉ máy chủ**, lưu bền vững bằng Jetpack DataStore mà không cần biên dịch lại mã nguồn:

| Môi trường chạy | Địa chỉ Server URL cần nhập | Giải thích |
| :--- | :--- | :--- |
| **Android Emulator** | `http://10.0.2.2:8088` *(Mặc định)* | `10.0.2.2` là địa chỉ loopback đặc biệt của máy ảo Android trỏ về `localhost` của máy tính đang chạy Spring Boot. |
| **Điện thoại thật qua Wi-Fi (LAN)** | `http://<IP-LAN-máy-tính>:8088` *(ví dụ: `http://192.168.1.10:8088`)* | Điện thoại và máy tính kết nối cùng mạng Wi-Fi. Mở cổng `8088` trên tường lửa máy tính nếu bị chặn. |
| **USB Debugging + `adb reverse`** | `http://127.0.0.1:8088` | Chạy `adb reverse tcp:8088 tcp:8088` trên máy tính khi cắm cáp USB. |

Khi đổi `Server URL`, cả hai kênh:
- **REST API**: `<Server URL>/api/...` (qua `DynamicBaseUrlInterceptor`)
- **Realtime STOMP WebSocket**: `ws://<host>:<port>/ws-native` (qua `StompRealtimeClient`)
đều tự động chuyển sang máy chủ mới ngay lập tức.

---

## 4. Kiến trúc mã nguồn (`nhom3.chatbln.android`)

Dự án tuân thủ mô hình **MVVM + Unidirectional Data Flow (StateFlow)** rõ ràng:

```text
app/src/main/java/nhom3/chatbln/android/
├── ChatBlnApp.kt                     # Khởi tạo AppContainer & cấu hình Coil ImageLoader
├── MainActivity.kt                   # Edge-to-Edge Activity & NavHost điều hướng chính
├── data/
│   ├── local/
│   │   └── SessionPreferences.kt     # DataStore lưu JWT, thông tin user, Dark Mode, Server URL
│   ├── model/
│   │   └── ApiModels.kt              # Toàn bộ DTO REST & STOMP đồng bộ với Spring Boot
│   ├── remote/
│   │   ├── AuthInterceptor.kt        # Tự động đính kèm Authorization: Bearer <token>
│   │   ├── DynamicBaseUrlInterceptor.kt # Đổi host/port động không cần tạo lại Retrofit
│   │   ├── ChatBlnApiService.kt      # Khai báo các endpoint REST (/auth, /users, /conversations, /friends, /uploads)
│   │   └── StompRealtimeClient.kt    # Bộ mã hóa/giải mã khung STOMP 1.2 trên OkHttp WebSocket (/ws-native)
│   └── repository/
│       └── ChatRepository.kt         # Xử lý nghiệp vụ API, đọc Uri từ ContentResolver & tải tệp đa phương tiện
└── ui/
    ├── components/
    │   ├── AvatarView.kt             # Hiển thị ảnh đại diện Coil + fallback chữ cái đầu + chấm trạng thái online
    │   └── ServerUrlDialog.kt        # Hộp thoại cấu hình nhanh URL máy chủ (Emulator / LAN)
    ├── screens/
    │   ├── LoginScreen.kt            # Đăng nhập & cấu hình Server URL
    │   ├── RegisterScreen.kt         # Đăng ký tài khoản đầy đủ trường thông tin
    │   ├── MainScaffoldScreen.kt     # Thanh điều hướng đáy Material 3 (Đoạn chat, Danh bạ, Lời mời, Hồ sơ)
    │   ├── ConversationsScreen.kt    # Danh sách hội thoại realtime, tìm kiếm, xem trước tin nhắn cuối
    │   ├── NewChatBottomSheet.kt     # Tạo chat 1-1 hoặc lập nhóm mới từ danh sách bạn bè
    │   ├── ChatRoomScreen.kt         # Phòng chat: phân trang lịch sử, Markdown link, ảnh/video/tệp, menu đính kèm tách riêng
    │   ├── EmojiPickerSheet.kt       # Bảng chọn biểu tượng cảm xúc phân nhóm theo chủ đề
    │   ├── ConversationInfoSheet.kt  # Thông tin đối phương / nhóm & kho ảnh, video, tệp đã chia sẻ
    │   ├── ContactsScreen.kt         # Danh bạ bạn bè kèm trạng thái hoạt động trực tuyến
    │   ├── AddFriendBottomSheet.kt   # Tìm kiếm người dùng theo từ khóa (ẩn danh sách cho tới khi nhập từ khóa)
    │   ├── FriendRequestsScreen.kt   # Quản lý lời mời kết bạn Đã nhận (Chấp nhận / Từ chối) & Đã gửi
    │   └── ProfileScreen.kt          # Cập nhật ảnh đại diện, thông tin cá nhân, giao diện Sáng/Tối, đăng xuất
    ├── theme/
    │   ├── Color.kt                  # Bảng màu Material 3 (#0B57D0 Light / #A8C7FA Dark, tương phản >= 4.5:1)
    │   ├── Type.kt                   # Hệ thống phân cấp chữ rõ ràng cho tiếng Việt
    │   └── Theme.kt                  # Thiết lập MaterialTheme & đồng bộ thanh trạng thái hệ thống
    ├── util/
    │   └── MarkdownLinkText.kt       # Phân tích cú pháp [Nhãn](https://...) & URL trần thành liên kết nhấn được
    └── viewmodel/
        ├── AuthViewModel.kt          # Quản lý phiên đăng nhập, đăng ký, giao diện và máy chủ
        └── MainChatViewModel.kt      # Quản lý trạng thái hội thoại, tin nhắn STOMP, bạn bè và hồ sơ
```

---

## 5. Các tính năng nổi bật đồng bộ với phiên bản Web

- **Nhắn tin & hiện diện thời gian thực**: Kết nối STOMP 1.2 tới `/ws-native`, gửi `heart-beat:10000,10000`, tự động kết nối lại khi rớt mạng và lắng nghe `/topic/presence`, `/topic/users/{username}`, `/topic/conversations/{id}`.
- **Định dạng liên kết thông minh**: Hỗ trợ cả cú pháp Markdown `[Tên hiển thị](https://example.com)` lẫn đường dẫn trần `https://...` ngay trong bong bóng chat bằng `LinkAnnotation.Url`.
- **Đính kèm đa phương tiện tách biệt**: Menu đính kèm tách rõ **Chọn gửi ảnh** (`PickVisualMedia(ImageOnly)`), **Chọn gửi video** (`PickVisualMedia(VideoOnly)`) và **Đính kèm tệp** (`OpenDocument`).
- **Quyền riêng tư khi tìm bạn**: Không liệt kê sẵn toàn bộ người dùng hệ thống; chỉ truy vấn `GET /api/users/search?keyword=...` khi người dùng chủ động nhập từ khóa.
