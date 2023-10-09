package io.jasercloud.sdwan;

import lombok.Data;

import java.net.InetSocketAddress;

@Data
public class CheckResult {

    private int localPort;
    private String mapping;
    private String filtering;
    private InetSocketAddress mappingAddress;

    public CheckResult(int localPort, String mapping, String filtering, InetSocketAddress mappingAddress) {
        this.localPort = localPort;
        this.mapping = mapping;
        this.filtering = filtering;
        this.mappingAddress = mappingAddress;
    }
}