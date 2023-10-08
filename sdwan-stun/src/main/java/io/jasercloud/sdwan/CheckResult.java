package io.jasercloud.sdwan;

import lombok.Data;

import java.net.InetSocketAddress;

@Data
public class CheckResult {

    private String mapping;
    private String filtering;
    private InetSocketAddress mappingAddress;

    public CheckResult(String mapping, String filtering, InetSocketAddress mappingAddress) {
        this.mapping = mapping;
        this.filtering = filtering;
        this.mappingAddress = mappingAddress;
    }
}