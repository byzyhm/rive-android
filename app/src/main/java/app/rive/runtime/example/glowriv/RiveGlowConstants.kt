package app.intent.library.common.ui.widget.glowriv

import app.rive.runtime.example.R


/**
 *
 * Created by zl on 2026/2/6 14:23
 * Desc: RiveGlowPlayer相关常量配置
 * 定义Rive发光边框动画的状态机参数和配置项
 */
object RiveGlowConstants {

    const val TAG = "RiveGlowConstants"

    /**
     * Rive状态机配置
     *
     * ai_glow.riv动画说明：
     * - 仅包含发光边框效果，不含背景
     * - 内部为透明区域，用于显示内容
     */
    object StateMachine {

        /** 获取AI发光效果Rive资源ID */
        fun getAiGlowRiv(): Int = R.raw.ai_glow

        /** 状态机名称 */
        const val STATE_MACHINE_NAME = "StateMachine_1"

        /**
         * 宽度输入参数名
         * - 可调范围为(125, 474)
         */
        const val RIV_WIDTH = "width"

        /**
         * 高度输入参数名
         * - 可调范围为(120, 973)
         */
        const val RIV_HEIGHT = "height"

        /** 宽度默认值 */
        const val DEFAULT_WIDTH = 474f

        /** 高度默认值 */
        const val DEFAULT_HEIGHT = 474f

        /** 宽度最小值 */
        const val MIN_WIDTH = 125f

        /** 宽度最大值 */
        const val MAX_WIDTH = 474f

        /** 高度最小值 */
        const val MIN_HEIGHT = 120f

        /** 高度最大值 */
        const val MAX_HEIGHT = 973f

        const val GLOW_PADDING = 13f
    }

}