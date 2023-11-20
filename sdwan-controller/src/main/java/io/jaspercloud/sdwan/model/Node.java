package io.jaspercloud.sdwan.model;

import io.jaspercloud.sdwan.Cidr;
import io.jaspercloud.sdwan.exception.ProcessCodeException;
import io.jaspercloud.sdwan.support.ErrorCode;
import io.jaspercloud.sdwan.support.MacAddressUtil;
import io.jaspercloud.sdwan.support.NodeType;
import lombok.Data;
import sun.net.util.IPAddressUtil;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

@Data
public class Node {

    private Long id;
    private NodeType nodeType;
    private String vip;
    private String macAddress;
    private String remark;

    private String mappingType;
    private InetSocketAddress internalAddress;
    private InetSocketAddress publicAddress;
    private String relayToken;
    private List<Cidr> routeList = new ArrayList<>();

    public void setVip(String vip) {
        if (!IPAddressUtil.isIPv4LiteralAddress(vip)) {
            throw new ProcessCodeException(ErrorCode.Ipv4FormatError);
        }
        this.vip = vip;
    }

    public void setMacAddress(String macAddress) {
        if (!MacAddressUtil.check(macAddress)) {
            throw new ProcessCodeException(ErrorCode.MacFormatError);
        }
        this.macAddress = macAddress;
    }
}
