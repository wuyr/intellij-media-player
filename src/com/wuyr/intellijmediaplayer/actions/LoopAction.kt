package com.wuyr.intellijmediaplayer.actions

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.wuyr.intellijmediaplayer.DEFAULT_LOOP
import com.wuyr.intellijmediaplayer.media.MediaPlayer
import icons.Icons

/**
 * @author wuyr
 * @github https://github.com/wuyr/intellij-media-player
 * @since 2021-06-21 12:10
 */
class LoopAction : AnAction() {

    private var isLoop = PropertiesComponent.getInstance().getBoolean(javaClass.name, DEFAULT_LOOP)

    override fun update(event: AnActionEvent) = event.updateStatus()

    override fun actionPerformed(event: AnActionEvent) {
        isLoop = !isLoop
        event.updateStatus()
        PropertiesComponent.getInstance().setValue(javaClass.name, isLoop, DEFAULT_LOOP)
    }

    private fun AnActionEvent.updateStatus() = presentation.run {
        MediaPlayer.loop = isLoop
        icon = if (isLoop) Icons.loop else Icons.single
    }
}