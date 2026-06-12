# Cấu trúc dự án NhatKy

```text
NhatKy/
├── .gitignore                         # Bỏ qua các tệp không cần đẩy lên Git (cấp dự án)
├── README.md                          # Tài liệu giới thiệu dự án
├── build.gradle.kts                   # Cấu hình Gradle chung cho toàn bộ dự án
├── settings.gradle.kts                # Khai báo các module (app) tham gia vào dự án
├── gradle.properties                  # Các cờ (flags) cấu hình môi trường cho Gradle
├── gradlew                            # Script thực thi build trên Mac/Linux
├── gradlew.bat                        # Script thực thi build trên Windows
│
├── gradle/
│   ├── libs.versions.toml             # Quản lý tập trung phiên bản các thư viện
│   ├── gradle-daemon-jvm.properties   # Cấu hình máy ảo Java (JVM) cho Gradle
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
│
└── app/
    ├── .gitignore                     # Bỏ qua các tệp không cần đẩy lên Git (cấp App)
    ├── build.gradle.kts               # Cấu hình Gradle riêng cho App
    ├── google-services.json           # File khóa kết nối Firebase
    ├── proguard-rules.pro             # Cấu hình làm rối mã nguồn
    │
    └── src/
        └── main/
            ├── AndroidManifest.xml    # Khai báo quyền và thành phần ứng dụng
            │
            ├── java/
            │   └── com/example/nhatky/
            │       ├── MainActivity.kt
            │       │
            │       ├── data/
            │       │   ├── dao/
            │       │   │   └── DiaryDao.kt
            │       │   │
            │       │   ├── database/
            │       │   │   └── AppDatabase.kt
            │       │   │
            │       │   ├── model/
            │       │   │   └── DiaryEntry.kt
            │       │   │
            │       │   └── preferences/
            │       │       └── SettingsManager.kt
            │       │
            │       ├── ui/
            │       │   ├── components/
            │       │   │   ├── AppBackground.kt
            │       │   │   ├── VideoPlayerDialog.kt
            │       │   │   └── ZoomableBox.kt
            │       │   │
            │       │   ├── screens/
            │       │   │   ├── AddEditDiaryScreen.kt
            │       │   │   ├── DiaryListScreen.kt
            │       │   │   ├── LoginScreen.kt
            │       │   │   └── SettingsScreen.kt
            │       │   │
            │       │   └── theme/
            │       │       ├── Color.kt
            │       │       ├── Theme.kt
            │       │       └── Type.kt
            │       │
            │       └── viewmodel/
            │           ├── AuthViewModel.kt
            │           └── DiaryViewModel.kt
            │
            └── res/
                ├── mipmap-hdpi/
                │   ├── ic_launcher.png
                │   └── ic_launcher_round.png
                │
                ├── mipmap-mdpi/
                │   ├── ic_launcher.png
                │   └── ic_launcher_round.png
                │
                ├── mipmap-xhdpi/
                │   ├── ic_launcher.png
                │   └── ic_launcher_round.png
                │
                ├── mipmap-xxhdpi/
                │   ├── ic_launcher.png
                │   └── ic_launcher_round.png
                │
                ├── mipmap-xxxhdpi/
                │   ├── ic_launcher.png
                │   └── ic_launcher_round.png
                │
                ├── values/
                │   ├── colors.xml
                │   ├── strings.xml
                │   └── themes.xml
                │
                ├── values-en/
                │   └── strings.xml
                │
                └── xml/
                    ├── backup_rules.xml
                    ├── data_extraction_rules.xml
                    └── file_paths.xml
```
