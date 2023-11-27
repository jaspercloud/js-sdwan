package io.jaspercloud.sdwan.domain.control.entity;

import io.jaspercloud.sdwan.exception.ProcessCodeException;
import io.jaspercloud.sdwan.infra.ErrorCode;
import io.jaspercloud.sdwan.infra.MacAddressUtil;
import io.jaspercloud.sdwan.domain.control.vo.NodeType;
import lombok.Data;
import sun.net.util.IPAddressUtil;

import java.util.List;

@Data
public class Node {

    private Long id;
    private NodeType nodeType;
    private String vip;
    private String macAddress;
    private String remark;

    private List<String> addressList;

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
