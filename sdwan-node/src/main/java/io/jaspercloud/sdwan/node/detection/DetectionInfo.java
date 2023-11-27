package io.jaspercloud.sdwan.node.detection;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class DetectionInfo {

    private String srcAddress;
    private String dstAddress;

    public DetectionInfo(String srcAddress, String dstAddress) {
        this.srcAddress = srcAddress;
        this.dstAddress = dstAddress;
    }
}
