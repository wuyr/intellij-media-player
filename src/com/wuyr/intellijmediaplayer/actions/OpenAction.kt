package com.wuyr.intellijmediaplayer.actions

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.wuyr.intellijmediaplayer.DEFAULT_VIDEO_URL
import com.wuyr.intellijmediaplayer.components.Controller
import com.wuyr.intellijmediaplayer.dialogs.URLInputDialog
import com.wuyr.intellijmediaplayer.media.MediaPlayer
import java.awt.KeyboardFocusManager
import javax.swing.JFrame

/**
 * @author wuyr
 * @github https://github.com/wuyr/intellij-media-player
 * @since 2021-04-17 16:06
 */
class OpenAction : AnAction() {

    override fun update(event: AnActionEvent) {
        event.presentation.isEnabled = MediaPlayer.isStopped
    }

    override fun actionPerformed(event: AnActionEvent) {
        val frame = KeyboardFocusManager.getCurrentKeyboardFocusManager().activeWindow as JFrame
        PropertiesComponent.getInstance().run {
            URLInputDialog(getValue(javaClass.name, DEFAULT_VIDEO_URL)) {
                if (it.isNotEmpty()) {
                    setValue(javaClass.name, it, DEFAULT_VIDEO_URL)
                    if (MediaPlayer.init(frame, it)) {
                        if (MediaPlayer.start()) {
                            if (ControllerAction.isShowController) {
                                Controller.show(frame)
                            }
                        }
                    }
                }
            }
        }
    }
}
