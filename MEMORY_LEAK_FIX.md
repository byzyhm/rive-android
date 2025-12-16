# OnboardingStateMachineActivity 内存泄漏修复报告

## 🐛 原问题描述

**症状**：
- 第一次进入 Activity 时可以正常播放动画
- 再次进入时无法播放
- 必须杀死应用后，再进入才可以正常播放
- **从此 Activity 退出后，其他 Activity（如 StressTestActivity）也无法播放** ⚠️

## 🔍 根本原因分析

### ⚠️ **致命问题**：错误修改全局渲染状态

```kotlin
// OnboardingAnimationController.kt - 第 84 行（原代码）
fun initialize(data: TranslationData) {
    riveView.isOpaque = false  // ❌ 这是致命错误！
    // ...
}
```

**为什么这是致命的？**

1. `isOpaque` 是 `TextureView` 的属性，控制渲染方式
2. 这个属性应该**只在 View 初始化时设置一次**（在 `onAttachedToWindow` 中）
3. **重复设置会影响 TextureView 的全局渲染状态**
4. 导致退出后其他使用 `RiveTextureView` 的 Activity 也无法正常渲染

**证据**：
- 从 OnboardingStateMachineActivity 退出后
- StressTestActivity 也无法播放
- 这是典型的全局状态污染问题

### 🔍 其他根本原因分析

### 1. Handler 内存泄漏 ⚠️ **严重**

```kotlin
// 问题代码
private val handler = Handler(Looper.getMainLooper())

handler.postDelayed({
    playTranslationSequence()
}, POPUP_ANIMATION_DELAY_MS)
```

**问题**：
- Handler 持有 `OnboardingAnimationController` 的引用
- Controller 持有 `RiveAnimationView` 的引用
- View 持有 Activity 的引用
- 如果 Activity 销毁时 Handler 回调还未执行，整个引用链都无法释放
- 延迟任务可能在 Activity 销毁后执行，导致操作已销毁的 View

### 2. 初始化时机不正确

```kotlin
// 问题代码
override fun onCreate(savedInstanceState: Bundle?) {
    animationView.post {
        initializeAnimation()
    }
}
```

**问题**：
- 第二次进入 Activity 时，`post` 回调可能不会执行
- 或者执行时机不对，导致初始化失败

### 3. 生命周期管理不完整

**问题**：
- 只在 `onDestroy` 时清理资源
- 用户按 Home 键或切换应用时，Activity 进入 `onPause`/`onStop`
- 此时 Handler 回调仍在运行，浪费资源且可能崩溃
- `onDestroy` 可能不会及时调用（系统内存紧张时）

### 4. 没有防护机制

**问题**：
- 没有 `isReleased` 标志位
- Handler 回调可能在资源释放后执行
- 导致访问已销毁的对象

### 5. XML 布局未指定状态机

```xml
<!-- 原 XML（错误）-->
<app.rive.runtime.kotlin.RiveAnimationView
    app:riveResource="@raw/onboarding_part_1_with_font"
    app:riveAutoPlay="true" />
    <!-- ❌ 缺少 app:riveStateMachine="StateMachine_1" -->
```

**问题**：
- 虽然设置了 `riveAutoPlay="true"`
- 但没有指定要播放的状态机
- 导致动画文件加载了，但状态机没有启动
- 初始化代码设置的 state=0 等参数无法生效

### 6. 状态机未显式启动

**问题**：
- 即使 XML 设置了 `riveAutoPlay="true"`
- 在某些情况下状态机可能没有自动播放
- 需要在代码中显式检查并启动

---

## ✅ 修复方案

### 修复 0：删除全局状态污染 ⚠️ **最关键！**

```kotlin
// ❌ 错误代码
fun initialize(data: TranslationData) {
    riveView.isOpaque = false  // 这会污染全局渲染状态！
    // ...
}

// ✅ 正确代码
fun initialize(data: TranslationData) {
    // 删除 riveView.isOpaque = false
    // isOpaque 应该在 RiveTextureView.onAttachedToWindow() 中设置
    // 不应该在业务逻辑中重复设置
    // ...
}
```

**为什么要删除？**
- `isOpaque` 是 TextureView 的底层渲染属性
- RiveTextureView 已经在 `onAttachedToWindow()` 中正确设置
- 重复设置会破坏渲染状态
- 影响所有使用 TextureView 的 View

### 修复 1：完善生命周期管理

```kotlin
class OnboardingStateMachineActivity : AppCompatActivity() {
    
    private var animationController: OnboardingAnimationController? = null  // ✅ 可空类型
    private var isInitialized = false  // ✅ 防止重复初始化
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.onbarding_state_machine)
        // ✅ 只设置布局，不初始化
    }
    
    override fun onStart() {
        super.onStart()
        // ✅ 每次 Activity 变为可见时都重新初始化
        if (!isInitialized) {
            animationView.postDelayed({
                initializeController()
            }, 100)
        }
    }
    
    override fun onPause() {
        super.onPause()
        // ✅ 暂停动画和 Handler
        animationView.pause()
        animationController?.reset()
    }
    
    override fun onStop() {
        super.onStop()
        // ✅ 释放控制器（关键！）
        animationController?.release()
        animationController = null
        isInitialized = false
    }
    
    override fun onDestroy() {
        // ✅ 双重保险
        animationController?.release()
        animationController = null
        super.onDestroy()
    }
}
```

### 修复 2：添加释放标志位

```kotlin
class OnboardingAnimationController(
    private val riveView: RiveAnimationView
) {
    @Volatile
    private var isReleased = false  // ✅ 防止释放后继续执行
    
    fun playTranslationSequence() {
        if (isReleased) {
            Log.w(TAG, "Controller已释放，跳过动画")
            return
        }
        
        // ... 播放动画
        
        handler.postDelayed({
            if (!isReleased) {  // ✅ 回调中检查
                setStateValue(2f)
            }
        }, TRANSLATION_DELAY_MS)
    }
    
    fun release() {
        isReleased = true  // ✅ 设置标志
        handler.removeCallbacksAndMessages(null)  // ✅ 移除所有回调
    }
}
```

### 修复 3：所有关键方法添加检查

```kotlin
private fun setStateValue(value: Float) {
    if (isReleased) {  // ✅ 检查释放状态
        return
    }
    
    try {
        riveView.setNumberState(stateMachineName, INPUT_STATE, value)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to set state: ${e.message}")
    }
}
```

### 修复 4：XML 中指定状态机

```xml
<!-- ✅ 正确的 XML 配置 -->
<app.rive.runtime.kotlin.RiveAnimationView
    android:id="@+id/onboarding_state_machine"
    android:layout_width="300dp"
    android:layout_height="0dp"
    app:riveResource="@raw/onboarding_part_1_with_font"
    app:riveAutoPlay="true"
    app:riveFit="COVER"
    app:riveStateMachine="StateMachine_1" />
    <!-- ✅ 关键：指定状态机名称 -->
```

### 修复 5：代码中确保状态机播放

```kotlin
private fun initializeAnimation() {
    val translationData = TranslationData(/* ... */)
    
    // 初始化动画数据
    animationController?.initialize(translationData)
    
    // ✅ 确保状态机正在播放
    if (!animationView.isPlaying) {
        Log.d(TAG, "State machine not playing, starting it...")
        animationView.play()
    }
    
    Log.d(TAG, "Animation initialized, isPlaying=${animationView.isPlaying}")
}
```

---

## 📋 修复对比

| 方面 | 修复前 | 修复后 |
|------|--------|--------|
| **初始化时机** | `onCreate` + `post` | `onStart` + `postDelayed(100ms)` |
| **资源释放时机** | 仅 `onDestroy` | `onPause` + `onStop` + `onDestroy` |
| **Handler 清理** | 不完整 | `removeCallbacksAndMessages(null)` |
| **防护机制** | 无 | `isReleased` 标志 + 异常捕获 |
| **控制器生命周期** | `lateinit`，难以控制 | `可空类型` + `isInitialized` 标志 |
| **回调检查** | 无 | 所有回调都检查 `isReleased` |

---

## 📚 Rive 动画资源管理 API

### RiveAnimationView 的控制方法

| 方法 | 用途 | 推荐使用场景 |
|------|------|-------------|
| `pause()` | 暂停所有动画 | Activity `onPause` |
| `pause(animationName)` | 暂停指定动画 | 暂停特定动画 |
| `stop()` | 停止所有动画 | Activity `onStop` |
| `stop(animationName)` | 停止指定动画 | 停止特定动画 |
| `play()` | 播放动画 | Activity `onResume` |

### Controller 相关 API

| 属性/方法 | 用途 | 说明 |
|----------|------|------|
| `controller.isActive = false` | 停用控制器 | `onDetachedFromWindow` 时自动调用 |
| `controller.reset()` | 清空动画和状态机引用 | 重置到初始状态 |
| `controller.release()` | 释放控制器资源 | 手动释放（如果需要） |

### 自动生命周期管理

**RiveAnimationView 已内置完善的生命周期管理**：

✅ `onAttachedToWindow` → 自动激活和启动
✅ `onDetachedFromWindow` → 自动停用和清理  
✅ `RiveViewLifecycleObserver` → Activity/Fragment `onDestroy` 时自动释放

**结论**：
- ✅ **通常不需要手动释放 RiveAnimationView**
- ✅ **只需要管理好自己的 Handler 和控制器**
- ✅ **使用 `pause()`/`stop()` 控制播放**

---

## 🧪 测试清单

修复后请测试以下场景：

### 基本功能测试

- [ ] **场景 1**：进入 → 点击自动播放 → 完整播放
- [ ] **场景 2**：进入 → 点击翻译 → 立即播放翻译动画
- [ ] **场景 3**：进入 → 点击重置 → 回到初始状态

### 生命周期测试

- [ ] **场景 4**：进入 → 退出 → 再进入
  - ✅ 应该能正常播放
  - ✅ 日志显示控制器重新初始化
  
- [ ] **场景 5**：进入 → 按 Home 键 → 返回
  - ✅ 动画应该暂停
  - ✅ 返回后可以重新播放

- [ ] **场景 6**：播放动画中途 → 按返回键退出
  - ✅ 不应该崩溃
  - ✅ Handler 回调应该被取消

- [ ] **场景 7**：快速进入退出多次（10次以上）
  - ✅ 不应该崩溃
  - ✅ 不应该出现内存泄漏

### 内存泄漏测试

- [ ] **场景 8**：使用 LeakCanary 或 Android Profiler
  - ✅ 进入退出多次后，不应该有 Activity 泄漏
  - ✅ Handler 回调应该被正确清理

### 日志验证

运行后查看 Logcat，应该看到：

```
D/OnboardingActivity: onCreate called
D/OnboardingActivity: onStart called
D/OnboardingActivity: Initializing controller...
D/OnboardingActivity: Controller initialized successfully
D/OnboardingActivity: onPause called - pausing animation and handlers
D/OnboardingAnimation: Resetting animation...
D/OnboardingActivity: onStop called - releasing controller
D/OnboardingAnimation: Releasing controller resources...
D/OnboardingAnimation: Controller resources released
D/OnboardingActivity: onDestroy called - final cleanup
```

---

## 💡 最佳实践建议

### 1. Handler 使用规范

```kotlin
// ❌ 错误：可能泄漏
handler.postDelayed({
    doSomething()
}, 1000)

// ✅ 正确：添加检查和清理
handler.postDelayed({
    if (!isReleased && !isFinishing) {
        doSomething()
    }
}, 1000)

// ✅ 在 onStop/onDestroy 中清理
handler.removeCallbacksAndMessages(null)
```

### 2. 生命周期管理

```kotlin
onCreate    → 设置布局
onStart     → 初始化（每次可见都初始化）
onResume    → 恢复播放（可选）
onPause     → 暂停动画 + 取消 Handler
onStop      → 释放资源
onDestroy   → 最终清理（双重保险）
```

### 3. Rive 动画最佳实践

```kotlin
// ✅ 在 onPause 暂停
override fun onPause() {
    super.onPause()
    riveAnimationView.pause()
}

// ✅ 在 onResume 恢复（如果需要）
override fun onResume() {
    super.onResume()
    riveAnimationView.play()
}

// ✅ 不需要手动释放 RiveAnimationView
// 它会在 onDetachedFromWindow 时自动清理
```

### 4. 防御性编程

```kotlin
// ✅ 使用标志位
@Volatile private var isReleased = false

// ✅ 所有异步操作都检查
if (isReleased) return

// ✅ 异常捕获
try {
    riveView.setNumberState(...)
} catch (e: Exception) {
    Log.e(TAG, "Error: ${e.message}")
}
```

---

## 📊 修复效果预期

修复后，应该达到以下效果：

✅ **功能正常**：每次进入都能正常播放  
✅ **无内存泄漏**：退出后资源完全释放  
✅ **性能良好**：后台不执行无用操作  
✅ **稳定可靠**：快速操作不崩溃

---

## 📞 如果还有问题

如果修复后仍有问题，请检查：

1. **Logcat 日志**：查看是否有异常或警告
2. **LeakCanary**：检测是否还有内存泄漏
3. **Android Profiler**：查看内存和 CPU 使用
4. **Rive 文件**：确认 .riv 文件本身没有问题

---

**修复完成日期**：2024-12-16  
**修复人**：AI Assistant  
**测试状态**：待测试

---

## 🆕 第二轮修复 (2024-12-16 更新)

### 发现的新问题

**症状**：
- 修复后第一次进入仍然无法播放
- 从 OnboardingStateMachineActivity 退出后，StressTestActivity 也无法播放
- 这说明存在全局状态污染问题

### 新发现的根本原因

#### 1. **致命错误：全局渲染状态污染** 🚨

```kotlin
// OnboardingAnimationController.kt line 84
fun initialize(data: TranslationData) {
    riveView.isOpaque = false  // ❌ 致命！会污染全局渲染状态
}
```

**影响范围**：
- 修改 `TextureView.isOpaque` 会影响底层渲染
- 导致所有使用 `RiveTextureView` 的 Activity 都无法渲染
- 这是典型的全局状态污染

**修复**：
```kotlin
fun initialize(data: TranslationData) {
    // ✅ 删除 riveView.isOpaque = false
    // 理由：RiveTextureView 已在 onAttachedToWindow() 中正确设置
}
```

#### 2. **XML 未指定状态机**

```xml
<!-- ❌ 错误：缺少状态机配置 -->
<app.rive.runtime.kotlin.RiveAnimationView
    app:riveResource="@raw/onboarding_part_1_with_font"
    app:riveAutoPlay="true" />

<!-- ✅ 正确：指定状态机 -->
<app.rive.runtime.kotlin.RiveAnimationView
    app:riveResource="@raw/onboarding_part_1_with_font"
    app:riveAutoPlay="true"
    app:riveStateMachine="StateMachine_1" />
```

#### 3. **状态机未显式启动**

```kotlin
// ✅ 确保状态机正在播放
private fun initializeAnimation() {
    animationController?.initialize(translationData)
    
    if (!animationView.isPlaying) {
        Log.d(TAG, "State machine not playing, starting it...")
        animationView.play()
    }
}
```

### 问题诊断流程

```
问题：第二次进入无法播放
    ↓
检查日志：Controller initialized successfully ✅
    ↓
检查日志：Animation initialized, isPlaying=? 
    ↓
如果 isPlaying=false → 状态机未启动
    ↓
检查 XML：是否有 app:riveStateMachine？
    ↓
如果没有 → 添加状态机配置
    ↓
如果有 → 代码中调用 play()
```

### 快速修复清单

- [ ] **删除** `riveView.isOpaque = false`
- [ ] **添加** `app:riveStateMachine="StateMachine_1"` 到 XML
- [ ] **添加** `if (!isPlaying) play()` 到初始化代码
- [ ] **测试** 第二次进入是否正常播放
- [ ] **测试** 退出后其他 Activity 是否正常

### 关键日志

成功的日志应该是：
```
D/OnboardingActivity: onStart called
D/OnboardingActivity: Initializing controller...
D/OnboardingAnimation: Initializing with state machine: StateMachine_1
D/OnboardingAnimation: Set state = 0.0
D/OnboardingActivity: Animation initialized, isPlaying=true  ← ✅ 关键
D/OnboardingActivity: Controller initialized successfully
```

如果看到 `isPlaying=false`，说明状态机没有启动！

