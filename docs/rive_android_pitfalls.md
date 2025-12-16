# Rive Android 开发踩坑指南

本文档总结了在 Android 中使用 Rive 动画时常见的问题和解决方案。

## 目录

1. [isPlaying 属性的误解](#1-isplaying-属性的误解)
2. [setNumberState 不起作用](#2-setnumberstate-不起作用)
3. [reset() 播放异常](#3-reset-播放异常)
4. [play() 指定状态机](#4-play-指定状态机)
5. [线性动画 vs 状态机](#5-线性动画-vs-状态机)
6. [setTextRunValue vs setNumberState](#6-settextrunvalue-vs-setnumberstate)
7. [循环播放的正确方式](#7-循环播放的正确方式)
8. [引用计数与内存泄漏](#8-引用计数与内存泄漏)

---

## 1. isPlaying 属性的误解

### 问题

```kotlin
// ❌ 错误理解
if (!riveView.isPlaying) {
    riveView.play()  // 以为动画没播放，需要启动
}
```

很多开发者以为 `isPlaying` 表示"动画是否在播放"，实际上不是。

### 真相

`isPlaying` 表示的是**渲染循环是否在运行**，而不是"动画是否在播放"。

```kotlin
// 源码：RiveAnimationView.kt
val isPlaying: Boolean
    get() = renderer?.isPlaying == true

// 源码：Renderer.kt
var isPlaying: Boolean = false  // 在 start() 中设为 true，在 stop() 中设为 false
```

| 场景 | isPlaying 值 | 说明 |
|------|-------------|------|
| RiveAnimationView 加载后 | `true` | 渲染循环已启动 |
| 调用 pause() 后 | `false` | 渲染循环停止 |
| 状态机播放完成 | `true` | 渲染循环仍在运行 |

### 正确做法

不需要检查 `isPlaying` 来决定是否调用 `play()`。状态机通过设置输入值来控制状态转换。

---

## 2. setNumberState 不起作用

### 问题场景

```kotlin
// ❌ 场景1：状态机未启动
riveView.reset()
riveView.setNumberState("StateMachine_1", "state", 1f)  // 不起作用！

// ❌ 场景2：想回到初始状态
riveView.setNumberState("StateMachine_1", "state", 0f)  // 动画不会重播！
```

### 原因分析

#### 原因1：状态机未启动

`setNumberState` 需要状态机实例存在才能生效。

```
setNumberState() → queueInput() → processAllInputs() → getOrCreateStateMachines()
                                                        ↓
                                              需要 activeArtboard 存在
```

#### 原因2：状态机转换是条件驱动的

状态机的转换基于**条件**，不是基于**值**：

```
状态机转换设计（通常是单向的）：
Entry → [state >= 1] → State1 → [state >= 2] → State2 → [state >= 3] → State3

当已经在 State3 时，设置 state=0：
├── ✓ 值确实变成了 0
└── ✗ 但没有定义"回到 Entry"的转换条件，所以不会回到初始状态
```

### 正确做法

```kotlin
// ✅ 正确顺序：先启动状态机，再设置值
riveView.play("StateMachine_1", isStateMachine = true)
riveView.setNumberState("StateMachine_1", "state", 1f)

// ✅ 要回到初始状态：重新加载文件
riveView.setRiveResource(R.raw.animation, stateMachineName = "StateMachine_1")
```

---

## 3. reset() 播放异常

### 问题场景

```kotlin
// ❌ reset 后动画播放异常
riveView.reset()
// 第三个消息气泡直接显示，没有弹出动画
```

### 原因分析

`reset()` 的内部流程：

```kotlin
fun reset() {
    controller.stopAnimations()
    controller.reset()           // 清空状态
    stop()
    controller.selectArtboard()  // 创建新的 Artboard 实例！
    start()
}
```

问题：
1. **Text Run 值丢失**：新 Artboard 使用 Rive 文件中的默认值
2. **状态机被清空**：需要重新启动
3. **Artboard 状态不对**：新 Artboard 的初始状态可能与文件首次加载时不同

### reset() vs setRiveResource() 对比

| 方法 | 行为 | 适用场景 |
|------|------|---------|
| `reset()` | 创建新 Artboard，状态可能不对 | 简单重置（无状态机） |
| `setRiveResource()` | 完全重新加载文件，状态和首次加载一致 | 需要完全重置的循环播放 |

### 正确做法

```kotlin
// ❌ 使用 reset()（可能有问题）
riveView.reset()
riveView.play("StateMachine_1", isStateMachine = true)
setupTextContent()
setupLengthInputs()

// ✅ 使用 setRiveResource()（推荐）
riveView.setRiveResource(
    resId = R.raw.animation,
    stateMachineName = "StateMachine_1",
    autoplay = true
)
setupTextContent()
setupLengthInputs()
```

---

## 4. play() 指定状态机

### 问题场景

```kotlin
// ❌ 错误：没有指定 isStateMachine
riveView.play("StateMachine_1")  // 会被当作线性动画名称！
```

### 原因分析

`play()` 方法的 `animationName` 参数可以是：
- 线性动画名称（默认）
- 状态机名称（需要设置 `isStateMachine = true`）

```kotlin
fun play(
    animationName: String,
    loop: Loop = Loop.AUTO,
    direction: Direction = Direction.AUTO,
    isStateMachine: Boolean = false,  // 默认是 false！
    settleInitialState: Boolean = true,
)
```

### 正确做法

```kotlin
// ✅ 播放线性动画
riveView.play("walk")

// ✅ 播放状态机
riveView.play("StateMachine_1", isStateMachine = true)

// ✅ 或者在 XML 中指定
// app:riveStateMachine="StateMachine_1"
```

---

## 5. 线性动画 vs 状态机

### 概念对比

| 特性 | 线性动画 (Animation) | 状态机 (State Machine) |
|------|---------------------|----------------------|
| 控制方式 | 时间轴驱动 | 输入驱动 |
| 播放方式 | play/pause/stop | 设置输入值触发转换 |
| 循环 | Loop.LOOP | 由状态机设计决定 |
| 交互 | 无 | 支持触摸、输入等交互 |

### Rive 文件结构

```
Rive 文件
├── Artboard
│   ├── 图形元素
│   ├── Text Run (文本)
│   │
│   ├── Animations (线性动画)
│   │   ├── "idle"
│   │   ├── "walk"
│   │   └── "run"
│   │
│   └── State Machines (状态机)
│       ├── "StateMachine_1"
│       │   └── Inputs (输入)
│       │       ├── Number: "state"
│       │       ├── Boolean: "isActive"
│       │       └── Trigger: "onClick"
│       └── "MainStateMachine"
```

### 使用方式

```kotlin
// 线性动画
riveView.play("walk", loop = Loop.LOOP)
riveView.pause("walk")
riveView.stop("walk")

// 状态机
riveView.play("StateMachine_1", isStateMachine = true)
riveView.setNumberState("StateMachine_1", "state", 1f)
riveView.setBooleanState("StateMachine_1", "isActive", true)
riveView.fireState("StateMachine_1", "onClick")
```

---

## 6. setTextRunValue vs setNumberState

### 关键区别

| 方法 | 作用 | 前提条件 | 是否触发动画 |
|------|------|---------|------------|
| `setTextRunValue` | 修改 Artboard 上的文本 | 无需状态机 | 否 |
| `setNumberState` | 修改状态机的输入值 | 状态机必须已启动 | 取决于转换条件 |

### 代码示例

```kotlin
// setTextRunValue：随时可用
riveView.setTextRunValue("Content_1", "Hello World")  // 立即生效

// setNumberState：需要状态机已启动
riveView.play("StateMachine_1", isStateMachine = true)
riveView.setNumberState("StateMachine_1", "length_1", 200f)
```

### 设置顺序建议

```kotlin
// ✅ 推荐顺序
fun setupAnimation() {
    // 1. 设置 Text Run（不需要状态机）
    riveView.setTextRunValue("Content_1", "Hello")
    riveView.setTextRunValue("Content_2", "World")
    
    // 2. 启动状态机
    riveView.play("StateMachine_1", isStateMachine = true)
    
    // 3. 设置 Number 输入（状态机已启动）
    riveView.setNumberState("StateMachine_1", "length_1", 200f)
}
```

---

## 7. 循环播放的正确方式

### 问题场景

需要实现动画循环播放（播放完成后自动重新开始）。

### 错误方式

```kotlin
// ❌ 使用 reset()
fun resetAndLoop() {
    riveView.reset()
    riveView.play("StateMachine_1", isStateMachine = true)
    // 可能导致动画异常
}
```

### 正确方式

```kotlin
// ✅ 使用 setRiveResource() 重新加载
fun resetAndLoop() {
    riveView.setRiveResource(
        resId = R.raw.animation,
        stateMachineName = "StateMachine_1",
        autoplay = true
    )
    // 重新设置参数
    setupTextContent()
    setupNumberInputs()
    // 开始动画
    startAnimationSequence()
}
```

### 完整示例

```kotlin
class AnimationActivity : ComponentActivity() {
    
    private val handler = Handler(Looper.getMainLooper())
    
    override fun onResume() {
        super.onResume()
        setupTextContent()
        setupNumberInputs()
        startAnimationLoop()
    }
    
    override fun onPause() {
        super.onPause()
        handler.removeCallbacksAndMessages(null)
    }
    
    private fun startAnimationLoop() {
        // 播放动画序列
        handler.postDelayed({ setStateValue(1f) }, 1500)
        handler.postDelayed({ setStateValue(2f) }, 1700)
        handler.postDelayed({ setStateValue(3f) }, 1900)
        
        // 完成后重新开始
        handler.postDelayed({ resetAndLoop() }, 5000)
    }
    
    private fun resetAndLoop() {
        // 重新加载文件（关键！）
        binding.riveView.setRiveResource(
            resId = R.raw.animation,
            stateMachineName = "StateMachine_1",
            autoplay = true
        )
        setupTextContent()
        setupNumberInputs()
        startAnimationLoop()
    }
}
```

---

## 总结

| 问题 | 原因 | 解决方案 |
|------|------|---------|
| isPlaying 判断无效 | 它表示渲染循环状态，不是动画播放状态 | 不需要检查，直接操作状态机 |
| setNumberState 不起作用 | 状态机未启动 / 没有匹配的转换条件 | 先 play() 再设置 / 重新加载文件 |
| reset() 后动画异常 | 新 Artboard 状态与首次加载不同 | 使用 setRiveResource() |
| play() 没启动状态机 | 没设置 isStateMachine = true | 设置 isStateMachine = true |
| Text Run 值丢失 | reset() 创建了新 Artboard | 重新调用 setTextRunValue() |
| 循环播放异常 | reset() 的问题 | 使用 setRiveResource() |
| 内存泄漏 | artboard()/stateMachine() 创建新实例未释放 | 使用只读属性或手动 release() |

---

## 8. 引用计数与内存泄漏

### 问题场景

```kotlin
// ❌ 调试代码导致内存泄漏
fun printDebugInfo() {
    val file = riveView.controller.file
    
    // 每次调用 artboard() 都会创建新实例！
    val artboard = file?.artboard("MyArtboard")  // 创建实例 1
    Log.d(TAG, "Artboard: ${artboard?.name}")
    
    val artboard2 = file?.firstArtboard  // 创建实例 2
    Log.d(TAG, "First: ${artboard2?.name}")
    
    // 这些实例没有被释放，造成内存泄漏！
}
```

### 原因分析

Rive Android SDK 使用**引用计数**管理 Native 对象的生命周期：

```kotlin
// File.kt 源码
fun artboard(name: String): Artboard {
    val artboardPointer = cppArtboard(cppPointer, name)
    val ab = Artboard(artboardPointer, lock, this)
    dependencies.add(ab)  // 添加到 dependencies 列表
    return ab
}
```

每次调用以下方法都会创建新实例：

| 方法 | 创建的对象 | 自动释放？ |
|------|-----------|-----------|
| `file.artboard(name)` | Artboard | ❌ 需要手动释放或等 File 释放 |
| `file.firstArtboard` | Artboard | ❌ 需要手动释放或等 File 释放 |
| `artboard.stateMachine(name)` | StateMachineInstance | ❌ 需要手动释放或等 Artboard 释放 |
| `artboard.animation(name)` | LinearAnimationInstance | ❌ 需要手动释放或等 Artboard 释放 |

### 引用计数机制

```
┌─────────────────────────────────────────────────────────────────┐
│  NativeObject 引用计数                                           │
│                                                                  │
│  acquire() → refs++                                              │
│  release() → refs--                                              │
│             └── if (refs == 0) → dispose() → cppDelete()        │
│                                                                  │
│  dependencies 机制：                                              │
│  - 父对象持有子对象的引用                                          │
│  - 父对象 release 时，会 release 所有 dependencies               │
│                                                                  │
│  File                                                            │
│   └── dependencies: [Artboard1, Artboard2, ...]                 │
│        └── Artboard.dependencies: [StateMachine1, Animation1]   │
└─────────────────────────────────────────────────────────────────┘
```

### 正确做法

#### 方式1：使用 Controller 管理的实例（推荐）

```kotlin
// ✅ 使用 controller.activeArtboard（由 Controller 管理生命周期）
fun printDebugInfo() {
    val activeArtboard = riveView.controller.activeArtboard
    if (activeArtboard != null) {
        Log.d(TAG, "Artboard: ${activeArtboard.name}")
        Log.d(TAG, "State Machines: ${activeArtboard.stateMachineNames}")  // 只读属性，不创建实例
        Log.d(TAG, "Animations: ${activeArtboard.animationNames}")         // 只读属性，不创建实例
    }
}
```

#### 方式2：手动释放

```kotlin
// ✅ 如果必须创建实例，记得释放
fun createAndRelease() {
    val file = riveView.controller.file ?: return
    val artboard = file.artboard("MyArtboard")
    
    try {
        // 使用 artboard
        Log.d(TAG, "Artboard: ${artboard.name}")
    } finally {
        artboard.release()  // 必须释放！
    }
}
```

#### 方式3：避免重复创建

```kotlin
// ❌ 错误：每次都创建新实例
for (i in 0..10) {
    val artboard = file.artboard("MyArtboard")  // 创建了 11 个实例！
    Log.d(TAG, "Iteration $i: ${artboard.name}")
}

// ✅ 正确：复用实例
val artboard = file.artboard("MyArtboard")
for (i in 0..10) {
    Log.d(TAG, "Iteration $i: ${artboard.name}")
}
artboard.release()
```

### 安全的只读属性

以下属性是安全的，**不会创建新实例**：

```kotlin
// 这些是安全的只读属性
file.artboardCount        // Int
file.artboardNames        // List<String>

artboard.name             // String
artboard.animationNames   // List<String>
artboard.stateMachineNames // List<String>
artboard.bounds           // RectF
```

### RiveAnimationView 的自动管理

使用 `RiveAnimationView` 时，大部分情况下不需要手动管理引用计数：

```kotlin
// RiveAnimationView 会自动管理
riveView.setRiveResource(R.raw.animation)  // 自动管理 File
riveView.play("StateMachine_1", isStateMachine = true)  // 自动管理 StateMachineInstance

// 只有在直接访问底层对象时需要小心
val file = riveView.controller.file  // 这个是由 controller 管理的，不要 release
```

### 调试建议

1. **不要在循环中调用 `artboard()` 或 `stateMachine()`**
2. **使用只读属性获取信息**（如 `artboardNames`、`stateMachineNames`）
3. **优先使用 `controller.activeArtboard`**（已由 controller 管理）
4. **如果手动创建，确保在 finally 中 release**

---

## 参考资料

- [Rive Android Runtime 官方文档](https://rive.app/community/doc/android/docvnULCzB9o)
- [Rive State Machine 指南](https://rive.app/community/doc/state-machines/docbSloV0uwC)
- [示例代码：AndroidPlayerActivity](../app/src/main/java/app/rive/runtime/example/AndroidPlayerActivity.kt)
- [示例代码：OnboardingStateMachineActivity](../app/src/main/java/app/rive/runtime/example/OnboardingStateMachineActivity.kt)
