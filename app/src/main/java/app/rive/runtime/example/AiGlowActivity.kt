package app.rive.runtime.example

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.widget.SeekBar
import androidx.activity.ComponentActivity
import androidx.constraintlayout.widget.ConstraintSet
import app.rive.runtime.example.databinding.ActivityAiGlowBinding
import app.rive.runtime.example.utils.setEdgeToEdgeContent
import app.rive.runtime.kotlin.core.Fit

class AiGlowActivity : ComponentActivity() {

    companion object {
        private const val TAG = "AiGlowActivity"
        
        // Width 范围: 125-474
        private const val WIDTH_MIN = 125f
        private const val WIDTH_MAX = 474f
        private const val WIDTH_INITIAL = 338f
        
        // Height 范围: 120-973
        private const val HEIGHT_MIN = 120f
        private const val HEIGHT_MAX = 973f
        private const val HEIGHT_INITIAL = 332f
    }

    private lateinit var binding: ActivityAiGlowBinding
    
    // 当前的宽高值
    private var currentWidth = WIDTH_INITIAL
    private var currentHeight = HEIGHT_INITIAL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAiGlowBinding.inflate(layoutInflater)
        setEdgeToEdgeContent(binding.root)
        
        // 设置 Rive 动画视图
        val riveView = binding.aiGlowRiv
        riveView.setRiveResource(
            resId = R.raw.ai_glow,
            stateMachineName = "StateMachine_1",
            autoplay = true
        )
        riveView.fit = Fit.FILL
        
        // 延迟一下，等待状态机初始化完成
        Handler(Looper.getMainLooper()).postDelayed({
            initializeStateMachine()
            setupControls()
        }, 100)
    }

    /**
     * 初始化状态机的 Number 变量
     */
    private fun initializeStateMachine() {
        try {
            val controller = binding.aiGlowRiv.controller
            
            // 设置初始的 width 和 height 值
            controller.setNumberState("StateMachine_1", "width", WIDTH_INITIAL)
            controller.setNumberState("StateMachine_1", "height", HEIGHT_INITIAL)
            
            // 设置初始View尺寸
            updateViewSize(WIDTH_INITIAL, HEIGHT_INITIAL)
            
            Log.d(TAG, "状态机初始化完成: width=$WIDTH_INITIAL, height=$HEIGHT_INITIAL")
        } catch (e: Exception) {
            Log.e(TAG, "初始化状态机失败", e)
        }
    }

    /**
     * 设置控制按钮和滑块
     */
    private fun setupControls() {
        // Width SeekBar
        binding.widthSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentWidth = WIDTH_MIN + progress
                updateWidth(currentWidth)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Width 减少按钮
        binding.btnDecreaseWidth.setOnClickListener {
            currentWidth = (currentWidth - 10).coerceIn(WIDTH_MIN, WIDTH_MAX)
            binding.widthSeekBar.progress = (currentWidth - WIDTH_MIN).toInt()
            updateWidth(currentWidth)
        }

        // Width 增加按钮
        binding.btnIncreaseWidth.setOnClickListener {
            currentWidth = (currentWidth + 10).coerceIn(WIDTH_MIN, WIDTH_MAX)
            binding.widthSeekBar.progress = (currentWidth - WIDTH_MIN).toInt()
            updateWidth(currentWidth)
        }

        // Height SeekBar
        binding.heightSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentHeight = HEIGHT_MIN + progress
                updateHeight(currentHeight)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Height 减少按钮
        binding.btnDecreaseHeight.setOnClickListener {
            currentHeight = (currentHeight - 10).coerceIn(HEIGHT_MIN, HEIGHT_MAX)
            binding.heightSeekBar.progress = (currentHeight - HEIGHT_MIN).toInt()
            updateHeight(currentHeight)
        }

        // Height 增加按钮
        binding.btnIncreaseHeight.setOnClickListener {
            currentHeight = (currentHeight + 10).coerceIn(HEIGHT_MIN, HEIGHT_MAX)
            binding.heightSeekBar.progress = (currentHeight - HEIGHT_MIN).toInt()
            updateHeight(currentHeight)
        }

        // 重置按钮
        binding.btnReset.setOnClickListener {
            currentWidth = WIDTH_INITIAL
            currentHeight = HEIGHT_INITIAL
            binding.widthSeekBar.progress = (WIDTH_INITIAL - WIDTH_MIN).toInt()
            binding.heightSeekBar.progress = (HEIGHT_INITIAL - HEIGHT_MIN).toInt()
            updateWidth(currentWidth)
            updateHeight(currentHeight)
        }

        // 初始化标签
        updateWidth(currentWidth)
        updateHeight(currentHeight)
    }

    /**
     * 更新宽度值
     */
    private fun updateWidth(width: Float) {
        try {
            binding.aiGlowRiv.controller.setNumberState("StateMachine_1", "width", width)
            binding.widthLabel.text = "Width: ${width.toInt()} (${WIDTH_MIN.toInt()}-${WIDTH_MAX.toInt()})"
            
            // 更新 View 的实际宽度（内部框宽度 + 左右边距各13dp）
//            updateViewSize(width, currentHeight)
            
            Log.d(TAG, "更新宽度: $width")
        } catch (e: Exception) {
            Log.e(TAG, "更新宽度失败", e)
        }
    }

    /**
     * 更新高度值
     */
    private fun updateHeight(height: Float) {
        try {
            binding.aiGlowRiv.controller.setNumberState("StateMachine_1", "height", height)
            binding.heightLabel.text = "Height: ${height.toInt()} (${HEIGHT_MIN.toInt()}-${HEIGHT_MAX.toInt()})"
            
            // 更新 View 的实际高度（内部框高度 + 上下边距各13dp）
//            updateViewSize(currentWidth, height)
            
            Log.d(TAG, "更新高度: $height")
        } catch (e: Exception) {
            Log.e(TAG, "更新高度失败", e)
        }
    }

    /**
     * 使用 ConstraintSet 更新 aiGlowRiv 和 contentV 的实际尺寸
     * 动画起始点在前景空白框左上各13px的位置
     * 所以 aiGlowRiv 尺寸 = 内部框尺寸 + 边距*2
     * contentV 尺寸 = 内部框尺寸
     */
    private fun updateViewSize(innerWidth: Float, innerHeight: Float) {
        try {
            val constraintLayout = binding.root
            val constraintSet = ConstraintSet()
            constraintSet.clone(constraintLayout)
            
            // 计算 aiGlowRiv 的实际尺寸（dp）
            // 内部框宽度/高度 + 左右/上下各13dp的边距
            val riveViewWidthDp = (innerWidth + 26).toInt()
            val riveViewHeightDp = (innerHeight + 26).toInt()
            
            // 将dp转换为px
            val riveWidthPx = dpToPx(riveViewWidthDp.toFloat()).toInt()
            val riveHeightPx = dpToPx(riveViewHeightDp.toFloat()).toInt()
            
            // 设置 aiGlowRiv 的宽高约束
            constraintSet.constrainWidth(R.id.aiGlowRiv, riveWidthPx)
            constraintSet.constrainHeight(R.id.aiGlowRiv, riveHeightPx)
            
            // 设置 contentV 的宽高（等于内部框尺寸）
//            val contentWidthPx = dpToPx(innerWidth).toInt()
//            val contentHeightPx = dpToPx(innerHeight).toInt()
//            constraintSet.constrainWidth(R.id.contentV, contentWidthPx)
//            constraintSet.constrainHeight(R.id.contentV, contentHeightPx)
            
            // 应用约束
            constraintSet.applyTo(constraintLayout)
            
            Log.d(TAG, "更新View尺寸: innerWidth=$innerWidth, innerHeight=$innerHeight, riveViewSize=${riveViewWidthDp}x${riveViewHeightDp}dp, contentSize=${innerWidth.toInt()}x${innerHeight.toInt()}dp")
        } catch (e: Exception) {
            Log.e(TAG, "更新View尺寸失败", e)
        }
    }

    /**
     * dp转px
     */
    private fun dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            resources.displayMetrics
        )
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

}
