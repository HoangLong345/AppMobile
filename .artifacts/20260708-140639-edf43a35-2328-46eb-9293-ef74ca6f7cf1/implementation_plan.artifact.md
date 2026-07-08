# Kế hoạch triển khai - Lưu trữ Đa phương tiện trên Google Drive

Kế hoạch này mô tả việc tách biệt lưu trữ: Ảnh và Video sẽ được lưu trên Google Drive của người dùng, trong khi nội dung nhật ký và dữ liệu người dùng vẫn được lưu trên Firebase Firestore. Điều này áp dụng cho tất cả các phương thức đăng nhập.

## Xác nhận từ người dùng

> [!IMPORTANT]
> - **Tất cả các loại phương tiện (Ảnh/Video)**: Sẽ được lưu lên Google Drive.
> - **Dữ liệu còn lại (Văn bản nhật ký)**: Vẫn lưu trên Firebase Firestore.
> - **Yêu cầu tài khoản Google**: Do Google Drive yêu cầu tài khoản Google, người dùng đăng nhập bằng Email/Số điện thoại sẽ cần cấp quyền truy cập Google Drive (đăng nhập Google) khi muốn lưu ảnh/video.

## Các thay đổi đề xuất

### 1. Cấu hình Build & Dependencies
- Thêm các thư viện Google Drive API, Google API Client và OkHttp.

### 2. Quản lý Xác thực (Auth)
- **LoginScreen.kt**: Cập nhật Google Sign-In để yêu cầu quyền `DriveScopes.DRIVE_FILE`.
- **AuthViewModel.kt**: Bổ sung phương thức để kiểm tra và yêu cầu quyền Drive cho những người dùng không đăng nhập bằng Google.

### 3. Dịch vụ Google Drive
- **[NEW] GoogleDriveService.kt**:
    - Quản lý việc tạo thư mục `NhatKy_Media`.
    - Tải file lên và lấy ID file.
    - Chuyển đổi ID file Drive thành URL có thể xem được (kèm Token xác thực).

### 4. Cấu trúc dữ liệu & Repository
- **DiaryRepository.kt**:
    - Thay đổi hàm `uploadMedia`: Thay vì tải lên Firebase Storage, nó sẽ sử dụng `GoogleDriveService`.
    - Các URL media trong Firestore sẽ có định dạng `googledrive://[FILE_ID]`.

### 5. Hiển thị Media (Coil & ExoPlayer)
- **GoogleDriveMediaInterceptor.kt**: Một Interceptor cho OkHttp để tự động thêm Header `Authorization` khi gặp URL `googledrive://`.
- Cấu hình **Coil** và **ExoPlayer** sử dụng OkHttpClient này để có thể hiển thị ảnh/video riêng tư từ Drive.

---

## Kế hoạch xác minh

### Kiểm tra thủ công
1. **Đăng nhập Google**: Kiểm tra việc cấp quyền Drive.
2. **Đăng nhập Email**: Kiểm tra xem ứng dụng có yêu cầu quyền Drive khi người dùng chọn thêm ảnh/video hay không.
3. **Tải lên**: Đảm bảo ảnh/video xuất hiện trong Google Drive của người dùng (thư mục `NhatKy_Media`).
4. **Hiển thị**: Đảm bảo ảnh hiện lên trong nhật ký và video phát được bình thường.
5. **Đồng bộ**: Kiểm tra xem nội dung văn bản có vẫn được lưu đúng trên Firestore không.

