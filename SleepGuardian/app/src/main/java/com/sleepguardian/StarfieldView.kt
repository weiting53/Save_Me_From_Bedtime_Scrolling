package com.sleepguardian

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.*
import kotlin.random.Random

/**
 * 全螢幕星空動畫 View。
 * - 背景：深邃漸層夜空
 * - 靜態星點：~160 顆，部分帶閃爍效果
 * - 流星：隨機角度、長度、速度，每隔幾秒出現一顆
 */
class StarfieldView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    // ── 資料結構 ──────────────────────────────────────

    private data class Star(
        val x: Float,
        val y: Float,
        val radius: Float,
        val baseAlpha: Float,
        val twinkleSpeed: Float,   // rad/ms，0 = 不閃
        val twinklePhase: Float
    )

    private data class Meteor(
        val startX: Float,
        val startY: Float,
        val angleDeg: Float,
        val totalDist: Float,
        val tailLen: Float,
        val spawnMs: Long,
        val durationMs: Long
    )

    // ── 狀態 ──────────────────────────────────────────

    private val stars   = mutableListOf<Star>()
    private val meteors = mutableListOf<Meteor>()
    private val rng     = Random.Default

    private var lastSpawn = 0L
    private var nextSpawn = 4_000L   // 第一顆流星 4 秒後出現

    // ── 畫筆 ──────────────────────────────────────────

    private var bgPaint = Paint()

    private val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val meteorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style    = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 2.2f
    }

    // ── 初始化 ────────────────────────────────────────

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w <= 0 || h <= 0) return

        // 三色漸層夜空
        bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, h.toFloat(),
                intArrayOf(
                    Color.parseColor("#010510"),
                    Color.parseColor("#030a1c"),
                    Color.parseColor("#050c1e")
                ),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        initStars(w, h)
    }

    private fun initStars(w: Int, h: Int) {
        stars.clear()
        repeat(160) {
            val twinkle = rng.nextFloat() < 0.42f
            stars += Star(
                x            = rng.nextFloat() * w,
                y            = rng.nextFloat() * h,
                radius       = rng.nextFloat() * 1.6f + 0.2f,
                baseAlpha    = rng.nextFloat() * 0.50f + 0.18f,
                twinkleSpeed = if (twinkle) rng.nextFloat() * 0.0014f + 0.0003f else 0f,
                twinklePhase = rng.nextFloat() * 2f * PI.toFloat()
            )
        }
    }

    // ── 繪製 ──────────────────────────────────────────

    override fun onDraw(canvas: Canvas) {
        val now = System.currentTimeMillis()
        val w   = width.toFloat()
        val h   = height.toFloat()

        // 背景夜空
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        // 星點（含閃爍）
        for (s in stars) {
            val alpha = if (s.twinkleSpeed > 0f) {
                (s.baseAlpha + sin(now * s.twinkleSpeed + s.twinklePhase) * 0.22f)
                    .toFloat().coerceIn(0.04f, 1f)
            } else {
                s.baseAlpha
            }
            starPaint.alpha = (alpha * 255).toInt()
            canvas.drawCircle(s.x, s.y, s.radius, starPaint)
        }

        // 產生新流星
        if (now - lastSpawn > nextSpawn) {
            spawnMeteor(w, h, now)
            lastSpawn = now
            nextSpawn = (rng.nextFloat() * 6_500 + 2_800).toLong()
        }

        // 繪製 & 清理流星
        val it = meteors.iterator()
        while (it.hasNext()) {
            val m = it.next()
            val t = (now - m.spawnMs).toFloat() / m.durationMs
            if (t > 1f) { it.remove(); continue }
            drawMeteor(canvas, m, t)
        }

        postInvalidateOnAnimation()
    }

    private fun spawnMeteor(w: Float, h: Float, now: Long) {
        meteors += Meteor(
            startX     = rng.nextFloat() * w * 0.85f,
            startY     = rng.nextFloat() * h * 0.55f,
            angleDeg   = 28f + rng.nextFloat() * 28f,    // 28°–56°
            totalDist  = rng.nextFloat() * 220f + 140f,
            tailLen    = rng.nextFloat() * 90f  + 55f,
            spawnMs    = now,
            durationMs = (rng.nextFloat() * 750f + 600f).toLong()
        )
    }

    private fun drawMeteor(canvas: Canvas, m: Meteor, t: Float) {
        val rad  = m.angleDeg * PI.toFloat() / 180f
        val cosA = cos(rad)
        val sinA = sin(rad)

        // 流星頭部（前進端）
        val hx = m.startX + cosA * m.totalDist * t
        val hy = m.startY + sinA * m.totalDist * t
        // 尾部（漸透明端）
        val tx = hx - cosA * m.tailLen
        val ty = hy - sinA * m.tailLen

        // 淡入淡出 alpha
        val alpha = when {
            t < 0.15f -> t / 0.15f
            t > 0.72f -> 1f - (t - 0.72f) / 0.28f
            else      -> 1f
        }.coerceIn(0f, 1f)

        // 漸層：尾部透明 → 頭部白藍光
        meteorPaint.shader = LinearGradient(
            tx, ty, hx, hy,
            intArrayOf(
                Color.TRANSPARENT,
                Color.argb((alpha * 190).toInt(), 210, 230, 255)
            ),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawLine(tx, ty, hx, hy, meteorPaint)

        // 頭部亮點
        starPaint.alpha = (alpha * 200).toInt()
        canvas.drawCircle(hx, hy, 2f, starPaint)
    }
}
