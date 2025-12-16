# Rive Android Demo Activities 完整指南

本文档是 rive-android 项目中所有 Demo Activity 的索引指南。

---

## 📚 文档导航

本指南已按主题拆分为多个文档，便于查阅：

| 文档 | 内容 | 说明 |
|------|------|------|
| **[基础示例](./demo_basic.md)** | ComposeActivity, SimpleActivity, EventsActivity | 入门必看 |
| **[交互与控制](./demo_interaction.md)** | 播放控制、循环模式、布局、按钮、性能指标 | 动画控制 |
| **[状态机与嵌套](./demo_state_machine.md)** | 状态机、嵌套 Artboard、动态文本、图片绑定 | 高级交互 |
| **[资源加载](./demo_resources.md)** | HTTP 加载、字体、音频、自定义加载器 | 资源管理 |
| **[UI 集成](./demo_ui_integration.md)** | Fragment、RecyclerView、ViewPager、Compose | UI 组件 |
| **[高级功能与 API](./demo_advanced.md)** | 底层 API、压力测试、API 速查表 | 进阶开发 |

---

## 目录总览

### 📋 必读指南
- [与设计师协作指南](#-与设计师协作指南) - ⭐ **必读**

### 🚀 基础示例 → [查看详情](./demo_basic.md)
- ComposeActivity - Jetpack Compose 集成
- SimpleActivity - 最简单的使用示例
- EventsActivity - Rive 事件监听

### 🎮 交互与控制 → [查看详情](./demo_interaction.md)
- InteractiveSamplesActivity - 交互式动画（时钟）
- MultipleArtboardsActivity - 多 Artboard
- AndroidPlayerActivity - 完整播放器控制
- LoopModeActivity - 循环模式控制
- LayoutActivity - Fit 和 Alignment 布局
- ButtonActivity - 自定义 Rive 按钮
- BlendActivity - 混合模式
- MetricsActivity - 性能指标（FPS）
- TouchPassthroughActivity - 触摸穿透

### ⚙️ 状态机与嵌套 → [查看详情](./demo_state_machine.md)
- SimpleStateMachineActivity - 状态机控制
- NestedInputActivity - 嵌套 Artboard 输入
- NestedTextRunActivity - 嵌套 Text Run
- DynamicTextActivity - 动态文本
- ImageBindingActivity - 图片绑定

### 🌐 资源加载 → [查看详情](./demo_resources.md)
- HttpActivity - 网络加载 .riv 文件
- AssetsActivity - Assets 资源加载
- AssetLoaderActivity - 自定义资源加载器
- FontLoadActivity - 字体加载
- FontFallback - 字体回退策略
- AudioAssetActivity - 音频资源
- AudioExternalAssetActivity - 外部音频资源

### 📱 UI 集成 → [查看详情](./demo_ui_integration.md)
- RiveFragmentActivity - Fragment 集成
- RecyclerActivity - RecyclerView 集成
- ViewPagerActivity - ViewPager2 集成
- ViewStubActivity - ViewStub 延迟加载
- LegacyComposeActivity - 传统 Compose 集成
- FrameActivity - Fragment 切换
- MeshesActivity - 网格动画

### 🧪 高级功能 → [查看详情](./demo_advanced.md)
- LowLevelActivity - 底层 API 使用
- StressTestActivity - 压力测试
- API 速查表

---

## 📋 与设计师协作指南

在使用 Rive 动画前，开发者需要从设计师处获取一些关键信息。以下是必需的信息清单：

### 🎨 设计师需要提供的信息

#### 1. Text Run 名称 ⚠️ **无法通过代码枚举**

Text Run 是动态文本，必须由设计师在 Rive 编辑器中设置名称：

**设计师操作步骤：**
```
1. 在 Rive 编辑器中选择文本元素
2. 在右侧面板找到 "Text" → "Runs"
3. 点击 "+" 添加一个 Run
4. 设置 "Export Name"（如 "title"、"content"）
5. 重新导出 .riv 文件
```

**开发者使用：**
```kotlin
// 使用设计师提供的 Text Run 名称
animationView.setTextRunValue("title", "Hello World")

// ❌ 错误：无法枚举所有 Text Run 名称
// Rive API 不提供此功能
```

#### 2. Artboard 嵌套路径 ⚠️ **无法通过代码枚举**

如果使用嵌套 Artboard，**必须从设计师处获取**完整的层级结构：

```
Main Artboard
├── ProfileCard          (路径: "ProfileCard")
│   └── Avatar          (路径: "ProfileCard/Avatar")
└── MessageList         (路径: "MessageList")
    └── MessageItem     (路径: "MessageList/MessageItem")
```

⚠️ **重要限制：**
- Kotlin API **没有提供**枚举嵌套 Artboard 的方法
- 虽然 C++ 层有 `nestedArtboards()` 方法，但未在 Kotlin 层绑定
- 只能通过**已知路径**访问嵌套内容
- **必须由设计师提供完整路径**

**开发者使用：**
```kotlin
// 访问嵌套的 Text Run - 需要设计师提供 path
animationView.setTextRunValue(
    textRunName = "username",
    textValue = "Alice",
    path = "ProfileCard/Avatar"  // ⚠️ 此路径无法枚举，需设计师提供
)
```

#### 3. State Machine 信息

- State Machine 名称
- Input 参数名称和类型（Boolean、Number、Trigger）

**开发者可以通过代码获取：**
```kotlin
val file = animationView.controller.file
val artboard = file?.firstArtboard
val stateMachineNames = artboard?.stateMachineNames // ✅ 可获取
```

#### 4. Animation 信息

- Animation 名称
- 是否循环

**开发者可以通过代码获取：**
```kotlin
val animationNames = artboard?.animationNames // ✅ 可获取
```

#### 5. Event 信息

- Event 名称
- Event 类型（General、OpenURL）
- Event 属性

⚠️ 事件只能在触发时获取，无法预先枚举。

#### 6. 字体和资源

- 字体是否嵌入到 .riv 文件
- 外部字体文件名称和路径
- 外部图片资源名称

### 📄 推荐的设计师交付清单模板

建议让设计师使用以下模板：

## Rive 文件清单 - filename.riv

### 基本信息
- 文件名: filename.riv
- 默认 Artboard: Main
- 文件大小: XX KB

### Artboards
| 名称 | 说明 |
|------|------|
| Main | 主画板 |
| Profile | 用户资料卡片 |

### Text Runs（动态文本）⭐ 重要
| Text Run 名称 | 所在位置 | 路径 (path) | 初始值 | 代码示例 |
|--------------|---------|------------|--------|---------|
| title | 主 Artboard | - | "Hello" | `setTextRunValue("title", "Hello")` |
| username | 嵌套 Artboard | Profile | "" | `setTextRunValue("username", "Alice", "Profile")` |

### State Machines
| State Machine | Input 名称 | 类型 | 说明 |
|---------------|-----------|------|------|
| Controller | isActive | Boolean | 是否激活 |
| Controller | progress | Number | 进度值 (0-100) |
| Controller | reset | Trigger | 重置动画 |

### Animations
| 名称 | 是否循环 | 时长 | 说明 |
|------|---------|------|------|
| idle | ✓ | 2s | 待机动画 |
| tap | ✗ | 0.5s | 点击反馈 |

### Events
| 名称 | 类型 | 属性 | 说明 |
|------|------|------|------|
| onComplete | General | action: String | 完成时触发 |
| openLink | OpenURL | url: String | 打开链接 |

### Artboard 嵌套结构
Main
├── Header
│   └── Logo
└── Content
    ├── Card1
    └── Card2


### 🔍 开发者可自行获取的信息

以下信息可以通过代码获取，无需设计师提供：

```kotlin
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
            // 不要调用 sm.release()
        }
        // 不要调用 artboard.release()
    }
}
```

### ⚠️ 关键提醒

| 信息类型 | 能否枚举 | 获取方式 |
|---------|---------|---------|
| Artboard 名称 | ✅ 可以 | `file.artboardNames` |
| Animation 名称 | ✅ 可以 | `artboard.animationNames` |
| State Machine 名称 | ✅ 可以 | `artboard.stateMachineNames` |
| State Machine Inputs | ✅ 可以 | `stateMachine.inputs` |
| **Text Run 名称** | ❌ **不能** | **必须由设计师提供** |
| **嵌套 Artboard 路径** | ❌ **不能** | **Kotlin API 未暴露（C++ 有但未绑定）** |
| Event 信息 | ⚠️ 运行时 | 触发时才能获取 |

---

## 📚 相关文档

- [内存管理指南](../MEMORY_MANAGEMENT_CN.md)
- [渲染器架构](./RENDERER_ARCHITECTURE_CN.md)
- [Onboarding 动画集成](./onboarding_animation_integration.md)

---

*文档生成时间：2025-12*
