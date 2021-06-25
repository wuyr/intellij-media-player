package com.wuyr.intellijmediaplayer.dialogs

import com.intellij.CommonBundle
import com.intellij.openapi.ui.MultiLineLabelUI
import com.intellij.openapi.ui.messages.MessageDialog
import com.wuyr.intellijmediaplayer.TEXT_TRANSPARENCY_MESSAGE
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.*
import javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW

/**
 * @author wuyr
 * @github https://github.com/wuyr/intellij-media-player
 * @since 2021-05-17 02:48
 */
class TransparencyDialog(private val defaultValue: Float, private val onValueChange: (Float) -> Unit) : MessageDialog(null, true) {

    private lateinit var slider: JSlider
    private lateinit var label: JLabel

    override fun createActions() = arrayOf<Action>(object : AbstractAction(CommonBundle.getOkButtonText()) {
        override fun actionPerformed(e: ActionEvent) = close(OK_EXIT_CODE)
    })

    override fun createCenterPanel(): JComponent? = null

    override fun createNorthPanel() = createIconPanel().apply {
        add(createMessagePanel(), BorderLayout.CENTER)
    }

    override fun createMessagePanel() = JPanel(BorderLayout()).apply {
        add(createTextComponent(), BorderLayout.NORTH)
        add(createSliderComponent(), BorderLayout.SOUTH)
    }

    private fun createTextComponent() = JLabel(myMessage).apply {
        setUI(MultiLineLabelUI())
        border = BorderFactory.createEmptyBorder(0, 0, 5, 20)
        label = this
    }

    private fun createSliderComponent() = JSlider(0, 0, 100, 0).apply {
        value = (defaultValue * 100).toInt()
        addChangeListener {
            label.text = getMessage(value)
            onValueChange(value.toFloat() / 100F)
        }
        registerKeyboardAction({ close(OK_EXIT_CODE) }, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), WHEN_IN_FOCUSED_WINDOW)
        preferredSize = Dimension(300, 30)
        slider = this
    }

    override fun getPreferredFocusedComponent() = slider

    private fun getMessage(transparency: Int) = String.format(TEXT_TRANSPARENCY_MESSAGE, "$transparency%")

    init {
        myMessage = getMessage((defaultValue * 100).toInt())
        init()
        show()
    }
}