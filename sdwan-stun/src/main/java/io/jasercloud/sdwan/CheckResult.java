package io.jasercloud.sdwan;

import lombok.Data;

import java.net.InetSocketAddress;

@Data
public class CheckResult {

    private StunMapping mapping;
    private StunFiltering filtering;
    private InetSocketAddress mappingAddress;

    public CheckResult(StunMapping mapping, StunFiltering filtering, InetSocketAddress mappingAddress) {
        this.mapping = mapping;
        this.filtering = filtering;
        this.mappingAddress = mappingAddress;
    }
}