# Rive Android 高级功能与 API 参考

本文档介绍 Rive Android 的高级功能、底层 API 和开发参考。

> 📚 返回 [Demo Activities 完整指南](./demo_activities_guide.md)

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
                    
                    // 注意：不要在这里调用 sm.release()
                    // 原因见 MEMORY_MANAGEMENT_CN.md
                } catch (e: Exception) {
                    Log.w("RiveInfo", "    Failed to load state machine $smName: ${e.message}")
                }
            }
            
            // 注意：不要在这里调用 artboard.release()
        } catch (e: Exception) {
            Log.w("RiveInfo", "  Failed to load artboard $artboardName: ${e.message}")
        }
    }
    
    // 5. 当前活动的 Artboard 信息
    riveView.controller.activeArtboard?.let { activeArtboard ->
        Log.d("RiveInfo", "")
        Log.d("RiveInfo", "--- Active Artboard ---")
        Log.d("RiveInfo", "Name: ${activeArtboard.name}")
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

### ⚠️ 注意事项

1. **Text Run 名称无法枚举**
   - Rive API **没有提供**获取所有 Text Run 名称的方法
   - 需要从设计师处获取 Text Run 名称列表
   - 只能通过 `artboard.textRun(name)` 按名称获取

2. **事件信息无法预先获取**
   - 事件只有在触发时才能获取
   - 需要从设计师处获取事件名称和属性列表

3. **资源释放**
   - ⚠️ **不要**对 `file.artboard()` 或 `artboard.stateMachine()` 获取的对象手动调用 `release()`
   - 这些对象会被自动添加到 `dependencies` 并在适当时候释放
   - 手动 `release()` 会导致引用计数错误，详见 [MEMORY_MANAGEMENT_CN.md](../MEMORY_MANAGEMENT_CN.md)

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

*返回 [Demo Activities 完整指南](./demo_activities_guide.md)*

