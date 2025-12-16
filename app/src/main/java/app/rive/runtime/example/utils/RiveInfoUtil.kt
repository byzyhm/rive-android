package app.rive.runtime.example.utils

import android.util.Log
import app.rive.runtime.kotlin.RiveAnimationView
import app.rive.runtime.kotlin.core.SMIBoolean
import app.rive.runtime.kotlin.core.SMINumber
import app.rive.runtime.kotlin.core.SMITrigger

object RiveInfoUtil {
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

            // ⚠️ 限制说明：
            // 1. Text Run 名称无法枚举 - 需要设计师提供
            // 2. 嵌套 Artboard 路径无法枚举 - Kotlin API 未暴露此功能
            //    （C++ API 有 nestedArtboards() 但 Kotlin 层未绑定）
            // 3. Event 信息只能在触发时获取

            Log.d("RiveInfo", "")
            Log.d("RiveInfo", "⚠️ API Limitations:")
            Log.d("RiveInfo", "  - Text Run names: Cannot enumerate (designer must provide)")
            Log.d("RiveInfo", "  - Nested Artboard paths: Cannot enumerate (not exposed in Kotlin API)")
            Log.d("RiveInfo", "  - Events: Only available when triggered")
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