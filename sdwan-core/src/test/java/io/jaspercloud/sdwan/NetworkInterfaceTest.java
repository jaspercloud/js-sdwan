package io.jaspercloud.sdwan;

import org.junit.jupiter.api.Test;
import org.springframework.util.Assert;

public class NetworkInterfaceTest {

    @Test
    public void test() throws Exception {
        NetworkInterfaceInfo networkInterfaceInfo = NetworkInterfaceUtil.findNetworkInterfaceInfo("192.222.0.66");
        Assert.isTrue(null != networkInterfaceInfo);
    }
}
