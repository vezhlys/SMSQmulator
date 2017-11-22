package smsqmulator;

/**
 * This handles TCP connections.
 * This is more complicated than Marcel's C code, since Java has three different objects for socket:
 * Socket (client side TCP socket), ServerSocket and DatagramSocket (UDP),and they are not interchangeable.
 * 
 *
 * This class is not thread safe.
 * Only 50 connections can be open at once.
 * 
 * @author and copyright (c) wolfgang lenerz 2015-2016
 * 
 * @version 
 * 1.02 handleTrap added case 7.
 * 1.01 interface change for get_netname (case 6), needs at least 3.31.
 * 1.00 implements only SCK and TCP, trap3, 1-7,50,51,53,58,59,5b,5e,62,7b,7c. No UDP.
 * 0.00 initial version, just with trap#3;1-7.
 * 
 */
public class IPHandler
{
    private final java.util.HashMap<Integer,IPSocket> socketMap;
    private static final int    MAX_SOCKETS=50;
    private static final byte   LF=10;
    
    /**
     * These are the I/O codes I'm supposed to handle.
     */
    private static final int IOB_TEST   = 0x00;
    private static final int IOB_FBYT   = 0x01;
    private static final int IOB_FLIN   = 0x02;
    private static final int IOB_FMUL   = 0x03;
    private static final int IOB_SBYT   = 0x05;    
    private static final int IOB_SMUL   = 0x07;
    private static final int IOF_LOAD   = 0x48;
    private static final int IOF_SAVE   = 0x49;

    private static final int IP_LISTEN  = 0x50;
    private static final int IP_SEND    = 0x51;
 //   private static final int IP_SENDTO  = 0x52;
    private static final int IP_RECV    = 0x53;
    private static final int IP_RECVFM  = 0x54;
//    private static final int IP_GETOPT  = 0x55;
  //  private static final int IP_SETOPT  = 0x56;
 //   private static final int IP_SHUTDWN = 0x57;
    private static final int IP_BIND    = 0x58;
    private static final int IP_CONNECT = 0x59;
    private static final int IP_GETHOSTNAME = 0x5b;
  //  private static final int IP_GETSOCKNAME = 0x5c;
    private static final int IP_GETPEERNAME = 0x5d;
    private static final int IP_GETHOSTBYNAME= 0x5e;
  //  private static final int IP_GETHOSTBYADDR= 0x5f;
//    private static final int IP_SETHOSTENT       0x60
//    private static final int IP_ENDHOSTENT       0x61
    private static final int IP_H_ERRNO     = 0x62;
 /*
    private static final int IP_GETSERVBYNAME   = 0x64;
    private static final int IP_GETSERVBYPORT   = 0x65;
    private static final int IP_GETPROTOBYNAME  = 0x6e;
    private static final int IP_GETPROTOBYNUMBER= 0x6f;
    private static final int IP_INET_ATON   = 0x72;
    private static final int IP_INET_ADDR   = 0x73;
    private static final int IP_INET_NETWORK= 0x74;
    private static final int IP_INET_NTOA   = 0x75;
    private static final int IP_IOCTL       = 0x79;
    private static final int IP_GETDOMAIN   = 0x7a;
*/
    private static final int IP_H_STRERROR  = 0x7b;
    private static final int IP_ERRNO       = 0x7c;
   
    private int lastError;
    /**
     * Creates the object.
     */
    public IPHandler()
    {
        this.socketMap=new java.util.HashMap<Integer,IPSocket>(MAX_SOCKETS);
    }
    
    /**
     * This is a very primitive way to allocate keys.
     * @return a new key
     */
    private int getNewKey()
    {
        for (int i=1;i<MAX_SOCKETS;i++)
            if (!this.socketMap.containsKey(i))
                return i;
        return -1;
    }
    
    /**
     * This handles all the "trap" calls related to this device.
     * 
     * @param cpu  the cpu used.
     */
    public void handleTrap(smsqmulator.cpu.MC68000Cpu cpu)
    {
        int d0;
        IPSocket m=null;
                
        switch (cpu.data_regs[0])
        {
            case 1:                                             // open a socket or make an accept call
                m=new IPSocket(cpu,this.socketMap);             // try to open a socket
                if (cpu.data_regs[0]==0)
                {    
                    int key = getNewKey();
                    if (key==-1)
                    {
                        cpu.data_regs[0]=Types.ERR_IMEM;
                    }
                    else
                    {
                        this.socketMap.put(key, m);
                        cpu.data_regs[7]=-key;                  // put negative key into D7
                    }
                }
                break;
                
            case 2:                                             // do I/O
                d0=cpu.readMemoryLong(cpu.addr_regs[7]);        // get IO key
                cpu.addr_regs[7]+=4;                            // adjust stack
                m=this.socketMap.get(-cpu.addr_regs[0]);
                if (m==null)
                    cpu.data_regs[0]=Types.ERR_ICHN;
                else
                    handleIO (d0,cpu,m);
                break;
                
            case 3:                                             // close socket
                m=this.socketMap.get(-cpu.addr_regs[0]);
                if (m!=null)
                    m.close();
                this.socketMap.remove(-cpu.addr_regs[0]);
                cpu.data_regs[0]=0;
                break;
                
            case 4:                                             // close all sockets, used at startup/reset
                java.util.Set<Integer> set=this.socketMap.keySet();        
                for (Integer i: set)
                {
                   this.socketMap.get(i).close();
                }
                this.socketMap.clear();
                cpu.data_regs[0]=0;
                break;
                
            case 5:                                             // get channel name into (a1), d2 = max length
                m=this.socketMap.get(-cpu.addr_regs[0]);
                String v;
                if (m==null)
                    v="UNKNOWN SOCKET";
                else
                {
                    v=m.getName();
                }
                
                int bytes=cpu.data_regs[2]&0xffff;
                cpu.writeSmsqeString(cpu.addr_regs[1],v,true,bytes);//write as many bytes as possible
                if (v.length()> bytes)
                    cpu.data_regs[0]=Types.ERR_IPAR;            // but signal buffer too small          
                else
                    cpu.data_regs[0]=0;                         // or signal all OK
                break;
                
            case 6:
                String s=getHName();                            // name of machine
                if (s==null)
                    s="Unknown name!";
                int l=s.length();
                int bufflen=cpu.readMemoryWord(cpu.addr_regs[1]+2);
                if (l>bufflen)
                {
                    cpu.data_regs[0]=Types.ERR_BFFL;
                }
                else
                {   
                    cpu.writeSmsqeString(cpu.readMemoryLong(cpu.addr_regs[1]+4), s, true, l);//write string
                    cpu.data_regs[0]=0;
                }
                break;
                
            case 7:
                try
                {
                    /*java.net.InetAddress a = java.net.InetAddress.getLocalHost();
                    s=a.getHostAddress();
                    byte [] b = a.getAddress();
                    b=b;
                    */
                    s=getLocalAddress();
                }
                catch (Exception u)
                { 
                    s="";
                }
                l=s.length();
                bufflen=cpu.readMemoryWord(cpu.addr_regs[1]+2);
                if (l>bufflen)
                {
                    cpu.data_regs[0]=Types.ERR_BFFL;
                }
                else
                {   
                    cpu.writeSmsqeString(cpu.readMemoryLong(cpu.addr_regs[1]+4), s, true, l);//write string
                    cpu.data_regs[0]=0;
                }    
                break;
            }
        
        if (cpu.data_regs[0]==0)
        {
            cpu.reg_sr|=4;
            this.lastError=0;
        }    
        else
        {
            cpu.reg_sr&=~4;                                     // set status reg according to D0;
            if (m!=null)
                this.lastError=m.getLastError();
        }
    }
    
    /**
     * This handles I/O.
     * 
     * The only operations allowed are : get 1 char, 1 line ended by LF, multiple chars, send 1 byte and multiple bytes
     * 
     * @param what type of operation
     * @param cpu the cpu used
     * @param m the socket this operation is to use
     */
    private void handleIO(int what,smsqmulator.cpu.MC68000Cpu cpu,IPSocket m)
    {
        int bytes;
        cpu.data_regs[0]=0;                                     // preset no error
        switch (what)
        {
           case IOB_TEST:                                      // test pending input
                if (m.getSocketType()==IPTypes.TYPE_SCK)
                {
                    cpu.data_regs[0]=Types.ERR_EOF;             // simple unbound socket can't receive
                    break;
                }
                switch (m.getConnectionStatus())
                {
                    case NOT_CONNECTED:
                    case CONNECTED:
                    case LISTENING:                             // for these three input could be pending
                        try
                        {
                            if (m.rawIn.available()==0)
                                cpu.data_regs[0]=Types.ERR_NC;
                        }
                        catch (Exception e)
                        {
                            cpu.data_regs[0]=Types.ERR_NC;
                        }
                        break;
                        
                    default :
                        cpu.data_regs[0]=Types.ERR_NC;          // everything else nothing is pending.
                }
                break;
                
            case IOB_FBYT:                                      // get ONE char    
                byte[]byt=new byte[1];
                cpu.data_regs[0]= m.receive1Byte(cpu,byt); 
                if (cpu.data_regs[0]==0)
                {
                    bytes=byt[0]&0xff;
                    cpu.data_regs[1]=(cpu.data_regs[1]&0xffffff00)|bytes;// return the byte in D1
                }
                break;
                
            case IOB_FLIN:                                      // get multiple bytes ending by LF into buffer at (a1), d2.w = buffer size
                int D2 = cpu.data_regs[2]&0xffff;               // size of buffer
                int D1W=cpu.data_regs[1]&0xffff;
                if (D1W>=D2)                                    // if this happens, the buffer was filled w/o finding an LF
                {
                    cpu.data_regs[0]=Types.ERR_BFFL;            // signal no linefeed found
                    break;                                      // I'm done, I've got all I can get
                }
               
                byt=new byte[1];
                int A1 = cpu.addr_regs[1];                      // where to put bytes
                int i;
                for (i=D1W;i<D2;i++)
                {
                    cpu.data_regs[0]= m.receive1Byte(cpu,byt);  // try to get one byte
                    if (cpu.data_regs[0]!=0)
                        break;                                  // but didn't
                    cpu.writeMemoryByte(A1++, byt[0]);          // put byte
                    if (byt[0]=='\n')
                        break;                                  // done
                }
                if (cpu.data_regs[0]!=0 && cpu.data_regs[0]!=Types.ERR_NC)
                        break;                                  // genuine error, so leave
                if (i==D2 && byt[0]!='\n')
                    cpu.data_regs[0]=Types.ERR_BFFL;            // signal no line feed found
                    
                D2 = A1-cpu.addr_regs[1];                       // nbr of bytes read (this time around)
                cpu.addr_regs[1]=A1;
                cpu.data_regs[1]=cpu.data_regs[1]&0xffff0000;   // put nbr of bytes got so far into D1 lower word
                cpu.data_regs[1]|=(D2+D1W);
                break;
                
            case IOB_FMUL:                                      // get multiple bytes into buffer at (a1), d2.w = buffer size
                                                                // remember this may be called several times for one operation if NC
                if (cpu.data_regs[1]>=cpu.data_regs[2])
                    break;                                      // I'm done, I've got all I need
                bytes= m.receive(cpu,cpu.data_regs[2],false,null);                 // get bytes
                if (bytes<0)                                    // ooops
                {
                    cpu.data_regs[0]=bytes;                     // show error
                    break;                                      // and we're done
                }
                cpu.data_regs[1]+=bytes;
                cpu.addr_regs[1]+=bytes;                        // A1 pointer is increased by bytes just got
                cpu.data_regs[0]=cpu.data_regs[1]<cpu.data_regs[2]?Types.ERR_NC:0;
                break;
                   
            case IOB_SBYT:                                      // send one byte, in D1.B
                cpu.data_regs[0]= m.send1Byte(cpu,cpu.data_regs[1]);
                break;
                
            case IOB_SMUL:
                int D1=cpu.data_regs[1]&0xffff;                 // keep upper half
                bytes = m.send(cpu,cpu.data_regs[2]&0xffff);    // D2.w = amount to send         
                if (bytes<0)
                {
                    cpu.data_regs[0]=bytes;                     // error code
                    cpu.data_regs[1]=D1;                        // nothing was sent
                }
                else
                    cpu.data_regs[1]=D1|bytes;                  // that many bytes were sent
                break;
                
            case IOF_LOAD:
                byt=new byte[cpu.data_regs[2]];
                while (cpu.data_regs[1]<cpu.data_regs[2])
                {
                    bytes= m.receive(cpu,cpu.data_regs[2]-cpu.data_regs[1],false,byt); // get bytes
                    if (bytes<0)                                    // ooops
                    {
                        cpu.data_regs[0]=bytes;                     // show error
                        break;                                      // and we're done
                    }
                    cpu.addr_regs[1]+=bytes;                        // A1 pointer is increased by bytes just got
                    cpu.data_regs[1]+=bytes;                         // nbr of bytes gotten
                }
                break;
                
            case IOF_SAVE:
                bytes = m.send(cpu,cpu.data_regs[2]);           // D2.w = amount to send         
                if (bytes<0)
                {
                    cpu.data_regs[0]=bytes;                     // error code
                    cpu.data_regs[1]=0;                         // nothing was sent
                }
                else
                    cpu.data_regs[1]=bytes;                     // that many bytes were sent
                break;
                
            case IP_LISTEN:                                     // set a (server) socket to listen
                cpu.data_regs[0]= m.listen(cpu);
                break;
                
            case IP_SEND:
                bytes= m.send(cpu,cpu.data_regs[2]);
                if (bytes<0)
                {
                    cpu.data_regs[0]=bytes;
                    cpu.data_regs[1]=0;
                }
                else
                    cpu.data_regs[1]=bytes;
                break;
                
     /*           
            case IP_SENDTO:
                cpu.data_regs[0]= m.sendTo(cpu,cpu.data_regs[2]);
                break;
       */         
            case IP_RECV:
                bytes= m.receive(cpu,cpu.data_regs[2],true,null);
                if (bytes<0)
                {
                    cpu.data_regs[0]=bytes;
                    cpu.data_regs[1]=0;
                }
                else
                    cpu.data_regs[1]=bytes;
                break;
                
            case IP_BIND :
                cpu.data_regs[0]= m.bind(cpu);
                break;        
        
            case IP_CONNECT:
                cpu.data_regs[0]= m.connect(cpu);
                break;
                
            case IP_GETHOSTNAME:
            {
                String localhostname = getHName();
                if (localhostname!=null)
                    writeCString (cpu,cpu.addr_regs[1],localhostname,cpu.data_regs[2]&0xffff);
                else
                    cpu.data_regs[0]=Types.ERR_FDNF;
            }
            break;  
                
            case IP_GETHOSTBYNAME:    
                /* ON EXIT
                 (Simplified) structure pointed to by A2

                0   pointer to A
                4   pointer to C
                8   long    type (2)
                12  long    4

                A       pointer to B
                B       pointer to inet address (4 bytes if ipv4)
                        B is followed by a long word of 0
                
                C       pointer to name
                */
                try
                {
                    int A2 = cpu.addr_regs[2];
                    String name =readCString(cpu,cpu.addr_regs[1]);
                    java.net.InetAddress addr=java.net.InetAddress.getByName(name);
                    byte[] rawaddr=addr.getAddress();
                    cpu.writeMemoryLong(A2+8,2);                //type
                    cpu.writeMemoryLong(A2+12,4);     
                    int temp=A2+16;  
                    cpu.writeMemoryLong(A2,temp);               // pointer to A
                    cpu.writeMemoryLong(temp,temp+12);           // ptr to B into A
                    temp+=12;
                    cpu.writeMemoryLong(temp,temp+8);           // ptr to value into B
                    cpu.writeMemoryLong(temp+4,0);              // long word of 0 after B
                    bytes = temp+8;
                    temp=(rawaddr[0]&0xff)<<24 ;
                    temp+=((rawaddr[1]&0xff)<<16);
                    temp+=((rawaddr[2]&0xff)<<8);
                    temp+=(rawaddr[3]&0xff);
                    cpu.writeMemoryLong(bytes,temp);
                    temp=bytes + 4;                             // next free space
                    
                    bytes = writeCString (cpu,temp,name,cpu.data_regs[2]&0xffff);
                    bytes+=1+temp;
                    bytes &=0xfffffffe;
                    
                    cpu.writeMemoryLong(A2+4,bytes);               // pointer to C
                    cpu.writeMemoryLong(bytes, temp);           // pointer to name
                } 
                catch (java.net.UnknownHostException ex)
                {
                    m.setLastError (4);
                    cpu.data_regs[0]=Types.ERR_FDNF;
                }
                break;
                
            case IP_GETPEERNAME:
                cpu.data_regs[0]=m.getRemote(cpu);
                break;
                
            case IP_H_ERRNO:
                cpu.data_regs[0]=0;
                cpu.data_regs[1]=m.getLastError();
                break;
                
     //!!!!! this is a really unsafe operation: the address for the text to be returned is in A1 but NO MAXIMAL LENGTH IS GIVEN!!!!           
            case IP_H_STRERROR:
                String s = IPError.getErrorString(m.getLastError());
                writeCString (cpu,cpu.addr_regs[1],s,s.length()+1);
                cpu.data_regs[0]=0;
                break;
            
            case IP_ERRNO:
                cpu.data_regs[0]=0;
                cpu.data_regs[1]=this.lastError;
                break;
                
            default:
                m.setLastError (14);
                cpu.data_regs[0]=Types.ERR_NIMP;                // what is that? whatever it is, it is not implemented for this device
                break;
        }
        this.lastError=m.getLastError();
     }
    
   
    
    /**
     * Closes all sockets that may still be open (eg if reset).
     */
    private void closeAllSockets()
    {
        java.util.Set<Integer> set=this.socketMap.keySet();        
        for (Integer i: set)
        {
           this.socketMap.get(i).close();
        }
        this.socketMap.clear();
    }
 
    /**
     * Gets the socket corresponding to the key.
     * 
     * @param key key allowing to retrieve the socket from the map.
     * 
     * @return the socket retieved, null if not found.
     */
    public IPSocket getSocketFromMap(int key)
    {
        return this.socketMap.get(key);
    }
    
    /**
     * Writes a null terminated string to memory.
     * 
     * @param cpu the CPU used
     * @param address where to write to
     * @param s       the string to write 
     * @param maxLength max length of string
     * 
     * @return nbr of bytes written
     */
    public static final int writeCString(smsqmulator.cpu.MC68000Cpu cpu,int address,String s,int maxLength)
    {
        cpu.writeSmsqeString(address,s+"\0",false,cpu.data_regs[2]&0xffff);
        int bytes = s.length()+1;
        if (bytes>(cpu.data_regs[2]&0xffff))
        {
            bytes=cpu.data_regs[2]&0xffff;
            cpu.writeMemoryByte(address+bytes-1, 0);
            cpu.data_regs[0]=Types.ERR_OVFL;
        }
        else
        {
            cpu.data_regs[0]=0;
        }
        return bytes;
    }
    
    /**
     * Reads a "C" type string from memory and converts it to a Java String, with conversion.
     * 
     * @param cpu   the cpu with the memory array.
     * @param address where to read string in the memory.
     * 
     * @return the corresponding String.
     */
    public static final String readCString(smsqmulator.cpu.MC68000Cpu cpu,int address)
    {
        StringBuilder p=new StringBuilder(100);
        byte b;
        while((b=(byte)cpu.readMemoryByte(address++))!=0)
        {
            p.append(smsqmulator.Helper.convertToJava(b));
        }
        return p.toString();
    }
    
    
    /**
     * Try to get the host name.
     * 
     * @return name or null if problem.
     */
    private static String getHName ()
    {
        try
        {
            return java.net.InetAddress.getLocalHost().getHostName();// try obvious solution
        }
        catch (Exception u)
        { /*NOP*/}                                              // if problem use code below

        String host = System.getenv("COMPUTERNAME");
        if (host != null)
            return stripSite(host);
        return stripSite(System.getenv("HOSTNAME"));
    }
    
    private static String stripSite(String s)
    {
        if (s==null)
            return null;
        return (s.toLowerCase().endsWith(".site") && s.length()>5)?s.substring(0,s.length()-5):s;
    }
    
    
    /**
     * Get the local inet v4 address as string, without it being a loopback address.
     *  
     * @return 
     */
    private static String getLocalAddress() //throws java.net.SocketException
    {
        java.util.Enumeration<java.net.NetworkInterface> ifaces;
        try
        {
            ifaces= java.net.NetworkInterface.getNetworkInterfaces();
        }
        catch (Exception e)
        {
            return "";
        }
        while( ifaces.hasMoreElements() )
        {
            java.net.NetworkInterface iface = ifaces.nextElement();
            java.util.Enumeration<java.net.InetAddress> addresses = iface.getInetAddresses();
            while( addresses.hasMoreElements() )
            {
                java.net.InetAddress addr = addresses.nextElement();
                if( addr instanceof java.net.Inet4Address && !addr.isLoopbackAddress() )
                {
                    return addr.getHostAddress();
                }
            }
        }
        return "";
    }
}
/*
        
        /*
        
        java.util.Enumeration<java.net.NetworkInterface> iterNetwork;
        java.util.Enumeration<java.net.InetAddress> iterAddress;
        java.net.NetworkInterface network;
        java.net.InetAddress address;
        String s="";
        try
        {
            iterNetwork = java.net.NetworkInterface.getNetworkInterfaces();
        }
        catch (Exception e)
        {
            return null;                                        // if I can event hget that, abandon
        }
        while (iterNetwork.hasMoreElements())
        {
            network = iterNetwork.nextElement();
            try
            {
                if (!network.isUp() || network.isLoopback())
                    continue;
            }
            catch (Exception e2)
            {
                continue;
            }
            iterAddress = network.getInetAddresses();
            while (iterAddress.hasMoreElements())
            {
                address = iterAddress.nextElement();
                if (address.isAnyLocalAddress() ||address.isMulticastAddress() ||address.isLoopbackAddress())
                    continue;
                return address.getHostName();
           //     return(address.getHostAddress());
            }
        }
        return null;
                */
        