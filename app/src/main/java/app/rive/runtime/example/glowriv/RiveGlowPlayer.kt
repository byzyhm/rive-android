package app.rive.runtime.example.glowriv

import android.view.View
import android.view.ViewTreeObserver
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import app.intent.library.common.ui.widget.glowriv.RiveGlowConstants
import app.rive.RiveLog
import app.rive.runtime.kotlin.RiveAnimationView
import app.rive.runtime.kotlin.controllers.RiveFileController
import app.rive.runtime.kotlin.core.Alignment
import app.rive.runtime.kotlin.core.Fit
import app.rive.runtime.kotlin.core.PlayableInstance

/**
 * ai_glow.riv动画播放器（父容器必须是ConstraintLayout）
 * Created by zl on 2026/2/6 14:27
 *
 * @param rivView Rive动画视图
 * @param anchorView 动画视图的锚点，发光边框将包裹此View
 */
class RiveGlowPlayer(
    private val rivView: RiveAnimationView,
    private val anchorView: View
) {

    /** GLOW_PADDING作为兜底使用 */
    private val fallbackGlowPaddingPx: Int =
        (rivView.resources.displayMetrics.density * RiveGlowConstants.StateMachine.GLOW_PADDING).toInt()

    private var riveListener: RiveFileController.Listener? = null

    private var anchorLayoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null

    private var marginLayoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null

    /** 记录上一次anchorView的高度，用于避免重复更新 */
    private var lastAnchorHeight: Int = -1

    /** 记录上一次rivView的宽度，用于检测margin变更后的宽度变化 */
    private var lastRivViewWidth: Int = -1

    /** 标记动画是否已初始化 */
    private var isInitialized = false

    /** 标记是否已销毁 */
    private var isDestroyed = false

    /** 当前设置的宽度 */
    private var currentWidth = RiveGlowConstants.StateMachine.DEFAULT_WIDTH

    /** 当前设置的高度 */
    private var currentHeight = RiveGlowConstants.StateMachine.DEFAULT_HEIGHT

    /**
     * 初始化并播放动画
     *
     * @param riveResId Rive资源ID
     * @param stateMachineName 状态机名称
     * @param autoplay 是否自动播放
     */
    fun initialize(
        riveResId: Int = RiveGlowConstants.StateMachine.getAiGlowRiv(),
        stateMachineName: String = RiveGlowConstants.StateMachine.STATE_MACHINE_NAME,
        autoplay: Boolean = true
    ) {
        if (isDestroyed) {
            RiveLog.w(RiveGlowConstants.TAG) { "${RiveGlowConstants.TAG}:initialize - 已销毁，跳过" }
            return
        }

        if (isInitialized) {
            RiveLog.d(RiveGlowConstants.TAG) { "${RiveGlowConstants.TAG}:initialize - 已初始化，跳过" }
            return
        }

        try {
            RiveLog.d(RiveGlowConstants.TAG) { "${RiveGlowConstants.TAG}:initialize - 开始初始化Rive动画" }

            // 设置监听器
            setupListener()

            // 加载Rive资源
            rivView.setRiveResource(
                resId = riveResId,
                stateMachineName = stateMachineName,
                autoplay = autoplay
            )
            rivView.fit = Fit.CONTAIN
            rivView.alignment = Alignment.TOP_LEFT

            // 设置固定宽度（只需初始化时设置一次）
            rivView.setNumberState(
                stateMachineName,
                RiveGlowConstants.StateMachine.RIV_WIDTH,
                RiveGlowConstants.StateMachine.DEFAULT_WIDTH
            )

            isInitialized = true
            RiveLog.d(RiveGlowConstants.TAG) { "${RiveGlowConstants.TAG}:initialize - 初始化完成，固定宽度=${RiveGlowConstants.StateMachine.DEFAULT_WIDTH}" }

            // 动态设置rivView的margin约束（使发光边框均匀包裹anchorView）
            setupRivViewMargins()

            // 设置anchorView高度监听，需等待View布局完成
            setupAnchorHeightListener()
        } catch (e: Exception) {
            RiveLog.e(
                RiveGlowConstants.TAG,
                e
            ) { "${RiveGlowConstants.TAG}:initialize - 初始化失败" }
            isDestroyed = true
        }
    }

    /**
     * 更新动画高度参数
     *
     * @param height 内容区域高度（px）
     */
    fun updateRiveSize(height: Float) {
        if (isDestroyed || !isInitialized) {
            RiveLog.w(RiveGlowConstants.TAG) { "${RiveGlowConstants.TAG}:updateSize - 未初始化或已销毁，跳过" }
            return
        }

        // 限制在有效范围内
        val clampedHeight = height.coerceIn(
            RiveGlowConstants.StateMachine.MIN_HEIGHT,
            RiveGlowConstants.StateMachine.MAX_HEIGHT
        )

        // 避免不必要的更新
        if (clampedHeight == currentHeight) {
            return
        }

        currentHeight = clampedHeight

        RiveLog.d(RiveGlowConstants.TAG) { "${RiveGlowConstants.TAG}:updateSize - height=$clampedHeight" }

        // 设置动态高度
        rivView.setNumberState(
            RiveGlowConstants.StateMachine.STATE_MACHINE_NAME,
            RiveGlowConstants.StateMachine.RIV_HEIGHT,
            clampedHeight
        )
    }

    /**
     * 显示rivView并播放动画
     */
    fun showAndPlay() {
        if (isDestroyed) return
        RiveLog.d(RiveGlowConstants.TAG) { "${RiveGlowConstants.TAG}:showAndPlay" }
        rivView.visibility = View.VISIBLE
        if (isInitialized) {
            rivView.play()
        }
    }

    /**
     * 停止动画并隐藏rivView
     */
    fun stopAndHide() {
        if (isDestroyed) return
        RiveLog.d(RiveGlowConstants.TAG) { "${RiveGlowConstants.TAG}:stopAndHide" }
        rivView.pause()
        rivView.visibility = View.GONE
    }

    /**
     * 播放动画
     */
    fun play() {
        if (isDestroyed) return
        RiveLog.d(RiveGlowConstants.TAG) { "${RiveGlowConstants.TAG}:play" }
        rivView.play()
    }

    /**
     * 暂停动画
     */
    fun pause() {
        if (isDestroyed) return
        RiveLog.d(RiveGlowConstants.TAG) { "${RiveGlowConstants.TAG}:pause" }
        rivView.pause()
    }

    /**
     * 重置动画
     */
    fun reset() {
        if (isDestroyed) return
        RiveLog.d(RiveGlowConstants.TAG) { "${RiveGlowConstants.TAG}:reset" }
        reRegisterListener()
        rivView.pause()
        rivView.reset()
        rivView.play()
    }

    /**
     * 销毁播放器，释放资源
     */
    fun destroy() {
        RiveLog.d(RiveGlowConstants.TAG) { "${RiveGlowConstants.TAG}:destroy - 释放资源" }

        isDestroyed = true
        isInitialized = false

        // 移除margin布局监听器
        removeMarginLayoutListener()

        // 移除anchorView高度监听器
        removeAnchorHeightListener()

        // 注销Rive状态机监听器
        riveListener?.let {
            try {
                rivView.unregisterListener(it)
            } catch (e: Exception) {
                RiveLog.e(
                    RiveGlowConstants.TAG,
                    e
                ) { "${RiveGlowConstants.TAG}:destroy - Listener注销失败" }
            }
        }
        riveListener = null
    }

    /**
     * 是否已初始化
     */
    fun isInitialized(): Boolean = isInitialized

    /**
     * 强制更新位置和尺寸
     *
     * 在锚点View的位置或尺寸发生变化但ViewTreeObserver未触发时调用。
     * 例如：当约束链中的其他View显示/隐藏时，可能需要主动调用此方法。
     */
    fun forceUpdatePosition() {
        if (isDestroyed || !isInitialized) {
            RiveLog.w(RiveGlowConstants.TAG) { "${RiveGlowConstants.TAG}:forceUpdatePosition - 未初始化或已销毁，跳过" }
            return
        }

        if (!anchorView.isLaidOut) {
            RiveLog.w(RiveGlowConstants.TAG) { "${RiveGlowConstants.TAG}:forceUpdatePosition - anchorView尚未布局，跳过" }
            return
        }

        // 重新计算并应用margin
        applyRivViewMargins()

        // 强制重新计算高度（通过重置lastAnchorHeight和lastRivViewWidth）
        val anchorHeight = anchorView.height
        val currentRivViewWidth = rivView.width

        // 重置记录值，强制触发下一次更新
        lastAnchorHeight = -1
        lastRivViewWidth = -1

        if (anchorHeight > 0 && currentRivViewWidth > 0) {
            try {
                val artboard = rivView.controller.activeArtboard
                if (artboard == null) {
                    RiveLog.w(RiveGlowConstants.TAG) { "${RiveGlowConstants.TAG}:forceUpdatePosition - Artboard尚未加载" }
                    return
                }

                val artboardWidth = artboard.bounds.width()
                val coefficient = artboardWidth / currentRivViewWidth.toFloat()
                val calculatedHeight = anchorHeight * coefficient

                // 更新记录值
                lastAnchorHeight = anchorHeight
                lastRivViewWidth = currentRivViewWidth

                updateRiveSize(calculatedHeight)
                RiveLog.d(RiveGlowConstants.TAG) {
                    "forceUpdatePosition - " +
                            "anchorHeight=${anchorHeight}px, rivViewWidth=${currentRivViewWidth}px, " +
                            "系数=$coefficient, 状态机height=${calculatedHeight.toInt()}"
                }
            } catch (e: Exception) {
                RiveLog.e(
                    RiveGlowConstants.TAG,
                    e
                ) { "${RiveGlowConstants.TAG}:forceUpdatePosition - 更新失败" }
            }
        }
    }

    /**
     * 设置rivView的margin约束（top、start、end）
     */
    private fun setupRivViewMargins() {
        if (anchorView.isLaidOut) {
            // View已完成布局，直接计算并应用margin
            applyRivViewMargins()
        } else {
            // View尚未完成布局，等待布局完成后再计算
            RiveLog.d(RiveGlowConstants.TAG) { "${RiveGlowConstants.TAG}:setupRivViewMargins - anchorView尚未布局，等待布局完成" }
            marginLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
                // 只执行一次，获取到值后立即移除监听器
                removeMarginLayoutListener()
                applyRivViewMargins()
            }
            anchorView.viewTreeObserver.addOnGlobalLayoutListener(marginLayoutListener)
        }
    }

    /**
     * 计算并应用rivView的margin约束（top、start、end）
     */
    private fun applyRivViewMargins() {
        val parent = rivView.parent as? ConstraintLayout ?: run {
            RiveLog.w(RiveGlowConstants.TAG) { "${RiveGlowConstants.TAG}:applyRivViewMargins - 父容器不是ConstraintLayout，跳过" }
            return
        }

        // 从Artboard运行时获取真实宽度
        val artboardWidth = try {
            rivView.controller.activeArtboard?.bounds?.width()
                ?: RiveGlowConstants.StateMachine.DEFAULT_WIDTH
        } catch (e: Exception) {
            RiveGlowConstants.StateMachine.DEFAULT_WIDTH
        }
        val glowPadding = RiveGlowConstants.StateMachine.GLOW_PADDING     // 13f
        val denominator = artboardWidth - 2 * glowPadding

        // ---- 水平方向：精确求解marginStart / marginEnd ----
        val parentPaddingStart = parent.paddingStart
        val parentPaddingEnd = parent.paddingEnd
        val parentContentWidth = parent.width - parentPaddingStart - parentPaddingEnd

        // anchorView相对于parent内容区域的有效margin
        val anchorMarginStart = anchorView.left - parentPaddingStart
        val anchorMarginEnd = parent.width - anchorView.right - parentPaddingEnd

        // 代数精确解：m = (w×A - g×P) / (w - 2g)
        val marginStart =
            ((artboardWidth * anchorMarginStart - glowPadding * parentContentWidth) / denominator)
                .toInt().coerceAtLeast(0)
        val marginEnd =
            ((artboardWidth * anchorMarginEnd - glowPadding * parentContentWidth) / denominator)
                .toInt().coerceAtLeast(0)

        // ---- 垂直方向：基于新宽度计算marginTop ----
        val newRivViewWidth = parentContentWidth - marginStart - marginEnd
        val actualGlowPadding = if (newRivViewWidth > 0) {
            (glowPadding * newRivViewWidth / artboardWidth).toInt()
        } else {
            fallbackGlowPaddingPx
        }

        // anchorView.top包含parent.paddingTop（fitsSystemWindows），
        // 而constraint系统会自动在paddingTop之后布局，需减去避免重复计算
        val parentPaddingTop = parent.paddingTop
        val marginTop = (anchorView.top - parentPaddingTop - actualGlowPadding).coerceAtLeast(0)

        // ---- 统一应用约束 ----
        val constraintSet = ConstraintSet()
        constraintSet.clone(parent)
        constraintSet.setMargin(rivView.id, ConstraintSet.TOP, marginTop)
        constraintSet.setMargin(rivView.id, ConstraintSet.START, marginStart)
        constraintSet.setMargin(rivView.id, ConstraintSet.END, marginEnd)
        constraintSet.applyTo(parent)

        RiveLog.d(
            RiveGlowConstants.TAG
        ) {
            "applyRivViewMargins - " +
                    "marginTop=$marginTop, marginStart=$marginStart, marginEnd=$marginEnd, " +
                    "actualGlowPadding=$actualGlowPadding, newRivViewWidth=$newRivViewWidth, " +
                    "artboardWidth=$artboardWidth " +
                    "(anchorView: left=${anchorView.left}, top=${anchorView.top}, right=${anchorView.right}, " +
                    "parentPadding: top=$parentPaddingTop, start=$parentPaddingStart, end=$parentPaddingEnd)"
        }
    }

    /**
     * 移除margin布局监听器
     */
    private fun removeMarginLayoutListener() {
        marginLayoutListener?.let {
            try {
                anchorView.viewTreeObserver.removeOnGlobalLayoutListener(it)
            } catch (e: Exception) {
                RiveLog.e(
                    RiveGlowConstants.TAG,
                    e
                ) { "removeMarginLayoutListener - 移除监听器失败" }
            }
            marginLayoutListener = null
            RiveLog.d(RiveGlowConstants.TAG) { "${RiveGlowConstants.TAG}:removeMarginLayoutListener - margin监听器已移除" }
        }
    }

    /**
     * 设置anchorView高度监听器
     *
     * 实时监听anchorView的高度变化，并根据高度计算状态机的height值。
     */
    private fun setupAnchorHeightListener() {
        anchorLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
            val anchorHeight = anchorView.height
            val currentRivViewWidth = rivView.width

            // 当anchorView高度变化 或 rivView宽度变化时，重新计算状态机height
            // rivView宽度变化场景：applyRivViewMargins()动态修改margin后，
            // 下一帧布局生效导致rivView宽度更新，需用新宽度重新计算系数
            val heightChanged = anchorHeight > 0 && anchorHeight != lastAnchorHeight
            val widthChanged = currentRivViewWidth > 0 && currentRivViewWidth != lastRivViewWidth

            if (heightChanged || widthChanged) {
                lastAnchorHeight = anchorHeight
                lastRivViewWidth = currentRivViewWidth

                try {
                    val artboard = rivView.controller.activeArtboard
                    if (artboard == null) {
                        RiveLog.w(RiveGlowConstants.TAG) { "${RiveGlowConstants.TAG}:anchorListener - Artboard尚未加载，跳过" }
                        return@OnGlobalLayoutListener
                    }

                    val artboardWidth = artboard.bounds.width()

                    if (currentRivViewWidth <= 0) {
                        RiveLog.w(RiveGlowConstants.TAG) { "${RiveGlowConstants.TAG}:anchorListener - View宽度为0，跳过" }
                        return@OnGlobalLayoutListener
                    }

                    // 计算变化系数
                    val coefficient = artboardWidth / currentRivViewWidth.toFloat()

                    // 计算状态机height值：anchorView高度(px) × 系数 = Artboard坐标系高度
                    val calculatedHeight = anchorHeight * coefficient

                    // 通过updateHeightSize更新，内部限制范围并设置状态机参数
                    updateRiveSize(calculatedHeight)

                    RiveLog.d(
                        RiveGlowConstants.TAG

                    ) {
                        "anchorListener - " +
                                "anchorHeight=${anchorHeight}px, rivViewWidth=${currentRivViewWidth}px, " +
                                "系数=$coefficient, 状态机height=${calculatedHeight.toInt()} " +
                                "(heightChanged=$heightChanged, widthChanged=$widthChanged)"
                    }
                } catch (e: Exception) {
                    RiveLog.e(
                        RiveGlowConstants.TAG,
                        e
                    ) { "${RiveGlowConstants.TAG}:anchorListener - 更新状态机height失败" }
                }
            }
        }

        anchorView.viewTreeObserver.addOnGlobalLayoutListener(anchorLayoutListener)
        RiveLog.d(RiveGlowConstants.TAG) { "${RiveGlowConstants.TAG}:setupAnchorHeightListener - anchorView高度监听器已设置" }
    }

    /**
     * 移除anchorView高度监听器
     */
    private fun removeAnchorHeightListener() {
        anchorLayoutListener?.let {
            try {
                anchorView.viewTreeObserver.removeOnGlobalLayoutListener(it)
            } catch (e: Exception) {
                RiveLog.e(
                    RiveGlowConstants.TAG,
                    e
                ) { "${RiveGlowConstants.TAG}:removeAnchorHeightListener - 移除监听器失败" }
            }
            anchorLayoutListener = null
            lastAnchorHeight = -1
            lastRivViewWidth = -1
            RiveLog.d(RiveGlowConstants.TAG) { "${RiveGlowConstants.TAG}:removeAnchorHeightListener - anchorView高度监听器已移除" }
        }
    }

    /**
     * 设置状态机监听器
     */
    private fun setupListener() {
        riveListener = object : RiveFileController.Listener {
            override fun notifyPlay(animation: PlayableInstance) {
                RiveLog.d(RiveGlowConstants.TAG) { "${RiveGlowConstants.TAG}: Event: Play ${animation.name}" }
            }

            override fun notifyPause(animation: PlayableInstance) {
                RiveLog.d(RiveGlowConstants.TAG) { "${RiveGlowConstants.TAG}: Event: Pause ${animation.name}" }
            }

            override fun notifyStop(animation: PlayableInstance) {
                RiveLog.d(RiveGlowConstants.TAG) { "${RiveGlowConstants.TAG}: Event: Stop ${animation.name}" }
            }

            override fun notifyLoop(animation: PlayableInstance) {
                RiveLog.d(RiveGlowConstants.TAG) { "${RiveGlowConstants.TAG}: Event: Loop ${animation.name}" }
            }

            override fun notifyStateChanged(stateMachineName: String, stateName: String) {
                RiveLog.d(RiveGlowConstants.TAG) { "${RiveGlowConstants.TAG}: Event: State Changed - $stateMachineName: $stateName" }
            }
        }
        riveListener?.let { rivView.registerListener(it) }
    }

    /**
     * 重新注册Listener
     */
    private fun reRegisterListener() {
        riveListener?.let { listener ->
            try {
                rivView.unregisterListener(listener)
            } catch (e: Exception) {
                RiveLog.e(
                    RiveGlowConstants.TAG,
                    e
                ) { "${RiveGlowConstants.TAG}:reRegisterListener - 注销旧Listener失败" }
            }
            rivView.registerListener(listener)
        }
    }
}
