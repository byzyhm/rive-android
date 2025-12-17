package app.rive.runtime.example

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import app.rive.runtime.example.databinding.OnbardingStateMachineBinding
import app.rive.runtime.example.font.FontPreloader
import app.rive.runtime.example.utils.setEdgeToEdgeContent
import app.rive.runtime.kotlin.controllers.RiveFileController
import app.rive.runtime.kotlin.core.PlayableInstance

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
class OnboardingStateMachineActivity : ComponentActivity() {

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

        // 字体回退策略已在 RiveExampleApplication 中全局设置
        // 使用 FontPreloader 预加载的字体，避免在此处同步加载导致卡顿

        // 设置字体加载器（使用 FontPreloader 中的静态类，避免内存泄漏）
        // 必须在 setRiveResource 之前设置 assetLoader
        val riveView = binding.onboardingStateMachine
        riveView.setAssetLoader(FontPreloader.createFontAssetLoader())
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

}
