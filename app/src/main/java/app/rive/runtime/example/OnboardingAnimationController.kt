package app.rive.runtime.example

import android.graphics.Paint
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.util.Log
import app.rive.runtime.kotlin.RiveAnimationView

/**
 * Onboarding 动画控制器
 *
 * 封装所有与 Rive 动画的交互逻辑，控制 onboarding_part_1.riv 的播放
 *
 * 动画流程：
 * 1. state=0: 消息气泡弹出动画
 * 2. state>=1: 第一条消息翻译动画
 * 3. state>=2: 第二条消息翻译动画 (+0.2s 延迟)
 * 4. state>=3: 第三条消息翻译动画 (+0.2s 延迟)
 */
class OnboardingAnimationController(
    private val riveView: RiveAnimationView
) {
    companion object {
        private const val TAG = "OnboardingAnimation"

        // 状态机名称（从日志确认：State Machine 1，有空格）
        private const val STATE_MACHINE_NAME = "StateMachine_1"

        // Number 输入参数名称（从截图确认，使用空格）
        private const val INPUT_STATE = "state"
        private const val INPUT_LENGTH_1 = "length_1"
        private const val INPUT_LENGTH_2 = "length_2"
        private const val INPUT_LENGTH_3 = "length_3"

        // Text Run 名称（需要设计师确认确切名称，先尝试空格版本）
        private const val TEXT_CONTENT_1 = "Content_1"
        private const val TEXT_CONTENT_2 = "Content_2"
        private const val TEXT_CONTENT_3 = "Content_3"
        private const val TEXT_TRANSLATED_1 = "Translated_1"
        private const val TEXT_TRANSLATED_2 = "Translated_2"
        private const val TEXT_TRANSLATED_3 = "Translated_3"

        // 消息弹出动画时长（约 1.5 秒后开始翻译）
        private const val POPUP_ANIMATION_DELAY_MS = 1500L
        
        // 翻译动画之间的延迟时间（设计要求 0.2s）
        private const val TRANSLATION_DELAY_MS = 200L
        
        // Length 计算系数
        // 根据截图：英文每个字符约 9-10 像素宽度（在 Rive 设计尺寸中）
        // "Hi! Welcome to Intent." (22字符) → length ≈ 200
        private const val LENGTH_PIXELS_PER_CHAR = 9f

        // 需要特殊处理的语言（目标语言是这些时，原文内容需要切换）
        private val SPECIAL_LANGUAGES = setOf("es", "ko", "zh-cn", "zh-hans", "zh_cn")
    }

    private val handler = Handler(Looper.getMainLooper())

    // 用于测量文本像素宽度的 Paint
    @Suppress("DEPRECATION")
    private val textPaint by lazy {
        Paint().apply {
            textSize = 16f * riveView.resources.displayMetrics.scaledDensity
            typeface = Typeface.DEFAULT
        }
    }

    // 使用设计师确认的状态机名称
    private val stateMachineName: String = STATE_MACHINE_NAME
    
    // ✅ 新增：标志位，防止释放后继续执行
    @Volatile
    private var isReleased = false

    /**
     * 初始化动画 - 设置所有文本内容和长度参数
     *
     * @param data 翻译数据
     */
    fun initialize(data: TranslationData) {
        Log.d(TAG, "Initializing with state machine: $stateMachineName")
        // ❌ 删除：riveView.isOpaque = false
        // 原因：isOpaque 应该在 View 初始化时设置，不应该在这里重复设置
        // 这个属性会影响 TextureView 的渲染，重复设置可能导致全局渲染问题
        
        // 1. 设置原文内容（根据目标语言选择不同的原文）
        setOriginalContents(data.targetLanguage)

        // 2. 设置翻译后的内容
        setTranslatedContents(data.contents)

        // 3. 设置长度参数（像素宽度）
        setLengthInputs(data.contents)

        // 4. 确保 state = 0（初始状态，播放消息弹出动画）
        setStateValue(0f)

        Log.d(TAG, "Animation initialized with ${data.contents.size} messages")
    }

    /**
     * 开始播放翻译动画序列（立即执行）
     *
     * 调用时机：当消息弹出动画完成后，需要开始翻译效果时调用
     * 
     * ✅ 修复：添加 isReleased 检查，防止释放后继续执行
     */
    fun playTranslationSequence() {
        if (isReleased) {
            Log.w(TAG, "Controller已释放，跳过翻译动画")
            return
        }
        
        Log.d(TAG, "Starting translation sequence...")

        // 第一条翻译动画
        setStateValue(1f)

        // 第二条翻译动画（延迟 0.2s）
        handler.postDelayed({
            if (!isReleased) {
                setStateValue(2f)
            }
        }, TRANSLATION_DELAY_MS)

        // 第三条翻译动画（再延迟 0.2s，累计 0.4s）
        handler.postDelayed({
            if (!isReleased) {
                setStateValue(3f)
            }
        }, TRANSLATION_DELAY_MS * 2)
    }
    
    /**
     * 自动播放完整动画序列
     * 
     * 时序：
     * - 0s: 消息弹出动画开始 (state=0)
     * - 1.5s: 第一条翻译 (state=1)
     * - 1.7s: 第二条翻译 (state=2)
     * - 1.9s: 第三条翻译 (state=3)
     * 
     * ✅ 修复：添加 isReleased 检查
     */
    fun playFullSequence() {
        if (isReleased) {
            Log.w(TAG, "Controller已释放，跳过完整动画序列")
            return
        }
        
        Log.d(TAG, "Starting full animation sequence...")
        
        // state=0 已在 initialize 中设置，消息弹出动画自动播放
        
        // 1.5秒后开始翻译序列
        handler.postDelayed({
            if (!isReleased) {
                playTranslationSequence()
            }
        }, POPUP_ANIMATION_DELAY_MS)
    }

    /**
     * 重置动画到初始状态
     * 
     * ✅ 修复：移除所有回调，防止泄漏
     */
    fun reset() {
        Log.d(TAG, "Resetting animation...")
        
        // 移除所有延迟任务（重要：防止 Handler 泄漏）
        handler.removeCallbacksAndMessages(null)
        
        // 只在未释放时重置状态
        if (!isReleased) {
            setStateValue(0f)
            Log.d(TAG, "Animation reset to initial state (state=0)")
        } else {
            Log.w(TAG, "Controller已释放，跳过状态重置")
        }
    }

    /**
     * 释放资源
     * 
     * ✅ 改进：设置释放标志，防止后续操作
     * 
     * 调用时机：
     * - Activity onStop 时
     * - Activity onDestroy 时（双重保险）
     */
    fun release() {
        Log.d(TAG, "Releasing controller resources...")
        
        // 设置释放标志
        isReleased = true
        
        // 移除所有 Handler 回调（重要：防止内存泄漏）
        handler.removeCallbacksAndMessages(null)
        
        Log.d(TAG, "Controller resources released")
    }

    /**
     * 调试：打印 Rive 文件信息
     * 
     * ⚠️ 重要：使用安全的只读方式获取信息
     * 不要调用 file.artboard()，因为那会创建新实例并添加到 file.dependencies
     * 改用 controller.activeArtboard（由 controller 管理生命周期）
     */
    fun printDebugInfo() {
        Log.d(TAG, "========== Rive File Info ==========")
        
        val file = riveView.controller.file
        if (file == null) {
            Log.e(TAG, "File not loaded yet!")
            Log.d(TAG, "========== End of Info ==========")
            return
        }

        // 使用只读属性，安全
        Log.d(TAG, "Artboard count: ${file.artboardCount}")
        
        // 使用 controller 的 activeArtboard（安全，由 controller 管理）
        val activeArtboard = riveView.controller.activeArtboard
        if (activeArtboard != null) {
            Log.d(TAG, "--- Artboard [0]: ${activeArtboard.name} ---")
            Log.d(TAG, "State Machines: ${activeArtboard.stateMachineNames}")
            Log.d(TAG, "Animations: ${activeArtboard.animationNames}")
        }
        
        // 打印屏幕密度信息，用于调试 length 计算
        Log.d(TAG, "Screen density: ${riveView.resources.displayMetrics.density}")
        Log.d(TAG, "Scaled density: ${riveView.resources.displayMetrics.scaledDensity}")
        
        Log.d(TAG, "Current state machine: $stateMachineName")
        Log.d(TAG, "========== End of Info ==========")
    }

    // ============ 私有方法 ============

    /**
     * 设置原文内容
     */
    private fun setOriginalContents(targetLanguage: String) {
        val isSpecialLanguage = targetLanguage.lowercase() in SPECIAL_LANGUAGES

        if (isSpecialLanguage) {
            // 目标语言是 Spanish/Korean/Simplified Chinese，切换原文
            trySetTextRun(TEXT_CONTENT_1, "Salut ! Bienvenue à Intent.")  // French
            trySetTextRun(TEXT_CONTENT_2, "我們會讓任何語言都能讀懂。")      // Traditional Chinese
            trySetTextRun(TEXT_CONTENT_3, "Dan itu bekerja dalam sekejap!") // Indonesian
            Log.d(TAG, "Set original contents for special language: $targetLanguage")
        } else {
            // 默认情况
            trySetTextRun(TEXT_CONTENT_1, "¡Hola! Bienvenido a Intent.")   // Spanish
            trySetTextRun(TEXT_CONTENT_2, "우린 어떤 언어든 읽게 해줘요.")   // Korean
            trySetTextRun(TEXT_CONTENT_3, "而且一眨眼就好！")               // Simplified Chinese
            Log.d(TAG, "Set original contents for default language")
        }
    }

    /**
     * 设置翻译后的内容
     */
    private fun setTranslatedContents(contents: List<ContentItem>) {
        contents.getOrNull(0)?.let {
            trySetTextRun(TEXT_TRANSLATED_1, it.translated)
        }
        contents.getOrNull(1)?.let {
            trySetTextRun(TEXT_TRANSLATED_2, it.translated)
        }
        contents.getOrNull(2)?.let {
            trySetTextRun(TEXT_TRANSLATED_3, it.translated)
        }
        Log.d(TAG, "Set translated contents")
    }

    /**
     * 设置长度参数（像素宽度）
     */
    private fun setLengthInputs(contents: List<ContentItem>) {
        contents.getOrNull(0)?.let {
            val width = measureTextWidth(it.translated)
            riveView.setNumberState(stateMachineName, INPUT_LENGTH_1, width)
            Log.d(TAG, "Set $INPUT_LENGTH_1 = $width")
        }
        contents.getOrNull(1)?.let {
            val width = measureTextWidth(it.translated)
            riveView.setNumberState(stateMachineName, INPUT_LENGTH_2, width)
            Log.d(TAG, "Set $INPUT_LENGTH_2 = $width")
        }
        contents.getOrNull(2)?.let {
            val width = measureTextWidth(it.translated)
            riveView.setNumberState(stateMachineName, INPUT_LENGTH_3, width)
            Log.d(TAG, "Set $INPUT_LENGTH_3 = $width")
        }
    }

    /**
     * 计算文本的 length 值
     * 
     * 注意：这个值需要与 Rive 设计中的 length 参数匹配
     * 根据截图，length 值在 100-400 范围，大约是每个字符 9-10 像素
     */
    private fun measureTextWidth(text: String): Float {
        // 方法1：使用字符数估算（简单但不够精确）
        val estimatedLength = text.length * LENGTH_PIXELS_PER_CHAR
        
        // 方法2：使用 Paint 测量后除以密度（更精确）
        // val measuredLength = textPaint.measureText(text) / riveView.resources.displayMetrics.density
        
        Log.d(TAG, "Text: '$text', chars: ${text.length}, estimated length: $estimatedLength")
        return estimatedLength
    }

    /**
     * 设置 state 输入值
     * 
     * ✅ 修复：添加释放检查，防止在释放后操作 RiveView
     */
    private fun setStateValue(value: Float) {
        if (isReleased) {
            Log.w(TAG, "Controller已释放，跳过设置 state=$value")
            return
        }
        
        try {
            riveView.setNumberState(stateMachineName, INPUT_STATE, value)
            Log.d(TAG, "Set $INPUT_STATE = $value")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set state value: ${e.message}")
        }
    }

    /**
     * 尝试设置 Text Run 的值（如果失败则记录日志）
     */
    private fun trySetTextRun(name: String, value: String) {
        try {
            riveView.setTextRunValue(name, value)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to set text run '$name': ${e.message}")
        }
    }
}

/**
 * 翻译动画数据
 */
data class TranslationData(
    val targetLanguage: String,  // 目标翻译语言代码 (如 "en", "es", "ko", "zh-CN")
    val contents: List<ContentItem>
)

/**
 * 单条消息内容
 */
data class ContentItem(
    val original: String,      // 原文
    val translated: String     // 翻译后的文本
)

