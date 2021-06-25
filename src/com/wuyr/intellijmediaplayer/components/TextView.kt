package com.wuyr.intellijmediaplayer.components

import com.wuyr.intellijmediaplayer.DEFAULT_TEXT_VIEW_PADDING
import com.wuyr.intellijmediaplayer.media.MediaPlayer
import com.wuyr.intellijmediaplayer.media.MediaPlayer.timestampString
import java.awt.EventQueue
import java.awt.Graphics
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.BorderFactory
import javax.swing.JLabel
import javax.swing.JLayeredPane

/**
 * @author wuyr
 * @github https://github.com/wuyr/intellij-media-player
 * @since 2021-05-04 00:30
 */
class TextView : JLabel(), Puppet {

    private val puppetLabel = JLabel()
    private var offsetX = 0
    private var offsetY = 0

    init {
        border = BorderFactory.createEmptyBorder(0, DEFAULT_TEXT_VIEW_PADDING, 0, DEFAULT_TEXT_VIEW_PADDING)
        puppetLabel.border = border
        addComponentListener(object : ComponentAdapter() {
            override fun componentResized(event: ComponentEvent) = reposition()

            override fun componentMoved(event: ComponentEvent) = reposition()

            private fun reposition() {
                puppetLabel.setSize(width, height)
                puppetLabel.setLocation(x, y)
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
        SeekBar.onDragged = {
            val duration = MediaPlayer.duration
            puppetLabel.text = "${(duration * it).toLong().timestampString}/${duration.timestampString}"
            if (MediaPlayer.isPaused) {
                EventQueue.invokeLater { repaint() }
            }
        }
    }

    override fun onFramePaint(graphics: Graphics) {
        if (MediaPlayer.isPlaying) {
            if (text.isEmpty()) {
                val durationString = MediaPlayer.durationString
                text = "$durationString/$durationString"
            } else {
                graphics.translate(offsetX, offsetY)
                ui.paint(graphics, puppetLabel)
                graphics.translate(-offsetX, -offsetY)
            }
        }
    }

    override fun onStateChange(newState: Int) {
        if (newState != MediaPlayer.STATE_PLAYING) {
            EventQueue.invokeLater { revalidate() }
        }
    }

    override fun onTicktock() {
        if (!SeekBar.dragging) {
            puppetLabel.text = "${MediaPlayer.currentPositionString}/${MediaPlayer.durationString}"
        }
    }

    override fun paint(graphics: Graphics) {
        if (!MediaPlayer.isPlaying) {
            ui.paint(graphics, puppetLabel)
        }
    }
}