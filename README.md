<div style="display:flex;justify-content:center">
<img src="https://kanokano.cn/wp-content/uploads/2025/04/5acb8625d65a3fd5d7b228830a9450a1.webp" style="width:50%;text-align:center" />
</div>

## 有哪些功能？
 
 
* **远程管理（需配合内网穿透）**
* **短信收发**
* **各种参数实时显示（cpu温度，内存负载 ，信号强度 , SNR、PCI、小区号、频段、v6地址等）**
* **锁频段，锁小区（免重启）**
* **USB调试（自带网络ADB调试辅助，关闭开启一下ADB功能既可生效）**
* **双端可用（既可手机安装使用（无），也可为F50作为服务端安装使用）**
* **开机自启**
* **性能模式、指示灯、文件共享开关**
* **3G/4G/5G 网络切换**
* **其他未来会继续更新功能**


![](https://kanokano.cn/wp-content/uploads/2025/04/33765df57d9acefe785d6b86974fdd3e.webp)
![](https://kanokano.cn/wp-content/uploads/2025/04/2e3b22b558b52cee342bfd292ad061e2.webp)



## 如何使用？

**安卓用户**

1. 首先下载软件apk，安装至自己的手机内并打开
2. 和随身WiFi处于同一个网络下，打开控制网页，登录并启用adb功能
3. 使用电脑或手机的ADB功能 连接上随身WIFI 并将apk安装至随身WiFi机内
4. 使用scrcpy等远程控制软件启动zte-ufi-tools，设置网关，启动服务，并关掉电池优化，启用通知（确保能顺利开机自启）
5. 手机访问随身WiFi的ip地址，端口为2333，即可食用

**ios用户**

> ios用户需要使用传统方法打开adb, 连接wifi 输入 http://192.168.0.1/index.html#usb_port 开启adb
> 之后的操作按照安卓用户 步骤3开始即可

备注：功能是否能使用取决于你的机型和版本，目前我测试完美的版本是 **MU300_ZYV1.0.0B09**

备注2：由于cpu使用率 温度 内存使用率并无官方接口，如果你将本apk安装在手机上使用，则温度和占用数据源是你的手机提供，并非随身WiFi数据。

下载链接： https://www.123684.com/s/7oa5Vv-dQLD3?提取码:CkSj