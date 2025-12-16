package app.rive.runtime.example.utils

import android.util.Log
import app.rive.runtime.kotlin.RiveAnimationView
import app.rive.runtime.kotlin.core.SMIBoolean
import app.rive.runtime.kotlin.core.SMINumber
import app.rive.runtime.kotlin.core.SMITrigger

/**
 * Rive 文件信息打印工具
 * 
 * ⚠️ 重要：引用计数管理
 * 
 * Rive SDK 使用引用计数来管理 Native 对象的生命周期。
 * 当调用 file.artboard() 或 artboard.stateMachine() 时：
 * 1. 会创建新的实例（refs = 1）
 * 2. 该实例会自动添加到父对象的 dependencies 中
 * 3. 当父对象被释放时，会自动释放所有 dependencies
 * 
 * ❌ 错误用法：
 *   val artboard = file.artboard("name")
 *   artboard.release()  // 错误！这会导致 refs=0，但它仍在 file.dependencies 中
 *                       // 当 file 释放时会再次 release()，导致 refs<0，崩溃！
 * 
 * ✅ 正确用法：
 *   - 让 File 统一管理所有创建的 Artboard 生命周期
 *   - 或者使用 controller.activeArtboard（已由 controller 管理）
 *   - 不要手动调用 release()，除非你明确知道自己在做什么
 */
object RiveInfoUtil {
    private const val TAG = "RiveInfo"
    
    /**
     * 打印 Rive 文件的详细信息
     * 
     * 此方法采用"安全只读"模式：
     * - 优先使用只读属性（如 artboardNames）
     * - 对于需要创建实例的操作，不手动释放（由 File 统一管理）
     * - 优先使用 controller 已管理的对象（如 activeArtboard）
     */
    fun printRiveFileInfo(riveView: RiveAnimationView) {
        val file = riveView.controller.file ?: run {
            Log.w(TAG, "File not loaded")
            return
        }

        Log.d(TAG, "========== Rive File Info ==========")

        // 1. 基本信息（只读属性，安全）
        Log.d(TAG, "Artboard count: ${file.artboardCount}")
        Log.d(TAG, "Artboard names: ${file.artboardNames}")
        Log.d(TAG, "Enums: ${file.enums.map { "${it.name}: ${it.values}" }}")
        Log.d(TAG, "ViewModel count: ${file.viewModelCount}")

        // 2. 使用 controller 的 activeArtboard（安全，由 controller 管理）
        val activeArtboard = riveView.controller.activeArtboard
        if (activeArtboard != null) {
            Log.d(TAG, "")
            Log.d(TAG, "--- Active Artboard: ${activeArtboard.name} ---")
            Log.d(TAG, "  Size: ${activeArtboard.width} x ${activeArtboard.height}")
            Log.d(TAG, "  Bounds: ${activeArtboard.bounds}")
            Log.d(TAG, "  Animation count: ${activeArtboard.animationCount}")
            Log.d(TAG, "  Animations: ${activeArtboard.animationNames}")
            Log.d(TAG, "  State Machine count: ${activeArtboard.stateMachineCount}")
            Log.d(TAG, "  State Machines: ${activeArtboard.stateMachineNames}")
        }

        // 3. 使用 controller 的 stateMachines（安全，由 controller 管理）
        riveView.stateMachines.forEach { sm ->
            Log.d(TAG, "")
            Log.d(TAG, "  [Active State Machine: ${sm.name}]")
            Log.d(TAG, "  Layer count: ${sm.layerCount}")
            Log.d(TAG, "  Input count: ${sm.inputCount}")
            Log.d(TAG, "  Input names: ${sm.inputNames}")

            // 遍历输入
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
                Log.d(TAG, "    Input: ${input.name} ($type) = $value")
            }
        }

        // 4. API 限制说明
        Log.d(TAG, "")
        Log.d(TAG, "⚠️ API Limitations:")
        Log.d(TAG, "  - Text Run names: Cannot enumerate (designer must provide)")
        Log.d(TAG, "  - Nested Artboard paths: Cannot enumerate (not exposed in Kotlin API)")
        Log.d(TAG, "  - Events: Only available when triggered")

        Log.d(TAG, "========== End of Info ==========")
    }
    
    /**
     * 打印完整的 Rive 文件信息（包括非活动的 Artboard）
     * 
     * ⚠️ 注意：此方法会创建临时 Artboard 实例
     * 这些实例会被添加到 File.dependencies，由 File 统一管理
     * 不要手动调用 release()！
     */
    fun printFullRiveFileInfo(riveView: RiveAnimationView) {
        val file = riveView.controller.file ?: run {
            Log.w(TAG, "File not loaded")
            return
        }

        Log.d(TAG, "========== Full Rive File Info ==========")
        Log.d(TAG, "Artboard count: ${file.artboardCount}")

        // 遍历所有 Artboard（会创建临时实例）
        file.artboardNames.forEachIndexed { index, artboardName ->
            Log.d(TAG, "")
            Log.d(TAG, "--- Artboard [$index]: $artboardName ---")

            try {
                // ⚠️ 这会创建新实例并添加到 file.dependencies
                // 不要调用 artboard.release()！
                val artboard = file.artboard(artboardName)

                Log.d(TAG, "  Size: ${artboard.width} x ${artboard.height}")
                Log.d(TAG, "  Animations: ${artboard.animationNames}")
                Log.d(TAG, "  State Machines: ${artboard.stateMachineNames}")

                // 打印状态机详情
                artboard.stateMachineNames.forEach { smName ->
                    try {
                        // ⚠️ 同样，不要调用 sm.release()！
                        val sm = artboard.stateMachine(smName)
                        Log.d(TAG, "    [SM: $smName] Inputs: ${sm.inputNames}")
                        // ❌ 不要调用 sm.release()！
                    } catch (e: Exception) {
                        Log.w(TAG, "    Failed to load SM $smName: ${e.message}")
                    }
                }
                // ❌ 不要调用 artboard.release()！
            } catch (e: Exception) {
                Log.w(TAG, "  Failed to load artboard: ${e.message}")
            }
        }

        Log.d(TAG, "========== End of Full Info ==========")
    }
}