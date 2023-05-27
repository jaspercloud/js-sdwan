/*
 * Copyright (c) 2021 Heiko Bornholdt and Kevin Röbert
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
package org.drasyl.channel.tun.jna.shared;

import com.sun.jna.*;
import com.sun.jna.ptr.IntByReference;

import java.nio.ByteBuffer;

/**
 * Java Native Access for platform's C runtime library.
 */
public final class LibC {
    static {
        Native.register(Platform.C_LIBRARY_NAME);
    }

    private LibC() {
        // jna class
    }

    // https://www.freebsd.org/cgi/man.cgi?query=open&sektion=2
    public static native int open(final String path, final int flags) throws LastErrorException;

    // https://www.freebsd.org/cgi/man.cgi?query=close&sektion=2
    public static native int close(final int fd) throws LastErrorException;

    // https://www.freebsd.org/cgi/man.cgi?query=read&sektion=2
    public static native int read(final int fd,
                                  final byte[] buf,
                                  final NativeLong nbytes) throws LastErrorException;

    // https://www.freebsd.org/cgi/man.cgi?query=read&sektion=2
    public static native int read(final int fd,
                                  final ByteBuffer buf,
                                  final NativeLong nbytes) throws LastErrorException;

    // https://www.freebsd.org/cgi/man.cgi?query=write&sektion=2
    public static native int write(final int fd,
                                   final byte[] buf,
                                   final NativeLong nbytes) throws LastErrorException;

    // https://www.freebsd.org/cgi/man.cgi?query=write&sektion=2
    public static native int write(final int fd,
                                   final ByteBuffer buf,
                                   final NativeLong nbytes) throws LastErrorException;

    /**
     * socket() creates an endpoint for communication and returns a descriptor.
     * <p>
     * The domain parameter specifies a communications domain within which communication will take
     * place; this selects the protocol family which should be used.  These families are defined in
     * the include file <sys/socket.h>.  The currently understood formats are
     * <pre>
     * PF_LOCAL        Host-internal protocols, formerly called PF_UNIX,
     * PF_UNIX          Host-internal protocols, deprecated, use PF_LOCAL,
     * PF_INET         Internet version 4 protocols,
     * PF_ROUTE        Internal Routing protocol,
     * PF_KEY          Internal key-management function,
     * PF_INET6        Internet version 6 protocols,
     * PF_SYSTEM       System domain,
     * PF_NDRV         Raw access to network device,
     * PF_VSOCK        VM Sockets protocols
     * </pre>
     * <p>
     * The socket has the indicated type, which specifies the semantics of communication.  Currently
     * defined types are:
     * <pre>
     * SOCK_STREAM
     * SOCK_DGRAM
     * SOCK_RAW
     * </pre>
     * <p>
     * A SOCK_STREAM type provides sequenced, reliable, two-way connection based byte streams.  An
     * out-of-band data transmission mechanism may be supported.  A SOCK_DGRAM socket supports
     * datagrams (connectionless, unreliable messages of a fixed (typically small) maximum length).
     * SOCK_RAW sockets provide access to internal network protocols and interfaces.  The type
     * SOCK_RAW, which is available only to the super-user.
     * <p>
     * The protocol specifies a particular protocol to be used with the socket.  Normally only a
     * single protocol exists to support a particular socket type within a given protocol family.
     * However, it is possible that many protocols may exist, in which case a particular protocol
     * must be specified in this manner.  The protocol number to use is particular to the
     * communication domain in which communication is to take place; see protocols(5).
     * <p>
     * Sockets of type SOCK_STREAM are full-duplex byte streams, similar to pipes.  A stream socket
     * must be in a connected state before any data may be sent or received on it.  A connection to
     * another socket is created with a connect(2) or connectx(2) call.  Once connected, data may be
     * transferred using read(2) and write(2) calls or some variant of the send(2) and recv(2)
     * calls.  When a session has been completed a close(2) may be performed.  Out-of-band data may
     * also be transmitted as described in send(2) and received as described in recv(2).
     * <p>
     * The communications protocols used to implement a SOCK_STREAM insure that data is not lost or
     * duplicated.  If a piece of data for which the peer protocol has buffer space cannot be
     * successfully transmitted within a reasonable length of time, then the connection is
     * considered broken and calls will indicate an error with -1 returns and with ETIMEDOUT as the
     * specific code in the global variable errno.  The protocols option- ally keep sockets ``warm''
     * by forcing transmissions roughly every minute in the absence of other activity.  An error is
     * then indicated if no response can be elicited on an otherwise idle connection for a extended
     * period (e.g. 5 minutes).  A SIGPIPE signal is raised if a process sends on a broken stream;
     * this causes naive processes, which do not handle the signal, to exit.
     * <p>
     * SOCK_DGRAM and SOCK_RAW sockets allow sending of datagrams to correspondents named in send(2)
     * calls.  Datagrams are generally received with recvfrom(2), which returns the next datagram
     * with its return address.
     * <p>
     * An fcntl(2) call can be used to specify a process group to receive a SIGURG signal when the
     * out-of-band data arrives.  It may also enable non-blocking I/O and asynchronous notification
     * of I/O events via SIGIO.
     * <p>
     * The operation of sockets is controlled by socket level options.  These options are defined in
     * the file <sys/socket.h>.  Setsockopt(2) and getsockopt(2) are used to set and get options,
     * respectively.
     */
    public static native int socket(final int domain,
                                    final int type,
                                    final int protocol) throws LastErrorException;

    /**
     * The parameter socket is a socket.  If it is of type SOCK_DGRAM, this call specifies the peer
     * with which the socket is to be associated; this address is that to which datagrams are to be
     * sent, and the only address from which datagrams are to be received.  If the socket is of type
     * SOCK_STREAM, this call attempts to make a connection to another socket.  The other socket is
     * specified by address, which is an address in the communications space of the socket.
     * <p>
     * Each communications space interprets the address parameter in its own way.  Generally, stream
     * sockets may successfully connect() only once; datagram sockets may use connect() multiple
     * times to change their association.  Datagram sockets may dissolve the association by calling
     * disconnectx(2), or by connecting to an invalid address, such as a null address or an address
     * with the address family set to AF_UNSPEC (the error EAFNOSUPPORT will be harmlessly
     * returned).
     */
    public static native int connect(final int socket,
                                     final Structure address,
                                     final int address_len) throws LastErrorException;

    /**
     * getsockopt() and setsockopt() manipulate the options associated with a socket.  Options may
     * exist at multiple protocol levels; they are always present at the uppermost ``socket''
     * level.
     * <p>
     * When manipulating socket options the level at which the option resides and the name of the
     * option must be specified.  To manipulate options at the socket level, level is specified as
     * SOL_SOCKET.  To manipulate options at any other level the protocol number of the appropriate
     * protocol controlling the option is supplied.  For example, to indicate that an option is to
     * be interpreted by the TCP protocol, level should be set to the protocol number of TCP; see
     * getprotoent(3).
     * <p>
     * The parameters option_value and option_len are used to access option values for setsockopt().
     *  For getsockopt() they identify a buffer in which the value for the requested option(s) are
     * to be returned.  For getsockopt(), option_len is a value-result parameter, initially
     * containing the size of the buffer pointed to by option_value, and modified on return to
     * indicate the actual size of the value returned.  If no option value is to be supplied or
     * returned, option_value may be NULL.
     * <p>
     * option_name and any specified options are passed uninterpreted to the appropriate protocol
     * module for interpretation.  The include file <sys/socket.h> contains definitions for socket
     * level options, described below.  Options at other protocol levels vary in format and name;
     * consult the appropriate entries in section 4 of the manual.
     * <p>
     * Most socket-level options utilize an int parameter for option_value.  For setsockopt(), the
     * parameter should be non-zero to enable a boolean option, or zero if the option is to be
     * disabled.  SO_LINGER uses a struct linger parameter, defined in <sys/socket.h>, which
     * specifies the desired state of the option and the linger interval (see below).  SO_SNDTIMEO
     * and SO_RCVTIMEO use a struct timeval parameter, defined in <sys/time.h>.
     * <p>
     * The following options are recognized at the socket level.  Except as noted, each may be
     * examined with getsockopt() and set with setsockopt().
     * <pre>
     * SO_DEBUG        enables recording of debugging information
     * SO_REUSEADDR    enables local address reuse
     * SO_REUSEPORT    enables duplicate address and port bindings
     * SO_KEEPALIVE    enables keep connections alive
     * SO_DONTROUTE    enables routing bypass for outgoing messages
     * SO_LINGER       linger on close if data present
     * SO_BROADCAST    enables permission to transmit broadcast messages
     * SO_OOBINLINE    enables reception of out-of-band data in band
     * SO_SNDBUF       set buffer size for output
     * SO_RCVBUF       set buffer size for input
     * SO_SNDLOWAT     set minimum count for output
     * SO_RCVLOWAT     set minimum count for input
     * SO_SNDTIMEO     set timeout value for output
     * SO_RCVTIMEO     set timeout value for input
     * SO_TYPE         get the type of the socket (get only)
     * SO_ERROR        get and clear error on the socket (get only)
     * SO_NOSIGPIPE    do not generate SIGPIPE, instead return EPIPE
     * SO_NREAD        number of bytes to be read (get only)
     * SO_NWRITE       number of bytes written not yet sent by the protocol (get only)
     * SO_LINGER_SEC   linger on close if data present with timeout in seconds
     * </pre>
     * <p>
     * SO_DEBUG enables debugging in the underlying protocol modules.
     * <p>
     * SO_REUSEADDR indicates that the rules used in validating addresses supplied in a bind(2) call
     * should allow reuse of local addresses.
     * <p>
     * SO_REUSEPORT allows completely duplicate bindings by multiple processes if they all set
     * SO_REUSEPORT before binding the port.  This option permits multiple instances of a program to
     * each receive UDP/IP multicast or broadcast datagrams destined for the bound port.
     * <p>
     * SO_KEEPALIVE enables the periodic transmission of messages on a connected socket.  Should the
     * connected party fail to respond to these messages, the connection is considered broken and
     * processes using the socket are notified via a SIGPIPE signal when attempting to send data.
     * <p>
     * SO_DONTROUTE indicates that outgoing messages should bypass the standard routing facilities.
     * Instead, messages are directed to the appropriate network interface according to the network
     * portion of the destina- tion address.
     * <p>
     * SO_LINGER controls the action taken when unsent messages are queued on socket and a close(2)
     * is performed.  If the socket promises reliable delivery of data and SO_LINGER is set, the
     * system will block the process on the close attempt until it is able to transmit the data or
     * until it decides it is unable to deliver the information (a timeout period, termed the linger
     * interval, is specified in the setsockopt() call when SO_LINGER is requested).  If SO_LINGER
     * is disabled and a close is issued, the system will process the close in a manner that allows
     * the process to continue as quickly as possible.
     * <p>
     * SO_LINGER_SEC is the same option as SO_LINGER except the linger time is in seconds for
     * SO_LINGER_SEC.
     * <p>
     * The option SO_BROADCAST requests permission to send broadcast datagrams on the socket.
     * Broadcast was a privileged operation in earlier versions of the system.
     * <p>
     * With protocols that support out-of-band data, the SO_OOBINLINE option requests that
     * out-of-band data be placed in the normal data input queue as received; it will then be
     * accessible with recv or read calls with- out the MSG_OOB flag.  Some protocols always behave
     * as if this option is set.
     * <p>
     * SO_SNDBUF and SO_RCVBUF are options to adjust the normal buffer sizes allocated for output
     * and input buffers, respectively.  The buffer size may be increased for high-volume
     * connections, or may be decreased to limit the possible backlog of incoming data.  The system
     * places an absolute limit on these values.
     * <p>
     * SO_SNDLOWAT is an option to set the minimum count for output operations.  Most output
     * operations process all of the data supplied by the call, delivering data to the protocol for
     * transmission and blocking as necessary for flow control.  Nonblocking output operations will
     * process as much data as permitted (subject to flow control) without blocking, but will
     * process no data if flow control does not allow the smaller of the low-water mark value or the
     * entire request to be processed.  A select(2) operation testing the ability to write to a
     * socket will return true only if the low-water mark amount could be processed.  The default
     * value for SO_SNDLOWAT is set to a convenient size for network efficiency, often 2048.
     * <p>
     * SO_RCVLOWAT is an option to set the minimum count for input operations.  In general, receive
     * calls will block until any (non-zero) amount of data is received, then return with the
     * smaller of the amount available or the amount requested.  The default value for SO_RCVLOWAT
     * is 1.  If SO_RCVLOWAT is set to a larger value, blocking receive calls normally wait until
     * they have received the smaller of the low-water mark value or the requested amount.  Receive
     * calls may still return less than the low-water mark if an error occurs, a signal is caught,
     * or the type of data next in the receive queue is different than that returned.
     * <p>
     * SO_SNDTIMEO is an option to set a timeout value for output operations.  It accepts a struct
     * timeval parameter with the number of seconds and microseconds used to limit waits for output
     * operations to complete. If a send operation has blocked for this much time, it returns with a
     * partial count or with the error EWOULDBLOCK if no data were sent.  In the current
     * implementation, this timer is restarted each time addi- tional data are delivered to the
     * protocol, implying that the limit applies to output portions ranging in size from the
     * low-water mark to the high-water mark for output.
     * <p>
     * SO_RCVTIMEO is an option to set a timeout value for input operations.  It accepts a struct
     * timeval parameter with the number of seconds and microseconds used to limit waits for input
     * operations to complete.  In the current implementation, this timer is restarted each time
     * additional data are received by the protocol, and thus the limit is in effect an inactivity
     * timer.  If a receive operation has been blocked for this much time without receiving
     * additional data, it returns with a short count or with the error EWOULDBLOCK if no data were
     * received.  The struct timeval parameter must represent a positive time interval; other- wise,
     * setsockopt() returns with the error EDOM.
     * <p>
     * SO_NOSIGPIPE is an option that prevents SIGPIPE from being raised when a write fails on a
     * socket to which there is no reader; instead, the write to the socket returns with the error
     * EPIPE when there is no reader.
     * <p>
     * Finally, SO_TYPE, SO_ERROR, SO_NREAD, and SO_NWRITE are options used only with getsockopt().
     * <p>
     * SO_TYPE returns the type of the socket, such as SOCK_STREAM; it is useful for servers that
     * inherit sockets on startup.
     * <p>
     * SO_ERROR returns any pending error on the socket and clears the error status.  It may be used
     * to check for asynchronous errors on connected datagram sockets or for other asynchronous
     * errors.
     * <p>
     * SO_NREAD returns the amount of data in the input buffer that is available to be received.
     * For datagram oriented sockets, SO_NREAD returns the size of the first packet -- this differs
     * from the ioctl() command FIONREAD that returns the total amount of data available.
     * <p>
     * SO_NWRITE returns the amount of data in the output buffer not yet sent by the protocol.
     */
    @SuppressWarnings("java:S117")
    public static native int getsockopt(final int socket,
                                        final int level,
                                        final int option_name,
                                        final Structure option_value,
                                        final IntByReference option_len) throws LastErrorException;

    /**
     * The ioctl() function manipulates the underlying device parameters of special files.  In
     * particular, many operating characteristics of character special files (e.g. terminals) may be
     * controlled with ioctl() requests.  The argument fildes must be an open file descriptor.
     * <p>
     * An  ioctl request has encoded in it whether the argument is an ``in'' parameter or ``out''
     * parameter, and the size of the argument argp in bytes.  Macros and defines used in specifying
     * an ioctl request are located in the file <sys/ioctl.h>.
     */
    public static native int ioctl(final int fildes,
                                   final NativeLong request,
                                   final Structure argp) throws LastErrorException;
}
