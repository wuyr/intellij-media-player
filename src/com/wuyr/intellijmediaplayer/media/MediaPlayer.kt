package com.wuyr.intellijmediaplayer.media

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.AbstractPainter
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.Painter
import com.intellij.openapi.wm.impl.IdeGlassPaneImpl
import com.jetbrains.rd.util.measureTimeMillis
import com.wuyr.intellijmediaplayer.*
import com.wuyr.intellijmediaplayer.utils.get
import com.wuyr.intellijmediaplayer.utils.invoke
import com.wuyr.intellijmediaplayer.utils.invokeVoid
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.Frame
import org.bytedeco.javacv.Java2DFrameConverter
import java.awt.*
import java.awt.image.BufferedImage
import java.awt.image.VolatileImage
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.ShortBuffer
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.ScheduledThreadPoolExecutor
import javax.sound.sampled.*
import javax.swing.JFrame
import javax.swing.JRootPane
import javax.swing.RepaintManager
import kotlin.math.roundToInt

/**
 * @author wuyr
 * @github https://github.com/wuyr/intellij-media-player
 * @since 2020-09-05 下午06:54
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
object MediaPlayer {
    const val STATE_STOPPED = 0
    const val STATE_PLAYING = 1
    const val STATE_PAUSED = 2

    @Volatile
    private var state = STATE_STOPPED
    val isStopped: Boolean get() = state == STATE_STOPPED
    val isPlaying: Boolean get() = state == STATE_PLAYING
    val isPaused: Boolean get() = state == STATE_PAUSED
    var onStateChange: ((Int) -> Unit)? = null
    var onFramePaint: ((Graphics) -> Unit)? = null
    var canvasAlpha = DEFAULT_CANVAS_TRANSPARENCY
        set(value) {
            if (field != value) {
                field = value.coerceAtMost(1F).coerceAtLeast(0F)
                alphaComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, field)
            }
        }
    var soundEnable = !DEFAULT_MUTE
        set(value) {
            if (field != value) {
                field = value
                audioOutputStream?.run {
                    if (isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                        (getControl(FloatControl.Type.MASTER_GAIN) as? FloatControl)?.let {
                            it.value = if (value) 0F else it.minimum
                        }
                    }
                }
            }
        }
    var cacheQueueSize = DEFAULT_CACHE_QUEUE_SIZE
    var maxCacheQueueSize = DEFAULT_MAX_CACHE_QUEUE_SIZE

    @Volatile
    private var initialized = true

    private lateinit var rootPane: JRootPane
    private var frameGrabber: FFmpegFrameGrabber? = null
    private var audioOutputStream: SourceDataLine? = null
    private var audioOutputBufferSize = 0
    private var sampleRate = 0

    private var alphaComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, canvasAlpha)
    private var grabInterval = 0L
    private var frameImage: VolatileImage? = null

    fun init(event: AnActionEvent, url: String): Boolean {
        if (initialized) stop()
        return if (injectPainter(event)) {
            runCatching {
                initialized = true
                frameGrabber = FFmpegFrameGrabber.createDefault(url)
            }.isSuccess
        } else false
    }

    val currentPosition: Long get() = currentAudioTimestamp

    val currentPositionString: String get() = currentPosition.timestampString

    val duration: Long get() = frameGrabber?.run { lengthInTime } ?: -1L

    val durationString: String get() = duration.timestampString

    val progress: Float get() = frameGrabber?.run { currentPosition.toFloat() / lengthInTime } ?: -1F

    fun seekTo(percent: Float) {
        if (initialized && !isStopped) {
            frameGrabber?.run {
                val fixedPercent = percent.coerceAtMost(1F).coerceAtLeast(0F)
                clearCacheQueue()
                timestamp = when (fixedPercent) {
                    0F -> 0L
                    1F -> lengthInTime
                    else -> (fixedPercent * lengthInTime).toLong()
                }
                if (isPlaying) notify()
                else resume()
            }
        }
    }

    private val threadPool = ScheduledThreadPoolExecutor(4)
    private var syncThreshold = 0L

    fun start(): Boolean {
        if (initialized && isStopped) {
            frameGrabber?.let { grabber ->
                try {
                    grabber.start()
                    val useScreenSize = grabber.imageWidth > rootPane.width || grabber.imageHeight > rootPane.height
                    if (useScreenSize) {
                        val widthScale = rootPane.width.toFloat() / grabber.imageWidth.toFloat()
                        val heightScale = rootPane.height.toFloat() / grabber.imageHeight.toFloat()
                        grabber.imageWidth = (grabber.imageWidth * widthScale).toInt()
                        grabber.imageHeight = (grabber.imageHeight * heightScale).toInt()
                    }
                    sampleRate = grabber.sampleRate
                    grabInterval = (1000 / grabber.frameRate).toLong()
                    syncThreshold = grabInterval * 3 * 1000
                    audioOutputStream?.close()
                    audioOutputStream = runCatching {
                        (AudioSystem.getLine(DataLine.Info(SourceDataLine::class.java,
                                AudioFormat(AudioFormat.Encoding.PCM_SIGNED, grabber.sampleRate.toFloat(), 16,
                                        grabber.audioChannels, grabber.audioChannels * 2, grabber.sampleRate.toFloat(), true),
                                AudioSystem.NOT_SPECIFIED)) as SourceDataLine).apply {
                            open()
                            audioOutputBufferSize = bufferSize
                            start()
                            if (isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                                (getControl(FloatControl.Type.MASTER_GAIN) as? FloatControl)?.let {
                                    it.value = if (soundEnable) 0F else it.minimum
                                }
                            }
                        }
                    }.getOrNull()
                    changeState(STATE_PLAYING)
                    frameImage?.flush()
                    frameImage = null
                    notify()
                    notify2()
                    clearCacheQueue()
                    lastRootPaneHeight = 0
                    lastRootPaneWidth = 0
                    threadPool.execute(frameGrabTask)
                    threadPool.execute(imageProcessTask)
                    threadPool.execute(sampleProcessTask)
                    threadPool.execute(repaintTask)
                    return true
                } catch (t: Throwable) {
                    t.printStackTrace()
                    t.showErrorDialog(TEXT_GRAB_ERROR)
                    stop()
                }
            }
        }
        return false
    }

    fun resume() {
        if (initialized && isPaused) {
            changeState(STATE_PLAYING)
            notify()
        }
    }

    fun pause() {
        if (initialized && isPlaying) {
            changeState(STATE_PAUSED)
        }
    }

    fun stop() {
        initialized = false
        release()
        changeState(STATE_STOPPED)
    }

    private fun changeState(newState: Int) {
        state = newState
        onStateChange?.invoke(newState)
    }

    private val imageQueue = LinkedBlockingDeque<Pair<Long, BufferedImage>>()
    private val sampleQueue = LinkedBlockingDeque<Pair<Long, ByteArray>>()
    private val imageConverter = object : Java2DFrameConverter() {
        override fun getBufferedImage(frame: Frame): BufferedImage? {
            bufferedImage = null
            return super.getBufferedImage(frame)
        }
    }
    private val frameGrabTask = Runnable {
        processSafely {
            while (!isStopped) {
                runCatching { frameGrabber?.grab() }.getOrNull()?.also { frame ->
                    frame.image?.let {
                        imageConverter.getBufferedImage(frame)?.let {
                            val image = frame.timestamp to it
                            val sampleQueueSize = sampleQueue.size
                            val imageQueueSize = imageQueue.size
                            if ((sampleQueueSize >= cacheQueueSize && imageQueueSize >= cacheQueueSize) ||
                                    (sampleQueueSize >= maxCacheQueueSize || imageQueueSize >= maxCacheQueueSize)) {
                                wait()
                            }
                            imageQueue.put(image)
                        }
                    }
                    frame.samples?.run {
                        val buffer = if (isNotEmpty()) this[0] else null
                        if (buffer is ShortBuffer) {
                            val sample = frame.timestamp to ByteArray(buffer.capacity() * 2).apply {
                                repeat(buffer.capacity()) { index ->
                                    val value = buffer.get(index)
                                    this[index shl 1] = (value.toInt() shr 8).toByte()
                                    this[(index shl 1) + 1] = value.toByte()
                                }
                            }
                            val sampleQueueSize = sampleQueue.size
                            val imageQueueSize = imageQueue.size
                            if ((sampleQueueSize >= cacheQueueSize && imageQueueSize >= cacheQueueSize) ||
                                    (sampleQueueSize >= maxCacheQueueSize || imageQueueSize >= maxCacheQueueSize)) {
                                wait()
                            }
                            sampleQueue.put(sample)
                        }
                    }
                } ?: break
            }
            sampleQueue.put(-1L to ByteArray(0))
            imageQueue.put(-1L to BufferedImage(1, 1, 1))
        }
        frameGrabber?.close()
        frameGrabber = null
    }

    private var currentAudioTimestamp = 0L
    private var nextAudioTimestamp = 0L
    private val imageProcessTask = Runnable {
        if (frameGrabber?.hasVideo() == true) {
            processSafely {
                while (!isStopped) {
                    if (isPlaying) {
                        imageQueue.take().let { (timestamp, image) ->
                            val sampleQueueSize = sampleQueue.size
                            val imageQueueSize = imageQueue.size
                            if (sampleQueueSize < maxCacheQueueSize && imageQueueSize < maxCacheQueueSize) {
                                notify()
                            }
                            if (timestamp == -1L) return@processSafely
                            (grabInterval - measureTimeMillis { image.draw() }).let {
                                if (it > 0) {
                                    if (sampleQueueSize < 2 && imageQueueSize > cacheQueueSize) {
                                        imageQueue.clear()
                                    } else {
                                        wait2(when {
                                            timestamp > nextAudioTimestamp + syncThreshold -> grabInterval * 2
                                            timestamp < currentAudioTimestamp - syncThreshold -> it / 2
                                            else -> it
                                        })
                                    }
                                }
                            }
                        }
                    } else if (!isStopped) wait()
                }
            }
            imageQueue.clear()
        }
    }

    private val sampleProcessTask = Runnable {
        if (frameGrabber?.hasAudio() == true) {
            processSafely {
                while (!isStopped) {
                    if (isPlaying) {
                        sampleQueue.take().let { (timestamp, sample) ->
                            if (sampleQueue.size < maxCacheQueueSize && imageQueue.size < maxCacheQueueSize) {
                                notify()
                            }
                            if (timestamp == -1L) return@processSafely
                            sample.flush()
                            currentAudioTimestamp = timestamp
                            nextAudioTimestamp = sampleQueue.peek()?.first ?: Long.MAX_VALUE - syncThreshold
                            val diff = (audioOutputBufferSize - (audioOutputStream?.available() ?: 0)
                                    .toDouble() / (sampleRate * 2 * 2) * 1000000.0).toLong()
                            currentAudioTimestamp -= diff
                            nextAudioTimestamp -= diff
                        }
                    } else if (!isStopped) wait()
                }
                nextAudioTimestamp = Long.MAX_VALUE - syncThreshold
            }
            sampleQueue.clear()
        }
    }

    private var repaintBarrier = false
    private val repaintTask = Runnable {
        if (frameGrabber?.hasVideo() == true) {
            processSafely {
                while (!isStopped) {
                    if (isPlaying) {
                        (grabInterval - measureTimeMillis {
                            EventQueue.invokeAndWait { updateFrameImmediately() }
                        }).let { if (it > 0) wait2(it) }
                    } else if (!isStopped) wait()
                }
            }
        }
    }

    private fun updateFrameImmediately() {
        repaintManager.addDirtyRegion(rootPane, 0, 0, rootPane.width, rootPane.height)
        repaintBarrier = false
        repaintRunnable.run()
        repaintBarrier = true
    }

    private inline fun processSafely(block: () -> Unit) = try {
        block()
    } catch (t: Throwable) {
        t.printStackTrace()
        t.showErrorDialog()
    } finally {
        stop()
    }

    private fun ByteArray.flush() {
        audioOutputStream?.write(this, 0, size)
    }

    private val imageSize = Rectangle()
    private val paneSize = Rectangle()
    private fun BufferedImage.draw() {
        updateCanvasSize()
        frameImage ?: initFrameImage()
        frameImage?.createGraphics()?.run {
            composite = AlphaComposite.Src
            setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            draw2Graphics(this, paneSize, imageSize)
            dispose()
            flush()
        }
    }

    private fun Image.draw2Graphics(g: Graphics, dstBounds: Rectangle, srcBounds: Rectangle) {
        fun Int.round() = toFloat().roundToInt()
        val sx = srcBounds.x.round()
        val sy = srcBounds.y.round()
        val sw = if (srcBounds.width >= 0) srcBounds.width.round() else getWidth(null).round() - sx
        val sh = if (srcBounds.height >= 0) srcBounds.height.round() else getHeight(null).round() - sy
        g.drawImage(this, dstBounds.x, dstBounds.y, dstBounds.x + dstBounds.width, dstBounds.y + dstBounds.height, sx, sy, sx + sw, sy + sh, null)
    }

    private var lastRootPaneWidth = 0
    private var lastRootPaneHeight = 0
    private var paneWidth = 0
    private var paneHeight = 0
    private fun BufferedImage.updateCanvasSize() {
        val imageWidth = getWidth(null)
        val imageHeight = getHeight(null)
        imageSize.setBounds(0, 0, imageWidth, imageHeight)
        if (rootPane.width != lastRootPaneWidth || rootPane.height != lastRootPaneHeight) {
            if (rootPane.width < rootPane.height) {
                val imageAspectRatio = imageHeight.toFloat() / imageWidth.toFloat()
                paneWidth = rootPane.width
                paneHeight = (paneWidth.toFloat() * imageAspectRatio).toInt()
            } else {
                val imageAspectRatio = imageWidth.toFloat() / imageHeight.toFloat()
                paneHeight = rootPane.height
                paneWidth = (paneHeight.toFloat() * imageAspectRatio).toInt()
            }
            lastRootPaneWidth = rootPane.width
            lastRootPaneHeight = rootPane.height
        }
        paneSize.setBounds(rootPane.width / 2 - paneWidth / 2, rootPane.height / 2 - paneHeight / 2, paneWidth, paneHeight)
    }

    private fun initFrameImage() {
        val config = GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration
        frameImage = try {
            config.createCompatibleVolatileImage(rootPane.width, rootPane.height, ImageCapabilities(true), 3)
        } catch (e: Exception) {
            config.createCompatibleVolatileImage(rootPane.width, rootPane.height, 3)
        }.apply {
            validate(null)
            accelerationPriority = 1F
        }
    }

    private var repaintManagerReplaced = false
    private lateinit var repaintRunnable: Runnable
    private lateinit var repaintManager: RepaintManager
    private var painter = object : AbstractPainter() {

        override fun needsRepaint() = !isStopped

        override fun executePaint(glassPane: Component, graphics: Graphics2D) {
            frameImage?.run {
                val oldComposite = graphics.composite
                graphics.composite = alphaComposite
                draw2Graphics(graphics, paneSize, paneSize)
                graphics.composite = oldComposite
                onFramePaint?.invoke(graphics)
            }
        }
    }

    private fun injectPainter(event: AnActionEvent): Boolean {
        try {
            clearBackground()
            val newRootPane = (event.inputEvent.component.parent as JFrame).rootPane
            if (::rootPane.isInitialized) {
                if (rootPane == newRootPane) {
                    return true
                } else {
                    rootPane.removePainter()
                }
            }
            rootPane = newRootPane.apply { addPainter() }
            replaceRepaintManager()
            return true
        } catch (t: Throwable) {
            t.printStackTrace()
            t.showErrorDialog(TEXT_PAINTER_INITIALIZATION_FAILED)
        }
        return false
    }

    private fun JRootPane.addPainter() = (glassPane as IdeGlassPaneImpl).paintersHelper.let { helper ->
        helper::class.invokeVoid(helper, "addPainter", Painter::class to painter, Component::class to glassPane)
    }

    private fun JRootPane.removePainter() = (glassPane as IdeGlassPaneImpl).paintersHelper.run {
        this::class.invokeVoid(this, "removePainter", Painter::class to painter)
    }

    private val IdeGlassPaneImpl.paintersHelper: Any get() = this::class.invoke<Any>(this, "getNamedPainters", String::class to "idea.background.editor")!!

    private fun clearBackground() = PropertiesComponent.getInstance().setValue("idea.background.editor", null)

    private fun replaceRepaintManager() {
        if (!repaintManagerReplaced) {
            "sun.awt.AppContext".invoke<Any>(null, "getAppContext")!!.let { appContext ->
                appContext::class.invokeVoid(appContext, "put", Object::class to RepaintManager::class.java,
                        Object::class to object : RepaintManager() {
                            override fun paintDirtyRegions() {
                                if (!repaintBarrier || !isPlaying) {
                                    super.paintDirtyRegions()
                                }
                            }
                        }.also { repaintManager = it })
                repaintRunnable = RepaintManager::class.get<Runnable>(repaintManager, "processingRunnable")!!
                repaintManagerReplaced = true
            }
        }
    }

    private val LOCK = Object()
    private val LOCK2 = Object()

    private fun wait() = runCatching { synchronized(LOCK) { LOCK.wait() } }.isSuccess

    private fun wait2(timeout: Long = 0L) = runCatching { synchronized(LOCK2) { LOCK2.wait(timeout) } }.isSuccess

    private fun notify() = runCatching { synchronized(LOCK) { LOCK.notifyAll() } }.isSuccess

    private fun notify2() = runCatching { synchronized(LOCK2) { LOCK2.notifyAll() } }.isSuccess

    private fun Throwable.showErrorDialog(message: String = "") = EventQueue.invokeLater {
        Messages.showErrorDialog(StringWriter().use { sw ->
            PrintWriter(sw).use { pw ->
                printStackTrace(pw)
                pw.flush()
            }
            sw.flush()
            sw.toString()
        }, message.ifEmpty { toString() })
    }

    private fun release() {
        notify()
        notify2()
        Thread.sleep(grabInterval * 4)
        audioOutputStream?.close()
        audioOutputStream = null
        clearCacheQueue()
        sampleQueue.put(-1L to ByteArray(0))
        imageQueue.put(-1L to BufferedImage(1, 1, 1))
        frameImage?.flush()
        frameImage = null
        if (::rootPane.isInitialized) {
            rootPane.repaint()
        }
        repaintBarrier = false
    }

    private fun clearCacheQueue() {
        imageQueue.clear()
        sampleQueue.clear()
    }

    inline val Long.timestampString: String
        get() = StringBuilder().apply {
            val millisecond = this@timestampString / 1000
            val seconds = millisecond / 1000
            val fixedSeconds = seconds % 60
            val minutes = seconds / 60
            val minutesLength = minutes.toString().length
            val secondsLength = (fixedSeconds).toString().length
            if (minutesLength < 2) repeat(2 - minutesLength) { append('0') }
            append(minutes).append(':')
            if (secondsLength < 2) repeat(2 - secondsLength) { append('0') }
            append(fixedSeconds)
        }.toString()
}