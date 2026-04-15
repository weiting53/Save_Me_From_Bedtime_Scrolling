package com.sleepguardian

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

/**
 * 純 Canvas 繪製的睡眠插畫：
 * - 左側：兔子（大耳、圓頭、橢圓身體、閉眼）
 * - 右側：小女巫（尖帽、圓頭、閉眼、靠在兔子身邊）
 * - 上方：漂浮的 Z z z 文字
 * - 底部：地面線
 * 純白 (#FFFFFF) 線條 / 填充，黑底透明。
 */
class SleepingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; style = Paint.Style.FILL
    }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND
    }
    private val blackStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK; style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND
    }
    private val dimText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; typeface = Typeface.MONOSPACE
    }

    private val path = Path()
    private val oval = RectF()

    override fun onDraw(canvas: Canvas) {
        val W = width.toFloat()
        val H = height.toFloat()
        val S = min(W, H)          // 用較小邊作比例基準

        // ── Zzz 文字 ──────────────────────────────────────────────
        dimText.textSize = S * 0.11f
        dimText.alpha = 110
        val zz = "Z  z  z"
        canvas.drawText(zz, W * 0.44f - dimText.measureText(zz) / 2f, H * 0.17f, dimText)

        // ── 地面線 ─────────────────────────────────────────────────
        val groundY = H * 0.86f
        stroke.strokeWidth = S * 0.005f
        canvas.drawLine(W * 0.04f, groundY, W * 0.96f, groundY, stroke)

        // ── 兔子 ──────────────────────────────────────────────────
        drawBunny(canvas, W * 0.32f, groundY, S)

        // ── 小女巫 ────────────────────────────────────────────────
        drawGirl(canvas, W * 0.68f, groundY, S)
    }

    // ────────────────────────────────────────────────────────────
    // 兔子：圓頭、大耳、橢圓身體
    // ────────────────────────────────────────────────────────────
    private fun drawBunny(canvas: Canvas, cx: Float, groundY: Float, S: Float) {
        val bodyRx = S * 0.155f
        val bodyRy = S * 0.125f
        val headR  = bodyRy * 0.78f
        val earW   = bodyRx * 0.20f
        val earH   = headR  * 1.15f

        val bodyTopY = groundY - bodyRy * 1.6f
        val headY    = bodyTopY - headR * 0.9f

        // 左耳
        oval.set(cx - earW * 3.2f - earW, headY - earH - headR * 0.2f,
                 cx - earW * 3.2f + earW, headY - headR * 0.2f)
        canvas.drawRoundRect(oval, earW, earW, fill)

        // 右耳（稍微偏右、偏低）
        oval.set(cx - earW * 0.8f - earW, headY - earH * 0.88f - headR * 0.2f,
                 cx - earW * 0.8f + earW, headY - headR * 0.2f)
        canvas.drawRoundRect(oval, earW, earW, fill)

        // 頭部
        canvas.drawCircle(cx - earW * 1.2f, headY, headR, fill)

        // 身體（橢圓，靠左傾，與女巫相接）
        oval.set(cx - bodyRx * 1.6f, bodyTopY - bodyRy * 0.4f,
                 cx + bodyRx * 0.6f, groundY)
        canvas.drawOval(oval, fill)

        // 閉眼（黑色弧線畫在白色頭部上）
        val eyeX = cx - earW * 1.2f
        val eyeR = headR * 0.32f
        blackStroke.strokeWidth = S * 0.012f
        oval.set(eyeX - eyeR, headY - eyeR * 0.2f, eyeX + eyeR, headY + eyeR * 0.8f)
        canvas.drawArc(oval, 10f, 160f, false, blackStroke)

        // 腮紅（小圓點，半透明白）
        fill.alpha = 60
        canvas.drawCircle(eyeX + headR * 0.38f, headY + headR * 0.30f, headR * 0.18f, fill)
        fill.alpha = 255
    }

    // ────────────────────────────────────────────────────────────
    // 小女巫：尖帽、圓臉、靠向兔子
    // ────────────────────────────────────────────────────────────
    private fun drawGirl(canvas: Canvas, cx: Float, groundY: Float, S: Float) {
        val bodyW  = S * 0.09f
        val bodyH  = S * 0.23f
        val headR  = bodyW * 1.2f

        val bodyBottom = groundY
        val bodyTop    = groundY - bodyH
        val headY      = bodyTop - headR * 0.85f

        // 身體（稍微靠左與兔子疊在一起）
        val bx = cx - bodyW * 0.3f
        oval.set(bx - bodyW, bodyTop, bx + bodyW, bodyBottom)
        canvas.drawRoundRect(oval, bodyW * 0.45f, bodyW * 0.45f, fill)

        // 頭部
        canvas.drawCircle(bx, headY, headR, fill)

        // 帽子帽緣
        val brimHalfW = headR * 1.55f
        val brimH     = headR * 0.22f
        val brimY     = headY - headR * 0.35f
        oval.set(bx - brimHalfW, brimY - brimH / 2f, bx + brimHalfW, brimY + brimH / 2f)
        canvas.drawRect(oval, fill)

        // 帽子錐體（三角形）
        val coneBaseLeft  = bx - headR * 0.72f
        val coneBaseRight = bx + headR * 0.72f
        val coneTipX      = bx + headR * 0.10f
        val coneTipY      = brimY - headR * 1.85f
        path.reset()
        path.moveTo(coneBaseLeft,  brimY)
        path.lineTo(coneBaseRight, brimY)
        path.lineTo(coneTipX,      coneTipY)
        path.close()
        canvas.drawPath(path, fill)

        // 帽頂圓球
        canvas.drawCircle(coneTipX, coneTipY - headR * 0.14f, headR * 0.13f, fill)

        // 閉眼
        blackStroke.strokeWidth = S * 0.011f
        val eyeR = headR * 0.34f
        oval.set(bx - eyeR, headY - eyeR * 0.2f, bx + eyeR, headY + eyeR * 0.75f)
        canvas.drawArc(oval, 10f, 160f, false, blackStroke)

        // 腮紅
        fill.alpha = 55
        canvas.drawCircle(bx + headR * 0.42f, headY + headR * 0.30f, headR * 0.17f, fill)
        fill.alpha = 255
    }
}
