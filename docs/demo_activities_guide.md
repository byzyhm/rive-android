# Rive Android Demo Activities 完整指南

本文档详细介绍了 rive-android 项目中所有 Demo Activity 的功能、使用的 API 和适用场景。

---

## 目录

1. [ComposeActivity](#1-composeactivity) - Jetpack Compose 集成
2. [SimpleActivity](#2-simpleactivity) - 最简单的使用示例
3. [EventsActivity](#3-eventsactivity) - Rive 事件监听
4. [InteractiveSamplesActivity](#4-interactivesamplesactivity) - 交互式动画（时钟）
5. [MultipleArtboardsActivity](#5-multipleartboardsactivity) - 多 Artboard
6. [AndroidPlayerActivity](#6-androidplayeractivity) - 完整播放器控制
7. [LoopModeActivity](#7-loopmodeactivity) - 循环模式控制
8. [LayoutActivity](#8-layoutactivity) - Fit 和 Alignment 布局
9. [RiveFragmentActivity](#9-rivefragmentactivity) - Fragment 集成
10. [LowLevelActivity](#10-lowlevelactivity) - 底层 API 使用
11. [HttpActivity](#11-httpactivity) - 网络加载 .riv 文件
12. [SimpleStateMachineActivity](#12-simplestatemachineactivity) - 状态机控制
13. [NestedInputActivity](#13-nestedinputactivity) - 嵌套 Artboard 输入
14. [NestedTextRunActivity](#14-nestedtextrunactivity) - 嵌套 Text Run
15. [ButtonActivity](#15-buttonactivity) - 自定义 Rive 按钮
16. [BlendActivity](#16-blendactivity) - 混合模式
17. [MetricsActivity](#17-metricsactivity) - 性能指标（FPS）
18. [AssetsActivity](#18-assetsactivity) - Assets 资源加载
19. [RecyclerActivity](#19-recycleractivity) - RecyclerView 集成
20. [ViewPagerActivity](#20-viewpageractivity) - ViewPager2 集成
21. [MeshesActivity](#21-meshesactivity) - 网格动画
22. [ViewStubActivity](#22-viewstubactivity) - ViewStub 延迟加载
23. [LegacyComposeActivity](#23-legacycomposeactivity) - 传统 Compose 集成
24. [FrameActivity](#24-frameactivity) - Fragment 切换
25. [DynamicTextActivity](#25-dynamictextactivity) - 动态文本
26. [AssetLoaderActivity](#26-assetloaderactivity) - 自定义资源加载器
27. [StressTestActivity](#27-stresstestactivity) - 压力测试
28. [FontLoadActivity](#28-fontloadactivity) - 字体加载
29. [AudioAssetActivity](#29-audioassetactivity) - 音频资源
30. [AudioExternalAssetActivity](#30-audioexternalassetactivity) - 外部音频资源
31. [FontFallback](#31-fontfallback) - 字体回退策略
32. [TouchPassthroughActivity](#32-touchpassthroughactivity) - 触摸穿透
33. [ImageBindingActivity](#33-imagebindingactivity) - 图片绑定

---

## 1. ComposeActivity

### 📝 描述
展示如何在 Jetpack Compose 中使用新的 Rive Compose API。

### 🔧 使用的 API

| API | 说明 |
|-----|------|
| `RiveUI` | Compose 中的 Rive 渲染组件 |
| `rememberCommandQueueOrNull` | 创建命令队列 |
| `rememberRiveFile` | 加载 Rive 文件 |
| `rememberArtboard` | 获取 Artboard |
| `rememberRegisteredFont` | 注册自定义字体 |
| `rememberViewModelInstance` | 创建 ViewModel 实例 |
| `getNumberFlow` | 获取 Number 属性的 Flow |
| `setNumber` | 设置 Number 属性 |
| `Fit` / `Alignment` | 布局控制 |

### ✅ 支持的功能
- Compose 原生集成
- 动态切换 Artboard
- 动态切换 Fit/Alignment
- ViewModel 数据绑定
- 自定义字体加载

### 🎯 使用场景
- Jetpack Compose 项目
- 需要响应式数据绑定
- 需要动态控制动画参数

### 💻 示例代码

```kotlin
@OptIn(ExperimentalRiveComposeAPI::class)
val commandQueue = rememberCommandQueueOrNull(errorState)
val riveFile = rememberRiveFile(
    RiveFileSource.RawRes(R.raw.animation, context.resources),
    commandQueue
)
val artboard = rememberArtboard(riveFile.value, artboardName)

RiveUI(
    file = riveFile.value,
    artboard = artboard,
    fit = Fit.CONTAIN,
    alignment = Alignment.CENTER
)
```

---

## 2. SimpleActivity

### 📝 描述
最简单的 Rive 动画使用示例，仅通过 XML 配置即可播放动画。

### 🔧 使用的 API

| API | 说明 |
|-----|------|
| `RiveAnimationView` | XML 布局中使用 |
| `app:riveResource` | 指定 .riv 资源 |
| `app:riveAutoPlay` | 自动播放 |

### ✅ 支持的功能
- XML 声明式配置
- 自动播放动画

### 🎯 使用场景
- 简单动画展示
- 不需要代码控制的静态动画
- 快速原型开发

### 💻 示例代码

```xml
<app.rive.runtime.kotlin.RiveAnimationView
    android:id="@+id/rive_view"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    app:riveResource="@raw/animation"
    app:riveAutoPlay="true" />
```

---

## 3. EventsActivity

### 📝 描述
演示如何监听 Rive 动画中的事件，包括通用事件和 URL 打开事件。

### ⚠️ 重要：事件数据来源

**Rive Events 是由设计师在 Rive 编辑器中配置的，嵌入在 .riv 文件中。**

```
┌─────────────────────────────────────────────────────────────────────┐
│                    Rive Events 数据流                               │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  1. 设计师在 Rive 编辑器中创建事件                                    │
│     ↓                                                                │
│  2. 事件配置保存在 .riv 文件中                                        │
│     ↓                                                                │
│  3. 动画播放到特定时间点触发事件                                      │
│     ↓                                                                │
│  4. 开发者通过 RiveEventListener 接收事件                            │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 📋 事件结构示例

以下是从日志中看到的事件数据，**这些都是 .riv 文件中内置的**：

```
// 1. 星级评分事件
General event received, name: Star Rating, delaySeconds: 0.0 properties: {rating=4.0}

// 2. URL 打开事件
Open URL Rive event: https://rive.app

// 3. 通用事件（带多个属性）
name: Demo
delay: 0.0
type: GeneralEvent
properties: {StringDemo=Demo value, NumberDemo=5.0, BooleanDemo=false}
data: {name=Demo, properties={StringDemo=Demo value, NumberDemo=5.0, BooleanDemo=false}}
```

### 📝 设计师配置的内容

| 设置项 | 示例值 | 说明 |
|--------|--------|------|
| **Event Name** | `Star Rating` | 事件名称（开发者用来区分事件） |
| **Event Type** | `GeneralEvent` / `OpenURLEvent` | 事件类型 |
| **Delay** | `0.0` | 触发延迟（秒） |
| **Properties** | `{rating: 4.0}` | 自定义属性（Key-Value） |
| **URL** | `https://rive.app` | OpenURL 事件的 URL |

### 📋 开发者需要从设计师获取的信息

| 信息 | 用途 | 示例 |
|------|------|------|
| **事件名称** | 区分不同事件 | `"Star Rating"` |
| **事件类型** | 处理逻辑不同 | `GeneralEvent` / `OpenURLEvent` |
| **属性名称** | 获取属性值 | `"rating"`, `"StringDemo"` |
| **属性类型** | 正确转换类型 | Number, String, Boolean |
| **触发时机** | 理解业务逻辑 | "用户点击星星时" |

### 🔧 使用的 API

| API | 说明 |
|-----|------|
| `RiveFileController.RiveEventListener` | 事件监听器接口 |
| `addEventListener` | 添加事件监听 |
| `removeEventListener` | 移除事件监听 |
| `RiveGeneralEvent` | 通用事件 |
| `RiveOpenURLEvent` | URL 打开事件 |
| `event.name` | 事件名称 |
| `event.delay` | 事件延迟 |
| `event.type` | 事件类型 |
| `event.properties` | 事件属性（Map） |
| `event.data` | 完整事件数据 |

### ✅ 支持的功能
- 监听动画内部事件
- 获取事件携带的数据（String, Number, Boolean）
- URL 事件自动打开浏览器
- 事件日志记录

### 🎯 使用场景
- 动画触发业务逻辑（如：评分、购买）
- 动画中的交互反馈
- 动画播放统计
- 跳转外部链接

### 💻 示例代码

```kotlin
val listener = object : RiveFileController.RiveEventListener {
    override fun notifyEvent(event: RiveEvent) {
        when (event) {
            is RiveGeneralEvent -> {
                Log.i("RiveEvent", "name: ${event.name}")
                Log.i("RiveEvent", "delay: ${event.delay}")
                Log.i("RiveEvent", "type: ${event.type}")
                Log.i("RiveEvent", "properties: ${event.properties}")
                
                // 根据设计师提供的属性名获取值
                val rating = event.properties["rating"] as? Double
                val stringValue = event.properties["StringDemo"] as? String
                val numberValue = event.properties["NumberDemo"] as? Double
                val boolValue = event.properties["BooleanDemo"] as? Boolean
                
                // 处理业务逻辑...
            }
            is RiveOpenURLEvent -> {
                // 打开设计师配置的 URL
                val uri = Uri.parse(event.url)
                startActivity(Intent(Intent.ACTION_VIEW, uri))
            }
        }
    }
}

// 添加监听
riveView.addEventListener(listener)

// 记得在 onDestroy 中移除
override fun onDestroy() {
    riveView.removeEventListener(listener)
    super.onDestroy()
}
```

### 💡 最佳实践

建议让设计师提供**事件清单文档**：

```markdown
## 动画事件清单

### 1. Star Rating 事件
- 类型: GeneralEvent
- 触发时机: 用户点击星星时
- 属性:
  - rating (Number): 1-5 的评分值
- 业务逻辑: 更新 UI 显示评分

### 2. Open Rive 事件  
- 类型: OpenURLEvent
- 触发时机: 用户点击按钮时
- URL: https://rive.app
- 业务逻辑: 打开浏览器

### 3. Demo 事件
- 类型: GeneralEvent
- 触发时机: 动画播放完成时
- 属性:
  - StringDemo (String): 字符串值
  - NumberDemo (Number): 数字值
  - BooleanDemo (Boolean): 布尔值
```

---

## 4. InteractiveSamplesActivity

### 📝 描述
展示交互式动画，通过定时更新 Number 输入实现实时时钟效果。

### 🔧 使用的 API

| API | 说明 |
|-----|------|
| `setNumberState` | 设置 Number 类型的状态机输入 |
| `Handler.postDelayed` | 定时更新 |

### ✅ 支持的功能
- 实时数据驱动动画
- 定时更新动画状态
- 生命周期感知（防止内存泄漏）

### 🎯 使用场景
- 实时时钟/仪表盘
- 数据可视化
- 实时状态展示

### 💻 示例代码

```kotlin
fun setTime() {
    val hours = Calendar.getInstance().get(Calendar.HOUR) % 12f + 
                Calendar.getInstance().get(Calendar.MINUTE) / 60f
    clockView.setNumberState("Time", "isTime", hours)
}

Handler(Looper.getMainLooper()).postDelayed(object : Runnable {
    override fun run() {
        setTime()
        handler.postDelayed(this, 360)
    }
}, 360)
```

---

## 5. MultipleArtboardsActivity

### 📝 描述
展示在单个布局中使用多个不同的 Artboard。

### 🔧 使用的 API

| API | 说明 |
|-----|------|
| `app:riveArtboard` | XML 中指定 Artboard 名称 |

### ✅ 支持的功能
- 同一 .riv 文件的多个 Artboard
- 布局中多个动画实例

### 🎯 使用场景
- 复杂 UI 中的多个动画组件
- 动画组合展示

---

## 6. AndroidPlayerActivity

### 📝 描述
完整的动画播放器，支持切换资源、Artboard、动画、状态机，以及控制播放/暂停/停止。

### 🔧 使用的 API

| API | 说明 |
|-----|------|
| `setRiveResource` | 加载 Rive 资源 |
| `artboardName` | 设置/获取当前 Artboard |
| `play()` / `pause()` / `stop()` | 播放控制 |
| `reset()` | 重置动画 |
| `Loop.AUTO/LOOP/ONESHOT/PINGPONG` | 循环模式 |
| `Direction.AUTO/FORWARDS/BACKWARDS` | 播放方向 |
| `fireState` | 触发 Trigger |
| `setBooleanState` | 设置 Boolean 输入 |
| `setNumberState` | 设置 Number 输入 |
| `RiveFileController.Listener` | 播放状态监听 |

### ✅ 支持的功能
- 动态切换 Rive 资源
- 动态切换 Artboard
- 播放/暂停/停止控制
- 循环模式控制
- 播放方向控制
- 状态机输入控制
- 播放状态回调

### 🎯 使用场景
- Rive 动画调试工具
- 动画预览器
- 需要完整播放控制的场景

### 💻 示例代码

```kotlin
// 播放控制
animationView.play("animationName", Loop.LOOP, Direction.FORWARDS)
animationView.pause("animationName")
animationView.stop("animationName")
animationView.reset()

// 状态机控制
animationView.fireState("stateMachine", "triggerName")
animationView.setBooleanState("stateMachine", "boolName", true)
animationView.setNumberState("stateMachine", "numberName", 1.5f)

// 监听状态
animationView.registerListener(object : RiveFileController.Listener {
    override fun notifyPlay(animation: PlayableInstance) { }
    override fun notifyPause(animation: PlayableInstance) { }
    override fun notifyStop(animation: PlayableInstance) { }
    override fun notifyLoop(animation: PlayableInstance) { }
    override fun notifyStateChanged(stateMachineName: String, stateName: String) { }
    override fun notifyAdvance(elapsed: Float) { }
})
```

---

## 7. LoopModeActivity

### 📝 描述
演示不同循环模式（OneShot, Loop, PingPong）和播放方向的效果。

### 🔧 使用的 API

| API | 说明 |
|-----|------|
| `Loop.ONESHOT` | 播放一次 |
| `Loop.LOOP` | 循环播放 |
| `Loop.PINGPONG` | 来回播放 |
| `Direction.FORWARDS` | 正向播放 |
| `Direction.BACKWARDS` | 反向播放 |
| `Direction.AUTO` | 自动方向 |

### ✅ 支持的功能
- OneShot 单次播放
- Loop 循环播放
- PingPong 来回播放
- 正向/反向播放

### 🎯 使用场景
- 按钮点击动画（OneShot）
- 加载动画（Loop）
- 呼吸灯效果（PingPong）

### 💻 示例代码

```kotlin
// 单次播放
animationView.play("animation", loop = Loop.ONESHOT)

// 循环播放
animationView.play("animation", loop = Loop.LOOP)

// 来回播放
animationView.play("animation", loop = Loop.PINGPONG)

// 反向播放
animationView.play("animation", direction = Direction.BACKWARDS)
```

---

## 8. LayoutActivity

### 📝 描述
演示 Fit 和 Alignment 属性的不同效果。

### 🔧 使用的 API

| API | 说明 |
|-----|------|
| `Fit.CONTAIN` | 保持比例，完整显示 |
| `Fit.COVER` | 保持比例，填充容器 |
| `Fit.FILL` | 拉伸填充 |
| `Fit.FIT_WIDTH` | 适应宽度 |
| `Fit.FIT_HEIGHT` | 适应高度 |
| `Fit.NONE` | 原始尺寸 |
| `Fit.SCALE_DOWN` | 仅缩小 |
| `Fit.LAYOUT` | 响应式布局 |
| `Alignment.*` | 9 种对齐方式 |
| `layoutScaleFactor` | 布局缩放因子 |

### ✅ 支持的功能
- 8 种 Fit 模式
- 9 种 Alignment 对齐
- 布局缩放因子控制

### 🎯 使用场景
- 不同屏幕尺寸适配
- 响应式动画布局
- 动画对齐控制

### 💻 示例代码

```kotlin
animationView.fit = Fit.CONTAIN
animationView.alignment = Alignment.CENTER

// 响应式布局缩放
animationView.fit = Fit.LAYOUT
animationView.layoutScaleFactor = 2f  // 手动设置
animationView.layoutScaleFactor = null // 自动
```

---

## 9. RiveFragmentActivity

### 📝 描述
在 Fragment 中使用 RiveAnimationView。

### 🔧 使用的 API

| API | 说明 |
|-----|------|
| `bundleOf` | 传递参数 |
| `supportFragmentManager.commit` | Fragment 事务 |

### ✅ 支持的功能
- Fragment 封装 Rive 动画
- 参数传递

### 🎯 使用场景
- 模块化 UI
- 可复用的动画组件
- Navigation 组件集成

### 💻 示例代码

```kotlin
// Fragment
class RiveFragment : Fragment() {
    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        riveView.setRiveResource(resId)
        riveView.fit = Fit.COVER
    }
}

// Activity
supportFragmentManager.commit {
    add<RiveFragment>(R.id.container, args = bundleOf(
        RIVE_FRAGMENT_ARG_RES_ID to R.raw.animation
    ))
}
```

---

## 10. LowLevelActivity

### 📝 描述
使用底层 API 直接控制 Rive 渲染，适用于需要自定义渲染逻辑的高级场景。

### 🔧 使用的 API

| API | 说明 |
|-----|------|
| `RiveTextureView` | 底层渲染视图 |
| `Renderer` | 自定义渲染器 |
| `File` | Rive 文件对象 |
| `Artboard` | Artboard 对象 |
| `StateMachineInstance` | 状态机实例 |
| `draw()` / `advance()` | 渲染方法 |
| `align()` | 对齐渲染 |
| `save()` / `restore()` | 渲染状态 |

### ✅ 支持的功能
- 完全自定义渲染逻辑
- 直接访问 Artboard 和 StateMachine
- 手动控制动画推进
- 自定义布局和变换

### 🎯 使用场景
- 游戏引擎集成
- 自定义渲染管线
- 高性能需求
- 特殊渲染效果

### 💻 示例代码

```kotlin
class CustomRiveView(context: Context) : RiveTextureView(context) {
    private lateinit var file: File
    private lateinit var artboard: Artboard
    private lateinit var stateMachine: StateMachineInstance

    override fun createRenderer(): Renderer {
        return object : Renderer() {
            override fun draw() {
                save()
                align(Fit.LAYOUT, Alignment.CENTER, 
                      RectF(0f, 0f, width, height), artboard.bounds)
                artboard.draw(cppPointer)
                restore()
            }

            override fun advance(elapsed: Float) {
                stateMachine.advance(elapsed)
                artboard.advance(elapsed)
            }
        }
    }
}
```

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

## 12. SimpleStateMachineActivity

### 📝 描述
简单的状态机控制示例。

### 🔧 使用的 API

| API | 说明 |
|-----|------|
| `setNumberState` | 设置 Number 输入 |
| `app:riveStateMachine` | XML 指定状态机 |

### ✅ 支持的功能
- Number 输入控制
- 状态机切换

### 🎯 使用场景
- 级别选择
- 进度指示
- 分数显示

### 💻 示例代码

```kotlin
animationView.setNumberState("StateMachine", "Level", 0f)  // Beginner
animationView.setNumberState("StateMachine", "Level", 1f)  // Intermediate
animationView.setNumberState("StateMachine", "Level", 2f)  // Advanced
```

---

## 13. NestedInputActivity

### 📝 描述
控制嵌套 Artboard 中的输入。

### 🔧 使用的 API

| API | 说明 |
|-----|------|
| `setBooleanStateAtPath` | 通过路径设置嵌套输入 |

### ✅ 支持的功能
- 嵌套 Artboard 输入控制
- 路径定位嵌套元素

### 🎯 使用场景
- 复杂的嵌套动画结构
- 组合动画控制

### 💻 示例代码

```kotlin
// 外层 Artboard
animationView.setBooleanStateAtPath("CircleOuterState", true, "CircleOuter")

// 内层嵌套 Artboard
animationView.setBooleanStateAtPath("CircleInnerState", true, "CircleOuter/CircleInner")
```

---

## 14. NestedTextRunActivity

### 📝 描述
控制嵌套 Artboard 中的 Text Run。

### 🔧 使用的 API

| API | 说明 |
|-----|------|
| `setTextRunValue(name, value, path)` | 设置嵌套 Text Run |
| `getTextRunValue(name, path)` | 获取嵌套 Text Run |

### ✅ 支持的功能
- 嵌套 Text Run 读写
- 路径定位

### 🎯 使用场景
- 复杂 UI 中的动态文本
- 嵌套组件文本更新

### 💻 示例代码

```kotlin
// 设置嵌套文本
animationView.setTextRunValue(
    textRunName = "ArtboardBRun",
    textValue = "Updated Text",
    path = "ArtboardB-1/ArtboardC-1"
)

// 获取嵌套文本
val text = animationView.getTextRunValue("ArtboardBRun", "ArtboardB-1")
```

---

## 15. ButtonActivity

### 📝 描述
自定义 Rive 按钮组件，支持点击动画。

### 🔧 使用的 API

| API | 说明 |
|-----|------|
| 自定义 `RiveButton` 类 | 继承 `RiveAnimationView` |
| `performClick` | 点击触发动画 |
| `controller.play()` | 播放点击动画 |

### ✅ 支持的功能
- 自定义按钮样式
- 点击动画反馈
- 可配置的动画名称

### 🎯 使用场景
- 自定义按钮
- 交互反馈
- 游戏 UI

### 💻 示例代码

```kotlin
class RiveButton(context: Context, attrs: AttributeSet?) : RiveAnimationView(context, attrs) {
    override fun performClick(): Boolean {
        controller.stopAnimations()
        controller.play(pressAnimation)
        return super.performClick()
    }
}
```

---

## 16. BlendActivity

### 📝 描述
展示混合模式动画。

### 🎯 使用场景
- 特效动画
- 图层混合效果

---

## 17. MetricsActivity

### 📝 描述
显示 Rive 动画的性能指标（FPS）。

### 🔧 使用的 API

| API | 说明 |
|-----|------|
| `artboardRenderer.averageFps` | 获取平均 FPS |
| `Choreographer.FrameCallback` | 帧回调 |

### ✅ 支持的功能
- 实时 FPS 监控
- 性能分析

### 🎯 使用场景
- 性能调试
- 优化验证

### 💻 示例代码

```kotlin
class MetricsActivity : AppCompatActivity(), Choreographer.FrameCallback {
    override fun doFrame(frameTimeNanos: Long) {
        val fps = riveView.artboardRenderer?.averageFps ?: -1f
        binding.fps.text = "FPS: $fps"
        Choreographer.getInstance().postFrameCallback(this)
    }
}
```

---

## 18. AssetsActivity

### 📝 描述
从 Assets 加载 Rive 资源。

### 🎯 使用场景
- Assets 目录资源管理

---

## 19. RecyclerActivity

### 📝 描述
在 RecyclerView 中高效使用 Rive 动画。

### 🔧 使用的 API

| API | 说明 |
|-----|------|
| `File` (共享) | 共享 Rive 文件实例 |
| `setRiveFile` | 使用共享文件 |
| `saveControllerState` | 保存控制器状态 |
| `restoreControllerState` | 恢复控制器状态 |
| `ControllerStateManagement` | 状态管理注解 |

### ✅ 支持的功能
- ViewHolder 复用
- 动画状态保存/恢复
- 共享 Rive 文件减少内存

### 🎯 使用场景
- 列表中的动画
- 瀑布流动画
- 高性能列表

### 💻 示例代码

```kotlin
@ControllerStateManagement
class RiveAdapter(private val sharedFile: File) : ListAdapter<...>() {
    val resourceCache = arrayOfNulls<ControllerState>(200)

    override fun onViewAttachedToWindow(holder: RiveViewHolder) {
        resourceCache[position]?.let { savedState ->
            holder.riveView.restoreControllerState(savedState)
        } ?: holder.riveView.setRiveFile(sharedFile)
    }

    override fun onViewDetachedFromWindow(holder: RiveViewHolder) {
        resourceCache[position] = holder.riveView.saveControllerState()
    }
}
```

---

## 20. ViewPagerActivity

### 📝 描述
在 ViewPager2 中使用 Rive 动画。

### 🔧 使用的 API

| API | 说明 |
|-----|------|
| `saveControllerState` | 保存状态 |
| `restoreControllerState` | 恢复状态 |
| `offscreenPageLimit` | 预加载页数 |

### ✅ 支持的功能
- 页面切换动画
- 状态保存/恢复
- 预加载优化

### 🎯 使用场景
- 引导页
- 轮播动画
- 画廊展示

---

## 21. MeshesActivity

### 📝 描述
展示网格变形动画。

### 🎯 使用场景
- 角色动画
- 变形效果

---

## 22. ViewStubActivity

### 📝 描述
使用 ViewStub 延迟加载 Rive 动画。

### 🔧 使用的 API

| API | 说明 |
|-----|------|
| `ViewStub.visibility` | 触发加载 |
| `setOnInflateListener` | 加载完成回调 |

### ✅ 支持的功能
- 延迟加载动画
- 显示/隐藏控制

### 🎯 使用场景
- 条件性显示动画
- 优化首次加载

### 💻 示例代码

```kotlin
viewStub.setOnInflateListener { _, _ ->
    supportFragmentManager.commit {
        add<RiveFragment>(R.id.container)
    }
}

// 显示
viewStub.visibility = View.VISIBLE
// 隐藏
viewStub.visibility = View.GONE
```

---

## 23. LegacyComposeActivity

### 📝 描述
使用 AndroidView 在 Compose 中嵌入 RiveAnimationView（传统方式）。

### 🔧 使用的 API

| API | 说明 |
|-----|------|
| `AndroidView` | Compose 中嵌入 View |
| `RiveAnimationView.setRiveResource` | 设置资源 |

### ✅ 支持的功能
- Compose 集成（传统方式）
- View 级别控制

### 🎯 使用场景
- 渐进式 Compose 迁移
- 需要 View API 的场景

### 💻 示例代码

```kotlin
@Composable
fun CustomRiveAnimationView(@RawRes animation: Int) {
    AndroidView(
        factory = { context ->
            RiveAnimationView(context).also {
                it.setRiveResource(resId = animation)
            }
        }
    )
}
```

---

## 24. FrameActivity

### 📝 描述
Fragment 切换示例，展示动画在 Fragment 生命周期中的行为。

### 🔧 使用的 API

| API | 说明 |
|-----|------|
| `supportFragmentManager.commit` | Fragment 事务 |
| `replace` | 替换 Fragment |

### ✅ 支持的功能
- Fragment 切换
- 动画生命周期管理

### 🎯 使用场景
- 页面切换动画
- Fragment 导航

---

## 25. DynamicTextActivity

### 📝 描述
动态修改 Rive 动画中的 Text Run。

### 🔧 使用的 API

| API | 说明 |
|-----|------|
| `artboard.textRun("name")` | 获取 Text Run 引用 |
| `textRun.text` | 读写文本内容 |
| `setTextRunValue` | 直接设置值 |
| `getTextRunValue` | 直接获取值 |

### ✅ 支持的功能
- 实时文本更新
- Text Run 引用
- 双向绑定

### 🎯 使用场景
- 用户名显示
- 动态数据展示
- 实时更新文本

### 💻 示例代码

```kotlin
// 方式 1：通过引用
val textRun = animationView.controller.activeArtboard?.textRun("name")
textRun?.text = "New Text"

// 方式 2：直接设置
animationView.setTextRunValue("name", "New Text")

// 获取当前值
val currentText = animationView.getTextRunValue("name")
```

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

## 27. StressTestActivity

### 📝 描述
压力测试，绘制大量动画实例测试性能。

### 🔧 使用的 API

| API | 说明 |
|-----|------|
| `RiveTextureView` | 底层渲染 |
| `Renderer.draw()` | 多次绘制 |
| `translate()` | 变换位置 |

### ✅ 支持的功能
- 多实例渲染
- FPS 监控
- 点击增加实例

### 🎯 使用场景
- 性能测试
- 渲染能力评估
- 优化验证

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

## 32. TouchPassthroughActivity

### 📝 描述
控制 Rive 动画是否允许触摸事件穿透到下层 View。

### 🔧 使用的 API

| API | 说明 |
|-----|------|
| `touchPassThrough` | 触摸穿透开关 |

### ✅ 支持的功能
- 触摸穿透控制
- 动态开关

### 🎯 使用场景
- 覆盖层动画
- 装饰性动画
- 不需要交互的动画

### 💻 示例代码

```kotlin
riveView.touchPassThrough = true  // 允许穿透
riveView.touchPassThrough = false // 拦截触摸
```

---

## 33. ImageBindingActivity

### 📝 描述
运行时动态绑定图片到 Rive 动画。

### 🔧 使用的 API

| API | 说明 |
|-----|------|
| `RiveRenderImage.fromBitmap()` | 从 Bitmap 创建 |
| `RiveRenderImage.fromEncoded()` | 从编码字节创建 |
| `RiveRenderImage.fromARGBInts()` | 从 ARGB 整数创建 |
| `RiveRenderImage.fromRGBABytes()` | 从 RGBA 字节创建 |
| `ImageAsset.image` | 设置图片资源 |
| `ImageAsset.decode()` | 解码图片 |
| `viewModelInstance.getImageProperty()` | 获取图片属性 |

### ✅ 支持的功能
- 多种图片格式支持
- Bitmap 直接绑定
- 编码字节绑定
- 原始像素绑定
- 预乘 Alpha 支持

### 🎯 使用场景
- 用户头像
- 动态图片内容
- 相册动画

### 💻 示例代码

```kotlin
// 从 Bitmap
val bitmap = BitmapFactory.decodeResource(resources, R.raw.image)
val renderImage = RiveRenderImage.fromBitmap(bitmap, RendererType.Rive)

// 从编码字节
val bytes = resources.openRawResource(R.raw.image).readBytes()
val renderImage = RiveRenderImage.fromEncoded(bytes, RendererType.Rive)

// 绑定到 ViewModel
val imageProp = stateMachine.viewModelInstance?.getImageProperty("Image")
imageProp?.set(renderImage)
renderImage.release()
```

---

## Rive 文件信息查询 API

在开发过程中，了解 .riv 文件的内容非常有用。以下是所有可用于查询 Rive 文件信息的 API。

### 📋 完整信息打印函数

```kotlin
/**
 * 打印 Rive 文件的完整信息
 * 包括：Artboard、动画、状态机、输入、Text Run 等
 */
fun printRiveFileInfo(riveView: RiveAnimationView) {
    val file = riveView.controller.file ?: run {
        Log.w("RiveInfo", "File not loaded")
        return
    }
    
    Log.d("RiveInfo", "========== Rive File Info ==========")
    
    // 1. Artboard 信息
    Log.d("RiveInfo", "Artboard count: ${file.artboardCount}")
    Log.d("RiveInfo", "Artboard names: ${file.artboardNames}")
    
    // 2. 枚举信息
    Log.d("RiveInfo", "Enums: ${file.enums.map { "${it.name}: ${it.values}" }}")
    
    // 3. ViewModel 信息
    Log.d("RiveInfo", "ViewModel count: ${file.viewModelCount}")
    
    // 4. 遍历每个 Artboard
    file.artboardNames.forEachIndexed { index, artboardName ->
        Log.d("RiveInfo", "")
        Log.d("RiveInfo", "--- Artboard [$index]: $artboardName ---")
        
        try {
            val artboard = file.artboard(artboardName)
            
            // Artboard 尺寸
            Log.d("RiveInfo", "  Size: ${artboard.width} x ${artboard.height}")
            Log.d("RiveInfo", "  Bounds: ${artboard.bounds}")
            
            // 动画列表
            Log.d("RiveInfo", "  Animation count: ${artboard.animationCount}")
            Log.d("RiveInfo", "  Animations: ${artboard.animationNames}")
            
            // 状态机列表
            Log.d("RiveInfo", "  State Machine count: ${artboard.stateMachineCount}")
            Log.d("RiveInfo", "  State Machines: ${artboard.stateMachineNames}")
            
            // 遍历每个状态机
            artboard.stateMachineNames.forEach { smName ->
                try {
                    val sm = artboard.stateMachine(smName)
                    Log.d("RiveInfo", "")
                    Log.d("RiveInfo", "    [State Machine: $smName]")
                    Log.d("RiveInfo", "    Layer count: ${sm.layerCount}")
                    Log.d("RiveInfo", "    Input count: ${sm.inputCount}")
                    Log.d("RiveInfo", "    Input names: ${sm.inputNames}")
                    
                    // 遍历每个输入
                    sm.inputs.forEach { input ->
                        val type = when {
                            input.isBoolean -> "Boolean"
                            input.isTrigger -> "Trigger"
                            input.isNumber -> "Number"
                            else -> "Unknown"
                        }
                        val value = when (input) {
                            is SMIBoolean -> input.value.toString()
                            is SMINumber -> input.value.toString()
                            is SMITrigger -> "(trigger)"
                            else -> ""
                        }
                        Log.d("RiveInfo", "      Input: ${input.name} ($type) = $value")
                    }
                    
                    sm.release()
                } catch (e: Exception) {
                    Log.w("RiveInfo", "    Failed to load state machine $smName: ${e.message}")
                }
            }
            
            artboard.release()
        } catch (e: Exception) {
            Log.w("RiveInfo", "  Failed to load artboard $artboardName: ${e.message}")
        }
    }
    
    // 5. 当前活动的 Artboard 信息
    riveView.controller.activeArtboard?.let { activeArtboard ->
        Log.d("RiveInfo", "")
        Log.d("RiveInfo", "--- Active Artboard ---")
        Log.d("RiveInfo", "Name: ${activeArtboard.name}")
        
        // 尝试获取 Text Run（需要知道名称才能获取）
        // 注意：Rive API 没有提供获取所有 Text Run 名称的方法
    }
    
    // 6. 当前状态机信息
    riveView.stateMachines.forEach { sm ->
        Log.d("RiveInfo", "")
        Log.d("RiveInfo", "--- Active State Machine: ${sm.name} ---")
        Log.d("RiveInfo", "Inputs: ${sm.inputNames}")
    }
    
    Log.d("RiveInfo", "========== End of Info ==========")
}
```

### 📊 可查询的信息表

#### File 级别

| API | 返回类型 | 说明 |
|-----|---------|------|
| `file.artboardCount` | `Int` | Artboard 数量 |
| `file.artboardNames` | `List<String>` | 所有 Artboard 名称 |
| `file.enums` | `List<Enum>` | 枚举定义列表 |
| `file.viewModelCount` | `Int` | ViewModel 数量 |
| `file.artboard(name)` | `Artboard` | 按名称获取 Artboard |
| `file.artboard(index)` | `Artboard` | 按索引获取 Artboard |
| `file.firstArtboard` | `Artboard` | 默认 Artboard |

#### Artboard 级别

| API | 返回类型 | 说明 |
|-----|---------|------|
| `artboard.name` | `String` | Artboard 名称 |
| `artboard.width` / `height` | `Float` | 尺寸 |
| `artboard.bounds` | `RectF` | 边界矩形 |
| `artboard.animationCount` | `Int` | 动画数量 |
| `artboard.animationNames` | `List<String>` | 所有动画名称 |
| `artboard.stateMachineCount` | `Int` | 状态机数量 |
| `artboard.stateMachineNames` | `List<String>` | 所有状态机名称 |
| `artboard.animation(name)` | `LinearAnimationInstance` | 获取动画 |
| `artboard.stateMachine(name)` | `StateMachineInstance` | 获取状态机 |
| `artboard.textRun(name)` | `RiveTextValueRun` | 获取 Text Run |
| `artboard.getTextRunValue(name)` | `String?` | 获取 Text Run 值 |

#### StateMachineInstance 级别

| API | 返回类型 | 说明 |
|-----|---------|------|
| `sm.name` | `String` | 状态机名称 |
| `sm.layerCount` | `Int` | 层数 |
| `sm.inputCount` | `Int` | 输入数量 |
| `sm.inputNames` | `List<String>` | 所有输入名称 |
| `sm.inputs` | `List<SMIInput>` | 所有输入 |
| `sm.input(name)` | `SMIInput` | 按名称获取输入 |
| `sm.input(index)` | `SMIInput` | 按索引获取输入 |
| `sm.statesChanged` | `List<LayerState>` | 上次 advance 改变的状态 |
| `sm.eventsReported` | `List<RiveEvent>` | 上次 advance 触发的事件 |

#### SMIInput（输入）级别

| API | 返回类型 | 说明 |
|-----|---------|------|
| `input.name` | `String` | 输入名称 |
| `input.isBoolean` | `Boolean` | 是否是 Boolean 类型 |
| `input.isNumber` | `Boolean` | 是否是 Number 类型 |
| `input.isTrigger` | `Boolean` | 是否是 Trigger 类型 |
| `(input as SMIBoolean).value` | `Boolean` | Boolean 值 |
| `(input as SMINumber).value` | `Float` | Number 值 |

### 📝 简化版打印函数

```kotlin
/**
 * 快速打印 Rive 文件基本信息
 */
fun printRiveBasicInfo(riveView: RiveAnimationView) {
    val file = riveView.controller.file ?: return
    
    Log.d("Rive", "=== Rive File ===")
    Log.d("Rive", "Artboards: ${file.artboardNames}")
    
    file.artboardNames.forEach { name ->
        val artboard = file.artboard(name)
        Log.d("Rive", "[$name] Animations: ${artboard.animationNames}")
        Log.d("Rive", "[$name] StateMachines: ${artboard.stateMachineNames}")
        
        artboard.stateMachineNames.forEach { smName ->
            val sm = artboard.stateMachine(smName)
            Log.d("Rive", "  [$smName] Inputs: ${sm.inputs.map { 
                "${it.name}(${when { 
                    it.isBoolean -> "Bool" 
                    it.isNumber -> "Num" 
                    it.isTrigger -> "Trigger" 
                    else -> "?" 
                }})" 
            }}")
            sm.release()
        }
        artboard.release()
    }
}
```

### ⚠️ 注意事项

1. **Text Run 名称无法枚举**
   - Rive API **没有提供**获取所有 Text Run 名称的方法
   - 需要从设计师处获取 Text Run 名称列表
   - 只能通过 `artboard.textRun(name)` 按名称获取

2. **事件信息无法预先获取**
   - 事件只有在触发时才能获取
   - 需要从设计师处获取事件名称和属性列表

3. **资源释放**
   - 使用 `file.artboard()` 或 `artboard.stateMachine()` 获取的对象需要手动 `release()`
   - 或者确保它们被 `dependencies` 管理

### 💡 开发建议

建议让设计师提供以下信息清单：

```markdown
## Rive 文件清单

### 基本信息
- 文件名: onboarding.riv
- 默认 Artboard: Main

### Text Runs（动态文本）
| 名称 | 初始值 | 说明 |
|------|--------|------|
| title | "Hello" | 标题文本 |
| content | "" | 内容文本 |

### Events（事件）
| 名称 | 类型 | 属性 | 说明 |
|------|------|------|------|
| onClick | General | action: String | 点击事件 |
| openUrl | OpenURL | url: String | 打开链接 |

### State Machine Inputs
| 状态机 | 输入名 | 类型 | 说明 |
|--------|--------|------|------|
| Main | progress | Number | 进度值 0-100 |
| Main | isActive | Boolean | 是否激活 |
| Main | submit | Trigger | 提交触发器 |
```

---

## API 速查表

### 核心类

| 类名 | 用途 |
|------|------|
| `RiveAnimationView` | 主要的动画视图组件 |
| `RiveTextureView` | 底层渲染视图 |
| `RiveFileController` | 文件和动画控制器 |
| `File` | Rive 文件对象 |
| `Artboard` | Artboard 对象 |
| `StateMachineInstance` | 状态机实例 |
| `LinearAnimationInstance` | 线性动画实例 |

### 常用方法

| 方法 | 用途 |
|------|------|
| `play()` / `pause()` / `stop()` | 播放控制 |
| `reset()` | 重置动画 |
| `setNumberState()` | 设置 Number 输入 |
| `setBooleanState()` | 设置 Boolean 输入 |
| `fireState()` | 触发 Trigger |
| `setTextRunValue()` | 设置 Text Run |
| `setRiveResource()` | 设置资源 |
| `setRiveBytes()` | 从字节加载 |
| `setRiveFile()` | 使用共享文件 |

### 枚举值

| 枚举 | 值 |
|------|-----|
| `Fit` | CONTAIN, COVER, FILL, FIT_WIDTH, FIT_HEIGHT, NONE, SCALE_DOWN, LAYOUT |
| `Alignment` | TOP_LEFT, TOP_CENTER, TOP_RIGHT, CENTER_LEFT, CENTER, CENTER_RIGHT, BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT |
| `Loop` | AUTO, LOOP, ONESHOT, PINGPONG |
| `Direction` | AUTO, FORWARDS, BACKWARDS |

---

*文档生成时间：2024-12*

