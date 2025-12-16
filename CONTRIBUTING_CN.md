# 贡献指南

我们非常欢迎社区贡献！如果你想在本地运行项目来测试更改、运行示例，或者只是想了解底层是如何工作的，请继续阅读。

---

## 📁 项目结构

### `/kotlin` - 核心库

这是 Android 库的主模块。在这里你可以找到：

| 路径 | 说明 |
|------|------|
| [`app.rive.runtime.kotlin`](https://github.com/rive-app/rive-android/tree/master/kotlin/src/main/java/app/rive/runtime/kotlin) | 高级 API 命名空间 |
| [`RiveAnimationView.kt`](https://github.com/rive-app/rive-android/blob/master/kotlin/src/main/java/app/rive/runtime/kotlin/RiveAnimationView.kt) | **主入口点**，最常用的组件 |
| [`app.rive.runtime.kotlin.core`](https://github.com/rive-app/rive-android/tree/master/kotlin/src/main/java/app/rive/runtime/kotlin/core) | 底层 C++ 运行时的 Kotlin 绑定 |

**底层 API 说明**：

`app.rive.runtime.kotlin.core` 命名空间中的类映射到底层的 [C++ 运行时](https://github.com/rive-app/rive-runtime)。这些类允许对 Rive 文件状态进行更精细的控制。`RiveAnimationView` 就是基于这些底层 API 构建的。

### `/kotlin/src/main/cpp` 和 `/submodules` - C++ 层

```
项目结构：
├── /submodules/rive-runtime/    # C++ 运行时子模块
└── /kotlin/src/main/cpp/        # Android 的 C++ 绑定
```

- 这个运行时构建在我们的 [C++ 运行时](https://github.com/rive-app/rive-runtime) 之上
- C++ 运行时作为 git 子模块包含在 [`/submodules`](https://github.com/rive-app/rive-android/tree/master/submodules) 中
- [`/cpp`](https://github.com/rive-app/rive-android/tree/master/kotlin/src/main/cpp) 文件夹包含 Android 绑定的 C++ 端代码

### `/app` - 示例应用

这里包含多个示例 Activity，是入门使用运行时的有用参考。

| 示例 | 说明 |
|------|------|
| `SimpleActivity` | 最基本的使用示例 |
| `StressTestActivity` | 性能压力测试 |
| `LowLevelActivity` | 底层 API 使用示例 |
| `StateMachineActivity` | 状态机交互示例 |
| 等等... | 更多示例请查看源码 |

---

## 🛠️ 开发工作流

### 在本地运行

#### 使用 Gradle

从项目根目录运行：

```shell
./gradlew :app:bundleDebug
```

#### 使用 Android Studio

1. 在 Android Studio 中，确保 `app` 构建变体设置为 `debug`
2. （或手动更新 `build.gradle` 依赖以使用本地 Rive 运行时作为资源）
3. 选择构建变体：**Build > Select Build Variant...** 然后从菜单中选择

### 运行测试

在对源代码进行任何更改后，请务必运行测试套件。

#### 使用 Gradle

从项目根目录运行：

```shell
./gradlew test
./gradlew connectedAndroidTest  # Android 设备/模拟器测试
```

#### 使用 Android Studio

1. 选择 "Project" 视图（右上角）
2. 右键点击 `kotlin/src/androidTest`
3. 选择 "Run All Tests"

---

## 🔧 构建 `.so` 文件

当 `rive-runtime` 子模块有新提交合并时，这里的运行时应该更新指向最新的子模块。这确保 `rive-android` 项目与其底层原生代码层保持同步，以获取最新的补丁、功能等。

在大多数情况下，当引入新的 `rive-runtime` 更改时，我们需要为不同架构构建新的 `.so` 文件。

### 先决条件

1. **安装 Ninja**
   ```bash
   brew install ninja
   ```

2. **下载 Premake5**
   - 访问 [Premake5 下载页面](https://premake.github.io/download)
   - 下载后添加到你的 PATH

### 构建步骤

Android NDK 为[不同架构](https://developer.android.com/ndk/guides/abis)构建 `.so` 文件：

| 架构 | 说明 |
|------|------|
| `armeabi-v7a` | 32位 ARM |
| `arm64-v8a` | 64位 ARM（最常见） |
| `x86` | 32位 x86（模拟器） |
| `x86_64` | 64位 x86（模拟器） |

**NDK 版本**：当前使用的 NDK 版本存储在 [.ndk_version](./kotlin/src/main/cpp/.ndk_version)。Rive 持续使用最新的 clang 特性，请确保你的 NDK 是最新的。

📖 [如何安装特定 NDK 版本](https://developer.android.com/studio/projects/install-ndk#specific-version)

### 重新构建原生库

当从 `rive-runtime` 拉取最新更改时，请确保重新构建原生库：

```bash
# 1. 进入 cpp 目录
cd kotlin/src/main/cpp/

# 2. 添加 NDK_PATH 变量到你的 .zshenv
NDK_VERSION=$(tr <.ndk_version -d " \t\n\r")
echo 'export NDK_PATH=~/Library/Android/sdk/ndk/${NDK_VERSION}' >> ~/.zshenv
source ~/.zshenv

# 3. 返回项目根目录
cd -

# 4. 确保一切仍然可以构建
./gradlew assembleDebug

# 5. 脚本成功完成后，提交你的更改
git add .
git commit -m "Update rive-runtime and rebuild native libraries"
```

---

## 📋 贡献检查清单

在提交 PR 之前，请确保：

- [ ] 代码通过所有现有测试
- [ ] 为新功能添加了适当的测试
- [ ] 更新了相关文档
- [ ] 代码风格符合项目规范
- [ ] 提交信息清晰描述了更改内容

---

## 🐛 报告问题

如果你发现了 bug 或有功能请求，请：

1. 首先搜索 [现有 Issues](https://github.com/rive-app/rive-android/issues)
2. 如果没有找到相关问题，创建一个新 Issue
3. 提供尽可能多的细节：
   - Android 版本
   - 设备型号
   - Rive Android 版本
   - 复现步骤
   - 相关日志或截图

---

## 📚 相关资源

| 资源 | 链接 |
|------|------|
| Rive 官方文档 | [rive.app/docs](https://rive.app/docs) |
| C++ 运行时 | [github.com/rive-app/rive-runtime](https://github.com/rive-app/rive-runtime) |
| Rive 社区 | [community.rive.app](https://community.rive.app) |
| API 文档 | 项目内 `/docs` 目录 |

---

**感谢你对 Rive Android 的贡献！** 🎉

