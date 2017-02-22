# JavaOverlay
实现java 级别的的Overlay机制  
###功能  
####1. 往后的java文件会覆盖前面同名的java 文件.  

![Paste_Image.png](http://upload-images.jianshu.io/upload_images/166866-c531518d5e2f3f94.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)
####2. java文件覆盖引用的第三方库class 文件.  
![Paste_Image.png](http://upload-images.jianshu.io/upload_images/166866-81a09b17a2d8eaac.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

###缺陷:  
1. 在ide 中在多个sourceSet 定义同一个java 文件会在编码期间报红:
Duplicate class file. 在编译期可以正常工作.
* java文件覆盖引用的第三方库class 文件.需要在build.gradle 中配置规则.同时支持通配符.  

###Feature:  
1. Duplicate class file   
* 支持通配符定义规则  
* 自动配置规则代替手动编写  




