# Rive AutoPlay 与循环机制详解

本文档详细解释 `riveAutoPlay` 属性的工作原理，以及线性动画和状态机在循环播放行为上的本质区别。

## 目录

1. [核心结论](#核心结论)
2. [线性动画 vs 状态机：本质区别](#线性动画-vs-状态机本质区别)
3. [状态机为什么不会自动重播](#状态机为什么不会自动重播)
4. [为什么背景会重播](#为什么背景会重播)
5. [代码层面的 Loop 处理](#代码层面的-loop-处理)
6. [如何让状态机动画循环](#如何让状态机动画循环)
7. [总结图解](#总结图解)

---

## 核心结论

`riveAutoPlay="true"` 对于**状态机控制的动画不会自动重播**，状态机是"一次性"的设计。而背景循环动画（如果设计为 `Loop`）可以独立重播。

**常见误解**：

```xml
<app.rive.runtime.kotlin.RiveAnimationView
    android:id="@+id/rivOnboarding"
    android:layout_width="0dp"
    android:layout_height="0dp"
    app:riveAutoPlay="true"
    app:riveFit="COVER"/>
```

设置 `riveAutoPlay="true"` 后：
- ❌ 状态机动画**不会**自动循环重播
- ✅ 线性动画**可以**循环（取决于动画的 Loop 设置）
- ✅ `riveAutoPlay` 只控制"是否自动启动"，**不控制**"是否循环"

---

## 线性动画 vs 状态机：本质区别

| 特性 | 线性动画 (LinearAnimation) | 状态机 (StateMachine) |
|------|---------------------------|----------------------|
| **驱动方式** | 时间驱动 | 输入驱动 |
| **Loop 属性** | ✅ 支持 `ONESHOT` / `LOOP` / `PINGPONG` | ❌ 无此概念 |
| **自动循环** | ✅ 可以通过 `Loop.LOOP` 无限循环 | ❌ 不支持 |
| **控制方式** | `play()` / `pause()` / `stop()` | 设置输入值触发状态转换 |
| **完成后行为** | 根据 Loop 决定 | 停在最终状态，`advance()` 返回 `false` |
| **适用场景** | 循环播放的装饰性动画 | 交互驱动的复杂动画 |

### Loop 枚举类型

```kotlin
enum class Loop {
    ONESHOT,   // 播放一次后停止
    LOOP,      // 无限循环
    PINGPONG,  // 来回播放
    AUTO       // 使用动画本身的设置
}
```

---

## 状态机为什么不会自动重播

### 核心机制

状态机的 `advance()` 方法返回一个 `Boolean`：

```kotlin
// StateMachineInstance.kt
fun advance(elapsed: Float): Boolean =
    synchronized(lock) { cppAdvance(cppPointer, elapsed) }
```

- `true`：状态机还有动画在进行中
- `false`：状态机已经到达**静止状态**（Exit State 或某个没有继续转换的状态）

### 控制器处理逻辑

在 `RiveFileController.kt` 中：

```kotlin
val stateMachinesToPause = mutableListOf<StateMachineInstance>()
stateMachines.forEach { stateMachineInstance ->
    if (playingStateMachines.contains(stateMachineInstance)) {
        val stillPlaying = resolveStateMachineAdvance(stateMachineInstance, elapsed)

        if (!stillPlaying) {
            stateMachinesToPause.add(stateMachineInstance)
        }
    }
}

// 当状态机完成所有动画后，会被自动暂停
if (elapsed > 0.0) {
    stateMachinesToPause.forEach { pause(stateMachine = it) }
}
```

**关键点**：当状态机完成所有动画后，会被自动暂停！这是设计意图，不是 bug。

---

## 为什么背景会重播

在 Rive 编辑器中，设计师可能做了这样的设计：

```
Rive 文件
├── Artboard
│   ├── 背景元素
│   │   └── 线性动画 "BackgroundLoop" (Loop: LOOP) ← 这个会一直循环
│   │
│   ├── 主要元素
│   │   └── 状态机控制 ← 这个播放一次就停了
│   │
│   └── State Machine
│       └── "State_Machine_1"
```

当你设置 `riveAutoPlay="true"` 时：

1. **状态机被启动** → 播放 Entry → 执行动画 → 到达最终状态 → **停止**
2. **背景循环动画**（如果存在且是独立的线性动画）→ 根据自己的 `Loop` 属性持续播放

这就是为什么你会观察到"背景重播，但状态机控制的元素不重播"的现象。

---

## 代码层面的 Loop 处理

### 线性动画的 Loop 处理

对于**线性动画**，可以设置循环模式：

```kotlin
// RiveFileController.kt
internal fun play(
    animationInstance: LinearAnimationInstance,
    loop: Loop,
    direction: Direction,
) {
    // 如果指定了 loop 模式，使用它；否则使用默认值
    val appliedLoop = if (loop == Loop.AUTO) this.loop else loop
    if (appliedLoop != Loop.AUTO) {
        animationInstance.loop = appliedLoop
    }
    // ...
}
```

线性动画在 advance 时会检查循环：

```kotlin
// RiveFileController.kt - advance() 方法
animations.forEach { animationInstance ->
    if (playingAnimations.contains(animationInstance)) {
        val advanceResult = animationInstance.advanceAndGetResult(elapsed)
        animationInstance.apply()

        when (advanceResult) {
            AdvanceResult.ONESHOT -> {
                stop(animationInstance)  // 播放一次后停止
            }
            AdvanceResult.LOOP, AdvanceResult.PINGPONG -> {
                notifyLoop(animationInstance)  // 循环继续
            }
            AdvanceResult.ADVANCED -> {
                // 正常推进
            }
            AdvanceResult.NONE -> Unit
        }
    }
}
```

### 状态机没有 Loop 机制

状态机完全由输入驱动，没有内置的循环机制：

```kotlin
// 状态机播放
internal fun play(
    stateMachineInstance: StateMachineInstance,
    settleStateMachineState: Boolean = true,
) {
    // 注意：没有 loop 参数！
    if (!stateMachineList.contains(stateMachineInstance)) {
        stateMachineList.add(stateMachineInstance)
    }
    // ...
}
```

---

## 如何让状态机动画循环

状态机不支持自动循环，你需要**手动重置并重新播放**。

### 方法一：使用 `setRiveResource()` 完全重新加载（推荐）

```kotlin
private fun resetAndLoop() {
    riveView.setRiveResource(
        resId = R.raw.your_animation,
        stateMachineName = "State_Machine_1",
        autoplay = true
    )
    // 重新设置 Text Run 和输入值
    setupTextContent()
    setupNumberInputs()
    // 如果需要，延迟触发状态转换
    startAnimationSequence()
}
```

### 方法二：使用 Handler 定时循环

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

### 方法三：在 Rive 编辑器中设计循环状态机

在 Rive 编辑器中，让最后一个状态**自动转换回 Entry 状态**，这样状态机就会自己循环。

```
Entry State → Animation 1 → Animation 2 → Animation 3
     ↑                                          │
     └──────────────────────────────────────────┘
                    (自动转换)
```

### ⚠️ 注意：不要使用 `reset()`

```kotlin
// ❌ 不推荐
fun resetAndLoop() {
    riveView.reset()
    riveView.play("StateMachine_1", isStateMachine = true)
    // 可能导致动画异常
}

// ✅ 推荐
fun resetAndLoop() {
    riveView.setRiveResource(
        resId = R.raw.animation,
        stateMachineName = "StateMachine_1",
        autoplay = true
    )
}
```

`reset()` 创建的 Artboard 的"初始状态"与文件首次加载时可能不同，使用 `setRiveResource()` 可以确保完全重置。

---

## 总结图解

```
┌─────────────────────────────────────────────────────────────┐
│                     Rive 文件结构                            │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ 线性动画 (LinearAnimation)                           │    │
│  │ ----------------------------------------             │    │
│  │ • Loop = LOOP → 自动循环 ✅                          │    │
│  │ • Loop = ONESHOT → 播放一次停止                       │    │
│  │ • 由时间驱动，riveAutoPlay + riveLoop 生效            │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                              │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ 状态机 (StateMachine)                                │    │
│  │ ----------------------------------------             │    │
│  │ • 无 Loop 概念 ❌                                     │    │
│  │ • 由输入值驱动 (Number/Boolean/Trigger)               │    │
│  │ • advance() 返回 false 后自动暂停                     │    │
│  │ • riveAutoPlay 只控制"是否启动"，不控制"是否循环"      │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### 常见场景对照

| 场景 | 观察到的现象 | 原因 |
|------|-------------|------|
| 背景一直动 | 背景是设置为 `LOOP` 的线性动画 | 线性动画支持循环 |
| 状态机动画只播一次 | 状态机到达最终状态后 `advance()` 返回 `false` | 状态机设计本身不支持自动循环 |
| 设置 `riveAutoPlay="true"` 但不循环 | `riveAutoPlay` 只控制启动，不控制循环 | 需要配合 Loop 属性或手动重置 |

---

## 相关文档

- [Rive Android 开发踩坑指南](./rive_android_pitfalls.md)
- [循环播放的正确方式](./rive_android_pitfalls.md#7-循环播放的正确方式)
- [线性动画 vs 状态机](./rive_android_pitfalls.md#5-线性动画-vs-状态机)
