package app.rive.runtime.example

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import app.rive.runtime.example.databinding.OnbardingStateMachineBinding
import app.rive.runtime.example.utils.setEdgeToEdgeContent
import app.rive.runtime.kotlin.controllers.RiveFileController
import app.rive.runtime.kotlin.core.FileAsset
import app.rive.runtime.kotlin.core.FileAssetLoader
import app.rive.runtime.kotlin.core.FontAsset
import app.rive.runtime.kotlin.core.PlayableInstance
import app.rive.runtime.kotlin.fonts.FontFallbackStrategy
import app.rive.runtime.kotlin.fonts.FontHelper
import app.rive.runtime.kotlin.fonts.Fonts

/**
 * Onboarding 动画示例 Activity（简化版）
 *
 * 功能：进入页面后自动循环播放动画
 *
 * 一次播放的时间线：
 * ├── 0s     : state=0, 消息弹出动画开始
 * ├── ~1s    : 消息弹出完成
 * ├── 1.5s   : state=1, 第一条消息翻译
 * ├── 1.7s   : state=2, 第二条消息翻译 (+0.2s)
 * ├── 1.9s   : state=3, 第三条消息翻译 (+0.2s)
 * └── ~3s    : 所有翻译完成，等待后重新开始
 *
 * 参考 AndroidPlayerActivity 的简洁设计，不过度封装。
 */
class OnboardingStateMachineActivity : ComponentActivity(), FontFallbackStrategy {

    companion object {
        private const val TAG = "OnboardingActivity"

        // 状态机配置
        private const val STATE_MACHINE_NAME = "StateMachine_1"
        private const val INPUT_STATE = "state"
        private const val INPUT_LENGTH_1 = "length_1"
        private const val INPUT_LENGTH_2 = "length_2"
        private const val INPUT_LENGTH_3 = "length_3"

        // Text Run 名称
        private const val TEXT_CONTENT_1 = "Content_1"
        private const val TEXT_CONTENT_2 = "Content_2"
        private const val TEXT_CONTENT_3 = "Content_3"
        private const val TEXT_TRANSLATED_1 = "Translated_1"
        private const val TEXT_TRANSLATED_2 = "Translated_2"
        private const val TEXT_TRANSLATED_3 = "Translated_3"

        // 动画时序（毫秒）
        private const val DELAY_BEFORE_TRANSLATE = 1500L   // 弹出完成后等待
        private const val DELAY_BETWEEN_ITEMS = 200L       // 翻译项之间间隔
        private const val DELAY_BEFORE_LOOP = 3000L        // 完成后等待，然后循环

        // 文本宽度计算
        private const val LENGTH_PIXELS_PER_CHAR = 9f
    }

    private lateinit var binding: OnbardingStateMachineBinding
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var fontLoader: SystemFontLoader

    // 翻译数据
    private val translatedTexts = listOf(
        "Hi! Welcome to Intent.",
        "We'll make any language readable for you.",
        "And it works in a blink!"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = OnbardingStateMachineBinding.inflate(layoutInflater)
        setEdgeToEdgeContent(binding.root)

        // 设置字体回退策略（支持多语言：中文、韩文、阿拉伯文等）
        // FontFallbackStrategy 会在遇到缺失字符时自动查找系统中支持该字符的字体
        FontFallbackStrategy.stylePicker = this

        // 创建字体加载器（使用系统字体替代 OOB 字体）
        fontLoader = SystemFontLoader()

        // 设置字体加载器并重新加载 Rive 文件
        // 必须在 setRiveResource 之前设置 assetLoader
        val riveView = binding.onboardingStateMachine
        riveView.setAssetLoader(fontLoader)
        riveView.setRiveResource(
            resId = R.raw.onboarding_part_1_unfont,
            stateMachineName = STATE_MACHINE_NAME,
            autoplay = true
        )

        // 设置监听器（可选，用于调试）
        setupListener()

        // 设置文本内容（在状态机启动前设置 Text Run）
        setupTextContent()

        Log.d(TAG, "onCreate - 等待状态机自动启动")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume - 开始动画循环")

        // 状态机由 XML 的 app:riveStateMachine 自动启动
        // 我们只需要设置 Number 参数并开始循环
        handler.post {
            setupLengthInputs()
            startAnimationLoop()
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause - 停止动画循环")
        handler.removeCallbacksAndMessages(null)
    }

    /**
     * 设置文本内容
     *
     * setTextRunValue 不需要状态机，可以在 onCreate 中直接设置
     */
    private fun setupTextContent() {
        val riveView = binding.onboardingStateMachine

        // 原文（默认语言）
        trySetTextRun(TEXT_CONTENT_1, "¡Hola! Bienvenido a Intent.")
        trySetTextRun(TEXT_CONTENT_2, "우린 어떤 언어든 읽게 해줘요.")
        trySetTextRun(TEXT_CONTENT_3, "而且一眨眼就好！")

        // 翻译后的文本
        trySetTextRun(TEXT_TRANSLATED_1, translatedTexts[0])
        trySetTextRun(TEXT_TRANSLATED_2, translatedTexts[1])
        trySetTextRun(TEXT_TRANSLATED_3, translatedTexts[2])

        Log.d(TAG, "文本内容已设置")
    }

    /**
     * 设置长度参数
     *
     * setNumberState 需要状态机已启动，在 onResume 中调用
     */
    private fun setupLengthInputs() {
        val riveView = binding.onboardingStateMachine

        translatedTexts.forEachIndexed { index, text ->
            val length = text.length * LENGTH_PIXELS_PER_CHAR
            val inputName = when (index) {
                0 -> INPUT_LENGTH_1
                1 -> INPUT_LENGTH_2
                2 -> INPUT_LENGTH_3
                else -> return@forEachIndexed
            }
            riveView.setNumberState(STATE_MACHINE_NAME, inputName, length)
            Log.d(TAG, "Set $inputName = $length")
        }
    }

    /**
     * 开始动画循环
     */
    private fun startAnimationLoop() {
        Log.d(TAG, "开始一次动画循环")

        // state=0: 弹出动画已经由状态机 Entry State 自动播放
        // 我们只需要在适当的时机设置 state=1,2,3

        // 1.5s 后：state=1
        handler.postDelayed({
            setStateValue(1f)

            // 1.7s 后：state=2
            handler.postDelayed({
                setStateValue(2f)

                // 1.9s 后：state=3
                handler.postDelayed({
                    setStateValue(3f)

                    // 完成后等待，然后重新开始循环
                    handler.postDelayed({
                        resetAndLoop()
                    }, DELAY_BEFORE_LOOP)

                }, DELAY_BETWEEN_ITEMS)
            }, DELAY_BETWEEN_ITEMS)
        }, DELAY_BEFORE_TRANSLATE)
    }

    /**
     * 重置并重新开始循环
     * 
     * ⚠️ 重要：不能使用 reset()！
     * reset() 创建的 Artboard 的"初始状态"与文件首次加载时不同。
     * 必须使用 setRiveResource() 重新加载文件，才能完全回到初始状态。
     */
    private fun resetAndLoop() {
        Log.d(TAG, "重置并重新开始循环")

        val riveView = binding.onboardingStateMachine

        // 1. 重新加载 Rive 文件（完全重置，和首次加载一样）
        riveView.setRiveResource(
            resId = R.raw.onboarding_part_1_unfont,
            stateMachineName = STATE_MACHINE_NAME,
            autoplay = true
        )

        // 2. 重新设置 Text Run
        setupTextContent()

        // 3. 重新设置 Number 参数
        setupLengthInputs()

        // 4. 开始新一轮循环
        startAnimationLoop()
    }

    private fun setStateValue(value: Float) {
        binding.onboardingStateMachine.setNumberState(STATE_MACHINE_NAME, INPUT_STATE, value)
        Log.d(TAG, "Set state = $value")
    }

    private fun trySetTextRun(name: String, value: String) {
        try {
            binding.onboardingStateMachine.setTextRunValue(name, value)
        } catch (e: Exception) {
            Log.w(TAG, "设置 TextRun '$name' 失败: ${e.message}")
        }
    }

    /**
     * 设置监听器（调试用）
     */
    private fun setupListener() {
        binding.onboardingStateMachine.registerListener(object : RiveFileController.Listener {
            override fun notifyPlay(animation: PlayableInstance) {
                Log.d(TAG, "Event: Play ${animation.name}")
            }

            override fun notifyPause(animation: PlayableInstance) {
                Log.d(TAG, "Event: Pause ${animation.name}")
            }

            override fun notifyStop(animation: PlayableInstance) {
                Log.d(TAG, "Event: Stop ${animation.name}")
            }

            override fun notifyLoop(animation: PlayableInstance) {
                Log.d(TAG, "Event: Loop ${animation.name}")
            }

            override fun notifyStateChanged(stateMachineName: String, stateName: String) {
                Log.d(TAG, "Event: State Changed - $stateMachineName: $stateName")
            }
        })
    }

    /**
     * 系统字体加载器
     * 
     * 当 Rive 文件中的字体资源缺失时（Out-of-Band 字体），
     * 使用此加载器提供系统默认字体作为基础字体。
     */
    private inner class SystemFontLoader : FileAssetLoader() {
        override fun loadContents(asset: FileAsset, inBandBytes: ByteArray): Boolean {
            if (asset is FontAsset) {
                Log.d(TAG, "加载 OOB 字体资源: ${asset.name}")
                
                // 使用系统默认字体作为基础字体
                val fontBytes = FontHelper.getFallbackFontBytes(Fonts.FontOpts.DEFAULT)
                if (fontBytes != null) {
                    Log.d(TAG, "使用系统默认字体: ${fontBytes.size} bytes")
                    return asset.decode(fontBytes)
                }
                
                // 如果获取系统字体失败，回退到 roboto.ttf
                Log.w(TAG, "无法获取系统字体，使用 roboto.ttf 作为回退")
                resources.openRawResource(R.raw.roboto).use { inputStream ->
                    return asset.decode(inputStream.readBytes())
                }
            }
            return false
        }
    }

    /**
     * 实现 FontFallbackStrategy 接口
     * 
     * 当基础字体不支持某个字符时（如中文、韩文），
     * Rive 运行时会调用此方法获取回退字体列表。
     * 系统会按顺序尝试这些字体，直到找到支持该字符的字体。
     */
    override fun getFont(weight: Fonts.Weight): List<ByteArray> {
        Log.d(TAG, "FontFallbackStrategy: 查找 weight=$weight 的回退字体")
        
        // 获取所有系统回退字体（包括中文、韩文、阿拉伯文等）
        val fallbackFonts = FontHelper.getFallbackFonts(
            Fonts.FontOpts(weight = weight)
        )
        
        Log.d(TAG, "找到 ${fallbackFonts.size} 个回退字体")
        
        // 将字体转换为 ByteArray 列表
        return fallbackFonts.mapNotNull { font ->
            FontHelper.getFontBytes(font)?.also {
                Log.d(TAG, "加载回退字体: ${font.name} (${it.size} bytes)")
            }
        }
    }
}
