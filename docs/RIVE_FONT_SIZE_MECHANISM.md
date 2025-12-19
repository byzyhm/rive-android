# Rive 字号（Font Size）处理机制调研

> 基于 rive-android 源码和 rive-runtime 分析

## 目录

- [概述](#概述)
- [字号在 .riv 文件中的存储](#字号在-riv-文件中的存储)
- [核心数据结构](#核心数据结构)
- [运行时字号控制](#运行时字号控制)
- [Android SDK 暴露的 API](#android-sdk-暴露的-api)
- [字号相关的高级特性](#字号相关的高级特性)
- [总结与建议](#总结与建议)

---

## 概述

Rive 中的字号（Font Size）遵循**设计时定义、运行时只读**的原则：

1. **字号在 Rive 编辑器中设置**，存储在 `.riv` 文件的 `TextStyle` 组件中
2. **字号支持动画**，可以在 Rive 编辑器中创建字号变化的动画
3. **Android SDK 目前不支持运行时直接修改字号**，只能修改文本内容

---

## 字号在 .riv 文件中的存储

### TextStyle 组件定义

字号存储在 `TextStyle` 组件中，源码定义位于：

```1:77:submodules/rive-runtime/dev/defs/text/text_style.json
{
  "name": "TextStyle",
  // ...
  "properties": {
    "fontSize": {
      "type": "double",
      "initialValue": "12",      // 默认字号 12
      "animates": true,          // ✅ 支持动画
      "key": {
        "int": 274,
        "string": "fontsize"
      },
      "bindable": true           // ✅ 支持数据绑定
    },
    "lineHeight": {
      "type": "double",
      "initialValue": "-1.0",    // -1 表示自动
      "animates": true,
      "bindable": true
    },
    "letterSpacing": {
      "type": "double",
      "initialValue": "0.0",
      "animates": true,
      "bindable": true
    }
    // ...
  }
}
```

### 关键属性说明

| 属性 | 类型 | 默认值 | 可动画 | 可绑定 | 说明 |
|------|------|--------|--------|--------|------|
| `fontSize` | double | 12 | ✅ | ✅ | 字号大小（单位：像素） |
| `lineHeight` | double | -1.0 | ✅ | ✅ | 行高，-1 表示自动 |
| `letterSpacing` | double | 0.0 | ✅ | ✅ | 字间距 |

---

## 核心数据结构

### 1. TextStyle（样式定义）

```45:54:submodules/rive-runtime/include/rive/generated/text/text_style_base.hpp
class TextStyleBase : public ContainerComponent
{
protected:
    float m_FontSize = 12.0f;      // 默认字号
    float m_LineHeight = -1.0f;
    float m_LetterSpacing = 0.0f;

public:
    inline float fontSize() const { return m_FontSize; }
    void fontSize(float value);    // 设置字号会触发重新排版
};
```

当字号改变时会触发文本重新排版：

```158:158:submodules/rive-runtime/src/text/text_style.cpp
void TextStyle::fontSizeChanged() { m_text->markShapeDirty(); }
```

### 2. TextRun（用户定义的文本运行）

`TextRun` 是文本排版的输入结构，包含字号信息：

```257:267:submodules/rive-runtime/include/rive/text_engine.hpp
struct TextRun
{
    rcp<Font> font;           // 字体
    float size;               // ⭐ 字号
    float lineHeight;         // 行高
    float letterSpacing;      // 字间距
    uint32_t unicharCount;    // 字符数
    uint32_t script;          // 脚本类型
    uint16_t styleId;         // 样式 ID
    uint8_t level;            // BiDi 级别
};
```

### 3. GlyphRun（排版后的字形运行）

系统排版后生成的 `GlyphRun` 同样包含字号：

```272:298:submodules/rive-runtime/include/rive/text_engine.hpp
struct GlyphRun
{
    rcp<Font> font;
    float size;               // ⭐ 字号（用于实际渲染）
    float lineHeight;
    float letterSpacing;
    // ... 字形数据
};
```

### 4. TextValueRun（可访问的文本运行）

这是 Android SDK 中可以访问的对象：

```1:42:submodules/rive-runtime/dev/defs/text/text_value_run.json
{
  "name": "TextValueRun",
  "properties": {
    "styleId": {
      "type": "Id",
      "description": "The id of the style to be applied to this run."
    },
    "text": {
      "type": "String",
      "animates": true,        // 文本内容可动画
      "description": "The text string value."
    }
  }
}
```

**注意**：`TextValueRun` 只暴露了 `styleId` 和 `text`，**没有直接暴露 `fontSize`**！

---

## 运行时字号控制

### 当前限制

在 Android SDK 中，`RiveTextValueRun` 类只暴露了文本内容的修改能力：

```1:23:kotlin/src/main/java/app/rive/runtime/kotlin/core/RiveTextValueRun.kt
open class RiveTextValueRun(unsafeCppPointer: Long) : NativeObject(unsafeCppPointer) {
    private external fun cppText(cppPointer: Long): String
    private external fun cppSetText(cppPointer: Long, name: String)

    var text: String
        get() = cppText(cppPointer)
        set(name) {
            cppSetText(cppPointer, name)
        }
}
```

**结论**：目前 **无法在运行时直接修改字号**。

### 可行的替代方案

#### 1. 使用动画控制字号

由于 `fontSize` 支持动画（`animates: true`），可以：

1. 在 Rive 编辑器中创建不同字号的动画状态
2. 通过状态机或时间轴动画切换字号
3. 在代码中触发相应的动画

#### 2. 使用 TextOverflow.fit 自动缩放

当文本容器设置了固定大小且溢出模式为 `fit` 时，文本会自动缩放：

```437:467:submodules/rive-runtime/src/text/text.cpp
if (overflow() == TextOverflow::fit)
{
    auto xScale = (effectiveSizing() != TextSizing::autoWidth &&
                   maxWidth > m_bounds.width())
                      ? m_bounds.width() / maxWidth
                      : 1;
    auto yScale = (effectiveSizing() == TextSizing::fixed &&
                   totalHeight > m_bounds.height())
                      ? (m_bounds.height() - baseline) / (totalHeight - baseline)
                      : 1;
    if (xScale != 1 || yScale != 1)
    {
        scale = std::max(0.0f, xScale > yScale ? yScale : xScale);
        // 应用缩放变换...
    }
}
```

**注意**：这是对整个文本块进行缩放变换，而非真正改变字号。

#### 3. 使用多个预设字号的 TextRun

设计时创建多个不同字号的文本元素，运行时通过显示/隐藏来"切换"字号。

---

## Android SDK 暴露的 API

### 文本相关 API

| API | 功能 | 能否修改字号 |
|-----|------|-------------|
| `artboard.textRun(name)` | 获取 TextRun 引用 | ❌ |
| `artboard.getTextRunValue(name)` | 获取文本内容 | ❌ |
| `artboard.setTextRunValue(name, value)` | 设置文本内容 | ❌ |
| `riveTextValueRun.text` | 读写文本内容 | ❌ |

### 使用示例

```kotlin
// 只能修改文本内容，不能修改字号
class DynamicTextActivity : ComponentActivity() {
    private val textRun by lazy {
        animationView.controller.activeArtboard?.textRun("name")
    }

    fun updateText(newText: String) {
        // ✅ 可以修改文本内容
        textRun?.text = newText
        
        // ❌ 无法修改字号（没有这个 API）
        // textRun?.fontSize = 24f  // 不存在
    }
}
```

---

## 字号相关的高级特性

### 1. TextShapeModifier（形状修改器）

Rive 内部有 `TextShapeModifier` 可以在排版时动态修改字号，但这是内部 API：

```9:16:submodules/rive-runtime/include/rive/text/text_shape_modifier.hpp
class TextShapeModifier : public TextShapeModifierBase
{
public:
    virtual float modify(Font* font,
                         std::unordered_map<uint32_t, float>& variations,
                         float fontSize,    // 可以修改字号
                         float strength) const = 0;
};
```

### 2. RawTextInput（原始文本输入）

底层的 `RawTextInput` 类支持动态设置字号：

```37:38:submodules/rive-runtime/include/rive/text/raw_text_input.hpp
float fontSize() const { return m_textRun.size; }
void fontSize(float value);
```

```139:147:submodules/rive-runtime/src/text/raw_text_input.cpp
void RawTextInput::fontSize(float value)
{
    if (m_textRun.size == value)
    {
        return;
    }
    m_textRun.size = value;
    flag(Flags::shapeDirty | Flags::measureDirty | Flags::selectionDirty);
}
```

**注意**：这是 C++ 层的 API，目前**未暴露给 Android SDK**。

### 3. 可变字体轴（Variable Font Axes）

如果使用可变字体，可以通过 `TextVariationModifier` 修改字重等属性：

```7:18:submodules/rive-runtime/src/text/text_variation_modifier.cpp
float TextVariationModifier::modify(
    Font* font,
    std::unordered_map<uint32_t, float>& variations,
    float fontSize,
    float strength) const
{
    // 修改字体变量轴值，但不修改字号
    variations[axisTag()] = fromValue * (1 - strength) + axisValue() * strength;
    return fontSize;  // 字号保持不变
}
```

---

## 总结与建议

### 关键结论

| 问题 | 答案 |
|------|------|
| riv 文件自带字号大小？ | ✅ 是的，存储在 `TextStyle.fontSize` 中，默认值 12 |
| 代码中可以设置字号？ | ❌ 目前 Android SDK 不支持运行时修改字号 |
| 字号可以动画？ | ✅ 是的，在 Rive 编辑器中可以创建字号动画 |

### 官方设计策略

Rive 的设计理念是：

1. **设计时确定**：字号等视觉属性在 Rive 编辑器中设计时确定
2. **动画驱动**：通过动画（时间轴/状态机）来改变字号等属性
3. **运行时轻量**：SDK 只暴露必要的运行时修改能力（如文本内容）

### 如果需要运行时修改字号

1. **推荐**：在 Rive 编辑器中创建不同字号的状态，通过状态机切换
2. **替代**：使用 `TextOverflow.fit` + 容器大小控制来间接控制视觉大小
3. **高级**：如有强需求，可考虑扩展 Android SDK，暴露 C++ 层的 `fontSize` setter

### 扩展 SDK 的可能性

如果需要添加运行时字号修改能力，需要修改：

1. `bindings_text_value_run.cpp` - 添加 JNI 方法
2. `RiveTextValueRun.kt` - 添加 Kotlin 属性
3. 需要访问关联的 `TextStyle` 而非 `TextValueRun` 本身

```cpp
// 示例：需要添加的 JNI 绑定
JNIEXPORT void JNICALL
Java_app_rive_runtime_kotlin_core_RiveTextValueRun_cppSetFontSize(
    JNIEnv* env, jobject, jlong ref, jfloat fontSize) {
    auto* run = reinterpret_cast<rive::TextValueRun*>(ref);
    if (run->style() != nullptr) {
        run->style()->fontSize(fontSize);  // 需要访问关联的 style
    }
}
```

---

## 参考资源

- [Rive Text Documentation](https://rive.app/community/doc/text/docn2E6y1lXo)
- 源码路径：`submodules/rive-runtime/include/rive/text/`
- 示例代码：`app/src/main/java/app/rive/runtime/example/DynamicTextActivity.kt`
