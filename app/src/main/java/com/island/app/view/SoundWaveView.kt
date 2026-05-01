package com.island.app.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator

class SoundWaveView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val barCount = 5
    private val barWidthDp = 4f
    private val barGapDp = 4f
    private val density = context.resources.displayMetrics.density

    private val barWidth = barWidthDp * density
    private val barGap = barGapDp * density

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        style = Paint.Style.FILL
        strokeCap = Paint.Cap.ROUND
    }

    private val barHeightFractions = FloatArray(barCount) { 0.5f }
    private val animators = ArrayList<ValueAnimator>(barCount)
    private var isPlaying = false

    fun setPlaying(playing: Boolean) {
        if (isPlaying == playing) return
        isPlaying = playing
        if (playing) startPlayingAnimation() else startPausedAnimation()
    }

    private fun startPlayingAnimation() {
        stopAnimators()
        for (i in 0 until barCount) {
            val minFraction = 0.3f
            val maxFraction = 1.0f
            val duration = (400 + i * 80).toLong()
            val animator = ValueAnimator.ofFloat(minFraction, maxFraction).apply {
                this.duration = duration
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                interpolator = LinearInterpolator()
                startDelay = (i * 60).toLong()
                addUpdateListener { anim ->
                    barHeightFractions[i] = anim.animatedValue as Float
                    invalidate()
                }
            }
            animators.add(animator)
            animator.start()
        }
    }

    private fun startPausedAnimation() {
        stopAnimators()
        for (i in 0 until barCount) {
            val current = barHeightFractions[i]
            val animator = ValueAnimator.ofFloat(current, 0.1f).apply {
                duration = 200
                interpolator = LinearInterpolator()
                addUpdateListener { anim ->
                    barHeightFractions[i] = anim.animatedValue as Float
                    invalidate()
                }
            }
            animators.add(animator)
            animator.start()
        }
    }

    private fun stopAnimators() {
        animators.forEach { it.cancel() }
        animators.clear()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val totalWidth = barCount * barWidth + (barCount - 1) * barGap
        var x = (width - totalWidth) / 2f
        for (i in 0 until barCount) {
            val barHeight = height * barHeightFractions[i]
            val top = (height - barHeight) / 2f
            val bottom = top + barHeight
            val radius = barWidth / 2f
            canvas.drawRoundRect(x, top, x + barWidth, bottom, radius, radius, paint)
            x += barWidth + barGap
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimators()
    }
}
