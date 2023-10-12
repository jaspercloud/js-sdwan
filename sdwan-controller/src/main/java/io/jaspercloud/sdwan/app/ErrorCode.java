package io.jaspercloud.sdwan.app;

public interface ErrorCode {

    int NotFoundNode = 1001;
    int IpNotInCidr = 1002;
    int NodeVipExist = 1003;
    int NodeMacExist = 1004;

    int NotFoundRoute = 2001;
    int CidrError = 2002;
    int MeshUsed = 2003;

    int Ipv4FormatError = 3001;
    int MacFormatError = 3002;
}
