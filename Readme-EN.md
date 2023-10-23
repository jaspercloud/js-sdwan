# netThunder

![banner](doc/img/banner.png)

**netThunder** is a private Software-Defined Wide Area Network (SD-WAN) solution designed to meet users' network needs. It combines peer-to-peer connections and end-to-end encrypted transmission to provide superior network performance and security.

- **Frontend**: [netThunder Dashboard](https://github.com/jaspercloud/js-sdwan-dashboard)
- **Download**: [netThunder Releases](https://github.com/jaspercloud/js-sdwan/releases)

## netThunder vs. Traditional VPN

In the traditional VPN model, all traffic is centralized on a protected network, and all clients connect to a VPN server. However, this can lead to several issues:

- VPN servers can easily become overloaded with an increasing number of connections.
- Server outages can result in costly system interruptions and remote teams being unable to work.
- All traffic passing through the VPN server leads to network latency and increased traffic usage.

**netThunder**, on the other hand, provides a more flexible and efficient solution:

- Through the dashboard, you can configure the flow of traffic, creating a private network that includes remote teams and data centers without affecting regular internet usage and gaming.

## Key Features

- **Point-to-Point Connections**: netThunder allows users to establish point-to-point connections, integrating branch offices, remote teams, and partner networks. This direct connection eliminates redundancy in the network topology, improving performance and efficiency.

- **End-to-End Encryption**: Security is a top priority. netThunder's SD-WAN solution provides ECDH-AES encryption for end-to-end secure transmission, reducing the risk of data leakage.

- **Centralized Management**: netThunder integrates powerful centralized management tools, making it easy to manage and monitor the entire network, including traffic, security policies, and performance metrics.

- **Dynamic Load Balancing**: The SD-WAN solution automatically adjusts traffic routing for optimal performance, avoiding congestion and providing an outstanding user experience.

- **High Scalability**: The product can easily adapt to changes in your network's scale, ensuring performance remains unaffected as your network grows.

## TUN Device Principles

In SD-WAN (Software-Defined Wide Area Network), the TUN device is a virtual network device that allows routing packets from one physical network interface to another. It is typically used to create secure tunnels for encryption, isolation, and routing purposes. Here's how TUN devices work in SD-WAN:

1. **Virtual TUN Device Creation**: SD-WAN controllers or devices create a virtual TUN/TAP (Tunnel) device in the operating system kernel. TUN represents a network layer (Layer 3) device, while TAP represents a data link layer (Layer 2) device.

2. **Route Rule Configuration**: SD-WAN systems configure a set of route rules that define how packets are routed between physical interfaces and TUN devices. These rules may be based on destination IP addresses, subnets, ports, and other conditions.

3. **Tunnel Encryption**: When packets are sent to the TUN device, they may need to be encrypted for secure transmission. This typically involves using encryption protocols (e.g., IPsec) to encrypt packets as secure data within the tunnel.

4. **TUN Device Forwarding**: Once packets are written to the TUN device, they are transferred to the virtual network stack in the SD-WAN device's kernel. From there, they can be further processed based on route rules.

5. **Routing and Policies**: The routing and policy engine in the SD-WAN device's kernel determines how to handle packets. This may include deciding whether to route packets to other network interfaces, perform acceleration, compression, splitting, or other network optimization operations.

6. **Packet Transmission**: Packets may be further routed to a physical interface to reach their final destination. This could be done via a WAN link or other network connection.

7. **Reverse Operations**: Upon receiving response packets from remote sites, the SD-WAN device writes the packets to the TUN device and transmits them back to the local site. This includes decrypting data (if it was encrypted) and routing through the local network stack.

In summary, TUN devices in SD-WAN act as virtual tunnel interfaces through which packets can be encrypted, routed, and transmitted, enabling core SD-WAN functionalities like secure connections, traffic management, and optimization. The use of virtualized network devices allows SD-WAN systems to manage and optimize network traffic more flexibly, providing improved performance and security.

## Component Dependencies

![Component Dependencies](doc/img/componentRel.png)

## Deployment

1.Deploying the Controller on a Public Cloud Host
```yaml
server:
  # Dashboard web address
  port: 8080
sdwan:
  controller:
    # Controller port (TCP)
    port: 8081
    # Define an address pool
    cidr: 10.1.0.0/20
    # ARP expiration time (seconds)
    sdArpTTL: 300
    # Database path
    dbPath: ${user.dir}/derby.db
  relay:
    # Relay port (UDP)
    port: 8082
    timeout: 30000
```

2.Deploying sdwan-node in a Data Center (Mesh Mode)
```yaml
sdwan:
  node:
    # Controller address
    controllerServer: 127.0.0.1:8081
    connectTimeout: 30000
    mtu: 1400
    # STUN server address (e.g., MiWiFi's)
    stunServer: stun.miwifi.com:3478
    # Relay server address
    relayServer: 127.0.0.1:8082
    # Configure the local IP for your machine, as there may be multiple network interfaces
    localIP: 192.168.1.2
```

3.Starting sdwan-node on Your Local Machine
```yaml
sdwan:
  node:
    # Controller address
    controllerServer: 127.0.0.1:8081
    connectTimeout: 30000
    mtu: 1400
    # STUN server address (e.g., MiWiFi's)
    stunServer: stun.miwifi.com:3478
    # Relay server address
    relayServer: 127.0.0.1:8082
    # Configure the local IP for your machine, as there may be multiple network interfaces
    localIP: 10.22.6.3
```
4.Defining Routes in the Dashboard

View online node information.

![node](doc/img/node.png)

Configure route information.

![route](doc/img/route.png)
