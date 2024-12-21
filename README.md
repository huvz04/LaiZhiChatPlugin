# 「来只」插件

> 基于 Mirai QQ机器人的发图插件

[![Static Badge](https://img.shields.io/badge/Mirai-2.16.0-yellow)](https://github.com/mamoe/mirai)
[![Static Badge](https://img.shields.io/badge/release-0.5.1-blue)](https://github.com/huvz04/LaiZhiChatPlugin/releases)


:warning:  **最新版本已经内置了 [MiSans](https://hyperos.mi.com/font/zh/download/) 字体**<br>

## 主要功能 ：关键字触发发图

无需配置，直接扔进 `bot/plugins` 即可使用

**分群存储**：不同的群组存储不同的群友图片，支持多群，再也不用担心隔壁的图泄露了<br>

**开关检索**：添加了开关关键字的指令，可以在指令触发和直接触发中选择

**获取列表**：获取图片列表，可以通过文字和图片两种方式获取，查询群友存了什么草图


### :pencil: 具体食用方法:

当你发送`来只 AAAAAA` 时 ，插件会从本地存储的 *AAAAAA* 文件夹中取出图片



在群里发送 `开关关键字` ，机器人会倒置当前状态。<br>
关闭=>开启/开启=>关闭



在群里发送 `#获取图库` 、`#图库`  机器人会返回一张渲染好的图片列表

当开关关键字=true时， 只要聊天中出现了关键字就会自动触发。


指令可以在config中修改..
