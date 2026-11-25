# Changelog — Chat BLN Android

Quy ước phiên bản: `1.0.x` / `1.x.x` cho các bản cập nhật và sửa lỗi nhỏ (mỗi bậc tối đa `10`, ví dụ `1.0.10` → `1.1.0`); `x.0.0` dành cho thay đổi lớn về kiến trúc, ngôn ngữ lập trình, nâng phiên bản HĐH tối thiểu hoặc đại tu UI/UX.

## v1.0.0 — Chat BLN Android (Bản nền tảng ban đầu)

- **Phiên bản**: `v1.0.0` (Build `10000`)
- **Phân loại**: Bản phát hành nền tảng đầu tiên (`Major 1.0.0` — Kotlin / Jetpack Compose Material 3, Android 8.0+ `minSdk 26`)

### ✨ Tính năng nền tảng
- Ứng dụng Android Native (Kotlin + Jetpack Compose Material 3) xuất đủ 3 gói APK (`armeabi-v7a`, `arm64-v8a`, `universal`).
- Nhắn tin thời gian thực 1-1 & nhóm qua STOMP WebSocket (`/ws-native`), gửi ảnh, video, tệp tin và xem trước liên kết.
- Xóa một hoặc nhiều tin nhắn (`Xóa ở phía tôi` / `Thu hồi với mọi người`), quản lý danh bạ, hủy kết bạn & thu hồi lời mời đã gửi.
- Đồng bộ realtime 5 trạng thái hoạt động (`Online`, `Idle`, `Sleep`, `DND`, `Offline`) trên mọi thiết bị.
