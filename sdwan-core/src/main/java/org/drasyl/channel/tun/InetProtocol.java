/*
 * Copyright (c) 2021-2022 Heiko Bornholdt and Kevin Röbert
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE
 * OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.drasyl.channel.tun;

import java.util.HashMap;
import java.util.Map;

/**
 * Assigned Internet Protocol Numbers.
 */
public enum InetProtocol {
    // https://www.iana.org/assignments/protocol-numbers/protocol-numbers.xhtml
    HOPOPT(0, "HOPOPT"),
    ICMP(1, "ICMP"),
    IGMP(2, "IGMP"),
    GGP(3, "GGP"),
    IPV4(4, "IPv4"),
    ST(5, "ST"),
    TCP(6, "TCP"),
    CBT(7, "CBT"),
    EGP(8, "EGP"),
    IGP(9, "IGP"),
    BBN_RCC_MON(10, "BBN-RCC-MON"),
    NVP_II(11, "NVP-II"),
    PUP(12, "PUP"),
    ARGUS(13, "ARGUS"),
    EMCON(14, "EMCON"),
    XNET(15, "XNET"),
    CHAOS(16, "CHAOS"),
    UDP(17, "UDP"),
    MUX(18, "MUX"),
    DCN_MEAS(19, "DCN-MEAS"),
    HMP(20, "HMP"),
    PRM(21, "PRM"),
    XNS_IDP(22, "XNS-IDP"),
    TRUNK_1(23, "TRUNK-1"),
    TRUNK_2(24, "TRUNK-2"),
    LEAF_1(25, "LEAF-1"),
    LEAF_2(26, "LEAF-2"),
    RDP(27, "RDP"),
    IRTP(28, "IRTP"),
    ISO_TP4(29, "ISO-TP4"),
    NETBLT(30, "NETBLT"),
    MFE_NSP(31, "MFE-NSP"),
    MERIT_INP(32, "MERIT-INP"),
    DCCP(33, "DCCP"),
    PC(34, "3PC"),
    IDPR(35, "IDPR"),
    XTP(36, "XTP"),
    DDP(37, "DDP"),
    IDPR_CMTP(38, "IDPR-CMTP"),
    TPPLUSPLUS(39, "TP++"),
    IL(40, "IL"),
    IPV6(41, "IPv6"),
    SDRP(42, "SDRP"),
    IPV6_ROUTE(43, "IPv6-Route"),
    IPV6_FRAG(44, "IPv6-Frag"),
    IDRP(45, "IDRP"),
    RSVP(46, "RSVP"),
    GRE(47, "GRE"),
    DSR(48, "DSR"),
    BNA(49, "BNA"),
    ESP(50, "ESP"),
    AH(51, "AH"),
    I_NLSP(52, "I-NLSP"),
    SWIPE(53, "SWIPE"),
    NARP(54, "NARP"),
    MOBILE(55, "MOBILE"),
    TLSP(56, "TLSP"),
    SKIP(57, "SKIP"),
    IPV6_ICMP(58, "IPv6-ICMP"),
    IPV6_NONXT(59, "IPv6-NoNxt"),
    IPV6_OPTS(60, "IPv6-Opts"),
    ANY_HI(61, "any host internal protocol"),
    CFTP(62, "CFTP"),
    ANY_LN(63, "any local network"),
    SAT_EXPAK(64, "SAT-EXPAK"),
    KRYPTOLAN(65, "KRYPTOLAN"),
    RVD(66, "RVD"),
    IPPC(67, "IPPC"),
    ANY_DFS(68, "any distributed file system"),
    SAT_MON(69, "SAT-MON"),
    VISA(70, "VISA"),
    IPCV(71, "IPCV"),
    CPNX(72, "CPNX"),
    CPHB(73, "CPHB"),
    WSN(74, "WSN"),
    PVP(75, "PVP"),
    BR_SAT_MON(76, "BR-SAT-MON"),
    SUN_ND(77, "SUN-ND"),
    WB_MON(78, "WB-MON"),
    WB_EXPAK(79, "WB-EXPAK"),
    ISO_IP(80, "ISO-IP"),
    VMTP(81, "VMTP"),
    SECURE_VMTP(82, "SECURE-VMTP"),
    VINES(83, "VINES"),
    TTP(84, "TTP"),
    IPTM(84, "IPTM"),
    NSFNET_IGP(85, "NSFNET-IGP"),
    DGP(86, "DGP"),
    TCF(87, "TCF"),
    EIGRP(88, "EIGRP"),
    OSPFIGP(89, "OSPFIGP"),
    SPRITE_RPC(90, "Sprite-RPC"),
    LARP(91, "LARP"),
    MTP(92, "MTP"),
    AX_25(93, "AX.25"),
    IPIP(94, "IPIP"),
    MICP(95, "MICP"),
    SCC_SP(96, "SCC-SP"),
    ETHERIP(97, "ETHERIP"),
    ENCAP(98, "ENCAP"),
    ANY_PES(99, "any private encryption scheme"),
    GMTP(100, "GMTP"),
    IFMP(101, "IFMP"),
    PNNI(102, "PNNI"),
    PIM(103, "PIM"),
    ARIS(104, "ARIS"),
    SCPS(105, "SCPS"),
    QNX(106, "QNX"),
    A_N(107, "A/N"),
    IPCOMP(108, "IPComp"),
    SNP(109, "SNP"),
    COMPAQ_PEER(110, "Compaq-Peer"),
    IPX_IN_IP(111, "IPX-in-IP"),
    VRRP(112, "VRRP"),
    PGM(113, "PGM"),
    ANY_0HOP(114, "any 0-hop protocol"),
    L2TP(115, "L2TP"),
    DDX(116, "DDX"),
    IATP(117, "IATP"),
    STP(118, "STP"),
    SRP(119, "SRP"),
    UTI(120, "UTI"),
    SMP(121, "SMP"),
    SM(122, "SM"),
    PTP(123, "PTP"),
    ISIS_OVER_IPV4(124, "ISIS over IPv4"),
    FIRE(125, "FIRE"),
    CRTP(126, "CRTP"),
    CRUDP(127, "CRUDP"),
    SSCOPMCE(128, "SSCOPMCE"),
    IPLT(129, "IPLT"),
    SPS(130, "SPS"),
    PIPE(131, "PIPE"),
    SCTP(132, "SCTP"),
    FC(133, "FC"),
    RSVP_E2E_IGNORE(134, "RSVP-E2E-IGNORE"),
    MOBILITY_HEADER(135, "Mobility Header"),
    UDPLITE(136, "UDPLite"),
    MPLS_IN_IP(137, "MPLS-in-IP"),
    MANET(138, "manet"),
    HIP(139, "HIP"),
    SHIM6(140, "Shim6"),
    WESP(141, "WESP"),
    ROHC(142, "ROHC"),
    ETHERNET(143, "Ethernet"),
    RESERVED(255, "Reserved"),
    ;
    public static final Map<Integer, InetProtocol> PROTOCOL_BY_DECIMAL;

    static {
        final Map<Integer, InetProtocol> protocols = new HashMap<>();
        for (InetProtocol protocol : InetProtocol.values()) {
            protocols.put(protocol.decimal, protocol);
        }
        PROTOCOL_BY_DECIMAL = protocols;
    }

    public final int decimal;
    public final String protocol;

    InetProtocol(final int decimal, final String protocol) {
        this.decimal = decimal;
        this.protocol = protocol;
    }

    public static String protocolByDecimal(int decimal) {
        if (decimal < 0 || decimal > 255) {
            throw new IllegalArgumentException("decimal must be within 0 and 255.");
        }
        final InetProtocol protocol = PROTOCOL_BY_DECIMAL.get(decimal);
        if (protocol != null) {
            return protocol.protocol;
        } else {
            return null;
        }
    }
}
