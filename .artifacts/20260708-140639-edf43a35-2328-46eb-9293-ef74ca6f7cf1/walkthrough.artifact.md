# Walkthrough - Lưu trữ Đa phương tiện trên Google Drive

Tôi đã hoàn thành việc tích hợp Google Drive API vào ứng dụng để thay thế Firebase Storage trong việc lưu trữ ảnh và video. Điều này giúp tận dụng 15GB miễn phí của người dùng và tránh phát sinh chi phí cho hệ thống.

## Các thay đổi chính

### 1. Tích hợp Google Drive API
- Đã thêm các thư viện cần thiết và cấu hình Gradle.
- Tạo [GoogleDriveService.kt](file:///C:/Users/ngoch/AndroidStudioProjects/AppMobile8/app/src/main/java/com/example/nhatky/data/service/GoogleDriveService.kt) để xử lý việc tải tệp lên thư mục `NhatKy_Media` trên Drive của người dùng.

### 2. Cập nhật Luồng Xác thực & Quyền
- Cập nhật [LoginScreen.kt](file:///C:/Users/ngoch/AndroidStudioProjects/AppMobile8/app/src/main/java/com/example/nhatky/ui/screens/LoginScreen.kt) để yêu cầu quyền `DRIVE_FILE` khi đăng nhập bằng Google.
- Thêm [DriveHelper.kt](file:///C:/Users/ngoch/AndroidStudioProjects/AppMobile8/app/src/main/java/com/example/nhatky/ui/utils/DriveHelper.kt) để kiểm tra và yêu cầu quyền Drive ngay cả khi người dùng đăng nhập bằng Email/Số điện thoại nhưng muốn lưu ảnh.

### 3. Thay đổi Cơ chế Lưu trữ & Hiển thị
- [DiaryRepository.kt](file:///C:/Users/ngoch/AndroidStudioProjects/AppMobile8/app/src/main/java/com/example/nhatky/data/repository/DiaryRepository.kt): Hiện tại hàm `uploadMedia` sẽ tải trực tiếp lên Google Drive. Đường dẫn lưu trong Firestore sẽ có dạng `googledrive://[FILE_ID]`.
- [GoogleDriveMediaInterceptor.kt](file:///C:/Users/ngoch/AndroidStudioProjects/AppMobile8/app/src/main/java/com/example/nhatky/data/network/GoogleDriveMediaInterceptor.kt): Một Interceptor cho OkHttp giúp tự động thêm Header xác thực khi tải ảnh/video từ Drive.
- [NhatKyApplication.kt](file:///C:/Users/ngoch/AndroidStudioProjects/AppMobile8/app/src/main/java/com/example/nhatky/NhatKyApplication.kt): Cấu hình Coil sử dụng Interceptor trên để hiển thị ảnh từ Drive.

## Hướng dẫn xác minh

### 1. Kiểm tra Đăng nhập & Quyền
- Mở ứng dụng, đăng nhập bằng Google. Đảm bảo có thông báo yêu cầu quyền truy cập Google Drive.
- Nếu đăng nhập bằng Email, hãy thử tạo một bài viết có ảnh. Ứng dụng sẽ yêu cầu bạn chọn một tài khoản Google để cấp quyền lưu trữ.

### 2. Kiểm tra Tải lên
- Viết một bài nhật ký mới và đính kèm ảnh hoặc video.
- Sau khi lưu, hãy truy cập vào Google Drive của bạn (trên web hoặc app Drive) và kiểm tra xem có thư mục `NhatKy_Media` chứa tệp vừa tải lên hay không.

### 3. Kiểm tra Hiển thị
- Quay lại danh sách nhật ký, mở bài viết vừa tạo.
- Đảm bảo ảnh hiển thị rõ nét và video có thể phát bình thường.

## Ghi chú kỹ thuật
- Dữ liệu văn bản vẫn được lưu an toàn trên **Firebase Firestore** để đảm bảo tốc độ truy xuất và tính năng đồng bộ.
- Việc tách biệt này giúp ứng dụng hoạt động hoàn toàn miễn phí cho bạn mà vẫn đảm bảo dung lượng lưu trữ lớn cho người dùng.
