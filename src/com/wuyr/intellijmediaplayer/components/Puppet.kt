package com.wuyr.intellijmediaplayer.components

import java.awt.Graphics

/**
 * @author wuyr
 * @github https://github.com/wuyr/intellij-media-player
 * @since 2021-06-05 09:22
 */
interface Puppet {
    fun onFramePaint(graphics: Graphics)

    fun onStateChange(newState: Int)

    fun onTicktock()
}