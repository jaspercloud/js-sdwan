package io.jasercloud.sdwan.tun;

import io.jaspercloud.sdwan.NetworkInterfaceInfo;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

public abstract class TunDevice {

    private String name;
    private String type;
    private String guid;
    private boolean active;

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getGuid() {
        return guid;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public TunDevice(String name, String type, String guid) {
        this.name = name;
        this.type = type;
        this.guid = guid;
    }

    public abstract void open() throws Exception;

    public abstract void close();

    public abstract boolean isClosed();

    public abstract int getVersion();

    public abstract void setIP(String addr, int netmaskPrefix) throws Exception;

    public abstract void setMTU(int mtu) throws Exception;

    public abstract ByteBuf readPacket(ByteBufAllocator alloc);

    public abstract void writePacket(ByteBufAllocator alloc, ByteBuf msg);

    public abstract void addRoute(NetworkInterfaceInfo interfaceInfo, String route, String ip) throws Exception;
}
