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
    
    // 用于防抖的 Handler 和 Runnable
    private val updateHandler = Handler(Looper.getMainLooper())
    private var updateRunnable: Runnable? = null
    private val UPDATE_DELAY = 50L // 50ms 防抖延迟

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

            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Width 减少按钮
        binding.btnDecreaseWidth.setOnClickListener {

        }

        // Width 增加按钮
        binding.btnIncreaseWidth.setOnClickListener {

        }

        // Height SeekBar
        binding.heightSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {

            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Height 减少按钮
        binding.btnDecreaseHeight.setOnClickListener {

        }

        // Height 增加按钮
        binding.btnIncreaseHeight.setOnClickListener {

        }

        // 重置按钮
        binding.btnReset.setOnClickListener {

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
            // 清理防抖任务
            updateRunnable?.let { updateHandler.removeCallbacks(it) }
            updateRunnable = null
            
            // 停止并销毁 Rive 动画
            binding.aiGlowRiv.stop()
//            binding.aiGlowRiv.destroyRenderer()
            
            Log.d(TAG, "资源已清理")
        } catch (e: Exception) {
            Log.e(TAG, "清理资源失败", e)
        }
    }

//    fun Float.dpToPx(): Float {
//        return TypedValue.applyDimension(
//            TypedValue.COMPLEX_UNIT_DIP,
//            this,
//            resources.displayMetrics
//        )
//    }

}
