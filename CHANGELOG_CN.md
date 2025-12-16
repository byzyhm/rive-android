# 版本更新日志

本文件记录了该项目的所有重要更改。日期以 UTC 时区显示。

由 [`auto-changelog`](https://github.com/CookPete/auto-changelog) 自动生成。

---

## 版本总览

| 版本 | 日期 | 主要更新 |
|------|------|----------|
| **10.5.x** | 2025年11月 | Compose 改进、剪切优化 |
| **10.4.x** | 2025年8月-10月 | Data Binding、多点触控、Artboard 生命周期修复 |
| **10.3.x** | 2025年7月-8月 | 脚本 API 重构、生命周期内存管理 |
| **10.0.0** | 2025年2月 | **重大更新**：移除 Skia 渲染器 |
| **9.x** | 2024-2025年 | 字体回退、布局支持、事件系统 |
| **8.x** | 2023年 | PLS 渲染器集成、Text Run API |
| **7.x** | 2023年7月 | Rive Renderer 引入 |

---

## 最新版本

### [10.5.3](https://github.com/rive-app/rive-android/compare/10.5.2...10.5.3)

- **重构(Android)**: Compose 相关改进 (#11057)
- **修复(EA)**: 列表索引可与数字比较 (#11194)
- **功能**: 将剪切应用为单独的 drawable (#11183)

### [10.5.2](https://github.com/rive-app/rive-android/compare/10.5.1...10.5.2)

> 2025年11月27日

- **修复(android)**: 画板调整大小时崩溃 (#11176)
- **修复**: 将 `stylePicker` 存储到变量以防止 GC 回收 (#11079)

### [10.5.1](https://github.com/rive-app/rive-android/compare/10.5.0...10.5.1)

> 2025年11月6日

- **样式(Android)**: 修复多个警告和代码风格问题 (#10886)
- **重构(Android)**: 构建系统重构 (#10938)

### [10.5.0](https://github.com/rive-app/rive-android/compare/10.4.5...10.5.0)

> 2025年10月24日

- **功能(Android)**: 处理解码后的图像 (#10755)
- **功能(Android)**: 多点触控和指针退出支持 (#10848)

---

## 10.4.x 系列

### [10.4.5](https://github.com/rive-app/rive-android/compare/10.4.4...10.4.5)

> 2025年10月8日

- 数据绑定画板 rcp file (#10214)
- **修复(runtime)**: 在 ForegroundLayoutDrawable 上使描边效果失效 (#10733)

### [10.4.4](https://github.com/rive-app/rive-android/compare/10.4.3...10.4.4)

> 2025年9月12日

- 修复 D3D 警告 (#10580)
- **功能**: 脚本防止无限执行 (#10570)

### [10.4.3](https://github.com/rive-app/rive-android/compare/10.4.2...10.4.3)

> 2025年9月3日

- **修复(Android)**: RiveArtboardRenderer 和 Artboard 生命周期修复 (#10496) ⭐
- **功能**: 在画板初始化时添加自动生成的状态机 (#10420)

### [10.4.2](https://github.com/rive-app/rive-android/compare/10.4.1...10.4.2)

> 2025年8月22日

- Artboard 列表项跟随路径 (#10417)
- **功能(webgpu)**: 添加 webgpu2 API 支持 (#10423)

### [10.4.1](https://github.com/rive-app/rive-android/compare/10.4.0...10.4.1)

> 2025年8月12日

- **修复**: 在不阻塞 UI 线程的情况下释放渲染器 (#10331) ⭐
- **功能**: 脚本化数据输入 (#10339)

### [10.4.0](https://github.com/rive-app/rive-android/compare/10.3.3...10.4.0)

> 2025年8月7日

- **功能(Android)**: 图像、列表和画板数据绑定 (#10052) ⭐

---

## 10.3.x 系列

### [10.3.3](https://github.com/rive-app/rive-android/compare/10.3.2...10.3.3)

> 2025年8月6日

- 修复嵌套事件与父事件冲突 (#10326)
- 添加 ViewModel 触发器监听器支持 (#10323)

### [10.3.2](https://github.com/rive-app/rive-android/compare/10.3.1...10.3.2)

> 2025年8月4日

- **修复**: API <= 25 的函数可见性修饰符

### [10.3.1](https://github.com/rive-app/rive-android/compare/10.3.0...10.3.1)

> 2025年7月28日

- **重构**: 脚本 API 重构 (#10218)
- **修复**: 生命周期内存管理 (#10237) ⭐

### [10.3.0](https://github.com/rive-app/rive-android/compare/10.2.1...10.3.0)

> 2025年7月21日

- **修复**: 从 OSSRH 迁移到 Maven Central Portal 恢复发布 (#10120)
- **功能(CommandQueue)**: 多个小功能添加 (#10215)

---

## 10.2.x 系列

### [10.2.1](https://github.com/rive-app/rive-android/compare/10.2.0...10.2.1)

> 2025年7月1日

- **修复**: 使用正确的字节数组初始化字体 (#10059)

### [10.2.0](https://github.com/rive-app/rive-android/compare/10.1.8...10.2.0)

> 2025年6月23日

---

## 10.1.x 系列

### [10.1.8](https://github.com/rive-app/rive-android/compare/10.1.7...10.1.8)

> 2025年6月23日

- **修复(parser)**: 未命名字体族不继承名称 (#9791)

### [10.1.7](https://github.com/rive-app/rive-android/compare/10.1.6...10.1.7)

> 2025年6月12日

- **修复(webgpu)**: 不分配不必要的纹理 (#9909)

### [10.1.6](https://github.com/rive-app/rive-android/compare/10.1.5...10.1.6)

> 2025年6月5日

- **功能(android)**: 添加触摸穿透到视图 (#9865)

### [10.1.5](https://github.com/rive-app/rive-android/compare/10.1.4...10.1.5)

> 2025年6月3日

- **修复(ci)**: 使 Activity 生命周期测试对分离时的竞态条件更健壮 (#9771)

### [10.1.4](https://github.com/rive-app/rive-android/compare/10.1.3...10.1.4)

> 2025年5月10日

- **修复(gl)**: 修复未初始化的像素本地存储 (#9638)
- 修复 Artboard List 崩溃问题 (#9633)
- **功能**: CommandQueue 的第一版草案 (#9620)

### [10.1.3](https://github.com/rive-app/rive-android/compare/10.1.2...10.1.3)

> 2025年5月7日

- **修复(renderer)**: 优雅处理空图像纹理 (#9600)

### [10.1.2](https://github.com/rive-app/rive-android/compare/10.1.1...10.1.2)

> 2025年5月2日

- **修复(playback)**: 当没有 StateMachineInstance 播放时，LinearAnimationInstance 推进 Artboard

### [10.1.1](https://github.com/rive-app/rive-android/compare/10.1.0...10.1.1)

> 2025年5月1日

- **功能(fonts)**: 实现索引字体回退策略和 `getFallbackFonts()` API ⭐

### [10.1.0](https://github.com/rive-app/rive-android/compare/10.0.5...10.1.0)

> 2025年4月16日

- **Android Data Binding** ⭐
- **修复**: 数据转换器范围映射参数顺序

---

## 10.0.x 系列

### [10.0.5](https://github.com/rive-app/rive-android/compare/10.0.4...10.0.5)

> 2025年4月8日

- **修复(Android)**: 渲染线程崩溃
- 修复 GL 缓冲区竞态条件

### [10.0.4](https://github.com/rive-app/rive-android/compare/10.0.3...10.0.4)

> 2025年4月3日

- **功能(Android)**: 移除 Kotlin Reflect 并降级到 Kotlin 1.9
- Libraries 功能

### [10.0.3](https://github.com/rive-app/rive-android/compare/10.0.2...10.0.3)

> 2025年3月24日

- 更多 Vulkan 引导清理
- 减少 Vulkan 上下文创建所需的参数数量

### [10.0.2](https://github.com/rive-app/rive-android/compare/10.0.1...10.0.2)

> 2025年3月10日

- **修复**: 移除 printmapping ProGuard 规则

### [10.0.1](https://github.com/rive-app/rive-android/compare/10.0.0...10.0.1)

> 2025年2月28日

- 当路径改变时使描边效果失效

### [10.0.0](https://github.com/rive-app/rive-android/compare/9.13.10...10.0.0) 🎉 主版本

> 2025年2月27日

- **从 Android 运行时移除 Skia** ⭐⭐⭐
- 最终 Skia 移除
- 添加 RenderPath::addRawPath

---

## 9.x 系列亮点

### 9.13.x (2024年12月 - 2025年1月)

- Feathers 功能支持
- 每个状态机层使用触发器
- 图像缩放修复（mesh）

### 9.12.x (2024年11月)

- 支持按样式提供移动端回退字体并缓存 ⭐
- 解决线程问题

### 9.11.x (2024年11月)

- JNI 检查和重新抛出
- 状态机状态改变时返回 keep going

### 9.10.x (2024年10月-11月)

- 运行时布局适配类型
- 布局修复
- ViewModel 和数据枚举系统核心对象

### 9.9.x (2024年9月-10月)

- 嵌套 Text Run 支持 ⭐
- 运行时虚线功能
- 暴露 Image Asset 宽度和高度

### 9.8.0 (2024年9月)

- **功能**: Android 嵌套 Text Run ⭐

### 9.7.0 (2024年9月)

- **Android 字体回退功能** ⭐⭐

### 9.6.x (2024年7-8月)

- 更好的堆栈跟踪和测试
- 暴露 Artboard 音量 getter/setter
- 嵌套 Text Run getter 和 setter

---

## 8.x 系列亮点

### 8.6.0 (2023年10月)

- **Android Out of Band Assets** ⭐

### 8.5.0 (2023年10月)

- 当 PLS 不支持时回退到 Skia

### 8.4.0 (2023年9月)

- **Android Events 支持** ⭐

### 8.3.0 (2023年9月)

- 帧和时间 API
- 实现 PLS 中的图像网格

### 8.2.0 (2023年8月)

- 在 PLS 中重用剪切内容

### 8.1.0 (2023年7月)

- **Text Run 绑定和 API** ⭐
- 压力测试活动

### 8.0.0 (2023年7月) 🎉 主版本

- Thumbnailer 文本更新

---

## 7.x 系列亮点

### 7.0.0 (2023年7月) 🎉 主版本

- **PLS Android 集成** ⭐⭐
- Rive Renderer Ref

---

## 更早版本

### 6.0.0 (2023年7月)

- 将运行时目录设置为 CI 环境变量

### 5.x (2023年5-6月)

- `play()` 函数重启暂停的动画
- 使用生命周期观察者在销毁时清理资源
- EGL 和 Skia 上下文共享
- 切换到单工作线程

### 4.x (2022-2023年)

- Android 内存增强
- 控制器增强和修复
- 引用计数机制改进

---

## 注意事项

⭐ = 重要功能/修复  
⭐⭐ = 非常重要的更新  
⭐⭐⭐ = 里程碑式更新

完整的详细日志请参阅 [CHANGELOG.md](./CHANGELOG.md) 英文原版。

---

**文档版本**：基于 CHANGELOG.md 翻译并整理  
**最后更新**：2024-12-16

