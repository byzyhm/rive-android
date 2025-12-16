package app.rive.runtime.example

import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import app.rive.runtime.example.utils.RiveInfoUtil.printRiveFileInfo
import app.rive.runtime.example.utils.setEdgeToEdgeContent
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
 * 
 * ⚠️ 生命周期管理：
 * - onCreate: 设置布局
 * - onStart: 初始化控制器（确保每次进入都重新初始化）
 * - onPause: 暂停动画和 Handler 回调
 * - onStop: 释放控制器资源
 * - onDestroy: 最终清理（双重保险）
 */
class OnboardingStateMachineActivity : ComponentActivity() {

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

    // ✅ 修复：使用可空类型，避免未初始化使用
    private var animationController: OnboardingAnimationController? = null
    
    // ✅ 修复：添加标志位，防止重复初始化
    private var isInitialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setEdgeToEdgeContent(R.layout.onbarding_state_machine)
        Log.d(TAG, "onCreate called")
    }
    
    /**
     * ✅ 修复：在 onStart 中初始化，确保每次进入都正确设置
     * 
     * 为什么在 onStart 而不是 onCreate？
     * - onCreate 只在 Activity 首次创建时调用
     * - onStart 在每次 Activity 变为可见时调用
     * - 这样可以确保从其他 Activity 返回时也能正确初始化
     */
    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart called")
        
        if (!isInitialized) {
            // 延迟初始化，确保 View 完全准备好
            animationView.postDelayed({
                if (!isDestroyed && !isFinishing) {
                    initializeController()
                }
            }, 100) // 100ms 延迟确保 RiveAnimationView 已完全加载
        }
    }
    
    /**
     * ✅ 新增：初始化控制器的统一方法
     */
    private fun initializeController() {
        if (isInitialized) {
            Log.w(TAG, "Controller already initialized, skipping")
            return
        }
        
        try {
            Log.d(TAG, "Initializing controller...")
            
            // 创建动画控制器
            animationController = OnboardingAnimationController(animationView)

            // 调试：打印 Rive 文件信息
            animationController?.printDebugInfo()
            printRiveFileInfo(animationView)
            
            // 初始化动画数据
            initializeAnimation()
            
            // 设置按钮事件
            setupButtons()
            
            isInitialized = true
            Log.d(TAG, "Controller initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize controller", e)
            isInitialized = false
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
        animationController?.initialize(translationData)
        
        // ✅ 确保状态机正在播放
        if (!animationView.isPlaying) {
            Log.d(TAG, "State machine not playing, starting it...")
            animationView.play()
        }
        
        Log.d(TAG, "Animation initialized, isPlaying=${animationView.isPlaying}")
    }

    /**
     * 设置按钮点击事件
     */
    private fun setupButtons() {
        // 自动播放按钮：播放完整动画序列（包括 1.5s 延迟后自动翻译）
        autoPlayButton.setOnClickListener {
            Log.d(TAG, "Auto play button clicked")
            animationController?.reset()
            initializeAnimation()
            animationController?.playFullSequence()
            updateButtonStates(isPlaying = true)
        }
        
        // 手动翻译按钮：立即开始翻译动画
        translateButton.setOnClickListener {
            Log.d(TAG, "Translate button clicked")
            animationController?.playTranslationSequence()
            updateButtonStates(isPlaying = true)
        }

        // 重置按钮
        resetButton.setOnClickListener {
            Log.d(TAG, "Reset button clicked")
            animationController?.reset()
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
    
    /**
     * ✅ 新增：在 onPause 中暂停动画和 Handler 回调
     * 
     * 为什么需要在 onPause 暂停？
     * - 用户按 Home 键或切换到其他 App 时，Activity 会暂停
     * - 继续运行动画和 Handler 会浪费资源
     * - 避免在后台执行 UI 操作导致崩溃
     */
    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause called - pausing animation and handlers")
        
        // 暂停 Rive 动画播放
        animationView.pause()
        
        // 取消所有延迟任务（防止 Handler 泄漏）
        animationController?.reset()
    }
    
    /**
     * ✅ 新增：在 onResume 中可以恢复动画（如果需要）
     */
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume called")
        // 注意：这里不自动恢复播放，由用户点击按钮控制
        // 如果需要自动恢复，可以添加状态保存和恢复逻辑
    }
    
    /**
     * ✅ 新增：在 onStop 中清理控制器
     * 
     * 为什么在 onStop 而不只在 onDestroy？
     * - onStop 表示 Activity 完全不可见
     * - 此时应该释放所有资源
     * - onDestroy 可能不会及时调用（系统内存紧张时）
     * - 这样可以确保资源及时释放，避免内存泄漏
     */
    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop called - releasing controller")
        
        // 释放控制器资源
        animationController?.release()
        animationController = null
        isInitialized = false
        
        // 注意：不需要手动释放 RiveAnimationView
        // 它会在 onDetachedFromWindow 时自动清理
    }

    /**
     * ✅ 改进：在 onDestroy 中确保彻底清理（双重保险）
     */
    override fun onDestroy() {
        Log.d(TAG, "onDestroy called - final cleanup")
        
        // 双重保险：确保释放资源（防止 onStop 未执行）
        animationController?.release()
        animationController = null
        isInitialized = false
        
        super.onDestroy()
    }
}
