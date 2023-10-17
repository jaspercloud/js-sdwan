package io.jaspercloud.sdwan.app;

public interface ErrorCode {

    int SysError = 500;

    int NotFoundNode = 1001;
    int NodeIsNotMesh = 1002;
    int VipNotInCidr = 1003;

    int NotFoundRoute = 2001;
    int CidrError = 2002;
    int MeshUsed = 2003;

    int Ipv4FormatError = 3001;
    int MacFormatError = 3002;
}
