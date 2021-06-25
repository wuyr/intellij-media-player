package com.wuyr.intellijmediaplayer.actions

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.wuyr.intellijmediaplayer.DEFAULT_SHOW_CONTROLLER
import com.wuyr.intellijmediaplayer.TEXT_HIDE_CONTROLLER
import com.wuyr.intellijmediaplayer.TEXT_SHOW_CONTROLLER
import com.wuyr.intellijmediaplayer.components.Controller
import com.wuyr.intellijmediaplayer.media.MediaPlayer
import java.awt.KeyboardFocusManager
import javax.swing.JFrame

/**
 * @author wuyr
 * @github https://github.com/wuyr/intellij-media-player
 * @since 2021-05-22 13:10
 */
class ControllerAction : AnAction() {

    companion object {
        var isShowController = PropertiesComponent.getInstance().getBoolean(ControllerAction::class.java.name, DEFAULT_SHOW_CONTROLLER)
    }

    override fun update(event: AnActionEvent) = event.updateStatus()

    override fun actionPerformed(event: AnActionEvent) {
        isShowController = !isShowController
        event.updateStatus()
        PropertiesComponent.getInstance().setValue(javaClass.name, isShowController, DEFAULT_SHOW_CONTROLLER)
    }

    private fun AnActionEvent.updateStatus() = presentation.run {
        if (isShowController) {
            if (!MediaPlayer.isStopped && !Controller.isShowing) {
                Controller.show(KeyboardFocusManager.getCurrentKeyboardFocusManager().activeWindow as JFrame)
            }
        } else {
            if (Controller.isShowing) {
                Controller.dismiss()
            }
        }
        text = if (isShowController) TEXT_HIDE_CONTROLLER else TEXT_SHOW_CONTROLLER
    }
}
