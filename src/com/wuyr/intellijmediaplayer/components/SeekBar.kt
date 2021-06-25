package com.wuyr.intellijmediaplayer.components

import com.intellij.ui.JBColor
import com.wuyr.intellijmediaplayer.*
import com.wuyr.intellijmediaplayer.media.MediaPlayer
import java.awt.Color
import java.awt.Dimension
import java.awt.EventQueue
import java.awt.Graphics
import java.awt.event.*
import javax.swing.JLayeredPane
import javax.swing.JProgressBar

/**
 * @author wuyr
 * @github https://github.com/wuyr/intellij-media-player
 * @since 2021-05-04 00:57
 */
class SeekBar : JProgressBar(), Puppet {

    private var progressChanged = false
    private var currentProgress = 0
    private var offsetX = 0
    private var offsetY = 0
    private val maxValue = DEFAULT_PROGRESS_BAR_MAXIMUM
    private val barHeight = DEFAULT_PROGRESS_BAR_HEIGHT
    private val indicatorSize = DEFAULT_PROGRESS_BAR_INDICATOR_SIZE

    companion object {
        private val INDICATOR_COLOR = Color(DEFAULT_PROGRESS_BAR_INDICATOR_COLOR, true)
        private val PRIMARY_COLOR = Color(DEFAULT_PROGRESS_BAR_PRIMARY_COLOR, true)
        private val SECONDARY_COLOR = Color(DEFAULT_PROGRESS_BAR_SECONDARY_COLOR, true)

        private val sIndicatorColor = JBColor(INDICATOR_COLOR, INDICATOR_COLOR)
        private val sPrimaryBarColor = JBColor(PRIMARY_COLOR, PRIMARY_COLOR)
        private val sSecondaryBarColor = JBColor(SECONDARY_COLOR, SECONDARY_COLOR)

        var dragging = false
        var onDragged: ((Float) -> Unit)? = null
    }

    init {
        preferredSize = Dimension(0, barHeight)
        maximum = maxValue
        addComponentListener(object : ComponentAdapter() {
            override fun componentResized(event: ComponentEvent) = reposition()

            override fun componentMoved(event: ComponentEvent) = reposition()

            private fun reposition() {
                offsetX = x
                offsetY = y
                var container = parent
                while (container != null) {
                    offsetY += container.y
                    container = container.parent
                    if (container is JLayeredPane) {
                        break
                    }
                }
            }
        })
        addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseDragged(event: MouseEvent) {
                dragging = true
                val percent = (event.x.toFloat() / width).fix()
                updateProgress(percent)
                onDragged?.invoke(percent)
            }
        })
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(event: MouseEvent) {
                val percent = (event.x.toFloat() / width).fix()
                MediaPlayer.seekTo(percent)
                updateProgress(percent)
                progressChanged = false
            }

            override fun mouseReleased(event: MouseEvent) {
                if (progressChanged) {
                    val percent = (event.x.toFloat() / width).fix()
                    MediaPlayer.seekTo(percent)
                    updateProgress(percent)
                    progressChanged = false
                }
                dragging = false
            }
        })
    }

    override fun onFramePaint(graphics: Graphics) {
        if (MediaPlayer.isPlaying) {
            graphics.translate(offsetX, offsetY)
            drawBar(graphics)
            graphics.translate(-offsetX, -offsetY)
        }
    }

    override fun onStateChange(newState: Int) {
        if (newState != MediaPlayer.STATE_PLAYING) {
            EventQueue.invokeLater { revalidate() }
        }
    }

    override fun onTicktock() {
        if (!dragging) {
            updateProgress(MediaPlayer.progress)
        }
    }

    override fun paint(graphics: Graphics) {
        if (!MediaPlayer.isPlaying) {
            drawBar(graphics)
        }
    }

    private fun drawBar(graphics: Graphics) {
        graphics.color = sSecondaryBarColor
        val centerY = height / 2
        val offsetX = (width * (currentProgress / maxValue.toFloat())).toInt()
        val halfBarHeight = barHeight / 2
        val halfIndicatorSize = indicatorSize / 2
        graphics.fillRect(0, centerY - halfBarHeight, width, barHeight)
        graphics.color = sPrimaryBarColor
        graphics.fillRect(0, centerY - halfBarHeight, offsetX, barHeight)
        graphics.color = sIndicatorColor
        graphics.fillOval(offsetX - halfIndicatorSize, height / 2 - halfIndicatorSize, indicatorSize, indicatorSize)
    }

    private fun updateProgress(percent: Float) {
        val newProgress = (percent * maxValue).toInt()
        if (currentProgress != newProgress) {
            currentProgress = newProgress.fix()
            progressChanged = true
        }
    }

    private fun Int.fix() = when {
        this > maxValue -> maxValue
        this < 0 -> 0
        else -> this
    }

    private fun Float.fix() = when {
        this > 1F -> 1F
        this < 0F -> 0F
        else -> this
    }
}