# Rive Android UI 集成

本文档介绍 Rive Android 与各种 UI 组件的集成方式。

> 📚 返回 [Demo Activities 完整指南](./demo_activities_guide.md)

---

## 9. RiveFragmentActivity

### 📝 描述
在 Fragment 中使用 RiveAnimationView。

### 🔧 使用的 API

| API | 说明 |
|-----|------|
| `bundleOf` | 传递参数 |
| `supportFragmentManager.commit` | Fragment 事务 |

### ✅ 支持的功能
- Fragment 封装 Rive 动画
- 参数传递

### 🎯 使用场景
- 模块化 UI
- 可复用的动画组件
- Navigation 组件集成

### 💻 示例代码

```kotlin
// Fragment
class RiveFragment : Fragment() {
    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        riveView.setRiveResource(resId)
        riveView.fit = Fit.COVER
    }
}

// Activity
supportFragmentManager.commit {
    add<RiveFragment>(R.id.container, args = bundleOf(
        RIVE_FRAGMENT_ARG_RES_ID to R.raw.animation
    ))
}
```

---

## 19. RecyclerActivity

### 📝 描述
在 RecyclerView 中高效使用 Rive 动画。

### 🔧 使用的 API

| API | 说明 |
|-----|------|
| `File` (共享) | 共享 Rive 文件实例 |
| `setRiveFile` | 使用共享文件 |
| `saveControllerState` | 保存控制器状态 |
| `restoreControllerState` | 恢复控制器状态 |
| `ControllerStateManagement` | 状态管理注解 |

### ✅ 支持的功能
- ViewHolder 复用
- 动画状态保存/恢复
- 共享 Rive 文件减少内存

### 🎯 使用场景
- 列表中的动画
- 瀑布流动画
- 高性能列表

### 💻 示例代码

```kotlin
@ControllerStateManagement
class RiveAdapter(private val sharedFile: File) : ListAdapter<...>() {
    val resourceCache = arrayOfNulls<ControllerState>(200)

    override fun onViewAttachedToWindow(holder: RiveViewHolder) {
        resourceCache[position]?.let { savedState ->
            holder.riveView.restoreControllerState(savedState)
        } ?: holder.riveView.setRiveFile(sharedFile)
    }

    override fun onViewDetachedFromWindow(holder: RiveViewHolder) {
        resourceCache[position] = holder.riveView.saveControllerState()
    }
}
```

---

## 20. ViewPagerActivity

### 📝 描述
在 ViewPager2 中使用 Rive 动画。

### 🔧 使用的 API

| API | 说明 |
|-----|------|
| `saveControllerState` | 保存状态 |
| `restoreControllerState` | 恢复状态 |
| `offscreenPageLimit` | 预加载页数 |

### ✅ 支持的功能
- 页面切换动画
- 状态保存/恢复
- 预加载优化

### 🎯 使用场景
- 引导页
- 轮播动画
- 画廊展示

---

## 21. MeshesActivity

### 📝 描述
展示网格变形动画。

### 🎯 使用场景
- 角色动画
- 变形效果

---

## 22. ViewStubActivity

### 📝 描述
使用 ViewStub 延迟加载 Rive 动画。

### 🔧 使用的 API

| API | 说明 |
|-----|------|
| `ViewStub.visibility` | 触发加载 |
| `setOnInflateListener` | 加载完成回调 |

### ✅ 支持的功能
- 延迟加载动画
- 显示/隐藏控制

### 🎯 使用场景
- 条件性显示动画
- 优化首次加载

### 💻 示例代码

```kotlin
viewStub.setOnInflateListener { _, _ ->
    supportFragmentManager.commit {
        add<RiveFragment>(R.id.container)
    }
}

// 显示
viewStub.visibility = View.VISIBLE
// 隐藏
viewStub.visibility = View.GONE
```

---

## 23. LegacyComposeActivity

### 📝 描述
使用 AndroidView 在 Compose 中嵌入 RiveAnimationView（传统方式）。

### 🔧 使用的 API

| API | 说明 |
|-----|------|
| `AndroidView` | Compose 中嵌入 View |
| `RiveAnimationView.setRiveResource` | 设置资源 |

### ✅ 支持的功能
- Compose 集成（传统方式）
- View 级别控制

### 🎯 使用场景
- 渐进式 Compose 迁移
- 需要 View API 的场景

### 💻 示例代码

```kotlin
@Composable
fun CustomRiveAnimationView(@RawRes animation: Int) {
    AndroidView(
        factory = { context ->
            RiveAnimationView(context).also {
                it.setRiveResource(resId = animation)
            }
        }
    )
}
```

---

## 24. FrameActivity

### 📝 描述
Fragment 切换示例，展示动画在 Fragment 生命周期中的行为。

### 🔧 使用的 API

| API | 说明 |
|-----|------|
| `supportFragmentManager.commit` | Fragment 事务 |
| `replace` | 替换 Fragment |

### ✅ 支持的功能
- Fragment 切换
- 动画生命周期管理

### 🎯 使用场景
- 页面切换动画
- Fragment 导航

---

*返回 [Demo Activities 完整指南](./demo_activities_guide.md)*

