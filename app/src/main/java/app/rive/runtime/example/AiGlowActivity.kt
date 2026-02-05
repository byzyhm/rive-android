package app.rive.runtime.example

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import app.rive.runtime.example.databinding.ActivityAiGlowBinding
import app.rive.runtime.example.databinding.OnbardingStateMachineBinding
import app.rive.runtime.example.font.FontPreloader
import app.rive.runtime.example.utils.setEdgeToEdgeContent
import app.rive.runtime.kotlin.controllers.RiveFileController
import app.rive.runtime.kotlin.core.Fit
import app.rive.runtime.kotlin.core.PlayableInstance

class AiGlowActivity : ComponentActivity() {



    private lateinit var binding: ActivityAiGlowBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAiGlowBinding.inflate(layoutInflater)
        setEdgeToEdgeContent(binding.root)
//
//        // 字体回退策略已在 RiveExampleApplication 中全局设置
//        // 使用 FontPreloader 预加载的字体，避免在此处同步加载导致卡顿
//
//        // 设置字体加载器（使用 FontPreloader 中的静态类，避免内存泄漏）
        // 必须在 setRiveResource 之前设置 assetLoader
        val riveView = binding.aiGlowRiv
        riveView.setRiveResource(
            resId = R.raw.ai_glow,
            stateMachineName = "StateMachine_1",
            autoplay = true
        )
        riveView.fit = Fit.FILL

//        // 设置监听器（可选，用于调试）
//        setupListener()
//
//        // 设置文本内容（在状态机启动前设置 Text Run）
//        setupTextContent()
//
//        Log.d(TAG, "onCreate - 等待状态机自动启动")
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }


}
