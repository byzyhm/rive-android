# Rive 动画字体回退策略

> 本文档说明 Intent Android 项目中 Rive 动画的字体加载策略，包括设计原则、实现细节和使用方式。

## 1. 问题背景

Rive 动画中的文本渲染需要字体支持。当 Rive 文件使用 **Out-of-Band (OOB) 字体**（即字体未嵌入 .riv 文件）时，需要在运行时提供字体。

**挑战**：
- Rive SDK 的 `FontHelper.getFallbackFonts()` 无法在所有设备上正确匹配某些语言的字体（如印地语 Devanagari）
- 不同 Android 厂商的系统字体配置差异很大
- CJK 字体体积巨大（10-20MB），不宜打包到 APK

## 2. 设计原则

| 原则 | 说明 |
|------|------|
| **一致性优先** | 关键语言使用内置字体，确保所有设备显示一致 |
| **APK 体积优化** | 仅内置必要的小体积字体，大体积字体使用系统字体 |
| **优雅降级** | 多层回退策略，确保总能显示文本 |
| **使用 Rive API** | 通过 Rive SDK 官方 API 实现，确保兼容性 |

## 3. 字体加载策略

### 3.1 整体架构

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            FontLoader                                        │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────────────┐    ┌─────────────────────┐                         │
│  │ RiveFontAssetLoader │    │ FontFallbackStrategy│                         │
│  │ (处理 OOB 字体)      │    │ (处理字符回退)       │                         │
│  └──────────┬──────────┘    └──────────┬──────────┘                         │
│             │                          │                                     │
│             ▼                          ▼                                     │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │                         字体加载优先级                                 │   │
│  │                                                                       │   │
│  │   ┌─────┐    ┌─────┐    ┌─────┐    ┌─────┐                           │   │
│  │   │  1  │───▶│  2  │───▶│  3  │───▶│  4  │                           │   │
│  │   └──┬──┘    └──┬──┘    └──┬──┘    └──┬──┘                           │   │
│  │      │          │          │          │                               │   │
│  │   内置字体    系统字体    默认内置    系统默认                          │   │
│  │  (res/raw)  (FontHelper)  (拉丁)     (sans-serif)                     │   │
│  │                                                                       │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 3.2 优先级详解

| 优先级 | 来源 | 说明 | 适用场景 |
|:------:|------|------|----------|
| **1** | 内置字体 (`res/raw/`) | 打包在 APK 中的字体文件 | 印地语等系统字体不可靠的语言 |
| **2** | 系统字体 (`FontHelper`) | 通过 Rive SDK 从 `/system/fonts/` 加载 | 中文、日语、韩语等大体积字体 |
| **3** | 默认内置字体 | `noto_sans_regular.ttf` | 其他拉丁字符语言 |
| **4** | 系统默认字体 | `sans-serif` (通常是 Roboto) | 最终回退 |

### 3.3 字体文件配置

```
res/raw/
├── noto_sans_regular.ttf           # 拉丁字符 (~629KB)
│   └── 支持：英、法、德、西、葡、印尼、土耳其语
│
└── noto_sans_devanagari_regular.ttf # 印地语 (~215KB)
    └── 支持：印地语 (Devanagari 脚本)
```

### 3.4 语言到字体的映射

```kotlin
// 内置字体映射（优先级 1）
"hi" (印地语) → noto_sans_devanagari_regular.ttf

// 系统字体配置（优先级 2）
"zh-Hans" (简体中文) → 系统 CJK 字体
"zh-Hant" (繁体中文) → 系统 CJK 字体
"ja"      (日语)     → 系统 CJK 字体
"ko"      (韩语)     → 系统韩文字体
"ru"      (俄语)     → 系统西里尔字体
"ar"      (阿拉伯语) → 系统阿拉伯字体
"th"      (泰语)     → 系统泰语字体

// 其他语言 → 默认拉丁字体 (noto_sans_regular.ttf)
```

## 4. Rive SDK API 使用

### 4.1 FileAssetLoader

用于加载 Rive 动画中的 **Out-of-Band 资源**（字体、图片等）。

```kotlin
class RiveFontAssetLoader(context: Context) : FileAssetLoader() {
    override fun loadContents(asset: FileAsset, inBandBytes: ByteArray): Boolean {
        if (asset !is FontAsset) return false
        
        // 加载字体并解码
        val fontBytes = loadFont(...)
        return asset.decode(fontBytes)
    }
}

// 使用方式
rivView.setAssetLoader(FontLoader.createFontAssetLoader(context))
```

### 4.2 FontFallbackStrategy

当 Rive 动画中的字符在当前字体中找不到时，会调用此策略获取备用字体。

```kotlin
val fallbackStrategy: FontFallbackStrategy = object : FontFallbackStrategy {
    override fun getFont(weight: Fonts.Weight): List<ByteArray> {
        // 返回所有可用字体，按优先级排序
        return listOf(
            latinFont,      // 拉丁字符
            devanagariFont, // 印地语
            chineseFont,    // 中文
            koreanFont,     // 韩文
            // ...
        )
    }
}

// 设置全局策略
FontFallbackStrategy.stylePicker = fallbackStrategy
```

### 4.3 FontHelper

Rive SDK 提供的字体工具类，用于查找和加载系统字体。

```kotlin
// 根据语言查找字体
val fonts = FontHelper.getFallbackFonts(Fonts.FontOpts(lang = "zh-Hans"))

// 读取字体字节
val fontBytes = FontHelper.getFontBytes(fonts.first())
```

## 5. 实现细节

### 5.1 预加载机制

为避免首次显示时的延迟，在 `Application.onCreate()` 时后台预加载所有字体：

```kotlin
// CommonHelper.init() 中调用
FontLoader.init(context)

// FontLoader 内部
Thread {
    preloadBuiltInFonts()  // 加载 res/raw/ 中的字体
    preloadSystemFonts()   // 加载系统字体
}.start()
```

### 5.2 缓存策略

```kotlin
// 内置字体缓存（按资源 ID）
private val builtInFontCache = ConcurrentHashMap<Int, ByteArray>()

// 系统字体缓存（列表形式）
@Volatile
private var systemFontsCache: List<ByteArray>? = null
```

### 5.3 内存管理

```kotlin
// 使用 WeakReference 持有 Context，避免内存泄漏
private var contextRef: WeakReference<Context>? = null

// ⚠️ 重要：fallbackStrategy 必须是强引用
// 因为 FontFallbackStrategy.stylePicker 使用 WeakReference
val fallbackStrategy: FontFallbackStrategy = object : FontFallbackStrategy { ... }
```

## 6. 日志示例

正常运行时的日志输出：

```
# 预加载阶段
FontLoader: 预加载内置字体成功: resId=xxx (629024 bytes)   # 拉丁
FontLoader: 预加载内置字体成功: resId=xxx (215368 bytes)   # 印地语
FontLoader: 预加载系统字体: Roboto-Regular.ttf (2371712 bytes)
FontLoader: 预加载系统字体: SECCJK-Regular.ttc (19093684 bytes)
FontLoader: 预加载系统字体: OneUISansKR-VF.ttf (6562904 bytes)
FontLoader: 未找到系统字体 (zh-Hant)  # 正常，部分设备可能没有
FontLoader: 系统字体预加载完成，共 3 个
FontLoader: 字体预加载完成，耗时 313ms

# 运行时加载
FontLoader: 加载 OOB 字体, asset=Noto Sans, locale=hi
FontLoader: ✓ 使用内置字体 [hi]: 215368 bytes
FontLoader: getAllFonts 返回 5 个字体
```

## 7. 扩展指南

### 添加新语言的内置字体

1. 将字体文件放入 `res/raw/` 目录（文件名只能包含小写字母、数字和下划线）
2. 在 `BuiltInFonts` 中添加资源引用
3. 在 `LANGUAGE_TO_BUILTIN_FONT` 中添加语言映射

```kotlin
private object BuiltInFonts {
    @RawRes val LATIN: Int = R.raw.noto_sans_regular
    @RawRes val DEVANAGARI: Int = R.raw.noto_sans_devanagari_regular
    @RawRes val ARABIC: Int = R.raw.noto_sans_arabic_regular  // 新增
}

private val LANGUAGE_TO_BUILTIN_FONT: Map<String, Int> = mapOf(
    "hi" to BuiltInFonts.DEVANAGARI,
    "ar" to BuiltInFonts.ARABIC,  // 新增
)
```

### 添加新的系统字体配置

```kotlin
private val SYSTEM_FONT_CONFIGS = listOf(
    // ... 现有配置
    Fonts.FontOpts(lang = "vi"),  // 越南语
)
```

## 8. 相关文件

| 文件 | 说明 |
|------|------|
| `FontLoader.kt` | 字体加载器主类 |
| `LanguageUtil.kt` | 语言工具类，获取当前 Locale |
| `CommonHelper.kt` | 初始化入口 |
| `BaseSignOnboardingPerformance.kt` | 使用示例 |

## 9. 参考资料

- [Rive Android SDK - Fonts](https://github.com/rive-app/rive-android)
- [Rive Android Demo - FontPreloader](https://github.com/rive-app/rive-android/blob/main/app/src/main/java/app/rive/runtime/example/font/FontPreloader.kt)
- [Android fonts.xml 结构](https://android.googlesource.com/platform/frameworks/base/+/master/data/fonts/fonts.xml)

