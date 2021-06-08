package com.wuyr.intellijmediaplayer.actions

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.wuyr.intellijmediaplayer.DEFAULT_CANVAS_TRANSPARENCY
import com.wuyr.intellijmediaplayer.dialogs.TransparencyDialog
import com.wuyr.intellijmediaplayer.media.MediaPlayer

/**
 * @author wuyr
 * @github https://github.com/wuyr/intellij-media-player
 * @since 2021-05-17 01:56
 */
class TransparencyAction : AnAction() {

    private var currentTransparency = PropertiesComponent.getInstance().getFloat(javaClass.name, DEFAULT_CANVAS_TRANSPARENCY)

    override fun update(event: AnActionEvent) = updateTransparency()

    override fun actionPerformed(event: AnActionEvent) {
        TransparencyDialog(currentTransparency) {
            currentTransparency = it
            PropertiesComponent.getInstance().setValue(javaClass.name, currentTransparency, DEFAULT_CANVAS_TRANSPARENCY)
            updateTransparency()
        }
    }

    private fun updateTransparency() {
        MediaPlayer.canvasAlpha = currentTransparency
    }
}
