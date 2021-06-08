package com.wuyr.intellijmediaplayer.components

import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.wuyr.intellijmediaplayer.actions.PauseAction
import com.wuyr.intellijmediaplayer.media.MediaPlayer
import icons.Icons
import java.awt.BorderLayout
import java.awt.Graphics
import java.util.*
import javax.swing.JFrame
import javax.swing.JMenuBar
import javax.swing.JPanel

/**
 * @author wuyr
 * @github https://github.com/wuyr/intellij-media-player
 * @since 2021-05-21 10:27
 */
object Controller {

    var isShowing = false
    private var controllerContainer: JPanel? = null
    private var seekBar: Puppet? = null
    private var timeView: Puppet? = null
    private val pauseAction: PauseAction by lazy { PauseAction() }
    private val pausePresentation: Presentation by lazy {
        Presentation().apply {
            isEnabled = false
            icon = Icons.pause
        }
    }
    private var menuBar: JMenuBar? = null
    private var timer: Timer? = null
    private val onStateChange: ((Int) -> Unit) = { newState ->
        if (newState == MediaPlayer.STATE_STOPPED) {
            dismiss()
        } else {
            if (newState == MediaPlayer.STATE_PLAYING) {
                startTimer()
            } else {
                stopTimer()
            }
            seekBar?.onStateChange(newState)
            timeView?.onStateChange(newState)
        }
        pausePresentation.run {
            isEnabled = !MediaPlayer.isStopped
            icon = if (MediaPlayer.isPaused) Icons.play else Icons.pause
        }
    }
    private val onFramePaint: ((Graphics) -> Unit) = { graphics ->
        seekBar?.onFramePaint(graphics)
        timeView?.onFramePaint(graphics)
    }

    private fun startTimer() {
        timer?.cancel()
        timer = Timer().apply {
            schedule(object : TimerTask() {
                override fun run() = ticktock()
            }, 0, 1000)
        }
    }

    private fun stopTimer() {
        timer?.cancel()
        timer = null
    }

    private fun ticktock() {
        seekBar?.onTicktock()
        timeView?.onTicktock()
    }

    fun show(event: AnActionEvent) {
        MediaPlayer.onStateChange = onStateChange
        MediaPlayer.onFramePaint = onFramePaint
        menuBar = (event.inputEvent.component.parent as JFrame).rootPane.jMenuBar.apply {
            add(controlPanel)
            revalidate()
        }
        isShowing = true
        startTimer()
    }

    private val controlPanel: JPanel
        get() = JPanel().apply {
            layout = BorderLayout()
            add(ActionButton(pauseAction, pausePresentation, "", ActionToolbar.NAVBAR_MINIMUM_BUTTON_SIZE), BorderLayout.LINE_START)
            add(SeekBar().also { seekBar = it }, BorderLayout.CENTER)
            add(TextView().also { timeView = it }, BorderLayout.LINE_END)
            controllerContainer = this
        }

    fun dismiss() {
        stopTimer()
        menuBar?.let {
            controllerContainer?.let { controller ->
                it.remove(controller)
                it.revalidate()
                controllerContainer = null
            }
            menuBar = null
        }
        seekBar = null
        timeView = null
        SeekBar.onDragged = null
        MediaPlayer.onStateChange = null
        MediaPlayer.onFramePaint = null
        isShowing = false
    }
}
