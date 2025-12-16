# Rive Android 交互与控制

本文档介绍 Rive Android 的交互式动画和播放控制。

> 📚 返回 [Demo Activities 完整指南](./demo_activities_guide.md)

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

*返回 [Demo Activities 完整指南](./demo_activities_guide.md)*

