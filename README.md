# 📔 NhatKy (Diary App) - Android Cloud Sync

Ứng dụng nhật ký cá nhân hiện đại, bảo mật và luôn luôn đồng bộ. Được xây dựng trên nền tảng Android với Kotlin và Jetpack Compose, kết hợp sức mạnh của Firebase để mang lại trải nghiệm ghi chép mượt mà trên nhiều thiết bị.

---

## 🌟 Tính năng nổi bật

*   **🔒 Bảo mật tuyệt đối**: Đăng nhập/Đăng ký an toàn qua Firebase Authentication.
*   **☁️ Đồng bộ đám mây (Cloud Sync)**: Dữ liệu nhật ký được gắn liền với tài khoản của bạn. Đăng nhập trên bất kỳ điện thoại nào cũng không lo mất dữ liệu.
*   **🚀 Hoạt động Offline**: Bạn vẫn có thể viết nhật ký khi không có mạng, dữ liệu sẽ tự động đồng bộ khi kết nối trở lại.
*   **📱 Giao diện hiện đại**: Xây dựng hoàn toàn bằng Jetpack Compose với phong cách Material Design 3.
*   **🖼️ Đa phương tiện (Roadmap)**: Hỗ trợ đính kèm hình ảnh và video vào từng bài viết.

---

## 🛠️ Công nghệ sử dụng

*   **Ngôn ngữ**: Kotlin
*   **UI Framework**: Jetpack Compose
*   **Kiến trúc**: MVVM (Model-View-ViewModel)
*   **Database**: 
    *   **Cloud**: Firebase Firestore (Lưu trữ thời gian thực)
    *   **Local**: Room Database (Cache dữ liệu offline)
*   **Authentication**: Firebase Auth (Email/Password)
*   **Navigation**: Jetpack Compose Navigation

---

## 👥 Hướng dẫn cho thành viên trong nhóm (Team Collaboration)

Để đảm bảo dự án chạy được ngay và giữ bảo mật thông tin, các thành viên khi tải code về cần thực hiện các bước sau:

### 1. Clone Project
```bash
git clone https://github.com/your-username/NhatKy.git
```

### 2. Thêm file cấu hình Firebase (Bắt buộc)
Do file `google-services.json` đã được đưa vào `.gitignore` để bảo mật, bạn cần:
*   Liên hệ với **Project Manager** để lấy file `google-services.json`.
*   Copy file này vào thư mục: `NhatKy/app/`

### 3. Cấu hình SHA-1 để chạy App
Để Firebase cho phép máy của bạn đăng nhập/đăng ký, bạn cần cung cấp mã SHA-1 cho Admin:
1.  Mở tab **Gradle** (bên phải Android Studio).
2.  Đi tới: `app` -> `Tasks` -> `android` -> `signingReport`.
3.  Copy mã **SHA-1** trong tab *Run* và gửi cho Admin để thêm vào Firebase Console.

### 4. Đồng bộ dự án
Nhấn vào biểu tượng **con voi (Sync Project with Gradle Files)** và đợi hệ thống tải các thư viện cần thiết.

---

## 🛡️ Lưu ý về Bảo mật (Security)

*   **Tuyệt đối không** gỡ bỏ `google-services.json` khỏi file `.gitignore`.
*   Nếu sử dụng các API Key khác (Maps, OpenAI...), vui lòng lưu vào file `local.properties` hoặc `secrets.properties` và không bao giờ push chúng lên GitHub.

---

## 📅 Lộ trình phát triển (Roadmap)

- [x] Đăng nhập/Đăng ký & Đồng bộ văn bản.
- [ ] Tính năng Tìm kiếm & Lọc nhật ký.
- [ ] Đính kèm Hình ảnh & Video (Firebase Storage).
- [ ] Khóa ứng dụng bằng Sinh trắc học (Vân tay/Khuôn mặt).
- [ ] Theo dõi cảm xúc (Mood Tracker) & Biểu đồ thống kê.

---
*Phát triển bởi nhóm AppMobile - 2026*
