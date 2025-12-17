package app.rive.runtime.example

import android.app.Application
import android.util.Log
import app.rive.runtime.example.font.FontPreloader
import app.rive.runtime.kotlin.fonts.FontFallbackStrategy
import app.rive.runtime.kotlin.fonts.Fonts

/**
 * Rive 示例应用的 Application 类
 * 
 * 主要职责：
 * 1. 在应用启动时预加载字体，避免进入动画页面时卡顿
 * 2. 设置全局的字体回退策略
 * 
 * ## 字体预加载说明
 * 
 * 字体预加载在后台 IO 线程执行，不会阻塞应用启动。
 * 预加载的字体会被缓存，后续使用时直接从缓存读取。
 * 
 * 如果用户在预加载完成前就进入了动画页面：
 * - [FontPreloader.getFontsOrFallback] 会使用同步加载作为回退
 * - 这种情况下仍会有短暂延迟，但属于边缘情况
 * 
 * ## 配置说明
 * 
 * 需要在 AndroidManifest.xml 中注册：
 * ```xml
 * <application
 *     android:name=".RiveExampleApplication"
 *     ...>
 * ```
 */
class RiveExampleApplication : Application() {
    
    companion object {
        private const val TAG = "RiveExampleApp"
    }
    
    override fun onCreate() {
        super.onCreate()
        
        Log.d(TAG, "Application onCreate 开始")
        val startTime = System.currentTimeMillis()
        
        // 1. 启动字体预加载（异步，不阻塞启动）
        preloadFonts()
        
        // 2. 设置全局字体回退策略
        setupFontFallbackStrategy()
        
        Log.d(TAG, "Application onCreate 完成，耗时: ${System.currentTimeMillis() - startTime}ms")
    }
    
    /**
     * 启动字体预加载
     * 
     * 预加载常用字重的字体，包括：
     * - NORMAL (400): 常规文本
     * - BOLD (700): 如需要可添加
     */
    private fun preloadFonts() {
        Log.d(TAG, "启动字体预加载...")
        
        // 预加载常用字重
        FontPreloader.preload(
            weights = listOf(
                Fonts.Weight.NORMAL,  // 400 - 常规
                // 如需要可添加其他字重：
                // Fonts.Weight.BOLD,    // 700 - 粗体
            )
        )
    }
    
    /**
     * 设置全局字体回退策略
     * 
     * 使用预加载的字体作为回退字体源。
     * 这样所有使用 Rive 动画的页面都会自动使用预加载的字体。
     * 
     * ⚠️ 重要：FontFallbackStrategy.stylePicker 使用 WeakReference，
     * 所以必须使用 FontPreloader.fallbackStrategy（一个被强引用的单例对象），
     * 而不是创建新的匿名对象，否则会被 GC 回收导致字体回退失效！
     */
    private fun setupFontFallbackStrategy() {
        FontFallbackStrategy.stylePicker = FontPreloader.fallbackStrategy
        Log.d(TAG, "全局字体回退策略已设置")
    }
}
