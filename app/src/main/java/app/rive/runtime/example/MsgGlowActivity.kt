package app.rive.runtime.example

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.SeekBar
import androidx.activity.ComponentActivity
import app.rive.runtime.example.databinding.ActivityMsgGlowBinding
import app.rive.runtime.example.utils.setEdgeToEdgeContent
import app.rive.runtime.kotlin.core.Fit

/**
 * MsgGlowActivity：展示消息气泡发光描边动画。
 *
 * 核心设计：RiveAnimationView（glowRiv）不再手动计算尺寸，
 * 直接通过 XML 约束自动跟随 glowContainerCL 的宽高。
 * 使用 Fit.FILL 允许动画拉伸变形，适配任意高度的消息气泡。
 *
 * 与 AiGlowActivity 的区别：
 * - 无 width/height 状态机输入，不需要手动换算坐标
 * - 使用 Fit.FILL 替代 Fit.CONTAIN，允许失真
 * - 布局层面解决循环依赖：glowRiv 作为兄弟节点约束到容器边界
 * - 只保留文本长度控制，简化 demo 控制面板
 */
class MsgGlowActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MsgGlowActivity"
        private const val TEXT_LENGTH_MAX = 100
        private const val TEXT_LENGTH_INITIAL = 100
        private const val TEXT_STEP = 5
    }

    private lateinit var binding: ActivityMsgGlowBinding

    private lateinit var fullText: String
    private var currentTextLength = TEXT_LENGTH_INITIAL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMsgGlowBinding.inflate(layoutInflater)
        setEdgeToEdgeContent(binding.root)

        fullText = binding.anchorView.text.toString()

        setupRiveAnimation()
        setupControls()
        updateLabels()
    }

    /**
     * 初始化 Rive 动画。
     * 使用 Fit.FILL 让动画充满 View 并允许拉伸变形，
     * 无需计算状态机的 width/height 输入参数。
     */
    private fun setupRiveAnimation() {
        val riveView = binding.glowRiv
        riveView.setRiveResource(
            resId = R.raw.ai_glow_for_msg_bubble,
            autoplay = true,
        )
        // FILL：动画直接拉伸填满 View，允许失真变形
        riveView.fit = Fit.FILL

        // 延迟打印尺寸信息，等待首帧布局完成
        Handler(Looper.getMainLooper()).postDelayed({
            logSizeInfo()
        }, 200)
    }

    /**
     * 打印 View 与 Artboard 尺寸信息，便于调试动画效果。
     */
    private fun logSizeInfo() {
        try {
            val riveView = binding.glowRiv
            val artboard = riveView.controller.activeArtboard

            Log.d(TAG, "========== 尺寸信息 ==========")
            Log.d(TAG, "glowRiv  View : ${riveView.width}px × ${riveView.height}px")
            Log.d(TAG, "glowContainerCL : ${binding.glowContainerCL.width}px × ${binding.glowContainerCL.height}px")
            Log.d(TAG, "anchorView  : ${binding.anchorView.width}px × ${binding.anchorView.height}px")

            artboard?.let {
                Log.d(TAG, "Artboard : ${it.width} × ${it.height}")
                val ratio = it.width / it.height
                Log.d(TAG, "Artboard 宽高比: ${"%.3f".format(ratio)}")
            } ?: Log.w(TAG, "Artboard 尚未加载")

            Log.d(TAG, "Fit 模式: ${riveView.fit}")
            Log.d(TAG, "==============================")
        } catch (e: Exception) {
            Log.e(TAG, "打印尺寸信息失败", e)
        }
    }

    /**
     * 设置文本长度控制：SeekBar + 步进按钮。
     */
    private fun setupControls() {
        binding.textLengthSeekBar.max = TEXT_LENGTH_MAX
        binding.textLengthSeekBar.progress = TEXT_LENGTH_INITIAL

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

        binding.btnDecreaseTextLength.setOnClickListener {
            applyTextLengthDelta(-TEXT_STEP)
        }

        binding.btnIncreaseTextLength.setOnClickListener {
            applyTextLengthDelta(+TEXT_STEP)
        }
    }

    private fun applyTextLengthDelta(delta: Int) {
        val newProgress = (binding.textLengthSeekBar.progress + delta)
            .coerceIn(0, TEXT_LENGTH_MAX)
        binding.textLengthSeekBar.progress = newProgress
        currentTextLength = newProgress
        updateTextContent()
        updateLabels()
    }

    private fun updateLabels() {
        binding.textLengthLabel.text = "Text Length: $currentTextLength%"
    }

    /**
     * 根据百分比截取 fullText，最短保留 1 个字符防止气泡高度坍塌。
     */
    private fun updateTextContent() {
        val targetLength = (fullText.length * currentTextLength / 100.0).toInt()
            .coerceIn(1, fullText.length)
        binding.anchorView.text = fullText.substring(0, targetLength)
    }

    override fun onPause() {
        super.onPause()
        try {
            binding.glowRiv.pause()
            Log.d(TAG, "动画已暂停")
        } catch (e: Exception) {
            Log.e(TAG, "暂停动画失败", e)
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            binding.glowRiv.play()
            Log.d(TAG, "动画已恢复")
        } catch (e: Exception) {
            Log.e(TAG, "恢复动画失败", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            binding.glowRiv.stop()
            Log.d(TAG, "资源已清理")
        } catch (e: Exception) {
            Log.e(TAG, "清理资源失败", e)
        }
    }
}
