# DingTalk SMS

![pipeline status](https://badges.git.reallct.com/qwe7002/dingtalk-sms/badges/master/pipeline.svg)
![Min Android Version](https://img.shields.io/badge/android-22+-orange.svg)
[![License](https://img.shields.io/badge/License-BSD%203--Clause-blue.svg)](https://github.com/qwe7002/dingtalk-sms/blob/master/LICENSE)

在Android设备上运行的机器人。

您可以在 https://reall.uk 上发布Issue和讨论问题。

[下载](https://github.com/qwe7002/dingtalk-sms/releases)

**预发布版本中可能存在未知错误，请自行承担风险。**

## 特征

- 转发短信到钉钉

- 监控未接来电

- 监控设备电池电量变化

- 通过SMS进行远程控制。

## 权限

此应用需要以下权限：

- 短信：阅读并发送短信。

- 电话：获取是否是双卡电话，卡状态和标识符ID。

- 通话记录：获取来电号码。

- 联系人：获取联系信息并自动识别来电者的号码。

您可以将此应用程序设置为默认的短信应用程序，它将阻止所有短信通知并自动设置为已读取。

## 使用可信手机控制机器人

您可以为自动转发指定可信电话号码。机器人从该号码收到消息后将自动转发，格式如下：

```
<接收方电话号码>
<短信内容>
```

例子：

```
10086
cxll
```

它会将内容 `cxll` 的短信发送到号码 `10086` 。

您可以通过发送命令 `restart-service` 重新启动所有后台进程

**如果您不在当前区域，请添加您的国家/地区呼叫区号（例如，中国大陆国际区号：+86）。**

## 致谢

该软件使用以下开源库：

- [okhttp](https://github.com/square/okhttp)

- [Gson](https://github.com/google/gson)

这个软件的诞生离不开他们的帮助：

- [@SumiMakito](https://github.com/SumiMakito)

- [@zsxsoft](https://github.com/zsxsoft)

以下组织为此页面提供图像存储：

- [sm.ms](https://sm.ms)

## 给一杯咖啡让我更好地维护这个项目？

- [Paypal](https://paypal.me/nicoranshi)
 
- 比特币 [**17wmCCzy7hSSENnRBfUBMUSi7kdHYePrae**]

- [云闪付](https://static.reallct.com/2019/02/21/5c6d812840bac.png)


您的捐款将使我更好地完成这个项目。