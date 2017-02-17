# Remote-DNS-Attack

远程DNS攻击是指攻击者和受害者不在同一个局域网下，攻击者无法嗅探到受害者发出的包。不过在这个项目中，没有刻意去制造这样的环境，攻击者、受害者、DNS服务器实际上都在同一局域网下的，只是我们默认攻击者不可以像上一节的本地攻击一样嗅探到受害者发出的包。

## 配置

### 网络配置

同样是用三台虚拟机，一台Apollo(本地DNS服务器，同时也是受害DNS服务器，Victim DNS server)，一台用户(User，或者说客户端)，一台攻击者(Attacker)，下文中统一称呼都是Apollo，用户和攻击者。 不过我这次没有配置静态IP，所以实验时IP地址会与下图不一致。依次是192.168.153.130、192.168.153.133和192.168.153.129。

![组网](https://raw.githubusercontent.com/familyld/Remote-DNS-Attack/master/graph/image28.png)

### 配置本地DNS服务器
第一步：安装bind9服务器，由于SEEDUbuntu已经预装了，所以无须操作。

第二步：修改配置文件named.conf.options，这个配置文件是DNS服务器启动时需要读取的，其中dump.db是用来转储DNS服务器的缓存的文件。

![local server](https://raw.githubusercontent.com/familyld/Local-DNS-Attack/master/graph/image11.png)

特别地，以下两条命令可以帮助我们更好地进行实验：

![local server](https://raw.githubusercontent.com/familyld/Remote-DNS-Attack/master/graph/image29.png)

其中第一条命令是把DNS缓存清空，第二条命令是把缓存转储到dump.db文件。

第三步：移除example.com zone，前面的本地DNS攻击实验是假设我们拥有example.com域名并负责其应答，而在这次的远程DNS缓存投毒攻击实验中，本地DNS服务器不负责这件事情，example.com的应答交给互联网上专门的example.com DNS 服务器来负责。这里把原来加入到named.conf文件的zone删掉即可。

第四步：启动DNS服务器

![bind9](https://raw.githubusercontent.com/familyld/Local-DNS-Attack/master/graph/image15.png)

或者使用sudo service bind9 restart 语句也可以，至此本地DNS服务器配置完毕。

### 配置客户端

在客户端我们需要配置使得Apollo (192.168.153.130) 成为客户端(192.168.153.133)的默认DNS服务器，通过修改客户端的DNS配置文件来实现。

第一步：修改etc文件夹下的resolv.conf文件，加入红框内的内容。

![client](https://raw.githubusercontent.com/familyld/Remote-DNS-Attack/master/graph/image30.png)

注意，因为Ubuntu系统中，resolv.conf会被DHCP客户端覆写，这样我们加入的内容就会被消除掉，为了避免这个状况我们要禁止DHCP。

第二步：禁止DHCP

![client](https://raw.githubusercontent.com/familyld/Local-DNS-Attack/master/graph/image17.png)

点击All Settings，然后点击Network，在Wired选项卡点击Options按钮，然后在IPv4 Settings选项卡中把Method更改为Automatic(DHCP) addresses only，然后把DNS servers更改为前面设置的本地DNS服务器的ip地址。

因为没有找到实验指导中Network Icon的Auto eth0选项，所以这里直接手动从命令行重启一下网卡：

![client](https://raw.githubusercontent.com/familyld/Remote-DNS-Attack/master/graph/image31.png)

为了确保万无一失，重启一次虚拟机来使修改生效也行。再查看一下此时的DNS服务器，确实就我们刚刚配置的，DHCP被禁止了，没有对我们的修改进行覆盖，这样就证明配置成功了。

![client](https://raw.githubusercontent.com/familyld/Remote-DNS-Attack/master/graph/image32.png)

### 配置攻击者

攻击者可以是网络上任意一台主机，我们可以通过raw socket编程来伪造DNS包进行攻击。但同时我们也要实现一个假的DNS服务器，这样当受害者使用域名访问网站时，我们可以把他们导向恶意网站了。这里我们把假的DNS服务器和攻击者的机器设置为同一台，但实际上可以是不同的主机。

1. 配置攻击者的默认DNS服务器，使得在攻击者的机器上查询DNS时会向Apollo发出请求。这一步骤和配置用户机是一样的，修改resolv.conf文件，然后关闭DHCP服务即可。

2. 配置Apollo的DNS查询端口为33333，如果不设置的话，端口是随机的，则远程攻击时难度会更大(除了猜transaction ID还得猜port)。具体来说是通过修改Apollo的/etc/bind/named.conf.options文件实现的。

3. 除了设置固定的端口以外，我们还需要关闭dnssec-validation服务，这是设置用来防止DNS缓存投毒攻击的。如果不关闭的话，攻击会非常困难。注释掉named.conf.options文件中的对应条目，并且加入关闭dnssec服务的语句：
![attacker](https://raw.githubusercontent.com/familyld/Remote-DNS-Attack/master/graph/image36.png)

4. 在Apollo上刷新DNS缓存，然后重启DNS服务器。
![attacker](https://raw.githubusercontent.com/familyld/Remote-DNS-Attack/master/graph/image37.png)

## 项目解析

首先，我们的目标是使得用户在访问[www.example.com](www.example.com)时，本地DNS服务器把该域名解析为我们设定的恶意IP地址，当用户访问该域下的其他域名时，本地DNS服务器会向假的DNS服务器请求应答。

[www.example.com](www.example.com)的真实权威域名服务器IP地址是93.184.216.119，它的DNS服务器由ICANN管理。 当用户对该域名执行dig命令或在浏览器打开网站时，用户的机器就会发送一个DNS请求到本地DNS服务器，并最终从www.example.com的DNS服务器获取到正确的IP地址。

**注意！**instruction中给出www.example.com的权威域名服务器IP地址是错误的，在ip.chinaz.com查询了一下，正确的权威域名服务器IP地址是93.184.216.34。

![client](https://raw.githubusercontent.com/familyld/Remote-DNS-Attack/master/graph/image33.png)

要实现目标，我们实际上是对Apollo(本地DNS服务器)进行投毒，使得用户在对该域名执行dig命令或在浏览器打开网站时，变成是向攻击者的DNS服务器ns.dnslabattacker.net 提交请求，由此我们可以自己设定一个恶意IP地址，并使得用户被重定向到这个地址而不是在[www.example.com](www.example.com) DNS服务器中设定的正确IP地址。

具体来说，这个项目可以分为两个任务，一是缓存投毒，二是结果验证。在第一个任务中，我们要对Apollo进行投毒，使得Apollo的DNS缓存中，ns.dnslabattacker.net 被设定为example.com域名的域名服务器。 在第二个任务中，要验证投毒是否成功，也即检验在用户机上对www.example.com 使用dig命令确实返回我们设定的恶意IP地址。

![client](https://raw.githubusercontent.com/familyld/Remote-DNS-Attack/master/graph/image34.png)
![client](https://raw.githubusercontent.com/familyld/Remote-DNS-Attack/master/graph/image35.png)

### 实现原理

先简单分析一下整个查询过程，当Apollo中没有example.com DNS服务器地址的缓存时，它首先要向根DNS服务器发出请求，然后根DNS服务器会告诉Apollo去查询.COM DNS服务器，.COM DNS服务器再把example.com DNS服务器的地址告诉Apollo，然后Apollo再向example.com DNS服务器发出请求，最终获得www.example.com 的IP地址。

如果Apollo中已经缓存好了example.com DNS服务器的地址，就不用再查询根DNS服务器了，这时会直接向example.com DNS服务器提出对域名的查询。攻击时利用的正是这一特性，我们可以向Apollo多次提交example.com 域下的不同域名的查询包，这样Apollo会向example.com DNS服务器提交多次查询，而此时我们通过大量伪造对应的应答包来进行匹配，只要能先于真正example.com DNS服务器应答包到达，则感染成功，Apollo会记录我们伪造包的信息，包括查询的域名和伪造的恶意IP，还有域名对应的伪造的DNS及DNS的ip，这样就成功毒化了。

### 毒化缓存分析

要实现投毒，我们就要先向Apollo发送一条DNS查询，然后在Apollo等待example.com DNS服务器发回应答的过程中，伪造DNS应答包。主要的困难是要猜出transaction ID，这个字段是16bit的，也就是说有65536种可能。如果我们在example.com DNS服务器发回应答前能伪造K个应答包，则成功的概率就是K/65536，还是比较高的。

归纳一下，毒化缓存主要有以下三个约束：

1. TTL约束： 域名不能够已经在dns cache server的缓存中

2. Guess约束：transaction id能够成功匹配。
3. Window
Time约束：伪造包要比真正DNS服务器返回包快。

一个很现实的问题就是约束1)。如果伪造包失败了，Apollo会把域名及其解析存在DNS缓存中，这样我们就无法伪造同一个域名的DNS应答包了，因为我们向Apollo发送同样域名的DNS查询时，Apollo不会再向example.com DNS服务器发出查询。要伪造同一个域名的DNS应答包就只能等到这条缓存记录超时被删除掉了，这通常需要等待好几个小时甚至好几天。

### The Kaminsky Attack

为了克服约束1，就有了Kaminsky Attack这种攻击方法。Kaminsky的主要技术是绕开TTL的约束，使得攻击具有较高的成功率。而且，Kaminsky并不只是毒化一个域名，它能把被攻击域名的权威域名服务器也进行毒化，改造为faked name server(伪造的权威域名服务器)，从而具有极大的危害性。

如何绕过TTL约束呢？我们可以从不存在TTL约束的域名着手。什么域名不存在TTL约束呢？答案是**不存在的随机化域名**，这种域名在DNS服务器中没有缓存，从而不存在TTL约束。但访问不存在的域名不能够有多少价值，因为**需要缓存的，存在的域名才有价值**。Kaminsky注意到了**additional region的可利用性**。因为additional region有ns的glue记录，即使是查询不存在的域名，也会返回这些additional region记录，并且是能够缓存在DNS服务器中。于是，Kaminsky把焦点转移到了将不存在域名与additonal region漏洞结合来进行攻击。

攻击者在查询随机化的域名的同时针对性地伪造ns的IP包返回。这种攻击尽管仍然受window time约束和guess约束，但由于有足够大的可用的连续攻击时间(不需要等待TTL结束，能够持续不断地进行攻击)，使得guess的低概率在足够多次的重复攻击下成为高可能。

### 伪造DNS应答包

在这个项目中，我们使用raw socket编写c代码来实现，需要填充好DNS包的各个字段。instruction提供了example.com DNS服务器的标准DNS应答包截图：

![fake DNS](https://raw.githubusercontent.com/familyld/Remote-DNS-Attack/master/graph/image38.png)

其中199.43.132.53是example.com真正的域名服务器的地址(但实际上收到的是199.43.135.53，这与instruction不同)，10.0.2.6是本地DNS服务器的地址。高亮的部分就是这个UDP包(DNS应答包都是UDP包)的payload数据，也是我们需要构造的部分。

### 为什么域名服务器地址不同了？
在[http://auditdns.net/?domain=example.com](http://auditdns.net/?domain=example.com)查询example.com域名，可以发现example.com有两个域名服务器，如下图：

![dns server](https://raw.githubusercontent.com/familyld/Remote-DNS-Attack/master/graph/image39.png)

显然没有199.43.132.53，我猜测是地址换了。不过一般每一个域名都至少要有两个DNS服务器，这样如果其中一个DNS服务器出现问题，另外一个也可以返回关于这个域名的数据，多个DNS服务器上的DNS记录应是相同的。

### 在攻击者机器上dig example.com

![dig example](https://raw.githubusercontent.com/familyld/Remote-DNS-Attack/master/graph/image40.png)

使用Wireshark抓包：

**DNS 查询**

![dig example](https://raw.githubusercontent.com/familyld/Remote-DNS-Attack/master/graph/image41.png)

**DNS 应答**

![dig example](https://raw.githubusercontent.com/familyld/Remote-DNS-Attack/master/graph/image42.png)

最开始是向192.35.51.30 (f.gtld-servers.net)发出查询www.example.com，gtld是什么呢？它是通用顶级域（英语：Generic top-level domain. 的意思。f.gtld-servers.net则是负责.com 和 .net 域名的一个顶级域名服务器。从返回的应答包来看，这一步得到的信息是可以从a.iana-servers.net和b.iana-servers.net这两个域名服务器找到www.example.com 的线索。这一步其实已经返回了a.iana-servers.net和b.iana-servers.net这两个域名服务器的地址，但是Apollo显然没有采用。

### 为什么Apollo不直接接受.com的DNS返回的example.com的DNS a.iana-servers.net 的地址呢？

这是因为gtld域名服务器和权威域名服务器的管辖范围不同，gtld域名服务器管辖的是顶级域，不负责域名服务器的授权；权威域名服务器管辖的是区(zone)，区是域(domain)的一个子集 ，负责该区主机的登记注册。这里有点容易混淆区和域两个概念，按照[https://support.microsoft.com/zh-cn/kb/164054](https://support.microsoft.com/zh-cn/kb/164054)的说法，区包含的是域的某些子域的数据，不是非常理解。但这个问题可以认为就是gtld不负责域的授权，这事得交给权威域名服务器来做，所以gtld返回的地址不会被采纳。

然后Apollo又向192.5.6.30发出查询，查询的是它从gtld得到的线索，也即a.iana-servers.net和b.iana-servers.net这两个域名服务器的地址。这个IP地址属于.COM级的DNS服务器。从它的应答包来看，它返回了这两个域名服务器的地址。同时返回了一条线索，告诉我们可以在ns.icann.org这个域名服务器上得到信息。

于是Apollo又向192.228.79.201 (b.root-servers.net)发出查询，查询ns.icann.org域名服务器的地址，192.228.79.201 (b.root-servers.net) 是一个根域名服务器，全球共13个，由a到m。根域名服务器也没有答案，不过它也返回一条线索，ns.icann.org域名服务器的地址就藏在下面这些主机里啦~

![dig example](https://raw.githubusercontent.com/familyld/Remote-DNS-Attack/master/graph/image43.png)

Apollo向其中的192.249.112.1 (a2.org.afilias-nst.info)发出查询，不负所望它终于返回了ns.icann.org域名服务器的地址：

![dig example](https://raw.githubusercontent.com/familyld/Remote-DNS-Attack/master/graph/image44.png)

Apollo继续向199.4.138.53 (ns.icann.org)发出查询，因为ns.icann.org是权威域名服务器，它返回的a.iana-servers.net和b.iana-servers.net这两个域名服务器的地址会被Apollo存储下来。

![dig example](https://raw.githubusercontent.com/familyld/Remote-DNS-Attack/master/graph/image45.png)

最后Apollo向b.iana-servers.net发起www.example.com 的查询，并且顺利得到了该域名对应的IP地址。再次访问该域名就不需要经过前面这么多步骤了，因为IP地址已经保存到Apollo的缓存中，可以直接访问。而如果访问该域下的别的域名，也不需要经过前面那么多步，只需要a.iana-servers.net或者b.iana-servers.net提交查询就可以了。

### DNS查询和响应包的格式

所有的DNS request和response数据包都是UDP数据报，经过ip层封装从网络发送到目标主机。DNS数据报中包含20字节的IP首部，8字节的UDP首部，12字节的DNS首部以及DNS数据。其中IP首部和UDP首部我们之前的实验已经尝试过了，主要是掌握如何填充DNS首部和数据。

![DNS packet](https://raw.githubusercontent.com/familyld/Remote-DNS-Attack/master/graph/image46.png)

Transaction ID是16bit的，这个前面已经提到了。然后标志flags有很多种组合，但这次实验我们需要用到的只有两种，分别是分别是0x0100(标准查询)和0x8400(标准授权应答)。具体可以看：[http://f.dataguru.cn/thread-573710-1-1.html](http://f.dataguru.cn/thread-573710-1-1.html)

然后四个数目字段，如果是DNS查询，则一般问题数为1，其他几个数目为0.如果是DNS应答就要看具体情况了。比方说这里查询example.com得到的应答包就包含1个查询(和查询包一致)，1个应答(如果有的话)，2条权威域名服务器资源记录，0条附加资源记录：

![DNS packet](https://raw.githubusercontent.com/familyld/Remote-DNS-Attack/master/graph/image47.png)

然后数据部分就是一一对应首部的：

![DNS packet](https://raw.githubusercontent.com/familyld/Remote-DNS-Attack/master/graph/image48.png)

这就是我们需要伪造的包，Queries部分采用一个不存在的域名，Answer部分采用伪造的恶意IP地址，权威域名服务器填上ns.dnslabattacker.net。

## 结果验证

如果攻击成功，那么Apollo的DNS缓存就会像下图一样，可以看到example.com 的NS记录变成了我们伪造的ns.dnslabattacker.net。为了检验是否真的成功，我们可以在用户机上使用对www.example.com 使用dig命令，查看返回的IP地址。

![result](https://raw.githubusercontent.com/familyld/Remote-DNS-Attack/master/graph/image49.png)

但是，当Apollo收到缓存中不存在的DNS记录的查询时，它就会向我们设置的伪造域名服务器ns.dnslabattacker.net提交查询，因为这个域名服务器是不存在的，Apollo会发现这一点，然后把这条DNS记录设置为无效记录，这样毒化就失效了。这时可能会想，**能不能在伪造应答包时给ns.dnslabattacker.net设置一个IP地址，从而使得伪造的域名服务器变为真实“存在”的呢？**

答案是否定的，原因和前面分析**为什么Apollo不直接接受.com的DNS返回的example.com的DNS a.iana-servers.net 的地址**是一样的。因为我们伪造应答包是从a.iana-servers.net或者b.iana-servers.net这两个域名服务器返回的，它们不是负责管辖example.com这个域的权威域名服务器，所以即使我们设置了IP地址，Apollo也不会采纳。

有两个方案解决这个问题：

**方案一、使用真正的域名**

> 如果我们有真正的域名就不需要用ns.dnslabattacker.net这个假的了，直接替换掉伪造应答包中的ns.dnslabattacker.net就可以了。当然前提是我们的域名解析到了主机上面，能够提供应答，像本地攻击实验那样配置就可以了。

**方案二、使用伪造的域名**

> 因为我们没有真正的域名，所以实验中采用这个方案。直接在Apollo的DNS配置中增加一个ns.dnslabattacker.net对应的IP地址，把它指向攻击者的主机。这样Apollo就不需要去问上级DNS服务器ns.dnslabattacker.net的IP地址是什么，自然也就不会穿帮了。

### 方案2的具体配置

1. 配置Apollo的/etc/bind/named.conf.default-zones文件，加入以下条目：<br>
![config](https://raw.githubusercontent.com/familyld/Remote-DNS-Attack/master/graph/image50.png)

2. 创建file对应的文件db.attacker，然后把对应的内容写进去。这个文件已经好了，直接复制到Apollo的虚拟机中对应位置(/etc/bind/)即可。<br>![config](https://raw.githubusercontent.com/familyld/Remote-DNS-Attack/master/graph/image51.png)

3. 在攻击者的主机上配置DNS服务器，这样才能对Apollo的查询提供应答。配置方式和本地攻击时进行的配置是类似的。首先在/etc/bind/named.conf.local文件中添加以下条目：<br>![config](https://raw.githubusercontent.com/familyld/Remote-DNS-Attack/master/graph/image52.png)

4. 然后创建file对应的文件，同样是已经提供好的，复制到攻击者的主机就可以了：<br>![config](https://raw.githubusercontent.com/familyld/Remote-DNS-Attack/master/graph/image53.png)

5. 配置完成后需要重启Apollo和攻击者主机，使得配置生效。如果配置成功，那么在用户主机上dig www.example.com，返回的就是上面db文件中设置得1.1.1.1。

### 攻击效果

攻击前Apollo的DNS缓存：

![result](https://raw.githubusercontent.com/familyld/Remote-DNS-Attack/master/graph/image54.png)

可以看到example.com对应的域名服务器是原来的a.iana-servers.net和b.iana-servers.net。当Apollo收到该域的其他域名时就会向这两个域名服务器发出DNS查询。

![result](https://raw.githubusercontent.com/familyld/Remote-DNS-Attack/master/graph/image55.png)

攻击时，我只借用攻击机的IP(可以是伪造IP)向Apollo发起DNS查询，因为查询的域名abdde.example.com(随机生成)在Apollo的DNS缓存中没有，所以Apollo就向域名服务器b.iana-servers.net(199.43.133.53)发起了DNS查询。2~7的包是伪造的应答，因为在程序中我设置的是在攻击者发出DNS查询的0.9s后开始发送大量的伪造应答包(100个，目的是猜出Apollo向域名服务器发出DNS查询时用的Transaction ID)，这个时间比Apollo实际向域名服务器发出DNS查询的时间短，所以会看到其中一部分伪造应答包已经发出了。

![result](https://raw.githubusercontent.com/familyld/Remote-DNS-Attack/master/graph/image56.png)

查看伪造应答包的内容，其中查询部分与之前发出的DNS查询是完全一致的，然后加入Answer部分，把查询域名对应的IP地址设置为恶意IP，然后权威命名服务器设置为ns.dnslabattacker.net。

#### 用户机上dig example.com

![result](https://raw.githubusercontent.com/familyld/Remote-DNS-Attack/master/graph/image57.png)

#### Apollo的DNS缓存

![result](https://raw.githubusercontent.com/familyld/Remote-DNS-Attack/master/graph/image58.png)

可以看到Apollo已经成功被毒化了，再次dig该域的其他域名，Apollo就会变成向ns.dnslabattacker.net这个假的权威域名服务器发出DNS查询了。
