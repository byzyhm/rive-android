# Rive Android 绘制机制详解

## 📚 目录

1. [核心概念](#核心概念)
2. [坐标系统](#坐标系统)
3. [Fit 模式详解](#fit-模式详解)
4. [Alignment 对齐方式](#alignment-对齐方式)
5. [坐标转换与系数计算](#坐标转换与系数计算)
6. [常见问题与陷阱](#常见问题与陷阱)
7. [最佳实践](#最佳实践)

---

## 核心概念

### 1. RiveAnimationView

`RiveAnimationView` 是 Android 中用于显示 Rive 动画的自定义 View。

**关键特性：**
- 继承自 Android View，遵循 Android View 的布局规则
- 尺寸由 Android LayoutParams 控制（dp/px）
- 负责将 Artboard 内容渲染到屏幕上

```kotlin
val riveView = binding.aiGlowRiv
// View 的屏幕像素尺寸
val viewWidth = riveView.width   // 例如: 1264px
val viewHeight = riveView.height // 例如: 2595px
```

### 2. Artboard

`Artboard` 是 Rive 文件中的画板，包含动画内容的原始定义。

**关键特性：**
- 有固定的设计尺寸（设计时定义）
- 独立于设备屏幕的逻辑坐标系
- 所有动画元素都在 Artboard 坐标系中定义

```kotlin
val artboard = riveView.controller.activeArtboard
val artboardBounds = artboard.bounds
val artboardWidth = artboardBounds.width()   // 例如: 500.0
val artboardHeight = artboardBounds.height() // 例如: 1000.0
```

**示例：**
```
Artboard 设计尺寸: 500 × 1000
- 这是在 Rive 编辑器中设计时的尺寸
- 所有元素的位置、大小都基于这个尺寸
- 与最终在手机上显示的像素尺寸无关
```

### 3. 状态机变量

状态机（StateMachine）中的变量是**基于 Artboard 坐标系**的。

```kotlin
// 状态机变量范围
width: 125 - 474   // Artboard 坐标系单位
height: 120 - 973  // Artboard 坐标系单位
```

**重要理解：**
- 这些数值**不是像素**，是 Artboard 坐标系中的单位
- 需要通过系数转换才能对应到屏幕像素

---

## 坐标系统

### 三层坐标系

```
┌─────────────────────────────────────────────┐
│ 1. Android 屏幕坐标系 (像素 px)              │
│    - 设备相关                                │
│    - 例如: 1264 × 2595 px                    │
│                                               │
│  ┌───────────────────────────────────────┐  │
│  │ 2. RiveAnimationView 坐标系 (像素 px) │  │
│  │    - Android View 的尺寸               │  │
│  │    - 由 LayoutParams 控制              │  │
│  │                                         │  │
│  │  ┌─────────────────────────────────┐  │  │
│  │  │ 3. Artboard 坐标系 (逻辑单位)  │  │  │
│  │  │    - 设计时定义，设备无关       │  │  │
│  │  │    - 例如: 500 × 1000           │  │  │
│  │  │    - 状态机变量基于此坐标系     │  │  │
│  │  └─────────────────────────────────┘  │  │
│  └───────────────────────────────────────┘  │
└─────────────────────────────────────────────┘
```

### 坐标系关系

```
屏幕像素 ←→ View 像素 ←→ Artboard 坐标
   (dp/px)     (px)      (逻辑单位)
```

---

## Fit 模式详解

`Fit` 控制 Artboard 如何适配到 RiveAnimationView 的尺寸。

### Fit.CONTAIN（最常用）

**行为：**
- Artboard **完整显示**在 View 内
- 保持 Artboard 的宽高比
- 可能会有留白（letterbox 或 pillarbox）

**示例：**

```kotlin
riveView.fit = Fit.CONTAIN
```

```
┌─────────────────────────┐
│  RiveAnimationView      │
│  ┏━━━━━━━━━━━━━━━━━┓   │ ← 上方留白
│  ┃                 ┃   │
│  ┃   Artboard      ┃   │
│  ┃   (完整显示)    ┃   │
│  ┃                 ┃   │
│  ┗━━━━━━━━━━━━━━━━━┛   │ ← 下方留白
└─────────────────────────┘
```

**计算规则：**
```kotlin
val artboardRatio = artboardWidth / artboardHeight  // 例如: 500/1000 = 0.5
val viewRatio = viewWidth / viewHeight               // 例如: 1264/2595 = 0.487

if (artboardRatio > viewRatio) {
    // Artboard 更宽，左右填满，上下留白
    actualWidth = viewWidth
    actualHeight = viewWidth / artboardRatio
} else {
    // Artboard 更高，上下填满，左右留白
    actualHeight = viewHeight
    actualWidth = viewHeight * artboardRatio
}
```

### Fit.FILL

**行为：**
- Artboard **填满整个 View**
- 不保持宽高比，可能会变形

```
┌─────────────────────────┐
│  RiveAnimationView      │
│ ┏━━━━━━━━━━━━━━━━━━━━┓ │
│ ┃   Artboard         ┃ │
│ ┃   (拉伸填满)       ┃ │
│ ┃                    ┃ │
│ ┗━━━━━━━━━━━━━━━━━━━━┛ │
└─────────────────────────┘
```

### Fit.COVER

**行为：**
- Artboard **填满 View**，保持宽高比
- 可能会裁剪部分内容

```
┌─────────────────────────┐
│ ┏━━━━━━━━━━━━━━━━━━━━┓ │ ← 裁剪上方
│ ┃                    ┃ │
│ ┃   Artboard (裁剪)  ┃ │
│ ┃                    ┃ │
│ ┗━━━━━━━━━━━━━━━━━━━━┛ │ ← 裁剪下方
└─────────────────────────┘
```

### 其他 Fit 模式

- `Fit.FIT_WIDTH`: 宽度填满，高度自适应
- `Fit.FIT_HEIGHT`: 高度填满，宽度自适应
- `Fit.SCALE_DOWN`: 类似 CONTAIN，但不会放大
- `Fit.NONE`: 不缩放，使用原始尺寸

---

## Alignment 对齐方式

`Alignment` 控制 Artboard 在 View 中的位置（当有留白时）。

### 常用对齐方式

```kotlin
riveView.alignment = Alignment.TOP_LEFT
```

```
┌─────────────────────────┐
│ ┏━━━━━━━┓              │  TOP_LEFT
│ ┃ Art   ┃              │
│ ┗━━━━━━━┛              │
│                         │
└─────────────────────────┘

┌─────────────────────────┐
│      ┏━━━━━━━┓          │  TOP_CENTER
│      ┃ Art   ┃          │
│      ┗━━━━━━━┛          │
│                         │
└─────────────────────────┘

┌─────────────────────────┐
│                         │  CENTER
│      ┏━━━━━━━┓          │
│      ┃ Art   ┃          │
│      ┗━━━━━━━┛          │
└─────────────────────────┘
```

### Alignment 枚举值

```kotlin
Alignment.TOP_LEFT       Alignment.TOP_CENTER       Alignment.TOP_RIGHT
Alignment.CENTER_LEFT    Alignment.CENTER           Alignment.CENTER_RIGHT
Alignment.BOTTOM_LEFT    Alignment.BOTTOM_CENTER    Alignment.BOTTOM_RIGHT
```

**使用场景：**
- `TOP_LEFT`: 内容从左上角开始，适合聊天气泡背景
- `CENTER`: 居中显示，适合图标、插图
- `BOTTOM_CENTER`: 底部居中，适合底部装饰

---

## 坐标转换与系数计算

### 核心公式

```kotlin
// 变化系数：Artboard 坐标系 → 屏幕像素的转换比例
val coefficient = artboardWidth / viewWidth.toFloat()

// 屏幕像素 → Artboard 坐标系
val artboardValue = screenPixels × coefficient

// Artboard 坐标系 → 屏幕像素
val screenPixels = artboardValue / coefficient
```

### 实际案例

**场景：** 根据 TextView 高度动态设置状态机的 height 值

```kotlin
// 已知数据
val artboardWidth = 500f        // Artboard 宽度
val artboardHeight = 1000f      // Artboard 高度
val viewWidth = 1264            // RiveAnimationView 宽度 (px)
val viewHeight = 2595           // RiveAnimationView 高度 (px)
val contentVHeight = 1051       // TextView 高度 (px)

// 计算变化系数
val coefficient = artboardWidth / viewWidth
// coefficient = 500 / 1264 = 0.3956

// 将 TextView 高度转换为 Artboard 坐标系
val stateMachineHeight = contentVHeight × coefficient
// stateMachineHeight = 1051 × 0.3956 = 415.8

// 设置状态机变量
controller.setNumberState("StateMachine_1", "height", stateMachineHeight)
```

### 为什么用 Artboard 宽度计算系数？

**关键理解：**

```
RiveAnimationView 宽度 = 屏幕宽度 (1264px)
Artboard 宽度 = 500 (逻辑单位)

比例关系：
1264px (屏幕) ←→ 500 (Artboard)
1px (屏幕) ←→ 0.3956 (Artboard)

所以：
系数 = Artboard宽度 / 屏幕宽度 = 500 / 1264 = 0.3956
```

**错误理解（坑）：**
❌ 使用状态机变量的最大值 474

```kotlin
// 错误！
val coefficient = 474 / screenWidth  // 474 是状态机变量范围，不是 Artboard 尺寸
```

**为什么错误？**
- 474 是状态机变量 `width` 的最大值
- 不是 Artboard 的物理宽度（500）
- 会导致 5% 的误差：`(500 - 474) / 500 = 5.2%`

---

## 常见问题与陷阱

### ❌ 陷阱 1：混淆 Artboard 尺寸和状态机变量

```kotlin
// 错误示例
val artboardWidth = 474f  // ❌ 这是状态机变量的最大值，不是 Artboard 宽度

// 正确做法
val artboard = riveView.controller.activeArtboard
val artboardWidth = artboard.bounds.width()  // ✅ 获取真实的 Artboard 宽度
```

**日志验证：**
```
Artboard 尺寸: 500.0 × 1000.0        ← Artboard 真实尺寸
状态机变量范围:
  width: 125 - 474                    ← 状态机变量范围（≠ Artboard 宽度）
  height: 120 - 973
```

### ❌ 陷阱 2：margin 计算错误

```kotlin
// 错误：将 Android 布局的 margin 加到状态机计算中
val marginInPx = (20 * density).toInt()
val stateMachineHeight = (contentVHeight + marginInPx) × coefficient  // ❌

// 正确：margin 是布局层概念，不影响动画内容高度
val stateMachineHeight = contentVHeight × coefficient  // ✅
```

**理解：**
- `android:layout_marginTop="10dp"` 是 Android View 的布局间距
- 状态机变量控制的是 **Artboard 内部绘制内容的范围**
- 两者是不同层面的概念，不应混合计算

### ❌ 陷阱 3：Fit 模式导致的实际渲染尺寸误解

```kotlin
// 使用 Fit.CONTAIN 时
riveView.width = 1264px    // View 的尺寸
riveView.height = 2595px

// 但 Artboard 实际渲染尺寸可能不同（有留白）
// 需要考虑 Artboard 的宽高比
```

**计算实际渲染尺寸：**
```kotlin
val artboardRatio = artboardWidth / artboardHeight  // 0.5
val viewRatio = viewWidth / viewHeight               // 0.487

// Artboard 更宽，上下有留白
val actualRenderWidth = viewWidth                    // 1264px
val actualRenderHeight = viewWidth / artboardRatio   // 1264 / 0.5 = 2528px

// 留白
val topPadding = (viewHeight - actualRenderHeight) / 2  // (2595 - 2528) / 2 = 33.5px
```

### ❌ 陷阱 4：屏幕密度 (density) 的影响

```kotlin
// dp 转 px
val marginPx = (10 * resources.displayMetrics.density).toInt()

// 不同设备的 density
// hdpi:  density = 1.5
// xhdpi: density = 2.0
// xxhdpi: density = 3.0

// 10dp 在不同设备上的实际像素：
// hdpi:  10dp = 15px
// xhdpi: 10dp = 20px
// xxhdpi: 10dp = 30px
```

### ❌ 陷阱 5：ViewTreeObserver 监听时机

```kotlin
// 错误：直接在 onCreate 中获取尺寸
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(binding.root)
    
    val height = binding.contentV.height  // ❌ 可能是 0，因为 View 还未布局
}

// 正确：使用 ViewTreeObserver 或 post
binding.contentV.viewTreeObserver.addOnGlobalLayoutListener {
    val height = binding.contentV.height  // ✅ View 已经完成布局
}

// 或者
binding.contentV.post {
    val height = binding.contentV.height  // ✅
}
```

---

## 最佳实践

### 1. 动态计算系数

```kotlin
/**
 * 获取 Artboard 到屏幕像素的转换系数
 */
private fun getArtboardToPixelCoefficient(): Float {
    val artboard = riveView.controller.activeArtboard ?: return 0f
    val artboardWidth = artboard.bounds.width()
    val viewWidth = riveView.width.toFloat()
    
    return artboardWidth / viewWidth
}

/**
 * 将屏幕像素转换为 Artboard 坐标系
 */
private fun pixelsToArtboard(pixels: Int): Float {
    return pixels * getArtboardToPixelCoefficient()
}

/**
 * 将 Artboard 坐标系转换为屏幕像素
 */
private fun artboardToPixels(artboardValue: Float): Int {
    return (artboardValue / getArtboardToPixelCoefficient()).toInt()
}
```

### 2. 监听高度变化时避免重复计算

```kotlin
private var lastContentHeight: Int = -1

private fun setupHeightListener() {
    contentView.viewTreeObserver.addOnGlobalLayoutListener {
        val currentHeight = contentView.height
        
        // 只有真正变化时才执行
        if (currentHeight > 0 && currentHeight != lastContentHeight) {
            lastContentHeight = currentHeight
            updateStateMachine(currentHeight)
        }
    }
}
```

### 3. 确保 Artboard 已加载

```kotlin
private fun updateStateMachine(height: Int) {
    val artboard = riveView.controller.activeArtboard
    if (artboard == null) {
        Log.w(TAG, "Artboard not loaded yet")
        return
    }
    
    val coefficient = artboard.bounds.width() / riveView.width.toFloat()
    val stateMachineHeight = height * coefficient
    
    riveView.controller.setNumberState("StateMachine_1", "height", stateMachineHeight)
}
```

### 4. 日志调试

```kotlin
private fun logRiveInfo() {
    val artboard = riveView.controller.activeArtboard ?: return
    val bounds = artboard.bounds
    
    Log.d(TAG, "═══════════════════════════════════")
    Log.d(TAG, "Artboard 尺寸: ${bounds.width()} × ${bounds.height()}")
    Log.d(TAG, "View 尺寸: ${riveView.width}px × ${riveView.height}px")
    Log.d(TAG, "屏幕密度: ${resources.displayMetrics.density}")
    Log.d(TAG, "屏幕宽度: ${resources.displayMetrics.widthPixels}px")
    Log.d(TAG, "Fit 模式: ${riveView.fit}")
    Log.d(TAG, "Alignment: ${riveView.alignment}")
    
    val coefficient = bounds.width() / riveView.width
    Log.d(TAG, "转换系数: $coefficient")
    Log.d(TAG, "═══════════════════════════════════")
}
```

### 5. 资源清理

```kotlin
private var layoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null

override fun onDestroy() {
    super.onDestroy()
    
    // 移除监听器，避免内存泄漏
    layoutListener?.let {
        contentView.viewTreeObserver.removeOnGlobalLayoutListener(it)
        layoutListener = null
    }
    
    // 停止 Rive 动画
    riveView.stop()
}
```

### 6. 完整示例

```kotlin
class RiveBackgroundActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityRiveBackgroundBinding
    private var lastContentHeight: Int = -1
    private var layoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRiveBackgroundBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupRiveView()
        
        // 等待 Rive 加载完成
        binding.riveView.post {
            setupContentHeightListener()
        }
    }
    
    private fun setupRiveView() {
        binding.riveView.apply {
            setRiveResource(
                resId = R.raw.background_animation,
                stateMachineName = "StateMachine_1",
                autoplay = true
            )
            fit = Fit.CONTAIN
            alignment = Alignment.TOP_LEFT
        }
    }
    
    private fun setupContentHeightListener() {
        layoutListener = ViewTreeObserver.OnGlobalLayoutListener {
            val contentHeight = binding.contentView.height
            
            if (contentHeight > 0 && contentHeight != lastContentHeight) {
                lastContentHeight = contentHeight
                updateRiveHeight(contentHeight)
            }
        }
        
        binding.contentView.viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
    }
    
    private fun updateRiveHeight(contentHeightPx: Int) {
        val artboard = binding.riveView.controller.activeArtboard ?: return
        
        // 计算转换系数
        val coefficient = artboard.bounds.width() / binding.riveView.width.toFloat()
        
        // 转换为 Artboard 坐标系
        val artboardHeight = contentHeightPx * coefficient
        
        // 限制在合理范围内（根据实际状态机变量范围设置）
        val clampedHeight = artboardHeight.coerceIn(120f, 973f)
        
        // 更新状态机
        binding.riveView.controller.setNumberState(
            "StateMachine_1", 
            "height", 
            clampedHeight
        )
        
        Log.d(TAG, "Content: ${contentHeightPx}px → Artboard: ${clampedHeight.toInt()}")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        layoutListener?.let {
            binding.contentView.viewTreeObserver.removeOnGlobalLayoutListener(it)
        }
        binding.riveView.stop()
    }
    
    companion object {
        private const val TAG = "RiveBackgroundActivity"
    }
}
```

---

## 总结

### 核心要点

1. **三层坐标系**：屏幕像素 → View 像素 → Artboard 坐标系
2. **Fit.CONTAIN**：保持 Artboard 宽高比，完整显示
3. **系数计算**：`coefficient = artboardWidth / viewWidth`
4. **坐标转换**：`artboardValue = screenPixels × coefficient`
5. **margin 不参与**：状态机变量只关心内容高度，不包含 Android 布局的 margin

### 调试检查清单

- [ ] Artboard 尺寸是否正确获取？
- [ ] View 尺寸是否已经完成布局？（不是 0）
- [ ] 系数计算是否使用 Artboard 真实宽度？（不是状态机变量）
- [ ] 是否错误地将 margin 加入计算？
- [ ] Fit 和 Alignment 设置是否符合预期？
- [ ] 状态机变量范围是否设置正确？
- [ ] 监听器是否在 onDestroy 中清理？

### 参考资源

- [Rive 官方文档](https://help.rive.app/)
- [Rive Android Runtime](https://github.com/rive-app/rive-android)
- [Rive Community Forum](https://rive.app/community/)

---

**文档版本：** 1.0  
**更新日期：** 2026-02-06  
**适用版本：** Rive Android Runtime 9.x+
