## 【🐟摸鱼专用】上班偷偷看视频📺而不会被老板打🔨的IDE插件，适配JetBrains全家桶

<br/>

### 博客详情： 敬请期待。。。

<br/>

### 插件起源：
今晚回去写。


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

**还可能有同学想问，为什么能把视频画面放在所有组件的下面，并且能设置透明度？这是怎么做到的？**

emmmm，在即将到来的文章里会带大家一起了解这个东西，一起从0开始做一个视频播放插件。

<br/>

### 安装：
很快就好。

<br/>

### 温馨提示： 
如果在播放视频中领导正在向你走来😰请不要犹豫，马上按下组合键*CTRL* + *ALT* + *SHIFT* + *Z* 视频会立即停止并隐藏相关控制按钮😃

<br/>

### 效果图：

![preview](https://github.com/wuyr/intellij-media-player/raw/main/previews/1.gif)

![preview](https://github.com/wuyr/intellij-media-player/raw/main/previews/2.gif)

![preview](https://github.com/wuyr/intellij-media-player/raw/main/previews/3.gif)

### 效果视频：
[效果视频1](https://github.com/wuyr/intellij-media-player/raw/main/previews/1.mp4) ，[效果视频2](https://github.com/wuyr/intellij-media-player/raw/main/previews/2.mp4)，[效果视频3](https://github.com/wuyr/intellij-media-player/raw/main/previews/3.mp4)

<br/>

### 更新日志：

- **1.0** 完成基本功能。

<br/>

### 感谢：

本插件依赖[javacv](https://github.com/bytedeco/javacv)和[FFmpeg](https://github.com/FFmpeg/FFmpeg)提供的API作音视频处理，感谢这两个仓库的所有参与者。

感谢wanandroid交流群里的 "[小学生](https://github.com/yuxitong)" 帮忙解答音视频同步问题。