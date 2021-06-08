package com.wuyr.intellijmediaplayer.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.wuyr.intellijmediaplayer.components.Controller
import com.wuyr.intellijmediaplayer.media.MediaPlayer

/**
 * @author wuyr
 * @github https://github.com/wuyr/intellij-media-player
 * @since 2021-04-17 19:34
 */
class StopAction : AnAction() {

    override fun update(event: AnActionEvent) {
        event.presentation.isEnabled = !MediaPlayer.isStopped
    }

    override fun actionPerformed(event: AnActionEvent) {
        MediaPlayer.stop()
        Controller.dismiss()
    }
}
