注册REG
IPEnableRouter.reg

开启服务 services.msc
Routing and Remote Acess服务

windows:
配置路由：
route add 192.222.8.0 mask 255.255.255.0 10.1.0.2


linux:
iptables -A FORWARD -i tun -o eth0 -j ACCEPT
iptables -t nat -A POSTROUTING -o eth0 -j MASQUERADE
iptables -L -n -v

vi /etc/sysctl.conf
net.ipv4.ip_forward = 1
net.ipv6.conf.all.forwarding=1