# ReelSplit 🎬✂️

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-24-orange.svg)](https://developer.android.com/about/versions/nougat)

**ReelSplit** is an Android app that seamlessly integrates with Instagram's share menu to download Instagram Reels, automatically split them into 90-second segments (WhatsApp Status limit), and share them directly to WhatsApp Status with one tap.

## ✨ Features

- **Instagram Integration**: Appears directly in Instagram's share menu for quick access
- **Smart Video Splitting**: Automatically splits videos longer than 90 seconds into WhatsApp-compatible segments
- **One-Tap Sharing**: Share split videos directly to WhatsApp Status or chats
- **Video History**: Keep track of all processed videos with pagination support
- **Background Processing**: Downloads and processes videos even when the app is closed
- **Material 3 Design**: Modern UI with light/dark theme support
- **Offline Support**: Local caching with Room database
- **Progress Tracking**: Real-time download and processing progress notifications

## 🏗️ Architecture

ReelSplit follows **Clean Architecture** principles with **MVVM** pattern:

```
┌─────────────────────────────────────────┐
│         Presentation Layer              │
│  (Jetpack Compose + ViewModels)         │
├─────────────────────────────────────────┤
│          Domain Layer                   │
│  (Use Cases + Repository Interfaces)    │
├─────────────────────────────────────────┤
│           Data Layer                    │
│  (Repository Impl + Data Sources)       │
└─────────────────────────────────────────┘
```

### Tech Stack

- **UI**: Jetpack Compose with Material 3
- **DI**: Hilt (Dagger)
- **Navigation**: Compose Destinations
- **Video Processing**: Media3 Transformer (hardware-accelerated)
- **Video Extraction**: youtubedl-android
- **Downloads**: PRDownloader + WorkManager
- **Database**: Room with Paging 3
- **Preferences**: DataStore + Encrypted SharedPreferences
- **Networking**: OkHttp + Retrofit
- **Analytics**: Firebase Crashlytics & Analytics
- **Animations**: Lottie
- **Async**: Kotlin Coroutines + Flow
- **Error Handling**: kotlin-result

## 📱 Screenshots

> Add screenshots here showing:
> - Home screen with instructions
> - Processing screen with progress
> - Result screen with split segments
> - WhatsApp sharing flow

## 🚀 Getting Started

### Prerequisites

- Android Studio Hedgehog or later
- JDK 17+
- Android SDK 34
- Minimum Android 7.0 (API 24)

### Installation

1. Clone the repository:
```bash
git clone https://github.com/yourusername/reelsplit-android.git
cd reelsplit-android
```

2. Add Firebase configuration:
   - Create a Firebase project at [Firebase Console](https://console.firebase.google.com/)
   - Download `google-services.json`
   - Place it in the `app/` directory

3. Build and run:
```bash
./gradlew assembleDebug
```

## 📖 How to Use

1. **Open Instagram** and find a Reel you want to share
2. **Tap the Share button** on the Reel
3. **Select ReelSplit** from the share menu
4. **Wait for processing** - the app will download and split the video automatically
5. **Share to WhatsApp** - tap "Share to Status" or "Share to Chat" for each segment

## 🎯 WhatsApp Status Limits

ReelSplit automatically ensures all video segments meet WhatsApp Status requirements:

- **Duration**: Maximum 90 seconds per segment
- **File Size**: Maximum 16MB per segment
- **Format**: MP4 (H.264 codec)

## 🛠️ Development

### Project Structure

```
com.reelsplit/
├── core/                    # Core utilities and base classes
│   ├── base/               # BaseViewModel, UiState
│   ├── constants/          # App-wide constants
│   ├── extensions/         # Kotlin extensions
│   └── utils/              # Utility classes
├── data/                   # Data layer
│   ├── local/             # Room database, DataStore
│   ├── remote/            # API services
│   ├── processing/        # Video extraction, download, splitting
│   ├── repository/        # Repository implementations
│   └── worker/            # WorkManager workers
├── di/                    # Dependency injection modules
├── domain/                # Domain layer
│   ├── model/            # Domain models
│   ├── repository/       # Repository interfaces
│   └── usecase/          # Business logic use cases
├── presentation/         # Presentation layer
│   ├── components/      # Reusable Compose components
│   ├── home/           # Home screen
│   ├── processing/     # Processing screen
│   ├── result/         # Result screen
│   ├── history/        # History screen
│   ├── share/          # Share receiver
│   └── theme/          # Material 3 theme
└── sharing/            # WhatsApp integration
```

### Key Components

#### Video Processing Pipeline

1. **VideoExtractor**: Extracts direct video URL from Instagram using youtubedl-android
2. **VideoDownloadManager**: Downloads video with progress tracking using PRDownloader
3. **VideoSplitter**: Splits video into 90-second segments using Media3 Transformer
4. **WhatsAppSharer**: Handles sharing to WhatsApp Status/Chat

#### Use Cases

- `ExtractVideoUrlUseCase`: Extract video URL from Instagram link
- `DownloadVideoUseCase`: Download video with WorkManager
- `SplitVideoUseCase`: Split video into WhatsApp-compatible segments
- `ShareToWhatsAppUseCase`: Share segments to WhatsApp

### Testing

Run unit tests:
```bash
./gradlew test
```

Run instrumented tests:
```bash
./gradlew connectedAndroidTest
```

## 🔒 Privacy & Security

- **No Data Collection**: ReelSplit does not collect or store any personal information
- **Local Processing**: All video processing happens locally on your device
- **Secure Storage**: Sensitive data is encrypted using Android Keystore
- **HTTPS Only**: All network requests use secure HTTPS connections
- **No Analytics Tracking**: Firebase Analytics is used only for crash reporting

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the project
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 🐛 Known Issues

- Instagram may change their API/URL structure, which could break video extraction
- Some videos with DRM protection cannot be downloaded
- Very long videos (>10 minutes) may take significant time to process

## 🗺️ Roadmap

- [ ] Support for Instagram Stories
- [ ] Batch processing of multiple Reels
- [ ] Custom segment duration settings
- [ ] Video quality selection
- [ ] Support for other social platforms (TikTok, YouTube Shorts)
- [ ] Video editing features (trim, filters)

## 📞 Support

If you encounter any issues or have questions:

- Open an [Issue](https://github.com/yourusername/reelsplit-android/issues)
- Check the [Wiki](https://github.com/yourusername/reelsplit-android/wiki) for documentation

## 🙏 Acknowledgments

- [Media3](https://developer.android.com/media/media3) for video processing
- [youtubedl-android](https://github.com/yausername/youtubedl-android) for video extraction
- [PRDownloader](https://github.com/MindorksOpenSource/PRDownloader) for download management
- [Jetpack Compose](https://developer.android.com/jetpack/compose) for modern UI

---

**Made with ❤️ for the Instagram and WhatsApp community**
