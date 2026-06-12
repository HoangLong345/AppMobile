# TÀI LIỆU ĐẶC TẢ YÊU CẦU PHẦN MỀM (SRS)

## Dự án: Ứng dụng Android "Nhật Ký" (NhatKy)

**Ngày cập nhật:** Tháng 6, 2026
**Phiên bản:** 2.0 (Cập nhật Kiến trúc Firebase)

---

# 1. Giới thiệu chung

## 1.1. Mục đích

Tài liệu Đặc tả Yêu cầu Phần mềm (Software Requirements Specification - SRS) này cung cấp cái nhìn tổng quan, chi tiết về các chức năng, phi chức năng và kiến trúc kỹ thuật của ứng dụng "Nhật Ký".

Phiên bản này đã được cập nhật để phản ánh chính xác việc tích hợp hệ sinh thái đám mây Firebase (Authentication, Firestore, Storage) vào hệ thống.

## 1.2. Phạm vi sản phẩm

"Nhật Ký" là một ứng dụng di động chạy trên hệ điều hành Android, được xây dựng bằng Kotlin và Jetpack Compose.

Ứng dụng cho phép người dùng ghi lại các kỷ niệm, suy nghĩ cá nhân dưới dạng văn bản và đính kèm phương tiện truyền thông (hình ảnh, video).

Hệ thống áp dụng kiến trúc đồng bộ hóa đám mây (Cloud Sync) kết hợp với lưu trữ cục bộ, sử dụng:

* Firebase Authentication để bảo mật
* Firebase Firestore để lưu trữ dữ liệu thời gian thực
* Firebase Storage để quản lý tệp tin đa phương tiện

---

# 2. Yêu cầu chức năng (Functional Requirements)

| Mã YC | Tên Chức Năng                             | Mô tả chi tiết                                                                                                                                                                                                      |
| ----- | ----------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| FR-01 | Quản lý Xác thực (Login qua Firebase)     | Xác thực người dùng an toàn bằng Firebase Authentication. Hỗ trợ đăng nhập qua Email/Mật khẩu hoặc Google Sign-In. Quản lý phiên đăng nhập xuyên suốt ứng dụng để bảo vệ quyền riêng tư. (Xử lý qua AuthViewModel). |
| FR-02 | Quản lý Danh sách Nhật ký (Đồng bộ Cloud) | Hiển thị danh sách các bài viết đã lưu. Dữ liệu được tải và đồng bộ hóa từ Firebase Firestore, cho phép người dùng truy cập trên nhiều thiết bị.                                                                    |
| FR-03 | Thêm/Sửa/Xóa Bài viết                     | Cho phép người dùng tạo bài nhật ký mới, chỉnh sửa hoặc xóa bài viết. Các thay đổi được cập nhật trực tiếp lên Firebase Firestore với cơ chế hỗ trợ offline.                                                        |
| FR-04 | Quản lý Đa phương tiện (Firebase Storage) | Cho phép chèn hình ảnh và video vào bài viết. Tệp được quản lý thông qua Firebase Storage. Hỗ trợ ZoomableBox và VideoPlayerDialog.                                                                                 |
| FR-05 | Cài đặt Hệ thống & Tài khoản              | Cho phép thay đổi giao diện Dark/Light Mode, quản lý tài khoản Firebase và các tùy chọn cá nhân hóa khác.                                                                                                           |

---

# 3. Yêu cầu phi chức năng (Non-Functional Requirements)

## Kiến trúc & Công nghệ

* Tuân thủ mô hình MVVM.
* Sử dụng Kotlin Coroutines.
* Quản lý trạng thái bằng StateFlow / SharedFlow.
* Giao diện xây dựng hoàn toàn bằng Jetpack Compose.

## Khả năng hoạt động ngoại tuyến (Offline Capabilities)

* Tận dụng Offline Persistence của Firestore.
* Kết hợp Room Database.
* Người dùng vẫn có thể xem và viết nhật ký khi mất mạng.
* Dữ liệu tự động đồng bộ khi có kết nối trở lại.

## Hiệu suất

* Tối ưu tải ảnh/video từ Firebase Storage.
* Sử dụng thư viện cache ảnh (ví dụ: Coil).
* Điều hướng giữa các màn hình dưới 0.5 giây.

## Bảo mật & Quyền riêng tư

* Áp dụng Firebase Security Rules.
* Dữ liệu được cách ly hoàn toàn theo UID người dùng.
* Người dùng chỉ có quyền đọc, ghi, sửa, xóa dữ liệu của chính mình.

---

# 4. Đề xuất Tính năng bổ sung (Roadmap)

## 1. Tìm kiếm và Lọc (Search & Filter)

Tận dụng các truy vấn của Firestore và Room để tìm kiếm nhật ký theo:

* Từ khóa
* Tháng
* Khoảng thời gian

---

## 2. Theo dõi Cảm xúc (Mood Tracker)

* Thêm trường dữ liệu biểu tượng cảm xúc vào bài viết.
* Thu thập dữ liệu cảm xúc.
* Hiển thị biểu đồ theo dõi sức khỏe tinh thần theo tháng.

---

## 3. Xác thực Sinh trắc học (Biometric Authentication)

* Hỗ trợ Vân tay.
* Hỗ trợ FaceID.
* Sử dụng thư viện androidx.biometric.
* Giảm nhu cầu đăng nhập Firebase liên tục.

---

## 4. Gắn Thẻ và Phân loại (Tags / Categories)

Thêm mảng Tags vào dữ liệu Firestore để phân loại nội dung:

Ví dụ:

* #GiaDinh
* #HocTap
* #TinhYeu

---

## 5. Chế độ Xem Lịch (Calendar View)

Một màn hình lịch theo tháng:

* Các ngày có nhật ký sẽ được đánh dấu.
* Người dùng có thể nhấn trực tiếp vào ngày để xem lại kỷ niệm.
* Hỗ trợ truy xuất nhanh dữ liệu nhật ký theo thời gian.

```
```
