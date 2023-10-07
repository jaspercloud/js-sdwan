package io.jasercloud.sdwan;

public enum StunFiltering {

    Blocked(1),
    Internet(2),
    EndpointIndependent(3),
    AddressAndPortDependent(4),
    AddressDependent(5);

    private int code;

    StunFiltering(int code) {
        this.code = code;
    }
}
