package app.rive.runtime.example

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.SeekBar
import androidx.activity.ComponentActivity
import app.rive.runtime.example.databinding.ActivityAiGlowBinding
import app.rive.runtime.example.utils.setEdgeToEdgeContent
import app.rive.runtime.kotlin.core.Fit

class AiGlowActivity : ComponentActivity() {

    companion object {
        private const val TAG = "AiGlowActivity"
        
        // 动画图层宽度范围: 125-474
        private const val ANIM_WIDTH_MIN = 125f
        private const val ANIM_WIDTH_MAX = 474f
        private const val ANIM_WIDTH_INITIAL = 338f
        
        // 动画图层高度范围: 120-973
        private const val ANIM_HEIGHT_MIN = 120f
        private const val ANIM_HEIGHT_MAX = 973f
        private const val ANIM_HEIGHT_INITIAL = 332f
        
        // SeekBar max 值
        private const val WIDTH_SEEKBAR_MAX = 349  // 474 - 125
        private const val HEIGHT_SEEKBAR_MAX = 853 // 973 - 120
        private const val TEXT_LENGTH_MAX = 100    // 文本长度最大值
        
        // SeekBar 初始 progress
        private val WIDTH_SEEKBAR_INITIAL = (ANIM_WIDTH_INITIAL - ANIM_WIDTH_MIN).toInt()   // 213
        private val HEIGHT_SEEKBAR_INITIAL = (ANIM_HEIGHT_INITIAL - ANIM_HEIGHT_MIN).toInt() // 212
        private const val TEXT_LENGTH_INITIAL = 100 // 文本长度初始值(100%)
    }

    private lateinit var binding: ActivityAiGlowBinding
    
    // 屏幕宽度
    private var screenWidth: Int = 0
    
    // 当前的动画参数值
    private var currentAnimWidth = ANIM_WIDTH_INITIAL
    private var currentAnimHeight = ANIM_HEIGHT_INITIAL
    
    // 文本内容
    private lateinit var fullText: String
    private var currentTextLength = TEXT_LENGTH_INITIAL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAiGlowBinding.inflate(layoutInflater)
        setEdgeToEdgeContent(binding.root)
        
        // 保存完整文本
        fullText = binding.contentV.text.toString()
        
        // 获取屏幕宽度
        screenWidth = resources.displayMetrics.widthPixels
        Log.d(TAG, "屏幕宽度: $screenWidth")
        
        // 设置 Rive 动画视图
        val riveView = binding.aiGlowRiv
        riveView.setRiveResource(
            resId = R.raw.ai_glow,
            stateMachineName = "StateMachine_1",
            autoplay = true
        )
        riveView.fit = Fit.FILL
        
        // 添加验证代码：检查 View 和 Artboard 的尺寸
        riveView.post {
            verifyViewAndArtboardSizes()
        }
        
        // 延迟一下，等待状态机初始化完成
        Handler(Looper.getMainLooper()).postDelayed({
            initializeStateMachine()
            setupControls()
        }, 100)
    }

    /**
     * 验证 View 和 Artboard 的尺寸信息
     */
    private fun verifyViewAndArtboardSizes() {
        try {
            val riveView = binding.aiGlowRiv
            val controller = riveView.controller
            val artboard = controller.activeArtboard
            
            Log.d(TAG, "========== 尺寸验证信息 ==========")
            
            // View 的实际尺寸
            Log.d(TAG, "View 实际宽度: ${riveView.width}px (${riveView.width / resources.displayMetrics.density}dp)")
            Log.d(TAG, "View 实际高度: ${riveView.height}px (${riveView.height / resources.displayMetrics.density}dp)")
            
            // View 的 LayoutParams
            riveView.layoutParams?.let { params ->
                Log.d(TAG, "LayoutParams 宽度: ${params.width}")
                Log.d(TAG, "LayoutParams 高度: ${params.height}")
            }
            
            // Artboard 信息
            artboard?.let {
                val bounds = it.bounds
                Log.d(TAG, "Artboard 宽度: ${it.width}")
                Log.d(TAG, "Artboard 高度: ${it.height}")
                Log.d(TAG, "Artboard bounds: left=${bounds.left}, top=${bounds.top}, right=${bounds.right}, bottom=${bounds.bottom}")
                Log.d(TAG, "Artboard bounds 尺寸: ${bounds.width()} x ${bounds.height()}")
                
                // 宽高比分析
                val artboardRatio = bounds.width() / bounds.height()
                val viewRatio = riveView.width.toFloat() / riveView.height.toFloat()
                Log.d(TAG, "Artboard 宽高比: ${String.format("%.3f", artboardRatio)} (${bounds.width()}:${bounds.height()})")
                Log.d(TAG, "View 宽高比: ${String.format("%.3f", viewRatio)} (${riveView.width}:${riveView.height})")
                Log.d(TAG, "宽高比匹配: ${if (Math.abs(artboardRatio - viewRatio) < 0.01f) "✅ 匹配" else "❌ 不匹配"}")
            } ?: Log.w(TAG, "Artboard 尚未加载")
            
            // Controller 信息
            Log.d(TAG, "Fit 模式: ${controller.fit}")
            Log.d(TAG, "Alignment: ${controller.alignment}")
            Log.d(TAG, "Target bounds: ${controller.targetBounds}")
            Log.d(TAG, "Artboard bounds (from controller): ${controller.artboardBounds}")
            
            // 屏幕密度信息
            Log.d(TAG, "屏幕密度: ${resources.displayMetrics.density}")
            Log.d(TAG, "屏幕宽度: ${screenWidth}px (${screenWidth / resources.displayMetrics.density}dp)")
            
            Log.d(TAG, "==================================")
            
        } catch (e: Exception) {
            Log.e(TAG, "验证尺寸信息失败", e)
        }
    }
    
    /**
     * 初始化状态机的 Number 变量
     */
    private fun initializeStateMachine() {
        try {
            val controller = binding.aiGlowRiv.controller
            
            // 设置初始的 width 和 height 值
            controller.setNumberState("StateMachine_1", "width", ANIM_WIDTH_INITIAL)
            controller.setNumberState("StateMachine_1", "height", ANIM_HEIGHT_INITIAL)

            Log.d(TAG, "状态机初始化完成: width=$ANIM_WIDTH_INITIAL, height=$ANIM_HEIGHT_INITIAL")
        } catch (e: Exception) {
            Log.e(TAG, "初始化状态机失败", e)
        }
    }
    
    /**
     * 根据动画参数计算 View 宽度
     * viewWidth = screenWidth × (animWidth / 474)
     */
    private fun calculateViewWidth(animWidth: Float): Int {
        return (screenWidth * animWidth / ANIM_WIDTH_MAX).toInt()
    }
    
    /**
     * 根据动画参数计算 View 高度
     * viewHeight = screenWidth × (animHeight / 474)
     * 注意：使用同一个比例因子 (screenWidth / 474) 保持动画比例
     */
    private fun calculateViewHeight(animHeight: Float): Int {
        return (screenWidth * animHeight / ANIM_WIDTH_MAX).toInt()
    }
    
    /**
     * 根据 SeekBar progress 计算动画宽度参数
     * animWidth = 125 + progress
     */
    private fun progressToAnimWidth(progress: Int): Float {
        return ANIM_WIDTH_MIN + progress
    }
    
    /**
     * 根据 SeekBar progress 计算动画高度参数
     * animHeight = 120 + progress
     */
    private fun progressToAnimHeight(progress: Int): Float {
        return ANIM_HEIGHT_MIN + progress
    }
    
    /**
     * 更新 RiveAnimationView 的尺寸
     * 使用 ConstraintSet 来动态修改约束
     */
    private fun updateViewSize() {
        val viewWidth = calculateViewWidth(currentAnimWidth)
        val viewHeight = calculateViewHeight(currentAnimHeight)
        
        // 获取父容器 ConstraintLayout
        val constraintLayout = binding.root as androidx.constraintlayout.widget.ConstraintLayout
        
        // 创建 ConstraintSet 并从当前布局克隆约束
        val constraintSet = androidx.constraintlayout.widget.ConstraintSet()
        constraintSet.clone(constraintLayout)
        
        // 修改宽度和高度约束
        constraintSet.constrainWidth(R.id.aiGlowRiv, viewWidth)
        constraintSet.constrainHeight(R.id.aiGlowRiv, viewHeight)
        
        // 应用新的约束
        constraintSet.applyTo(constraintLayout)
        
        Log.d(TAG, "View尺寸更新(ConstraintSet): ${viewWidth}x${viewHeight}")
    }
    
    /**
     * 更新动画状态机参数（不改变View尺寸）
     */
    private fun updateAnimationAndView() {
        try {
            // 1. 更新动画状态机参数
            val controller = binding.aiGlowRiv.controller
            controller.setNumberState("StateMachine_1", "width", currentAnimWidth)
            controller.setNumberState("StateMachine_1", "height", currentAnimHeight)
            
            // 2. 更新标签显示
            updateLabels()
            
        } catch (e: Exception) {
            Log.e(TAG, "更新动画状态机失败", e)
        }
    }
    
    /**
     * 更新标签显示当前值
     */
    private fun updateLabels() {
        binding.widthLabel.text = "Width: ${currentAnimWidth.toInt()} (${ANIM_WIDTH_MIN.toInt()}-${ANIM_WIDTH_MAX.toInt()})"
        binding.heightLabel.text = "Height: ${currentAnimHeight.toInt()} (${ANIM_HEIGHT_MIN.toInt()}-${ANIM_HEIGHT_MAX.toInt()})"
        binding.textLengthLabel.text = "Text Length: $currentTextLength%"
    }
    
    /**
     * 更新文本内容的显示长度
     */
    private fun updateTextContent() {
        val targetLength = (fullText.length * currentTextLength / 100.0).toInt()
        binding.contentV.text = fullText.substring(0, targetLength.coerceAtMost(fullText.length))
    }

    /**
     * 设置控制按钮和滑块
     */
    private fun setupControls() {
        // 设置 SeekBar 的 max 和初始 progress
        binding.widthSeekBar.max = WIDTH_SEEKBAR_MAX
        binding.widthSeekBar.progress = WIDTH_SEEKBAR_INITIAL
        binding.heightSeekBar.max = HEIGHT_SEEKBAR_MAX
        binding.heightSeekBar.progress = HEIGHT_SEEKBAR_INITIAL
        binding.textLengthSeekBar.max = TEXT_LENGTH_MAX
        binding.textLengthSeekBar.progress = TEXT_LENGTH_INITIAL
        
        // 更新初始标签
        updateLabels()
        
        // Width SeekBar
        binding.widthSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    currentAnimWidth = progressToAnimWidth(progress)
                    // 直接更新，不使用防抖，保证顺滑
                    updateAnimationAndView()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Width 减少按钮
        binding.btnDecreaseWidth.setOnClickListener {
            val newProgress = (binding.widthSeekBar.progress - 10).coerceAtLeast(0)
            binding.widthSeekBar.progress = newProgress
            currentAnimWidth = progressToAnimWidth(newProgress)
            updateAnimationAndView()
        }

        // Width 增加按钮
        binding.btnIncreaseWidth.setOnClickListener {
            val newProgress = (binding.widthSeekBar.progress + 10).coerceAtMost(WIDTH_SEEKBAR_MAX)
            binding.widthSeekBar.progress = newProgress
            currentAnimWidth = progressToAnimWidth(newProgress)
            updateAnimationAndView()
        }

        // Height SeekBar
        binding.heightSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    currentAnimHeight = progressToAnimHeight(progress)
                    // 直接更新，不使用防抖，保证顺滑
                    updateAnimationAndView()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Height 减少按钮
        binding.btnDecreaseHeight.setOnClickListener {
            val newProgress = (binding.heightSeekBar.progress - 10).coerceAtLeast(0)
            binding.heightSeekBar.progress = newProgress
            currentAnimHeight = progressToAnimHeight(newProgress)
            updateAnimationAndView()
        }

        // Height 增加按钮
        binding.btnIncreaseHeight.setOnClickListener {
            val newProgress = (binding.heightSeekBar.progress + 10).coerceAtMost(HEIGHT_SEEKBAR_MAX)
            binding.heightSeekBar.progress = newProgress
            currentAnimHeight = progressToAnimHeight(newProgress)
            updateAnimationAndView()
        }

        // Text Length SeekBar
        binding.textLengthSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    currentTextLength = progress
                    updateTextContent()
                    updateLabels()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Text Length 减少按钮
        binding.btnDecreaseTextLength.setOnClickListener {
            val newProgress = (binding.textLengthSeekBar.progress - 5).coerceAtLeast(0)
            binding.textLengthSeekBar.progress = newProgress
            currentTextLength = newProgress
            updateTextContent()
            updateLabels()
        }

        // Text Length 增加按钮
        binding.btnIncreaseTextLength.setOnClickListener {
            val newProgress = (binding.textLengthSeekBar.progress + 5).coerceAtMost(TEXT_LENGTH_MAX)
            binding.textLengthSeekBar.progress = newProgress
            currentTextLength = newProgress
            updateTextContent()
            updateLabels()
        }

    }
    

    override fun onPause() {
        super.onPause()
        try {
            // 暂停 Rive 动画
            binding.aiGlowRiv.pause()
            Log.d(TAG, "Rive 动画已暂停")
        } catch (e: Exception) {
            Log.e(TAG, "暂停动画失败", e)
        }
    }
    
    override fun onResume() {
        super.onResume()
        try {
            // 恢复 Rive 动画
            binding.aiGlowRiv.play()
            Log.d(TAG, "Rive 动画已恢复")
        } catch (e: Exception) {
            Log.e(TAG, "恢复动画失败", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            // 停止并销毁 Rive 动画
            binding.aiGlowRiv.stop()
            
            Log.d(TAG, "资源已清理")
        } catch (e: Exception) {
            Log.e(TAG, "清理资源失败", e)
        }
    }
}
