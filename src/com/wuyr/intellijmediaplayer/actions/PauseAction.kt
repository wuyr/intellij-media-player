package com.wuyr.intellijmediaplayer.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.wuyr.intellijmediaplayer.media.MediaPlayer
import icons.Icons

/**
 * @author wuyr
 * @github https://github.com/wuyr/intellij-media-player
 * @since 2021-05-16 23:11
 */
class PauseAction : AnAction() {

    override fun update(event: AnActionEvent) {
        event.presentation.run {
            isEnabled = !MediaPlayer.isStopped
            icon = if (MediaPlayer.isPaused) Icons.play else Icons.pause
        }
    }

    override fun actionPerformed(event: AnActionEvent) {
        if (MediaPlayer.isPlaying) {
            MediaPlayer.pause()
        } else if (MediaPlayer.isPaused) {
            MediaPlayer.resume()
        }
        update(event)
    }
}
