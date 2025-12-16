# Rive Android 基础示例

本文档介绍 Rive Android 的基础使用示例。

> 📚 返回 [Demo Activities 完整指南](./demo_activities_guide.md)

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
```

### 📝 设计师配置的内容

| 设置项 | 示例值 | 说明 |
|--------|--------|------|
| **Event Name** | `Star Rating` | 事件名称（开发者用来区分事件） |
| **Event Type** | `GeneralEvent` / `OpenURLEvent` | 事件类型 |
| **Delay** | `0.0` | 触发延迟（秒） |
| **Properties** | `{rating: 4.0}` | 自定义属性（Key-Value） |
| **URL** | `https://rive.app` | OpenURL 事件的 URL |

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
                Log.i("RiveEvent", "properties: ${event.properties}")
                
                // 根据设计师提供的属性名获取值
                val rating = event.properties["rating"] as? Double
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

---

*返回 [Demo Activities 完整指南](./demo_activities_guide.md)*

