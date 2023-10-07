//package io.jaspercloud.sdwan;
//
//import com.sun.jna.*;
//import com.sun.jna.platform.win32.IPHlpAPI;
//import com.sun.jna.platform.win32.WinDef;
//import com.sun.jna.ptr.LongByReference;
//
//import java.net.NetworkInterface;
//import java.util.ArrayList;
//import java.util.Enumeration;
//import java.util.List;
//
//public class WMITest {
//
//    public interface IPHlpAPIEx extends Library {
//
//        IPHlpAPIEx INSTANCE = Native.load("iphlpapi", IPHlpAPIEx.class);
//
//        int NO_ERROR = 0;
//        int ERROR_INSUFFICIENT_BUFFER = 122;
//        int ERROR_BUFFER_OVERFLOW = 111;
//
//        int GetIfTable(Pointer pIfTable, LongByReference pdwSize, boolean bOrder);
//
//        int GetAdaptersInfo(Pointer adapterInfo, LongByReference sizePointer);
//    }
//
//    @Structure.FieldOrder({"Next", "ComboIndex", "AdapterName", "Description", "Index"})
//    public static class PIP_ADAPTER_INFO extends Structure {
//
//        public static final int MAX_ADAPTER_NAME_LENGTH = 256;
//        public static final int MAX_ADAPTER_DESCRIPTION_LENGTH = 128;
//
//        public PIP_ADAPTER_INFO.ByReference Next;
//        public WinDef.DWORD ComboIndex;
//        public char[] AdapterName = new char[MAX_ADAPTER_NAME_LENGTH + 4];
//        public char[] Description = new char[MAX_ADAPTER_DESCRIPTION_LENGTH + 4];
//        public int Index;
//
//        public PIP_ADAPTER_INFO() {
//        }
//
//        public PIP_ADAPTER_INFO(Pointer p) {
//            super(p);
//            read();
//        }
//
//        public static class ByReference extends PIP_ADAPTER_INFO implements Structure.ByReference {
//        }
//    }
//
//    @Structure.FieldOrder({"dwNumEntries", "table"})
//    public static class MIB_IFTABLE extends Structure {
//
//        public int dwNumEntries;
//        public IPHlpAPI.MIB_IFROW[] table;
//
//        public MIB_IFTABLE() {
//        }
//
//        public MIB_IFTABLE(Pointer p, int size) {
//            super(p);
//            table = new IPHlpAPI.MIB_IFROW[size];
//            read();
//        }
//    }
//
//    public static void main(String[] args) throws Exception {
//        {
//            LongByReference pdwSize = new LongByReference();
//            int ret = IPHlpAPIEx.INSTANCE.GetIfTable(null, pdwSize, false);
//            Memory iftablePointer = new Memory(pdwSize.getValue());
//            if (IPHlpAPIEx.ERROR_INSUFFICIENT_BUFFER == ret) {
//                ret = IPHlpAPIEx.INSTANCE.GetIfTable(iftablePointer, pdwSize, false);
//            }
//            if (IPHlpAPIEx.NO_ERROR != ret) {
//                return;
//            }
//            int size = iftablePointer.getInt(0);
//            MIB_IFTABLE iftable = new MIB_IFTABLE(iftablePointer, size);
//            for (IPHlpAPI.MIB_IFROW item : iftable.table) {
//                System.out.println(String.format("%s, %s, %s",
//                        new String(item.bDescr, 0, item.dwDescrLen), item.dwIndex, item.dwMtu));
//            }
//        }
//        {
////            LongByReference sizePointer = new LongByReference();
////            int ret = IPHlpAPIEx.INSTANCE.GetAdaptersInfo(null, sizePointer);
////            Memory adapterInfoMalloc = new Memory(sizePointer.getValue());
////            if (IPHlpAPIEx.ERROR_BUFFER_OVERFLOW == ret) {
////                ret = IPHlpAPIEx.INSTANCE.GetAdaptersInfo(adapterInfoMalloc, sizePointer);
////            }
////            if (IPHlpAPIEx.NO_ERROR != ret) {
////                return;
////            }
////            PIP_ADAPTER_INFO adapterInfo = new PIP_ADAPTER_INFO(adapterInfoMalloc);
////            System.out.println();
//            List<NetworkInterface> interfaceList = new ArrayList<>();
//            Enumeration<NetworkInterface> enumeration = NetworkInterface.getNetworkInterfaces();
//            while (enumeration.hasMoreElements()) {
//                NetworkInterface networkInterface = enumeration.nextElement();
//                if (networkInterface.isUp()) {
//                    interfaceList.add(networkInterface);
//                }
//            }
//            System.out.println();
//        }
//    }
//}
