package app.rive.runtime.example

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import app.rive.runtime.kotlin.RiveAnimationView
import app.rive.runtime.kotlin.controllers.RiveFileController
import app.rive.runtime.kotlin.core.RiveEvent
import app.rive.runtime.kotlin.core.RiveGeneralEvent
import app.rive.runtime.kotlin.core.SMIBoolean
import app.rive.runtime.kotlin.core.SMINumber
import app.rive.runtime.kotlin.core.SMITrigger
import java.util.Calendar

class InteractiveSamplesActivity : AppCompatActivity() {
    private var keepGoing = true
    fun setTime() {
        val hours =
            (Calendar.getInstance().get(Calendar.HOUR) % 12f + Calendar.getInstance()
                .get(Calendar.MINUTE) / 60f + Calendar.getInstance()
                .get(Calendar.SECOND) / 3600f)
        clockView.setNumberState("Time", "isTime", hours)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.interactive_samples)
        setTime()
        val h = Handler(Looper.getMainLooper())
        h.postDelayed(object : Runnable {
            override fun run() {
                // do stuff then
                // can call h again after work!
                if (keepGoing) {
                    setTime()
                    h.postDelayed(this, 360)
                }
            }
        }, 360) // 1 second dela

        setPlayView()

    }

    private lateinit var playListener: RiveFileController.RiveEventListener

    private fun setPlayView() {

        playListener = object : RiveFileController.RiveEventListener {
            override fun notifyEvent(event: RiveEvent) {

                Log.i("RiveEvent", "name: ${event.name}")
                Log.i("RiveEvent", "delay: ${event.delay}")
                Log.i("RiveEvent", "type: ${event.type}")
                Log.i("RiveEvent", "properties: ${event.properties}")
                // `data` contains all information in the event
                Log.i("RiveEvent", "data: ${event.data}")
            }
        }
        playView.addEventListener(playListener)

        playView.setOnClickListener {
            printRiveFileInfo(playView)
        }
    }

    override fun onDetachedFromWindow() {
        // This the exit point for any RiveAnimationView, if we try to access
        // underlying properties (e.g. setNumberState() above) _after_ we detached, underlying
        // objects have probably been deallocated and this will result in a crash.
        keepGoing = false
        super.onDetachedFromWindow()
    }

    private val clockView by lazy(LazyThreadSafetyMode.NONE) {
        findViewById<RiveAnimationView>(R.id.clock)
    }
    private val playView by lazy(LazyThreadSafetyMode.NONE) {
        findViewById<RiveAnimationView>(R.id.playView)
    }


    /**
     * 打印 Rive 文件的完整信息
     * 包括：Artboard、动画、状态机、输入、Text Run 等
     */
    fun printRiveFileInfo(riveView: RiveAnimationView) {
        val file = riveView.controller.file ?: run {
            Log.w("RiveInfo", "File not loaded")
            return
        }

        Log.d("RiveInfo", "========== Rive File Info ==========")

        // 1. Artboard 信息
        Log.d("RiveInfo", "Artboard count: ${file.artboardCount}")
        Log.d("RiveInfo", "Artboard names: ${file.artboardNames}")

        // 2. 枚举信息
        Log.d("RiveInfo", "Enums: ${file.enums.map { "${it.name}: ${it.values}" }}")

        // 3. ViewModel 信息
        Log.d("RiveInfo", "ViewModel count: ${file.viewModelCount}")

        // 4. 遍历每个 Artboard
        file.artboardNames.forEachIndexed { index, artboardName ->
            Log.d("RiveInfo", "")
            Log.d("RiveInfo", "--- Artboard [$index]: $artboardName ---")

            try {
                val artboard = file.artboard(artboardName)

                // Artboard 尺寸
                Log.d("RiveInfo", "  Size: ${artboard.width} x ${artboard.height}")
                Log.d("RiveInfo", "  Bounds: ${artboard.bounds}")

                // 动画列表
                Log.d("RiveInfo", "  Animation count: ${artboard.animationCount}")
                Log.d("RiveInfo", "  Animations: ${artboard.animationNames}")

                // 状态机列表
                Log.d("RiveInfo", "  State Machine count: ${artboard.stateMachineCount}")
                Log.d("RiveInfo", "  State Machines: ${artboard.stateMachineNames}")

                // 遍历每个状态机
                artboard.stateMachineNames.forEach { smName ->
                    try {
                        val sm = artboard.stateMachine(smName)
                        Log.d("RiveInfo", "")
                        Log.d("RiveInfo", "    [State Machine: $smName]")
                        Log.d("RiveInfo", "    Layer count: ${sm.layerCount}")
                        Log.d("RiveInfo", "    Input count: ${sm.inputCount}")
                        Log.d("RiveInfo", "    Input names: ${sm.inputNames}")

                        // 遍历每个输入
                        sm.inputs.forEach { input ->
                            val type = when {
                                input.isBoolean -> "Boolean"
                                input.isTrigger -> "Trigger"
                                input.isNumber -> "Number"
                                else -> "Unknown"
                            }
                            val value = when (input) {
                                is SMIBoolean -> input.value.toString()
                                is SMINumber -> input.value.toString()
                                is SMITrigger -> "(trigger)"
                                else -> ""
                            }
                            Log.d("RiveInfo", "      Input: ${input.name} ($type) = $value")
                        }

                        sm.release()
                    } catch (e: Exception) {
                        Log.w("RiveInfo", "    Failed to load state machine $smName: ${e.message}")
                    }
                }

                artboard.release()
            } catch (e: Exception) {
                Log.w("RiveInfo", "  Failed to load artboard $artboardName: ${e.message}")
            }
        }

        // 5. 当前活动的 Artboard 信息
        riveView.controller.activeArtboard?.let { activeArtboard ->
            Log.d("RiveInfo", "")
            Log.d("RiveInfo", "--- Active Artboard ---")
            Log.d("RiveInfo", "Name: ${activeArtboard.name}")

            // 尝试获取 Text Run（需要知道名称才能获取）
            // 注意：Rive API 没有提供获取所有 Text Run 名称的方法
        }

        // 6. 当前状态机信息
        riveView.stateMachines.forEach { sm ->
            Log.d("RiveInfo", "")
            Log.d("RiveInfo", "--- Active State Machine: ${sm.name} ---")
            Log.d("RiveInfo", "Inputs: ${sm.inputNames}")
        }

        Log.d("RiveInfo", "========== End of Info ==========")
    }
}
