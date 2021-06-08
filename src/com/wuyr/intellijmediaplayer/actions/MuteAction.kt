package com.wuyr.intellijmediaplayer.actions

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.wuyr.intellijmediaplayer.DEFAULT_MUTE
import com.wuyr.intellijmediaplayer.TEXT_SWITCH_TO_MUTE
import com.wuyr.intellijmediaplayer.TEXT_SWITCH_TO_VOICED
import com.wuyr.intellijmediaplayer.media.MediaPlayer
import icons.Icons

/**
 * @author wuyr
 * @github https://github.com/wuyr/intellij-media-player
 * @since 2021-05-16 23:49
 */
class MuteAction : AnAction() {

    private var isMute = PropertiesComponent.getInstance().getBoolean(javaClass.name, DEFAULT_MUTE)

    override fun update(event: AnActionEvent) = event.updateStatus()

    override fun actionPerformed(event: AnActionEvent) {
        isMute = !isMute
        event.updateStatus()
        PropertiesComponent.getInstance().setValue(javaClass.name, isMute, DEFAULT_MUTE)
    }

    private fun AnActionEvent.updateStatus() = presentation.run {
        MediaPlayer.soundEnable = !isMute
        if (isMute) {
            icon = Icons.mute
            text = TEXT_SWITCH_TO_VOICED
        } else {
            icon = Icons.voiced
            text = TEXT_SWITCH_TO_MUTE
        }
    }
}
