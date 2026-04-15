package com.sleepguardian

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.widget.FrameLayout

/**
 * 可發光白框卡片，作為模式選擇的容器。
 * - 一般狀態：細白邊框 (2dp)
 * - 選中狀態：粗白邊框 + setShadowLayer 白色外光暈（需 LAYER_TYPE_SOFTWARE）
 * 黑色底、白色線條，符合像素風格。
 */
class GlowCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    var isGlowing = false
        set(value) {
            if (field == value) return
            field = value
            setLayerType(
                if (value) LAYER_TYPE_SOFTWARE else LAYER_TYPE_HARDWARE,
                null
            )
            invalidate()
        }

    private val glowPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 5f
        isAntiAlias = false
        setShadowLayer(28f, 0f, 0f, Color.WHITE)
    }

    private val normalPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2f
        isAntiAlias = false
    }

    init {
        setWillNotDraw(false)
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    override fun onDraw(canvas: Canvas) {
        // 黑色底
        canvas.drawColor(Color.BLACK)
        // 外框（發光或普通）
        val paint = if (isGlowing) glowPaint else normalPaint
        val h = paint.strokeWidth / 2f + 1f
        canvas.drawRect(h, h, width - h, height - h, paint)
    }
}
