package app.rive.runtime.example

import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import app.rive.runtime.kotlin.RiveAnimationView

/**
 * Onboarding 动画示例 Activity
 *
 * 演示如何使用 onboarding_part_1.riv 动画：
 * 1. 加载动画后显示消息弹出效果 (state=0)
 * 2. 点击按钮后依次播放翻译动画 (state=1,2,3)
 * 
 * 动画时序：
 * ├── 0s     : state=0, 消息弹出动画开始
 * ├── ~1.5s  : 消息弹出完成
 * ├── 1.5s   : state=1, 第一条消息翻译
 * ├── 1.7s   : state=2, 第二条消息翻译 (+0.2s)
 * ├── 1.9s   : state=3, 第三条消息翻译 (+0.2s)
 * └── ~3s    : 所有翻译完成
 */
class OnboardingStateMachineActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "OnboardingActivity"
    }

    private val animationView by lazy(LazyThreadSafetyMode.NONE) {
        findViewById<RiveAnimationView>(R.id.onboarding_state_machine)
    }

    private val autoPlayButton by lazy(LazyThreadSafetyMode.NONE) {
        findViewById<Button>(R.id.btn_auto_play)
    }

    private val translateButton by lazy(LazyThreadSafetyMode.NONE) {
        findViewById<Button>(R.id.btn_translate)
    }

    private val resetButton by lazy(LazyThreadSafetyMode.NONE) {
        findViewById<Button>(R.id.btn_reset)
    }

    private lateinit var animationController: OnboardingAnimationController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.onbarding_state_machine)

        // 创建动画控制器
        animationController = OnboardingAnimationController(animationView)

        // 等待动画加载完成后初始化
        animationView.post {
            // 调试：先打印 Rive 文件信息
            animationController.printDebugInfo()
            
            initializeAnimation()
            setupButtons()
        }
    }

    /**
     * 初始化动画数据
     */
    private fun initializeAnimation() {
        // 准备翻译数据（实际项目中应从翻译接口获取）
        val translationData = TranslationData(
            targetLanguage = "en",  // 目标语言：英语
            contents = listOf(
                ContentItem(
                    original = "Salut ! Bienvenue à Intent.",
                    translated = "Hi! Welcome to Intent."
                ),
                ContentItem(
                    original = "우린 어떤 언어든 읽게 해줘요.",
                    translated = "We'll make any language readable for you."
                ),
                ContentItem(
                    original = "而且一眨眼就好！",
                    translated = "And it works in a blink!"
                )
            )
        )

        // 初始化动画
        animationController.initialize(translationData)
        Log.d(TAG, "Animation initialized")
    }

    /**
     * 设置按钮点击事件
     */
    private fun setupButtons() {
        // 自动播放按钮：播放完整动画序列（包括 1.5s 延迟后自动翻译）
        autoPlayButton.setOnClickListener {
            Log.d(TAG, "Auto play button clicked")
            animationController.reset()
            initializeAnimation()
            animationController.playFullSequence()
            updateButtonStates(isPlaying = true)
        }
        
        // 手动翻译按钮：立即开始翻译动画
        translateButton.setOnClickListener {
            Log.d(TAG, "Translate button clicked")
            animationController.playTranslationSequence()
            updateButtonStates(isPlaying = true)
        }

        // 重置按钮
        resetButton.setOnClickListener {
            Log.d(TAG, "Reset button clicked")
            animationController.reset()
            initializeAnimation()
            updateButtonStates(isPlaying = false)
        }

        // 初始状态
        updateButtonStates(isPlaying = false)
    }
    
    private fun updateButtonStates(isPlaying: Boolean) {
        autoPlayButton.isEnabled = !isPlaying
        translateButton.isEnabled = !isPlaying
        resetButton.isEnabled = isPlaying
    }

    override fun onDestroy() {
        super.onDestroy()
        animationController.release()
    }
}
