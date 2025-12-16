package app.rive.runtime.example

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import app.rive.runtime.example.utils.RiveInfoUtil.printRiveFileInfo
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
                    h.postDelayed(this, 1000)
                }
            }
        }, 1000) // 1 second dela

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

}
