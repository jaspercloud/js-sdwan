package io.jaspercloud.sdwan.domian;

import io.jaspercloud.sdwan.Cidr;
import io.jaspercloud.sdwan.app.ErrorCode;
import io.jaspercloud.sdwan.exception.ProcessCodeException;
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
    private String vip;
    private String macAddress;
    private String remark;

    private NodeType nodeType;
    private String stunMapping;
    private String stunFiltering;
    private InetSocketAddress internalAddress;
    private InetSocketAddress publicAddress;
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
