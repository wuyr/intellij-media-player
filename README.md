## 【🐟摸鱼专用】上班偷偷看视频📺而不会被老板打🔨的IDE插件，适配JetBrains全家桶

<br/>

### 博客详情： <https://blog.csdn.net/u011387817/article/details/119217486>

<br/>

### 插件起源：

去年在新电脑上看视频的时候，在触摸板上做了一个缩放的手势把程序列表Call出来了：

![preview](https://github.com/wuyr/intellij-media-player/raw/main/previews/1.jpg)

我那时候是纯黑色的壁纸，再加上视频也刚好播放到白色衣服人物在黑夜中的画面，这就使得画面中的人物变得立体起来了！甚至有一种身临其境的感觉！

![preview](https://github.com/wuyr/intellij-media-player/raw/main/previews/2.jpg)

我当时就觉得，哇这种效果好棒啊，就像在播放透明背景的视频一样。记得那时候还在鸿神的群里讨论了一下关于播放透明视频的话题，后面还说到Android Studio有个设置透明背景图的功能，但图片毕竟是静态的，肯定没有动态的好玩。

第二天我很随意的debug了一下Android Studio的设置背景图片的功能（完全不抱希望的），没想到居然发现了它是通过很简单的一句代码来指定图片：

```kotlin
PropertiesComponent.getInstance().setValue("idea.background.editor", "image path")
```

于是我就想：**播放视频不也就是一张张图片快速切换嘛？如果我弄一个插件，里面用计时器周期性刷新这个图片，不就实现了动态的效果了？**

第二天按照这个想法尝试了下，果然可以！接着我把一个视频每一帧图片都导出来，在插件中周期性调用上面的方法把每一帧图片路径都传进去。。。。

你应该也猜到了，没错，很卡，非常非常卡！比PPT还要卡，卡到不能正常使用！

认真看了下代码之后发现，原来这个设置背景图片是IDEA提供的，IDEA窗口的RootPane下面有个叫GlassPane的Child，那个含有透明度的背景图就是在GlassPane里面的PaintHelper中绘制的。更多细节会在即将到来的文章上面讲清楚，敬请期待。


<br/>

### 大致原理：
解析视频的每一帧画面和声音，根据帧率分别刷新到背景板和写入到声卡里。这一步在有API提供的情况下都不难实现，**主要难度在于优化绘制效率！！！**

如果你熟悉Java GUI编程的话，你会知道swing是通过脏区(Dirty Area)来找到需要重新绘制的Components，但是这里偏偏又碰上了视频画面，视频画面通常都是占满了整个IDE窗口的。也就是说，当你刷新视频帧的时候，会连带着整个IDE窗口里的所有Component也重新绘制一遍！！！这是什么概念？如果你播放的是60帧的视频，就代表着视频帧的刷新周期是16ms，并且IDE窗口内Component的绘制总时长不能超过16ms（超过就有卡顿感了）。可现实是你电脑的cpu不可能一直偏心这个绘制线程的，平时编译项目的时候基本上都是90%使用率以上，如果不花点手段优化下这个绘制效率，那在IDE窗口里播放视频就跟播放PPT差不多了。。。

**当前已使用的优化方案有：**
- 最大程度复用临时对象（这个不用说）；

- 开启硬件加速（网上搜到的）；

- 使用VolatileImage代替BufferedImage来绘制（这个是在IDEA源码里偷学到的）；

- 干掉了swing原来的RepaintManager，替换成自己实现的。自己实现的RepaintManager会过滤掉重复绘制的区域，也就是在同一时间段内，同一区域只会绘制一次（独家！根据源码总结出来的，网上绝对没有）；

- 重绘画面时绕过AWT的消息队列，不参与排队，直接交给RepaintManager处理（独家！根据源码总结出来的，网上绝对没有）。优化前每次重绘都要排2次队；


经过优化之后，现在已经可以轻松播放1080p的视频，4k25帧的也能hold住，不过4k60帧的就比较吃力了，配置高点的电脑应该可以。我已经尽力了。。。

>2021-06-11 15:37:22 我悟了，既然绝大部分时间都是带有透明度播放的，也就是看不到视频原本的画质，那我何必要分4k不4k的呢？要那么高也没有用，何不主动降低分辨率以提高绘制效率？

>2021-06-25 17:47:50 我自闭了，尽管主动降低分辨率之后在Windows和Linux的1080p显示器上都能流畅播放4k视频了，但是MacBook Pro的高分辨率（3072*1920）屏幕每次绘制还是耗时100~300ms左右，太难了！

>2021-08-26 01:40:00 好消息！在MacBook Pro(16寸)上已经可以流畅播放1080p 60帧和4k视频了！


**还可能有同学想问，为什么能把视频画面放在所有组件的下面，并且能设置透明度？这是怎么做到的？**

请参考上面贴出的博客详情链接里面的文章。

<br/>

### 安装：
**在线安装：**

*Settings -> Plugins -> Marketplace*里搜索***Media Player***：

![preview](https://github.com/wuyr/intellij-media-player/raw/main/previews/6.png)

点击安装即可；

**本地安装：**

到 [releases](https://github.com/wuyr/intellij-media-player/releases) 里下载最新的 *intellij-media-player.zip* 后拖把它拖进IDE中并重启。

<br/>

### 温馨提示： 

如果在播放视频中领导正在向你走来😰请不要犹豫，马上按下组合键 *ALT* + *X* 视频会立即停止并隐藏相关控制按钮😃

<br/>

### 效果图：

![preview](https://github.com/wuyr/intellij-media-player/raw/main/previews/1.gif)

![preview](https://github.com/wuyr/intellij-media-player/raw/main/previews/2.gif)

![preview](https://github.com/wuyr/intellij-media-player/raw/main/previews/3.gif)

### 效果视频：

[效果视频1](https://github.com/wuyr/intellij-media-player/raw/main/previews/1.mp4) ，[效果视频2](https://github.com/wuyr/intellij-media-player/raw/main/previews/2.mp4)，[效果视频3](https://github.com/wuyr/intellij-media-player/raw/main/previews/3.mp4)

<br/>

### 更新日志：

 - **1.0.4** 修复版本兼容性问题。

 - **1.0** 完成基本功能。

<br/>

### 感谢：

本插件依赖[javacv](https://github.com/bytedeco/javacv)和[FFmpeg](https://github.com/FFmpeg/FFmpeg)提供的API作音视频处理，感谢这两个仓库的所有参与者。

感谢wanandroid交流群里的 "[小学生](https://github.com/yuxitong)" 帮忙解答音视频同步问题。
