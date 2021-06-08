package com.wuyr.intellijmediaplayer.dialogs

import com.intellij.CommonBundle
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.ui.ComponentWithBrowseButton
import com.intellij.openapi.ui.MultiLineLabelUI
import com.intellij.openapi.ui.messages.MessageDialog
import com.intellij.ui.components.fields.ExtendableTextField
import com.wuyr.intellijmediaplayer.TEXT_ENTER_VIDEO_URL
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.ActionEvent
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.*

/**
 * @author wuyr
 * @github https://github.com/wuyr/intellij-media-player
 * @since 2021-04-17 16:43
 */
class URLInputDialog(private val defaultValue: String, private val onEnter: (String) -> Unit) : MessageDialog(null, true) {

    private lateinit var extendableTextField: ExtendableTextField

    override fun createActions() = arrayOf<Action>(object : AbstractAction(CommonBundle.getOkButtonText()) {
        override fun actionPerformed(e: ActionEvent) {
            if (extendableTextField.text.isNotEmpty()) {
                onEnter(extendableTextField.text)
            }
            close(OK_EXIT_CODE)
        }
    }, object : AbstractAction(CommonBundle.getCancelButtonText()) {
        override fun actionPerformed(e: ActionEvent) = close(CANCEL_EXIT_CODE)
    })

    override fun createCenterPanel(): JComponent? = null

    override fun createNorthPanel() = createIconPanel().apply {
        add(createMessagePanel(), BorderLayout.CENTER)
    }

    override fun createMessagePanel() = JPanel(BorderLayout()).apply {
        add(createTextComponent(), BorderLayout.NORTH)
        add(createComponentWithBrowseButton(), BorderLayout.SOUTH)
    }

    private fun createTextComponent() = JLabel(myMessage).apply {
        setUI(MultiLineLabelUI())
        border = BorderFactory.createEmptyBorder(0, 0, 5, 20)
    }

    private fun createComponentWithBrowseButton() = ComponentWithBrowseButton<ExtendableTextField>(ExtendableTextField().apply {
        addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_ENTER) {
                    if (text.isNotEmpty()) {
                        onEnter(text)
                    }
                    close(OK_EXIT_CODE)
                }
            }
        })
        text = defaultValue
        extendableTextField = this
    }) {
        FileChooser.chooseFile(FileChooserDescriptor(true, false, false, false,
                false, false), null, null)?.path?.let {
            if (it.isNotEmpty()) {
                onEnter(it)
                close(OK_EXIT_CODE)
            }
        }
    }.apply { preferredSize = Dimension(500, 30) }

    override fun getPreferredFocusedComponent() = extendableTextField

    init {
        myMessage = TEXT_ENTER_VIDEO_URL
        init()
        show()
    }
}