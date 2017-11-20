package smsqmulator;

/**
 * This class just holds some constants for IP comms and data structures.
 * 
 * @author Wolfgang Lenerz copyright (c) 2016
 * @version 0.00
 */
public class IPTypes
{
    /**
     * Connection status.
     */
    public static enum CONN_STATUS {NOT_CONNECTED, CONNECTING, CONNECTED, LISTENING};
    
    /**
     * Type of socket
     */
    public static final int TYPE_SCK=0x53434b5f;                // "SCK_"
    public static final int TYPE_TCP=0x5443505f;                // "TCP_"
    public static final int TYPE_UDP=0x5544505f;                // "UDP_"

}
