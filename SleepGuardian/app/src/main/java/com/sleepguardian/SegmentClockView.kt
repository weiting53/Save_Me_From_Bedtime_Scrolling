package com.sleepguardian

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.*

/**
 * Flip-clock 機械風格時鐘 View。
 *
 * 視覺特色：
 *  - 每個數字放在厚邊框正方卡片內
 *  - 7-segment 線段超寬（像素風格）、端點方形不斜切
 *  - 卡片之間有水平 Axle 軸（短矩形 + 圓頭螺帽）穿插
 *  - 冒號用兩個白色方點；旁邊繪製小齒輪裝飾
 */
class SegmentClockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var hour = 23
        set(value) { field = value; invalidate() }
    var minute = 0
        set(value) { field = value; invalidate() }

    // 7-segment：A=top B=topR C=botR D=bot E=botL F=topL G=mid
    private val SEG = arrayOf(
        intArrayOf(1,1,1,1,1,1,0), // 0
        intArrayOf(0,1,1,0,0,0,0), // 1
        intArrayOf(1,1,0,1,1,0,1), // 2
        intArrayOf(1,1,1,1,0,0,1), // 3
        intArrayOf(0,1,1,0,0,1,1), // 4
        intArrayOf(1,0,1,1,0,1,1), // 5
        intArrayOf(1,0,1,1,1,1,1), // 6
        intArrayOf(1,1,1,0,0,0,0), // 7
        intArrayOf(1,1,1,1,1,1,1), // 8
        intArrayOf(1,1,1,1,0,1,1), // 9
    )

    private val whiteFill = Paint().apply {
        color = Color.WHITE; style = Paint.Style.FILL; isAntiAlias = false
    }
    private val whiteStroke = Paint().apply {
        color = Color.WHITE; style = Paint.Style.STROKE; isAntiAlias = false
    }
    private val blackFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK; style = Paint.Style.FILL
    }
    private val whiteFillAA = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; style = Paint.Style.FILL
    }

    private val rf  = RectF()
    private val gearPath = Path()

    // ─────────────────────────────────────────────────────────────
    // onDraw：排版計算
    // ─────────────────────────────────────────────────────────────
    override fun onDraw(canvas: Canvas) {
        val W = width.toFloat()
        val H = height.toFloat()

        // 卡片高度佔 View 高度的 84%，卡片寬度略窄
        val cardH = H * 0.84f
        val cardW = cardH * 0.86f
        val topY  = (H - cardH) / 2f
        val midY  = topY + cardH / 2f

        val borderW = cardH * 0.052f
        whiteStroke.strokeWidth = borderW

        // axle 軸：短矩形 + 圓頭
        val axleLen  = cardW * 0.22f
        val axleHalf = borderW * 0.9f    // 軸半高
        val boltR    = axleHalf * 1.55f  // 螺帽半徑

        // 冒號區寬度
        val colonW = cardW * 0.54f

        // 整體寬度，置中
        val totalW = cardW * 4f + axleLen * 4f + boltR * 4f + colonW
        var x = (W - totalW) / 2f

        // ── 左外 axle ──────────────────────────────────────────
        drawAxle(canvas, x, midY, axleLen, axleHalf, boltR, leftBolt = true)
        x += axleLen + boltR

        // ── 卡片 H1 ─────────────────────────────────────────────
        drawCard(canvas, hour / 10, x, topY, cardW, cardH)
        x += cardW

        // ── 中間 axle（H1–H2）──────────────────────────────────
        drawAxle(canvas, x, midY, axleLen, axleHalf, boltR, leftBolt = false)
        x += axleLen + boltR

        // ── 卡片 H2 ─────────────────────────────────────────────
        drawCard(canvas, hour % 10, x, topY, cardW, cardH)
        x += cardW

        // ── 冒號 + 齒輪 ─────────────────────────────────────────
        val colonX = x
        drawColon(canvas, colonX, topY, colonW, cardH)
        // 齒輪：在冒號右上角
        drawGear(canvas, colonX + colonW * 0.70f, topY + cardH * 0.09f, colonW * 0.21f)
        x += colonW

        // ── 卡片 M1 ─────────────────────────────────────────────
        drawCard(canvas, minute / 10, x, topY, cardW, cardH)
        x += cardW

        // ── 中間 axle（M1–M2）──────────────────────────────────
        drawAxle(canvas, x, midY, axleLen, axleHalf, boltR, leftBolt = false)
        x += axleLen + boltR

        // ── 卡片 M2 ─────────────────────────────────────────────
        drawCard(canvas, minute % 10, x, topY, cardW, cardH)
        x += cardW

        // ── 右外 axle ──────────────────────────────────────────
        drawAxle(canvas, x, midY, axleLen, axleHalf, boltR, leftBolt = false)
    }

    // ─────────────────────────────────────────────────────────────
    // 卡片（厚邊框 + 超寬 7-segment）
    // ─────────────────────────────────────────────────────────────
    private fun drawCard(canvas: Canvas, digit: Int, x: Float, y: Float, w: Float, h: Float) {
        val sw   = whiteStroke.strokeWidth
        val half = sw / 2f
        // 外框
        rf.set(x + half, y + half, x + w - half, y + h - half)
        canvas.drawRect(rf, whiteStroke)
        // segment 區域（水平中線劃一條凹槽模擬翻牌分割線）
        val divH = sw * 0.6f
        rf.set(x + sw, y + h / 2f - divH / 2f, x + w - sw, y + h / 2f + divH / 2f)
        canvas.drawRect(rf, blackFill)
        // segment 字
        val pad = w * 0.14f
        drawSegs(canvas, digit, x + pad, y + pad, w - pad * 2f, h - pad * 2f)
    }

    private fun drawSegs(canvas: Canvas, digit: Int, x: Float, y: Float, w: Float, h: Float) {
        val s = SEG[digit.coerceIn(0, 9)]
        // 線段厚度：30% 寬度，偏粗像素風
        val t    = w * 0.30f
        val half = h / 2f

        fun block(l: Float, top: Float, r: Float, b: Float) {
            rf.set(l, top, r, b)
            canvas.drawRect(rf, whiteFill)
        }

        if (s[0] == 1) block(x,       y,         x+w,    y+t)          // A top
        if (s[1] == 1) block(x+w-t,   y,         x+w,    y+half)       // B topR
        if (s[2] == 1) block(x+w-t,   y+half,    x+w,    y+h)          // C botR
        if (s[3] == 1) block(x,       y+h-t,     x+w,    y+h)          // D bot
        if (s[4] == 1) block(x,       y+half,    x+t,    y+h)          // E botL
        if (s[5] == 1) block(x,       y,         x+t,    y+half)       // F topL
        if (s[6] == 1) block(x,       y+half-t*0.5f, x+w, y+half+t*0.5f) // G mid
    }

    // ─────────────────────────────────────────────────────────────
    // Axle 軸（水平連接桿 + 螺帽圓點）
    // ─────────────────────────────────────────────────────────────
    private fun drawAxle(
        canvas: Canvas,
        x: Float, midY: Float,
        len: Float, halfH: Float, boltR: Float,
        leftBolt: Boolean
    ) {
        // 矩形桿
        rf.set(x, midY - halfH, x + len, midY + halfH)
        canvas.drawRect(rf, whiteFill)
        // 螺帽（填充圓 + 黑心）
        val boltX = if (leftBolt) x else x + len
        canvas.drawCircle(boltX, midY, boltR, whiteFillAA)
        canvas.drawCircle(boltX, midY, boltR * 0.38f, blackFill)
    }

    // ─────────────────────────────────────────────────────────────
    // 冒號（兩個白色方點）
    // ─────────────────────────────────────────────────────────────
    private fun drawColon(canvas: Canvas, x: Float, topY: Float, w: Float, h: Float) {
        val sz = w * 0.30f
        val cx = x + w / 2f
        rf.set(cx - sz/2f, topY + h * 0.295f - sz/2f, cx + sz/2f, topY + h * 0.295f + sz/2f)
        canvas.drawRect(rf, whiteFill)
        rf.set(cx - sz/2f, topY + h * 0.705f - sz/2f, cx + sz/2f, topY + h * 0.705f + sz/2f)
        canvas.drawRect(rf, whiteFill)
    }

    // ─────────────────────────────────────────────────────────────
    // 齒輪裝飾（8 齒，圓心黑洞）
    // ─────────────────────────────────────────────────────────────
    private fun drawGear(canvas: Canvas, cx: Float, cy: Float, outerR: Float) {
        val teeth   = 8
        val innerR  = outerR * 0.64f
        val toothHalfAngle = (PI / teeth * 0.52f).toFloat()

        gearPath.reset()
        for (i in 0 until teeth) {
            val baseAngle = (i * 2f * PI / teeth).toFloat() - PI.toFloat() / 2f
            val midAngle  = baseAngle + (PI / teeth).toFloat()

            // 內圓弧到齒根左
            val ax1 = cx + innerR * cos(baseAngle)
            val ay1 = cy + innerR * sin(baseAngle)
            // 齒根左
            val tx1 = cx + innerR * cos(midAngle - toothHalfAngle)
            val ty1 = cy + innerR * sin(midAngle - toothHalfAngle)
            // 齒頂左
            val top1x = cx + outerR * cos(midAngle - toothHalfAngle * 0.5f)
            val top1y = cy + outerR * sin(midAngle - toothHalfAngle * 0.5f)
            // 齒頂右
            val top2x = cx + outerR * cos(midAngle + toothHalfAngle * 0.5f)
            val top2y = cy + outerR * sin(midAngle + toothHalfAngle * 0.5f)
            // 齒根右
            val tx2 = cx + innerR * cos(midAngle + toothHalfAngle)
            val ty2 = cy + innerR * sin(midAngle + toothHalfAngle)
            // 內圓弧到下一齒根
            val nextBase = baseAngle + (2f * PI / teeth).toFloat()
            val ax2 = cx + innerR * cos(nextBase)
            val ay2 = cy + innerR * sin(nextBase)

            if (i == 0) gearPath.moveTo(ax1, ay1)
            else gearPath.lineTo(ax1, ay1)
            gearPath.lineTo(tx1, ty1)
            gearPath.lineTo(top1x, top1y)
            gearPath.lineTo(top2x, top2y)
            gearPath.lineTo(tx2, ty2)
            gearPath.lineTo(ax2, ay2)
        }
        gearPath.close()
        canvas.drawPath(gearPath, whiteFillAA)
        // 齒輪圓心黑洞
        canvas.drawCircle(cx, cy, innerR * 0.42f, blackFill)
    }
}
