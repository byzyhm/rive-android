# Rive Android 资源加载

本文档介绍 Rive Android 的资源加载、字体和音频相关功能。

> 📚 返回 [Demo Activities 完整指南](./demo_activities_guide.md)

---

## 11. HttpActivity

### 📝 描述
从网络加载 .riv 文件。

### 🔧 使用的 API

| API | 说明 |
|-----|------|
| `setRiveBytes` | 从字节数组加载 |
| `ViewModel` + `LiveData` | 异步加载 |

### ✅ 支持的功能
- 网络加载动画
- 异步加载
- 加载完成回调

### 🎯 使用场景
- 动态下载动画
- 服务端动画配置
- 减少 APK 体积

### 💻 示例代码

```kotlin
class HttpViewModel : ViewModel() {
    val byteLiveData = MutableLiveData<ByteArray>()
    
    fun fetchUrl(url: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                byteLiveData.postValue(URL(url).openStream().readBytes())
            }
        }
    }
}

// 使用
httpViewModel.byteLiveData.observe(this) { bytes ->
    animationView.setRiveBytes(bytes, fit = Fit.COVER)
}
```

---

## 18. AssetsActivity

### 📝 描述
从 Assets 加载 Rive 资源。

### 🎯 使用场景
- Assets 目录资源管理

---

## 26. AssetLoaderActivity

### 📝 描述
自定义资源加载器，支持从不同来源加载图片、字体等资源。

### 🔧 使用的 API

| API | 说明 |
|-----|------|
| `FileAssetLoader` | 资源加载器基类 |
| `ContextAssetLoader` | 带 Context 的加载器 |
| `FileAsset.decode()` | 解码资源 |
| `BytesRequest` (Volley) | 网络请求 |

### ✅ 支持的功能
- 自定义图片加载
- 网络资源加载
- 随机资源选择

### 🎯 使用场景
- CDN 图片加载
- 动态资源替换
- 外部资源集成

### 💻 示例代码

```kotlin
class CustomAssetLoader(context: Context) : ContextAssetLoader(context) {
    override fun loadContents(asset: FileAsset, inBandBytes: ByteArray): Boolean {
        context.resources.openRawResource(R.raw.custom_image).use {
            asset.decode(it.readBytes())
        }
        return true
    }
}

// 使用
val riveView = RiveAnimationView.Builder(this)
    .setAssetLoader(CustomAssetLoader(this))
    .setResource(R.raw.animation)
    .build()
```

---

## 28. FontLoadActivity

### 📝 描述
自定义字体加载，随机选择不同字体。

### 🔧 使用的 API

| API | 说明 |
|-----|------|
| `ContextAssetLoader` | 自定义加载器 |
| `FileAsset.decode()` | 解码字体 |

### ✅ 支持的功能
- 自定义字体加载
- 多字体选择
- 动态字体切换

### 🎯 使用场景
- 品牌字体
- 多语言字体
- 动态字体选择

### 💻 示例代码

```kotlin
class FontAssetLoader(context: Context) : ContextAssetLoader(context) {
    private val fontPool = arrayOf(R.raw.font1, R.raw.font2, R.raw.font3)

    override fun loadContents(asset: FileAsset, inBandBytes: ByteArray): Boolean {
        val fontToLoad = fontPool[Random.nextInt(fontPool.size)]
        context.resources.openRawResource(fontToLoad).use {
            return asset.decode(it.readBytes())
        }
    }
}
```

---

## 31. FontFallback

### 📝 描述
实现字体回退策略，根据字重选择不同的回退字体。

### 🔧 使用的 API

| API | 说明 |
|-----|------|
| `FontFallbackStrategy` | 字体回退接口 |
| `FontFallbackStrategy.stylePicker` | 设置策略 |
| `getFont(weight)` | 根据字重返回字体 |
| `FontHelper.getFallbackFontBytes` | 获取回退字体 |
| `Fonts.FontOpts` | 字体选项 |

### ✅ 支持的功能
- 按字重选择字体
- 多字体回退链
- 多语言支持（如泰语）

### 🎯 使用场景
- 多语言文本
- 缺字回退
- 品牌字体 + 系统回退

### 💻 示例代码

```kotlin
class FontFallbackActivity : AppCompatActivity(), FontFallbackStrategy {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FontFallbackStrategy.stylePicker = this
    }

    override fun getFont(weight: Fonts.Weight): List<FontBytes> {
        val fonts = listOf(
            Fonts.FontOpts(familyName = "serif"),
            Fonts.FontOpts("NotoSansThai-Regular.ttf")  // 泰语支持
        )
        return fonts.mapNotNull { FontHelper.getFallbackFontBytes(it) }
    }
}
```

---

## 29. AudioAssetActivity

### 📝 描述
使用嵌入的音频资源。

### 🎯 使用场景
- 带音效的动画
- 游戏音效

---

## 30. AudioExternalAssetActivity

### 📝 描述
加载外部音频资源。

### 🔧 使用的 API

| API | 说明 |
|-----|------|
| `AudioAsset` | 音频资源类型 |
| `RiveAudio.make()` | 创建音频 |
| `asset.audio` | 设置音频 |
| `setVolume()` | 设置音量 |

### ✅ 支持的功能
- 外部音频加载
- 音量控制
- 音频资源匹配

### 🎯 使用场景
- 外部音效
- 动态音频加载
- 音量控制

### 💻 示例代码

```kotlin
class AudioDecoder(private val context: Context) : FileAssetLoader() {
    override fun loadContents(asset: FileAsset, inBandBytes: ByteArray): Boolean {
        if (asset is AudioAsset) {
            val audio = context.resources.openRawResource(R.raw.sound).use {
                RiveAudio.make(it.readBytes())
            }
            asset.audio = audio
            return true
        }
        return false
    }
}

riveView.setVolume(0.75f)
```

---

*返回 [Demo Activities 完整指南](./demo_activities_guide.md)*

