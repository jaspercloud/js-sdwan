package io.jasercloud.sdwan;

import lombok.Data;

import java.net.InetSocketAddress;

@Data
public class CheckResult {

    public static final String Blocked = "Blocked";
    public static final String Internet = "Internet";
    public static final String EndpointIndependent = "EndpointIndependent";
    public static final String AddressAndPortDependent = "AddressAndPortDependent";
    public static final String AddressDependent = "AddressDependent";

    private String mapping;
    private String filtering;
    private InetSocketAddress mappingAddress;

    public CheckResult(String mapping, String filtering, InetSocketAddress mappingAddress) {
        this.mapping = mapping;
        this.filtering = filtering;
        this.mappingAddress = mappingAddress;
    }
}