package app.rive.runtime.example.font

import android.util.Log
import app.rive.runtime.kotlin.fonts.FontFallbackStrategy
import app.rive.runtime.kotlin.fonts.FontHelper
import app.rive.runtime.kotlin.fonts.Fonts
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 字体预加载器
 * 
 * 在 App 启动时异步预加载字体，避免在 Activity 中同步加载导致卡顿。
 * 
 * ## 使用方式
 * 
 * ### 1. 在 Application.onCreate 中启动预加载
 * ```kotlin
 * class MyApplication : Application() {
 *     override fun onCreate() {
 *         super.onCreate()
 *         FontPreloader.preload()
 *     }
 * }
 * ```
 * 
 * ### 2. 在 Activity 中使用预加载的字体
 * ```kotlin
 * // 方式 A: 异步等待（推荐，在 ViewModel 或协程中使用）
 * val fonts = FontPreloader.awaitFonts(weight)
 * 
 * // 方式 B: 同步获取（如果未完成则使用回退方案）
 * val fonts = FontPreloader.getFontsOrFallback(weight)
 * ```
 * 
 * ## 设计考虑
 * 
 * 1. **结构化并发**：使用 SupervisorJob，单个字体加载失败不影响其他
 * 2. **异常处理**：捕获并记录异常，不会导致 App 崩溃
 * 3. **取消安全**：支持协程取消，App 退出时自动清理
 * 4. **线程安全**：使用 ConcurrentHashMap 和 Mutex 保证并发安全
 * 5. **回退策略**：预加载未完成时提供同步回退方案
 */
object FontPreloader {
    
    private const val TAG = "FontPreloader"
    
    /**
     * 需要预加载的字体配置
     * 
     * 每个配置项包含：
     * - category: 类别标识，用于日志和备用查找
     * - familyName: 字体族名称（如 "sans-serif"）
     * - lang: 语言代码（如 "zh-Hans"），null 表示不限语言
     * 
     * 注意：不同厂商的 ROM 可能有不同的字体文件名，
     * 所以我们按字体族和语言查询，而不是硬编码字体名。
     */
    private data class FontConfig(
        val category: String,       // 类别标识
        val familyName: String?,    // 字体族名称
        val lang: String?,          // 语言代码
    )
    
    private val REQUIRED_FONTS_CONFIG = listOf(
        // 默认字体（拉丁字符：英文、西班牙文等）
        FontConfig(category = "latin", familyName = "sans-serif", lang = null),
        
        // 简体中文
        FontConfig(category = "zh-Hans", familyName = null, lang = "zh-Hans"),
        
        // 韩文
        FontConfig(category = "ko", familyName = null, lang = "ko"),
        
        // 如需支持更多语言，添加配置：
        // FontConfig(category = "zh-Hant", familyName = null, lang = "zh-Hant"),  // 繁体中文
        // FontConfig(category = "ja", familyName = null, lang = "ja"),            // 日文
        // FontConfig(category = "ar", familyName = null, lang = "ar"),            // 阿拉伯文
    )
    
    /**
     * 备用字体名称列表（当按语言查询失败时使用）
     * 
     * 这些是常见的系统字体名称，覆盖大部分 Android 设备。
     * 按优先级排序，每个类别只取第一个存在的字体。
     * 
     * 使用部分匹配（contains）而不是精确匹配，以兼容不同厂商的命名差异。
     */
    private val FALLBACK_FONT_PATTERNS = mapOf(
        // 拉丁字符（按优先级）
        "latin" to listOf(
            "Roboto-Regular",
            "Roboto.ttf",
            "DroidSans.ttf",
            "NotoSans-Regular",
        ),
        // 中日韩字符（按优先级）- 用于 zh-Hans, ko, ja 等
        "zh-Hans" to listOf(
            "NotoSansCJK",           // 标准 CJK 字体
            "NotoSansSC",            // 简体中文
            "NotoSansHans",          // 简体中文变体
            "DroidSansFallback",     // 旧版 Android
            "MiSans",                // 小米
            "HarmonyOS_Sans_SC",     // 华为
        ),
        "ko" to listOf(
            "NotoSansCJK",           // 标准 CJK 字体（包含韩文）
            "NotoSansKR",            // 韩文专用
            "NotoSansKorean",        // 韩文变体
        ),
        "ja" to listOf(
            "NotoSansCJK",           // 标准 CJK 字体（包含日文）
            "NotoSansJP",            // 日文专用
            "NotoSansJapanese",      // 日文变体
        ),
    )
    
    /**
     * 预加载超时时间（毫秒）
     * 如果超时，awaitFonts 会返回空列表，getFontsOrFallback 会使用同步回退
     */
    private const val PRELOAD_TIMEOUT_MS = 10_000L
    
    // ==================== 内部状态 ====================
    
    /** 字体缓存：weight -> fonts */
    private val fontCache = ConcurrentHashMap<Fonts.Weight, List<ByteArray>>()
    
    /** 加载完成信号：weight -> deferred */
    private val loadingSignals = ConcurrentHashMap<Fonts.Weight, CompletableDeferred<List<ByteArray>>>()
    
    /** 是否已启动预加载 */
    private val isPreloading = AtomicBoolean(false)
    
    /** 是否预加载完成 */
    private val isPreloaded = AtomicBoolean(false)
    
    /** 用于保护预加载启动的互斥锁 */
    private val preloadMutex = Mutex()
    
    /** 
     * 协程作用域
     * - SupervisorJob: 子任务失败不会取消其他任务
     * - Dispatchers.IO: 在 IO 线程池执行，不阻塞主线程
     */
    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, throwable ->
            Log.e(TAG, "协程异常", throwable)
        }
    )
    
    // ==================== 公开 API ====================
    
    /**
     * 启动字体预加载
     * 
     * 应在 Application.onCreate 中调用。多次调用是安全的（幂等）。
     * 
     * @param weights 需要预加载的字重列表，默认只加载 NORMAL
     */
    fun preload(weights: List<Fonts.Weight> = listOf(Fonts.Weight.NORMAL)) {
        // 防止重复启动
        if (!isPreloading.compareAndSet(false, true)) {
            Log.d(TAG, "预加载已在进行中，跳过")
            return
        }
        
        Log.d(TAG, "开始预加载字体，weights: $weights")
        val startTime = System.currentTimeMillis()
        
        scope.launch {
            try {
                // 为每个字重启动加载任务
                weights.forEach { weight ->
                    launch {
                        loadFontsForWeight(weight)
                    }
                }
            } finally {
                Log.d(TAG, "预加载完成，耗时: ${System.currentTimeMillis() - startTime}ms")
                isPreloaded.set(true)
            }
        }
    }
    
    /**
     * 异步等待字体加载完成
     * 
     * 适用于可以挂起的场景（如 ViewModel、LaunchedEffect）。
     * 如果预加载未启动，会自动启动预加载。
     * 
     * @param weight 字重
     * @param timeoutMs 超时时间，默认使用 PRELOAD_TIMEOUT_MS
     * @return 字体字节数组列表，超时或失败返回空列表
     */
    suspend fun awaitFonts(
        weight: Fonts.Weight = Fonts.Weight.NORMAL,
        timeoutMs: Long = PRELOAD_TIMEOUT_MS
    ): List<ByteArray> {
        // 如果已缓存，直接返回
        fontCache[weight]?.let { return it }
        
        // 确保预加载已启动
        ensurePreloadStarted(weight)
        
        // 等待加载完成（带超时）
        val signal = loadingSignals[weight] ?: return emptyList()
        
        return withTimeoutOrNull(timeoutMs) {
            try {
                signal.await()
            } catch (e: CancellationException) {
                Log.w(TAG, "等待字体加载被取消")
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "等待字体加载失败", e)
                emptyList()
            }
        } ?: run {
            Log.w(TAG, "等待字体加载超时 (${timeoutMs}ms)")
            emptyList()
        }
    }
    
    /**
     * 同步获取字体，未完成时使用回退方案
     * 
     * 适用于不能挂起的场景（如 FontFallbackStrategy.getFont）。
     * 
     * @param weight 字重
     * @return 字体字节数组列表
     */
    fun getFontsOrFallback(weight: Fonts.Weight = Fonts.Weight.NORMAL): List<ByteArray> {
        // 优先使用缓存
        fontCache[weight]?.let { 
            Log.d(TAG, "使用缓存的字体 (weight=$weight)")
            return it 
        }
        
        // 缓存未命中，使用同步回退方案
        Log.w(TAG, "字体缓存未命中，使用同步回退加载 (weight=$weight)")
        return loadFontsSync(weight)
    }
    
    /**
     * 检查指定字重的字体是否已加载完成
     */
    fun isLoaded(weight: Fonts.Weight = Fonts.Weight.NORMAL): Boolean {
        return fontCache.containsKey(weight)
    }
    
    /**
     * 检查所有预加载是否完成
     */
    fun isFullyLoaded(): Boolean = isPreloaded.get()
    
    /**
     * 清除缓存（用于测试或内存回收）
     */
    fun clearCache() {
        fontCache.clear()
        loadingSignals.clear()
        isPreloading.set(false)
        isPreloaded.set(false)
        Log.d(TAG, "字体缓存已清除")
    }
    
    /**
     * 全局字体回退策略实例
     * 
     * ⚠️ 重要：FontFallbackStrategy.stylePicker 使用 WeakReference，
     * 必须持有策略对象的强引用，否则会被 GC 回收导致字体回退失效！
     * 
     * 使用方式：
     * ```kotlin
     * FontFallbackStrategy.stylePicker = FontPreloader.fallbackStrategy
     * ```
     */
    val fallbackStrategy: FontFallbackStrategy = object : FontFallbackStrategy {
        override fun getFont(weight: Fonts.Weight): List<ByteArray> {
            return getFontsOrFallback(weight)
        }
    }
    
    /**
     * 创建一个使用预加载字体的 FileAssetLoader
     * 
     * ⚠️ 重要：这是一个静态类，不持有 Activity 引用，避免内存泄漏。
     * 
     * 当 Rive 文件中的字体资源缺失时（Out-of-Band 字体），
     * 使用此加载器提供预加载的系统默认字体作为基础字体。
     * 
     * 使用方式：
     * ```kotlin
     * riveView.setAssetLoader(FontPreloader.createFontAssetLoader())
     * ```
     */
    fun createFontAssetLoader(): app.rive.runtime.kotlin.core.FileAssetLoader {
        return FontAssetLoader()
    }
    
    /**
     * 字体资源加载器（静态类，不持有 Activity 引用）
     */
    private class FontAssetLoader : app.rive.runtime.kotlin.core.FileAssetLoader() {
        override fun loadContents(
            asset: app.rive.runtime.kotlin.core.FileAsset,
            inBandBytes: ByteArray
        ): Boolean {
            if (asset is app.rive.runtime.kotlin.core.FontAsset) {
                Log.d(TAG, "加载 OOB 字体资源: ${asset.name}")
                
                // 优先使用预加载的字体（第一个作为基础字体）
                val preloadedFonts = getFontsOrFallback(Fonts.Weight.NORMAL)
                if (preloadedFonts.isNotEmpty()) {
                    val fontBytes = preloadedFonts.first()
                    Log.d(TAG, "使用预加载的字体: ${fontBytes.size} bytes")
                    return asset.decode(fontBytes)
                }
                
                // 如果预加载失败，尝试系统默认字体
                val fontBytes = FontHelper.getFallbackFontBytes(Fonts.FontOpts.DEFAULT)
                if (fontBytes != null) {
                    Log.d(TAG, "使用系统默认字体: ${fontBytes.size} bytes")
                    return asset.decode(fontBytes)
                }
                
                Log.e(TAG, "无法获取任何字体!")
                return false
            }
            return false
        }
    }
    
    // ==================== 内部方法 ====================
    
    /**
     * 确保预加载已启动
     */
    private suspend fun ensurePreloadStarted(weight: Fonts.Weight) {
        preloadMutex.withLock {
            if (!loadingSignals.containsKey(weight)) {
                // 创建加载信号
                loadingSignals[weight] = CompletableDeferred()
                
                // 启动加载
                scope.launch {
                    loadFontsForWeight(weight)
                }
            }
        }
    }
    
    /**
     * 加载指定字重的字体（异步）
     */
    private suspend fun loadFontsForWeight(weight: Fonts.Weight) {
        val signal = loadingSignals.getOrPut(weight) { CompletableDeferred() }
        
        // 如果已经完成，直接返回
        if (signal.isCompleted) return
        
        try {
            val startTime = System.currentTimeMillis()
            
            val fonts = withContext(Dispatchers.IO) {
                loadFontsInternal(weight)
            }
            
            // 存入缓存
            fontCache[weight] = fonts
            
            Log.d(TAG, "字体加载完成: weight=$weight, count=${fonts.size}, " +
                    "耗时=${System.currentTimeMillis() - startTime}ms")
            
            // 通知等待者
            signal.complete(fonts)
            
        } catch (e: CancellationException) {
            Log.w(TAG, "字体加载被取消: weight=$weight")
            signal.cancel(e)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "字体加载失败: weight=$weight", e)
            signal.completeExceptionally(e)
        }
    }
    
    /**
     * 实际加载字体的内部方法
     * 
     * 加载策略：
     * 1. 按 REQUIRED_FONTS_CONFIG 中的配置查询字体
     * 2. 每个配置只取第一个匹配的字体（系统最优先的）
     * 3. 如果查询失败，使用 FALLBACK_FONT_NAMES 兜底
     */
    private fun loadFontsInternal(weight: Fonts.Weight): List<ByteArray> {
        val loadedFonts = mutableListOf<ByteArray>()
        val loadedFontNames = mutableSetOf<String>()
        
        // 获取所有系统字体列表
        val allFontFamilies = FontHelper.getSystemFontList()
        
        // 1. 按配置加载字体
        for (config in REQUIRED_FONTS_CONFIG) {
            val font = findFont(allFontFamilies, config, weight)
            if (font != null && font.name !in loadedFontNames) {
                try {
                    FontHelper.getFontBytes(font)?.let { bytes ->
                        loadedFonts.add(bytes)
                        loadedFontNames.add(font.name)
                        Log.d(TAG, "加载字体 [${config.category}]: ${font.name} (${bytes.size} bytes)")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "加载字体失败 [${config.category}]: ${font.name}", e)
                }
            } else if (font == null) {
                Log.w(TAG, "未找到字体 [${config.category}]: family=${config.familyName}, lang=${config.lang}")
                // 尝试使用备用字体
                loadFallbackFont(config.category, weight, loadedFonts, loadedFontNames)
            }
        }
        
        // 2. 确保至少有一个拉丁字体
        if (loadedFonts.isEmpty()) {
            Log.w(TAG, "没有加载任何字体，尝试使用系统默认字体")
            FontHelper.getFallbackFontBytes(Fonts.FontOpts.DEFAULT)?.let { bytes ->
                loadedFonts.add(bytes)
                Log.d(TAG, "使用系统默认字体: ${bytes.size} bytes")
            }
        }
        
        Log.d(TAG, "字体加载完成: 共 ${loadedFonts.size} 个字体")
        return loadedFonts
    }
    
    /**
     * 根据配置查找字体
     * 
     * 语言匹配规则（兼容不同 ROM）：
     * - "zh-Hans" 匹配 "zh-Hans", "zh-CN", "zh" 等
     * - "ko" 匹配 "ko", "ko-KR" 等
     * - "und-Hans" (无语言但有简体中文脚本) 也会被匹配
     */
    private fun findFont(
        families: List<Fonts.Family>,
        config: FontConfig,
        weight: Fonts.Weight
    ): Fonts.Font? {
        // 筛选匹配的字体族
        val matchingFamilies = families.filter { family ->
            val familyMatch = when {
                // 如果指定了字体族名称，精确匹配
                config.familyName != null -> family.name.equals(config.familyName, ignoreCase = true)
                // 否则不限制字体族
                else -> true
            }
            
            val langMatch = when {
                // 如果指定了语言，进行智能匹配
                config.lang != null -> matchLanguage(family.lang, config.lang)
                // 如果没指定语言，只匹配没有语言标记的字体族
                else -> family.lang == null
            }
            
            familyMatch && langMatch
        }
        
        if (matchingFamilies.isEmpty()) {
            Log.d(TAG, "未找到匹配的字体族: category=${config.category}, lang=${config.lang}")
        }
        
        // 从匹配的字体族中取第一个指定字重的字体
        for (family in matchingFamilies) {
            val fonts = family.fonts[weight]
            if (!fonts.isNullOrEmpty()) {
                return fonts.first()
            }
        }
        
        // 如果没有精确匹配的字重，取任意字重的第一个字体
        for (family in matchingFamilies) {
            val fonts = family.fonts.values.flatten()
            if (fonts.isNotEmpty()) {
                return fonts.first()
            }
        }
        
        return null
    }
    
    /**
     * 语言匹配逻辑
     * 
     * 兼容不同 ROM 的语言标记格式：
     * - 标准格式: zh-Hans, zh-Hant, ko, ja
     * - 地区格式: zh-CN, zh-TW, ko-KR, ja-JP
     * - 脚本格式: und-Hans (无语言但有脚本)
     */
    private fun matchLanguage(familyLang: String?, targetLang: String): Boolean {
        if (familyLang == null) return false
        
        val familyLangLower = familyLang.lowercase()
        val targetLangLower = targetLang.lowercase()
        
        // 精确匹配
        if (familyLangLower == targetLangLower) return true
        
        // 提取语言代码和脚本
        val targetParts = targetLangLower.split("-")
        val targetLangCode = targetParts.first()  // zh, ko, ja
        val targetScript = targetParts.getOrNull(1)  // Hans, Hant, null
        
        val familyParts = familyLangLower.split("-")
        val familyLangCode = familyParts.first()  // zh, ko, ja, und
        val familyScript = familyParts.getOrNull(1)  // Hans, Hant, CN, KR, null
        
        // 语言代码匹配（zh 匹配 zh-CN, zh-Hans 等）
        if (familyLangCode == targetLangCode) return true
        
        // 脚本匹配（处理 und-Hans 等情况）
        if (targetScript != null && familyScript != null) {
            if (familyScript.equals(targetScript, ignoreCase = true)) return true
        }
        
        // 中文特殊处理：zh-Hans 匹配 zh-CN，zh-Hant 匹配 zh-TW
        if (targetLangCode == "zh") {
            when (targetScript?.lowercase()) {
                "hans" -> {
                    // 简体中文：匹配 zh-CN, zh-SG, und-Hans
                    if (familyLangCode == "und" && familyScript?.lowercase() == "hans") return true
                    if (familyLangCode == "zh" && familyScript?.lowercase() in listOf("cn", "sg")) return true
                }
                "hant" -> {
                    // 繁体中文：匹配 zh-TW, zh-HK, und-Hant
                    if (familyLangCode == "und" && familyScript?.lowercase() == "hant") return true
                    if (familyLangCode == "zh" && familyScript?.lowercase() in listOf("tw", "hk")) return true
                }
            }
        }
        
        return false
    }
    
    /**
     * 加载备用字体
     * 
     * 使用部分匹配（contains）查找字体，以兼容不同厂商的命名差异。
     */
    private fun loadFallbackFont(
        category: String,
        weight: Fonts.Weight,
        loadedFonts: MutableList<ByteArray>,
        loadedFontNames: MutableSet<String>
    ) {
        val fallbackPatterns = FALLBACK_FONT_PATTERNS[category] ?: return
        val allFonts = FontHelper.getFallbackFonts(Fonts.FontOpts(weight = weight))
        
        Log.d(TAG, "尝试加载备用字体: category=$category, 可用字体数=${allFonts.size}")
        
        for (pattern in fallbackPatterns) {
            // 使用部分匹配，忽略大小写
            val font = allFonts.find { it.name.contains(pattern, ignoreCase = true) }
            if (font != null && font.name !in loadedFontNames) {
                try {
                    FontHelper.getFontBytes(font)?.let { bytes ->
                        loadedFonts.add(bytes)
                        loadedFontNames.add(font.name)
                        Log.d(TAG, "使用备用字体: $category -> ${font.name} (${bytes.size} bytes)")
                        return  // 找到一个就够了
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "备用字体加载失败: ${font.name}", e)
                }
            }
        }
        
        Log.w(TAG, "未找到任何备用字体: category=$category")
    }
    
    /**
     * 同步加载字体（回退方案）
     */
    private fun loadFontsSync(weight: Fonts.Weight): List<ByteArray> {
        val startTime = System.currentTimeMillis()
        
        val fonts = loadFontsInternal(weight)
        
        // 存入缓存供后续使用
        fontCache[weight] = fonts
        
        Log.d(TAG, "同步加载字体完成: weight=$weight, count=${fonts.size}, " +
                "耗时=${System.currentTimeMillis() - startTime}ms")
        
        return fonts
    }
}
