# StarRailChatBox 编译指南

## 环境准备

### 1. 安装 JDK 17

```bash
# macOS (Homebrew)
brew install openjdk@17

# Ubuntu/Debian
sudo apt install openjdk-17-jdk

# Windows
# 下载并安装：https://adoptium.net/
```

### 2. 安装 Android Studio

下载地址：https://developer.android.com/studio

推荐版本：Hedgehog (2023.1.1) 或更高

### 3. 安装 Android SDK

在 Android Studio 中：
1. 打开 `File → Settings → Appearance & Behavior → System Settings → Android SDK`
2. 安装以下组件：
   - **SDK Platforms:** Android 14 (API 34)
   - **SDK Tools:**
     - Android SDK Build-Tools 34
     - Android SDK Command-line Tools
     - Android SDK Platform-Tools
     - Android NDK (Side by side) 25
     - CMake 3.22.1

### 4. 配置环境变量

```bash
# 添加到 ~/.bashrc 或 ~/.zshrc
export ANDROID_HOME=$HOME/Library/Android/sdk  # macOS
# export ANDROID_HOME=$HOME/Android/Sdk  # Linux

export PATH=$PATH:$ANDROID_HOME/emulator
export PATH=$PATH:$ANDROID_HOME/tools
export PATH=$PATH:$ANDROID_HOME/tools/bin
export PATH=$PATH:$ANDROID_HOME/platform-tools
```

## 获取源码

```bash
# 克隆仓库
git clone https://ghfast.top/https://github.com/Azure-Ocean/StarRailChatBox.git
cd StarRailChatBox

# 切换到开发分支
git checkout feature/character-memory

# 初始化子模块
git submodule update --init --recursive
```

## 配置项目

### 1. 创建 local.properties

```bash
# 在项目根目录创建
cat > local.properties << EOF
sdk.dir=$ANDROID_HOME
EOF
```

### 2. 配置 API Key（可选）

如果需要默认 API Key，可以添加到 `local.properties`：

```properties
OPENAI_API_HOST=https://api.openai.com/v1
OPENAI_API_KEY=your_api_key_here
```

## 编译运行

### Android

```bash
# Debug 版本
./gradlew :androidApp:assembleDebug

# Release 版本
./gradlew :androidApp:assembleRelease

# 安装到设备
./gradlew :androidApp:installDebug
```

APK 输出位置：`androidApp/build/outputs/apk/debug/`

### Desktop (Windows/macOS/Linux)

```bash
# 运行
./gradlew :desktopApp:run

# 热重载（开发推荐）
./gradlew :desktopApp:hotRun --auto

# 打包 MSI (Windows)
./gradlew :desktopApp:packageMsi

# 打包 DMG (macOS)
./gradlew :desktopApp:packageDmg

# 打包 DEB (Linux)
./gradlew :desktopApp:packageDeb
```

### Web (WasmJS)

```bash
# 开发模式
./gradlew :webApp:wasmJsBrowserDevelopmentRun

# 生产构建
./gradlew :webApp:wasmJsBrowserProductionWebpack
```

## 常见问题

### 1. Gradle 同步失败

**问题：** `Could not resolve com.android.tools.build:gradle:X.X.X`

**解决方案：**
```bash
# 使用国内镜像
# 修改 build.gradle.kts
repositories {
    maven { url = uri("https://maven.aliyun.com/repository/google") }
    maven { url = uri("https://maven.aliyun.com/repository/central") }
    maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
    google()
    mavenCentral()
    gradlePluginPortal()
}
```

### 2. NDK 版本不匹配

**问题：** `NDK not installed` 或版本错误

**解决方案：**
```bash
# 安装 NDK 25
sdkmanager --install "ndk;25.2.9519653"

# 或在 Android Studio 中安装
# File → Settings → Android SDK → SDK Tools → NDK (Side by side)
```

### 3. 内存不足

**问题：** `OutOfMemoryError`

**解决方案：**
```bash
# 修改 gradle.properties
org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=1024m
```

### 4. CMake 版本不匹配

**问题：** `CMake X.X.X not found`

**解决方案：**
```bash
# 安装 CMake 3.22.1
sdkmanager --install "cmake;3.22.1"

# 或在 Android Studio 中安装
# File → Settings → Android SDK → SDK Tools → CMake
```

### 5. 子模块初始化失败

**问题：** `Submodule not found`

**解决方案：**
```bash
# 重新初始化子模块
git submodule update --init --recursive

# 如果仍然失败，尝试手动克隆
git submodule foreach --recursive git clean -fdx
git submodule update --init --recursive
```

### 6. 网络问题

**问题：** 下载依赖超时

**解决方案：**
```bash
# 使用代理
export http_proxy=http://127.0.0.1:7890
export https_proxy=http://127.0.0.1:7890

# 或使用国内镜像
# 修改 settings.gradle.kts
dependencyResolution {
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        google()
        mavenCentral()
    }
}
```

## 开发调试

### 1. 日志查看

```bash
# Android
adb logcat | grep "StarRailChatBox"

# Desktop
# 直接在控制台查看
```

### 2. 数据库查看

```bash
# Android
adb shell run-as com.kaixuan.starrailchatbox cat databases/star_rail_db

# 使用 Android Studio Database Inspector
# View → Tool Windows → App Inspection → Database Inspector
```

### 3. 热重载

Desktop 开发推荐使用热重载：

```bash
./gradlew :desktopApp:hotRun --auto
```

修改代码后会自动重新编译并重启应用。

## 测试

### 单元测试

```bash
# 运行所有测试
./gradlew test

# 运行特定模块测试
./gradlew :shared:test
```

### UI 测试

```bash
# Android
./gradlew :androidApp:connectedAndroidTest

# Desktop
./gradlew :desktopTest:test
```

## 发布

### Android

1. 签名 APK：
```bash
./gradlew :androidApp:assembleRelease
```

2. 生成的 APK 在：`androidApp/build/outputs/apk/release/`

### Desktop

1. 打包安装包：
```bash
# Windows
./gradlew :desktopApp:packageMsi

# macOS
./gradlew :desktopApp:packageDmg

# Linux
./gradlew :desktopApp:packageDeb
```

2. 生成的安装包在：`desktopApp/build/compose/binaries/main/`

## 参考资料

- [Kotlin Multiplatform 文档](https://kotlinlang.org/docs/multiplatform.html)
- [Compose Multiplatform 文档](https://www.jetbrains.com/lp/compose-multiplatform/)
- [Android Studio 文档](https://developer.android.com/studio)
- [Gradle 文档](https://docs.gradle.org/)

---

最后更新：2026-06-14
