# Changelog — Chat BLN Android

Quy ước phiên bản: `1.0.x` / `1.x.x` cho các bản cập nhật và sửa lỗi nhỏ (mỗi bậc tối đa `10`, ví dụ `1.0.10` → `1.1.0`); `x.0.0` dành cho thay đổi lớn về kiến trúc, ngôn ngữ lập trình, nâng phiên bản HĐH tối thiểu hoặc đại tu UI/UX.

## v4.0.4 — Chat BLN Android (2026-11-27 01:13 UTC+7)

- **Phiên bản**: `v4.0.4` (Build `40004`)
- **Phân loại**: Bản cập nhật & sửa lỗi nhỏ (`1.x.x`, giới hạn tối đa `.10`)

### ✨ Tính năng mới
- feat(brand): redesign default user and group avatars matching B-Dialogue identity (`7763d52`)

## v4.0.3 — Chat BLN Android (2026-11-27 01:04 UTC+7)

- **Phiên bản**: `v4.0.3` (Build `40003`)
- **Phân loại**: Bản cập nhật & sửa lỗi nhỏ (`1.x.x`, giới hạn tối đa `.10`)

### ✨ Tính năng mới
- feat(brand): ship B-Dialogue flat Adaptive/Monochrome App Icons and Compose brand mark (`7a38a0b`)

## v4.0.2 — Chat BLN Android (2026-11-26 23:54 UTC+7)

- **Phiên bản**: `v4.0.2` (Build `40002`)
- **Phân loại**: Bản cập nhật & sửa lỗi nhỏ (`1.x.x`, giới hạn tối đa `.10`)

### ✨ Tính năng mới
- feat(brand): upgrade to symmetric Dual Dialogue adaptive/monochrome icons and brand mark (`ba5c7c6`)

## v4.0.1 — Chat BLN Android (2026-11-26 23:40 UTC+7)

- **Phiên bản**: `v4.0.1` (Build `40001`)
- **Phân loại**: Bản cập nhật & sửa lỗi nhỏ (`1.x.x`, giới hạn tối đa `.10`)

### ✨ Tính năng mới
- feat(brand): apply Signal Aperture adaptive/monochrome app icon, brand mark, and profile text overflow fixes (`90b5b31`)

## v4.0.0 — Chat BLN Android (2026-11-26 23:17 UTC+7)

- **Phiên bản**: `v4.0.0` (Build `40000`)
- **Phân loại**: Cập nhật lớn (`x.0.0` — Kiến trúc / Ngôn ngữ / HĐH / Đại tu UI-UX)

### 🚀 Thay đổi lớn (Cấu trúc / HĐH / UI-UX)
- arch!: revert Android app to native Kotlin + Jetpack Compose with GitHub Releases update checker (`bfa8d04`)

## v3.0.0 — Chat BLN Android (Rust) (2026-11-26 23:02 UTC+7)

- **Phiên bản**: `v3.0.0` (Build `30000`)
- **Phân loại**: Cập nhật lớn (`x.0.0` — Kiến trúc / Ngôn ngữ / HĐH / Đại tu UI-UX)

### 🚀 Thay đổi lớn (Cấu trúc / HĐH / UI-UX)
- arch!: rebuild complete Android app in 100% Rust (Dioxus 0.7) and add GitHub Releases update checker (`196a0fa`)

### 🐛 Sửa lỗi
- fix(ci): use relative jks_file path for Dioxus Android release signing (`046a37b`)
- fix(ci): route Maven Central through Google GCS mirror to avoid HTTP 429 (`31cc1f7`)

## v2.0.0 — Chat BLN Android (2026-11-26 02:18 UTC+7)

- **Phiên bản**: `v2.0.0` (Build `20000`)
- **Phân loại**: Cập nhật lớn (`x.0.0` — Kiến trúc / Ngôn ngữ / HĐH / Đại tu UI-UX)

### 🚀 Thay đổi lớn (Cấu trúc / HĐH / UI-UX)
- ui!: complete minimalist Material 3 adaptive redesign, dynamic color, forgot-password OTP, and LazyColumn optimization (`68a8c95`)

## v1.0.2 — Chat BLN Android (2026-11-26 01:31 UTC+7)

- **Phiên bản**: `v1.0.2` (Build `10002`)
- **Phân loại**: Bản cập nhật & sửa lỗi nhỏ (`1.x.x`, giới hạn tối đa `.10`)

### ✨ Tính năng mới
- feat(group): add deputy role, leader transfer, public/private invite links, and join approvals (`0591627`)

## v1.0.1 — Chat BLN Android (2026-11-26 00:43 UTC+7)

- **Phiên bản**: `v1.0.1` (Build `10001`)
- **Phân loại**: Bản cập nhật & sửa lỗi nhỏ (`1.x.x`, giới hạn tối đa `.10`)

### ✨ Tính năng mới
- feat(android): sync ONLINE/IDLE/SLEEP/DND/OFFLINE presence modes with backend and render live status badges (`fef3280`)
- feat(android): add group members list and unfriend/re-friend support (`e30a983`)
- feat(android): add single and multi-select message deletion with realtime sync (`0bd4089`)
- feat(android): add persistent conversation deletion via DELETE /api/conversations/:id (`0128919`)
- feat(android): add interactive user presence status selector (`ff5fe27`)

### 🐛 Sửa lỗi
- fix(android): show sent friend request recipient details and support withdrawing pending requests (`e4b2888`)
- fix(android): sync rejected friend requests and display daily limit feedback (`06a8a21`)
- fix(android): use lambda receiver in sendFriendRequestById error update (`4e34d49`)
- fix(android): default to https://chat-bln.fishu.io.vn and smart HTTPS scheme (`0bb95e2`)
- fix(android): make FriendResponse.isOnline non-null Boolean (`7f3828e`)

### 🔧 Cải tiến & Bảo trì
- ci(release): auto-bump version (max .10 per slot), generate changelog, and publish Android APKs Release on build (`c1fb819`)
- ci(android): accept SDK licenses directly via preinstalled sdkmanager (`cfc820e`)

## v1.0.0 — Chat BLN Android (Bản nền tảng ban đầu)

- **Phiên bản**: `v1.0.0` (Build `10000`)
- **Phân loại**: Bản phát hành nền tảng đầu tiên (`Major 1.0.0` — Kotlin / Jetpack Compose Material 3, Android 8.0+ `minSdk 26`)

### ✨ Tính năng nền tảng
- Ứng dụng Android Native (Kotlin + Jetpack Compose Material 3) xuất đủ 3 gói APK (`armeabi-v7a`, `arm64-v8a`, `universal`).
- Nhắn tin thời gian thực 1-1 & nhóm qua STOMP WebSocket (`/ws-native`), gửi ảnh, video, tệp tin và xem trước liên kết.
- Xóa một hoặc nhiều tin nhắn (`Xóa ở phía tôi` / `Thu hồi với mọi người`), quản lý danh bạ, hủy kết bạn & thu hồi lời mời đã gửi.
- Đồng bộ realtime 5 trạng thái hoạt động (`Online`, `Idle`, `Sleep`, `DND`, `Offline`) trên mọi thiết bị.
