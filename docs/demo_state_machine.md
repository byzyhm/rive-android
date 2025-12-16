# Rive Android 状态机与嵌套

本文档介绍 Rive Android 的状态机控制和嵌套 Artboard 使用。

> 📚 返回 [Demo Activities 完整指南](./demo_activities_guide.md)

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

#### 方式 1：访问当前活动 Artboard 的 Text Run（不需要 path）

```kotlin
// 不带 path - 访问主 Artboard 上的 Text Run
animationView.setTextRunValue(
    textRunName = "title",
    textValue = "Hello World"
)

// 获取当前 Artboard 的 Text Run
val text = animationView.getTextRunValue("title")
```

#### 方式 2：访问嵌套 Artboard 的 Text Run（需要 path）

```kotlin
// 带 path - 访问嵌套 Artboard 上的 Text Run
animationView.setTextRunValue(
    textRunName = "ArtboardBRun",
    textValue = "Updated Text",
    path = "ArtboardB-1/ArtboardC-1"  // 指定嵌套路径
)

// 获取嵌套 Artboard 的 Text Run
val text = animationView.getTextRunValue("ArtboardBRun", "ArtboardB-1")
```

### 🔀 两个重载方法的区别

`setTextRunValue` 有**两个重载版本**：

| 方法签名 | 用途 | 何时使用 |
|---------|------|---------|
| `setTextRunValue(textRunName, textValue)` | 访问**当前活动 Artboard** | Text Run 在主 Artboard 上 |
| `setTextRunValue(textRunName, textValue, path)` | 访问**嵌套 Artboard** | Text Run 在嵌套的 Artboard 上 |

### 🤔 如何判断是否需要 path 参数？

```
决策流程：

┌─────────────────────────────────┐
│ Text Run 在哪个 Artboard 上？   │
└────────────┬────────────────────┘
             │
    ┌────────┴────────┐
    │                 │
    ▼                 ▼
┌─────────┐      ┌──────────┐
│ 主画板   │      │ 嵌套画板  │
└────┬────┘      └─────┬────┘
     │                 │
     ▼                 ▼
不需要 path        需要 path 参数
只需要 2 个参数    需要 3 个参数
```

**快速判断方法：**
1. 如果 Text Run 直接在主 Artboard → **不需要 path**
2. 如果 Text Run 在嵌套的子 Artboard → **需要 path**
3. 不确定？看设计师是否使用了嵌套 Artboard 结构

**实际场景对比：**

```kotlin
// ========== 场景 1: 简单动画（不嵌套）==========
// Rive 文件结构：
// 📦 Main Artboard
//    ├── 🎨 Shape: Background
//    ├── 📝 Text Run: "title"      ← 直接在主画板
//    └── 📝 Text Run: "subtitle"   ← 直接在主画板

// 代码使用 - 不需要 path：
animationView.setTextRunValue("title", "Welcome")      // ✅ 2 个参数
animationView.setTextRunValue("subtitle", "Hello")     // ✅ 2 个参数

// ========== 场景 2: 复杂嵌套动画 ==========
// Rive 文件结构：
// 📦 Main Artboard
//    ├── 📦 Nested Artboard: "Header"
//    │   └── 📝 Text Run: "title"           ← 在嵌套画板中
//    └── 📦 Nested Artboard: "ProfileCard"
//        ├── 📝 Text Run: "username"        ← 在嵌套画板中
//        └── 📦 Nested Artboard: "Avatar"
//            └── 📝 Text Run: "initials"    ← 在深层嵌套中

// 代码使用 - 需要 path：
animationView.setTextRunValue("title", "Welcome", "Header")              // ✅ 3 个参数
animationView.setTextRunValue("username", "Alice", "ProfileCard")        // ✅ 3 个参数
animationView.setTextRunValue("initials", "AB", "ProfileCard/Avatar")    // ✅ 3 个参数，多层路径
```

### 📋 参数来源说明

#### `textRunName` - Text Run 名称

**完全由设计师在 Rive 编辑器中设置**，开发者无法枚举获取。

设计师操作流程：
```
1. 在 Rive 编辑器中选择文本元素
2. 在右侧面板找到 "Text" → "Runs"
3. 点击 "+" 添加一个 Run
4. 设置 "Export Name"（如 "ArtboardBRun"）
5. 重新导出 .riv 文件
```

⚠️ **重要提示**：
- Rive API **没有提供**枚举所有 Text Run 名称的方法
- 只能通过 `artboard.textRun(name)` 按名称获取
- 如果名称不匹配或未设置，会抛出 `TextValueRunException`

#### `path` - 嵌套 Artboard 路径

**由设计师在 Rive 中创建的 Artboard 嵌套结构决定**

路径规则：
- 单层嵌套：`"ArtboardB-1"`
- 多层嵌套：`"ArtboardB-1/ArtboardC-1"`（用 `/` 分隔）
- Artboard 名称由设计师在 Rive 编辑器中命名

### 💡 最佳实践

**建议让设计师提供文档清单：**

```markdown
## Rive 文件清单 - nested_text_run.riv

### Text Runs（动态文本）
| Text Run 名称 | 所在路径 | 说明 |
|--------------|----------|------|
| ArtboardBRun | ArtboardB-1 | B-1 画板的文本 |
| ArtboardCRun | ArtboardB-1/ArtboardC-1 | B-1/C-1 画板的文本 |

### Artboard 嵌套结构
Main Artboard
├── ArtboardB-1
│   ├── ArtboardC-1
│   └── ArtboardC-2
└── ArtboardB-2
    ├── ArtboardC-1
    └── ArtboardC-2
```

### 🔍 参数获取方式对比

| 参数 | 来源 | 能否通过代码枚举 | 如何获取 |
|------|------|----------------|----------|
| `textRunName` | 设计师在 Rive 编辑器设置 | ❌ 不能 | 必须由设计师提供 |
| `path` | Rive 文件的 Artboard 层级 | ⚠️ 部分可以 | 可通过 `file.artboardNames` 获取名称，但层级需要设计师说明 |
| `textValue` | 开发者动态设置 | ✅ 是 | 通过 `getTextRunValue()` 获取当前值 |

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

// 方式 2：直接设置（不带 path - 用于当前活动 Artboard）
animationView.setTextRunValue("name", "New Text")

// 获取当前值
val currentText = animationView.getTextRunValue("name")
```

**注意**：此示例中 Text Run 位于**主 Artboard** 上，所以**不需要** `path` 参数。如果 Text Run 位于嵌套 Artboard，需要使用带 `path` 参数的重载方法。

### ⚠️ 重要提示

**Text Run 名称必须由设计师在 Rive 编辑器中设置**

1. Text Run 名称无法通过代码枚举获取
2. 必须使用设计师提供的确切名称
3. 如果名称不匹配，会抛出 `TextValueRunException`

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

*返回 [Demo Activities 完整指南](./demo_activities_guide.md)*

