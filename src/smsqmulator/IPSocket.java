package smsqmulator; 

/**
 * A socket which may be of different types.
 * 
 * This class tries to mask the difference between a java.net.Socket and a java.net.ServerSocket.
 * For TCP:
 * In java, A socket is either a server socket that is listening for (and accepting) connections, or a
 * client socket that tries to get a. connection to the server .
 * The problem is that a 'C' socket may be used indifferently (within reason) as client or server socket. What it will be
 * is determined, more or less, by the "connect" and "listen/accept"  calls : the first sets the socket to be a client
 * socket, the second to be a server socket.
 * 
 * So here I sometimes have to defer creating the actual real socket until connect or listen calls are made. 
 * 
 * Moreover, the listen function as such doesn't exist in java. However, when that call is issued, I know that the corresponding
 * socket should be a server socket.
 * 
 * * 
 * @author Wolfgang Lenerz copyright (c) 2016
 * 
 * @version 
 * 1.00 several tweeks, should be ready for release.
 * 0.02 use IPReadAheadBuffer.
 * 0.01 handles unnamed SCK, all cases of client TCP. UDP not yet handled    
 * 0.00 could only handle limited type (fully specified TCP socket)
 */    
public class IPSocket
{
    private java.net.Socket socket;        
    private java.net.ServerSocket serverSocket;
    private java.net.DatagramSocket udpSocket;                  // the three types of socket that I can handle
    public  java.io.InputStream rawIn;
    private IPReadAheadBuffer inBuffer;
    public  java.io.OutputStream rawOut;
    private int type;                                           // whet type of socket am I (TCP, UDP, SCK)
    private IPTypes.CONN_STATUS status;                         // current connection status
    private int errorNumber;                                    // error number
    private int port;                                           // port number
    private int backlog;                                        // backlog for TCP server sockets
    private int family;                                         // the IP family
    private java.net.InetAddress ina;                           // getHostAddress()
    private static final int TIMEOUT=1;
    private static final int SERVERTIMEOUT=5;                  // this needs to be 55 for my messenger, else 5
  /*                                                            // perhaps later?
    private IPSocket original;                                  // possible original socket if I'm a copy
    private int revcount=1;
 */
 
    /**
     * Create a socket.
     * 
     * @param cpu the cpu used.
     * @param socketMap the map with all open sockets.
     */
    public IPSocket(smsqmulator.cpu.MC68000Cpu cpu, java.util.HashMap<Integer,IPSocket> socketMap) 
    {
        switch (cpu.data_regs[7])
        {
            case IPTypes.TYPE_SCK:
            case IPTypes.TYPE_TCP:
                this.type=cpu.data_regs[7];
                break;
            default:
                cpu.data_regs[0]=Types.ERR_ITNF;
                return;
        }
        int openType=cpu.data_regs[3];
        if (openType>2)
        {
            cpu.data_regs[0]=Types.ERR_IPAR;
            return;
        }
        if (this.type == IPTypes.TYPE_UDP || this.type == IPTypes.TYPE_TCP)
        {   
            if (openType<0)
            {
                cpu.data_regs[0]=Types.ERR_IPAR;
                return;
            }                                                   // wrong open type
            String mname=cpu.readSmsqeString(cpu.readMemoryLong(cpu.addr_regs[7]+8));
        
            if (mname.length()==4)
                mname="";
            else
                mname=mname.substring(4);                       // remove device part

            String []nameparts=mname.split(":");                // [0] = host , [1] = port

            if ((openType!=0) && (nameparts==null || nameparts.length!=2 || nameparts[0].length()<1|| nameparts[1].length()<1))
            {
                cpu.data_regs[0]=Types.ERR_IPAR;
                return;
            }                                                   // wrong open type/parametr
            if (this.type == IPTypes.TYPE_TCP)                  // 
            {
                this.status=IPTypes.CONN_STATUS.NOT_CONNECTED;
                if (openType!=0)
                {                                             
                    try
                    {
                        this.port=Integer.parseInt(nameparts[1]);           // port number
                        this.socket=new java.net.Socket(nameparts[0],this.port);
                        this.ina=this.socket.getInetAddress();
                        this.rawIn=this.socket.getInputStream();
                        this.inBuffer=new IPReadAheadBuffer(this.rawIn);
                        java.io.BufferedInputStream c=new java.io.BufferedInputStream (this.rawIn);
                        boolean b= c.markSupported();
                        this.rawOut = this.socket.getOutputStream();
                        this.type=IPTypes.TYPE_TCP;
                        this.status=IPTypes.CONN_STATUS.CONNECTED;
                        cpu.data_regs[0]=0;
                    }
                    catch (Exception e) 
                    {
                        cpu.data_regs[0]=Types.ERR_IPAR;
                    }
                }
                else
                {
                    cpu.data_regs[0]=0;
                }
            }
            else if (this.type == IPTypes.TYPE_UDP)
            {
                cpu.data_regs[0]=Types.ERR_ITNF;  
            }
        }
        else                                                    // can only be SCK
        {
            if (openType>-1)
            {                                                   // I can't handle that.
                this.status=IPTypes.CONN_STATUS.NOT_CONNECTED;
                cpu.data_regs[0]=0;
            }                                                   
            else                                                // ACCEPT a connection from an exisitng (server) socket
            {
                IPSocket sock=socketMap.get(-openType);         // the socket from which I'm supposed to accept a connection
                if (sock == null || sock.status!=IPTypes.CONN_STATUS.LISTENING || sock.type !=IPTypes.TYPE_TCP)
                {
                    cpu.data_regs[0]=Types.ERR_ICHN;
                    return;
                }                                                   // wrong open type/parametr
                java.net.ServerSocket serversock=sock.getServerSocket();
                if (serversock == null)
                {
                    cpu.data_regs[0]=Types.ERR_ICHN;
                    return;
                }                                                   // there is no server socket to do an accept on !
                try 
                {
                    this.type=IPTypes.TYPE_TCP;
                    this.errorNumber=0;
                    this.socket=serversock.accept();                // try to get a  connection, may raise exception if blocking or timeout
   // if we get here a connection was made
                    this.socket.setSoTimeout(IPSocket.TIMEOUT);
                    this.rawIn=this.socket.getInputStream();
                    this.inBuffer=new IPReadAheadBuffer(this.rawIn);
                    this.rawOut = this.socket.getOutputStream();
                    this.port=this.socket.getLocalPort();
                    this.ina=this.socket.getLocalAddress();
                    this.status=IPTypes.CONN_STATUS.CONNECTED;
                    cpu.data_regs[0]=0;
           /*         
                    this.port=sock.port;
                    this.ina=sock.ina;
*/
                }
                catch (java.nio.channels.IllegalBlockingModeException | java.net.SocketTimeoutException e) // on accept, channel would have blocked
                {
                    cpu.data_regs[0]=Types.ERR_NC;
                }                                                   // there is no server socket to do an accept on
                catch (Exception e)
                { 
                    cpu.data_regs[0]=Types.ERR_ICHN;
                }
            }
        }
    }

    /**
     * Close the socket and all associated channels.
     */
    public void close()
    {
        try
        {
            this.rawIn.close();
        }
        catch (Exception e)
        {/*NOP*/}

        try
        {
            this.rawOut.close();
        }
        catch (Exception e)
        {/*NOP*/}

        try
        {
            if (this.socket!=null)
                this.socket.close();
            else if (this.serverSocket!=null)
                this.serverSocket.close();
            else if (this.udpSocket!=null)
                this.udpSocket.close();
        }
        catch (Exception e)
        {/*NOP*/}
    }

    /**
     * Gets the connection status.
     * 
     * @return the connection status, one of IPTypes.CONN_STATUS.
     */
    public IPTypes.CONN_STATUS getConnectionStatus()
    {
        return this.status;
    }

    /**
     * Sets the connection status.
     * 
     * @param g the connection status wished, one of IPTypes.CONN_STATUS.
     */
    public void setConnectionStatus(IPTypes.CONN_STATUS g)
    {
        this.status=g;
    }

    /**
     * Get the socket type.
     * 
     * @return the SOCKET_TYPE {SCK,TCP,UDP,NONE}
     */
    public int getSocketType()
    {
        return this.type;
    }  
    
    /**
     * Sets the socket type.
     * @param g the type of socket this should be SOCKET_TYPE {SCK,TCP,UDP,NONE}
     */
    public void setSocketType(int g)
    {
        this.type=g;
    }

    /**
     * Get the TCP client socket.
     * 
     * @return the TCP client socket
     */
    public java.net.Socket getSocket()
    {
        return this.socket;
    } 
    
    /**
     * Get the TCP server socket.
     * 
     * @return the TCP server socket
     */
    public java.net.ServerSocket getServerSocket()
    {
        return this.serverSocket;
    } 

    
    /**
     * Gets the last error from the socket.
     * 
     * @return the last error encountered by the socket when send/receiving,connecting/listening.
     */
    public int getLastError()
    {
        return this.errorNumber;
    }
    
    /**
     * Gets the last error from the socket.
     * 
     * @param err the last error encountered by the socket when send/receiving,connecting/listening.
     */
    public void setLastError(int err)
    {
        this.errorNumber=err;
    }
    
   
    /**
     * Binds a socket. This means the port and host addresses are set.
     * 
     * @param cpu the smsqmulator.cpu.MC68000Cpu object.
     * 
     * @return 0 or SMSQ/E error code.
     */
    public int bind(smsqmulator.cpu.MC68000Cpu cpu)
    {
        switch (this.type)
        {
            case IPTypes.TYPE_SCK:       
                this.errorNumber=2;
                return Types.ERR_ICHN;
                
            case IPTypes.TYPE_TCP:
            case IPTypes.TYPE_UDP:
                this.port = cpu.readMemoryWord(cpu.addr_regs[2]+2);
                this.family = cpu.readMemoryWord(cpu.addr_regs[2]);
                byte[]id=new byte[4];
                for (int i=0;i<4;i++)
                {
                   id[i]=(byte)(cpu.readMemoryByte(cpu.addr_regs[2]+4+i));
                }
                try
                {
                    this.ina=java.net.InetAddress.getByAddress(id);
                } 
                catch (java.net.UnknownHostException e)
                {
                    this.errorNumber=4;                         // error in java.net.InetAddress.getByAddress creation
                    return Types.ERR_IPAR;
                } 
                catch (Exception e)
                {
                    this.errorNumber=4;                         // this should never happen
                    return Types.ERR_IPAR;
                }   
                this.errorNumber=0;
                return 0;
               
            default:
                this.errorNumber=1;                             // unknown socket can't be bound
                return Types.ERR_ICHN;
        }
    }
    
    
    /**
     * Connects an existing socket.
     * This means that the socket will be a client socket (TCP).
     * 
     * @param cpu the smsqmulator.cpu.MC68000Cpu (A1 points to a sockaddr structure)
     * 
     * @return SMSQ/E error code or 0
     */
    public int connect(smsqmulator.cpu.MC68000Cpu cpu)
    {
        switch (this.type)
        {
            case IPTypes.TYPE_TCP:
                if (this.status==IPTypes.CONN_STATUS.CONNECTED)
                {
                    this.errorNumber=3;
                    return Types.ERR_FDIU;
                }
                int pport = cpu.readMemoryWord(cpu.addr_regs[2]+2);
                this.family = cpu.readMemoryWord(cpu.addr_regs[2]);//53250
                byte[]id=new byte[4];
                for (int i=0;i<4;i++)
                {
                   id[i]=(byte)(cpu.readMemoryByte(cpu.addr_regs[2]+4+i));
                }
                try
                {
                    this.ina=java.net.InetAddress.getByAddress(id);
                    java.net.InetSocketAddress m=new java.net.InetSocketAddress(this.ina, pport);
                    java.net.Socket nsocket = new java.net.Socket();
                    nsocket.connect(m, 200);
                    this.socket= nsocket;
                    this.port=this.socket.getLocalPort();
                    this.ina=this.socket.getLocalAddress();
                    this.rawIn=this.socket.getInputStream();
                    this.inBuffer=new IPReadAheadBuffer(this.rawIn);
//                        java.io.BufferedInputStream c=new java.io.BufferedInputStream (this.rawIn);
                    this.rawOut = this.socket.getOutputStream();
                    this.socket.setSoTimeout(IPSocket.TIMEOUT);
                    this.type=IPTypes.TYPE_TCP;
                    this.status=IPTypes.CONN_STATUS.CONNECTED;
                }
                catch (java.net.UnknownHostException | NullPointerException e)
                {
                    this.errorNumber=4;                         // error in java.net.InetAddress.getByAddress or socket creation
                    return Types.ERR_IPAR;
                }
                catch (java.lang.IllegalArgumentException  e)
                {
                    this.errorNumber=5;                         // error when creating java.net.InetSocketAddress;
                    return Types.ERR_IPAR;
                }
                catch (SecurityException e)
                {
                    this.errorNumber=7;                         // security error when creating the socket
                    return Types.ERR_IPAR;
                }
                catch (java.net.ConnectException e)
                {
                    this.errorNumber=15;
                    return Types.ERR_ACCD;
                }
                catch (java.io.IOException e)
                {   
                    this.errorNumber=6;                         // i/o error when creating the socket;
                    return Types.ERR_IPAR;
                }
                catch (Exception e)
                {
                    this.errorNumber=8;
                    return Types.ERR_IPAR;
                }
                break;
                
            case IPTypes.TYPE_SCK:       
                this.errorNumber=2;
                return Types.ERR_ICHN;
                
            default:
                this.errorNumber=1;
                return Types.ERR_ICHN;
        }
        this.errorNumber=0;
        return 0;
    }
     
    /**
     * Makes an existing socket a "listening" one.
     * .
     * This means that the socket will be a server socket (TCP). This socket is actually created here.
     * 
     * @param cpu (a1 points to a sockaddr structure)
     * @return error code or 0
     */
    public int listen(smsqmulator.cpu.MC68000Cpu cpu)
    {
        switch (this.type)
        {
            case IPTypes.TYPE_TCP:
                if (this.status==IPTypes.CONN_STATUS.CONNECTED)
                {
                    this.errorNumber=3;
                    return Types.ERR_FDIU;
                }
                if (this.socket!=null)                          // a client can't become a server
                {
                    this.errorNumber=12;
                    return Types.ERR_FDIU;
                }
                try
                {
                    this.backlog=cpu.data_regs[1];
                    if (this.serverSocket==null)
                    {
                        this.serverSocket= new java.net.ServerSocket(this.port,this.backlog,this.ina);
                        this.serverSocket.setSoTimeout(IPSocket.SERVERTIMEOUT);
                    }
                   
                    this.type=IPTypes.TYPE_TCP;
                    this.status=IPTypes.CONN_STATUS.LISTENING;
                    this.errorNumber=0;
                }
                catch (java.net.UnknownHostException e)
                {
                    this.errorNumber=4;                         // error in java.net.InetAddress.getByAddress 
                    return Types.ERR_IPAR;
                }
                catch (java.lang.IllegalArgumentException  e)
                {
                    this.errorNumber=5;                         // wrong port number when creating the socket
                    return Types.ERR_IPAR;
                }
                catch (SecurityException e)
                {
                    this.errorNumber=7;                         // security error when creating the socket
                    return Types.ERR_IPAR;
                }
                catch (java.nio.channels.ClosedChannelException e)
                {
                    this.errorNumber=13;                         // security error when creating the socket
                    return Types.ERR_IPAR; 
                }
                 catch (java.net.BindException e)
                {   
                    this.errorNumber=16;                         // socket is already bound;
                    return Types.ERR_FDIU;
                }
                catch (java.io.IOException e)
                {   
                    this.errorNumber=6;                         // i/o error when creating the socket;
                    return Types.ERR_IPAR;
                }
                catch (Exception e)
                {
                    this.errorNumber=8;                         // some form of unknown error
                    return Types.ERR_IPAR;
                }
                break;
                
              case IPTypes.TYPE_SCK:       
                this.errorNumber=2;
                return Types.ERR_ICHN;
            
            default:
                this.errorNumber=1;
                return Types.ERR_ICHN;
        }
        this.errorNumber=0;
        return 0;
    }
    
    /**
     * Actual IO operations.---------------------------------------------------------------------------------
     */
    
    /**
     * Send bytes out over the port.
     * 
     * Note this is sent over an unprotected, naked, unbuffered socket.
     * 
     * @param cpu the cpu to get data to be sent from, it lies at (A1), ndr of bytes to be sent in D2.L.
     * @param size nbr of bytes to send
     * 
     * @return nbr of bytes sent or negative SMSQ/E error code.
     */
    public int send(smsqmulator.cpu.MC68000Cpu cpu,int size)
    {
        if (size==0)
        {
            return 0;
        }
        switch (this.type)
        {
            case IPTypes.TYPE_TCP:
                if (this.status!=IPTypes.CONN_STATUS.CONNECTED)
                {
                    this.errorNumber=9;
                    return Types.ERR_EOF;                       // if we're not connected, nothing can be sent;
                }
                
                byte[] byt = new byte[size];                    // nbr of bytes to send
                int A1 = cpu.addr_regs[1];                      // where to get bytes from
                if ((A1&1)==0)
                    fastRead(cpu,byt,A1,size);
                else
                {
                    for (int i=0;i<size;i++)
                    {
                        byt[i]=(byte)(cpu.readMemoryByte(A1++));
                    }
                }
                try
                {
                    this.rawOut.write(byt,0,size);
                    this.rawOut.flush();
                    cpu.addr_regs[1]+=size;
                    this.errorNumber=0;
                    return size;
                }
                catch (java.io.IOException e)
                {
                    this.errorNumber=10;
                    return Types.ERR_TRNS;
                }
                catch (Exception e)
                {
                    this.errorNumber=8;
                    return Types.ERR_TRNS;
                }
            
                    
            case IPTypes.TYPE_UDP:
            case IPTypes.TYPE_SCK:
                this.errorNumber=2;
                return Types.ERR_ICHN;
            
            default:
                this.errorNumber=1;
                return Types.ERR_ICHN;
        }
    }
    
    /**
     * Sendto - only allowed for TCP where it is the same as send.
     * 
     * @param cpu the cpu used.
     * @param size size of data to be set, in bytes.
     * 
     * @return SMSQ/E type error code (0 if no error).
     */
    public int sendTo(smsqmulator.cpu.MC68000Cpu cpu,int size)
    {
        if (size==0)
        {
            cpu.data_regs[1]=0;
            return 0;
        }
        if (this.type==IPTypes.TYPE_TCP)
            return send (cpu,size);
        else
        {
            this.errorNumber=2;
            return Types.ERR_ICHN;   
        }   
    }
    
    /**
     * Send one byte out over the socket.
     * 
     * @param cpu the cpu used
     * @param byt the byte to send
     * 
     * @return 0 or SMSQ/E error.
     */
    public int send1Byte(smsqmulator.cpu.MC68000Cpu cpu,int byt)
    {
        this.errorNumber=0;
        switch (this.type)
        {
            case IPTypes.TYPE_TCP:
                if (this.status!=IPTypes.CONN_STATUS.CONNECTED)
                {
                    this.errorNumber=9;
                    return Types.ERR_EOF;                       // if we're not connected, nothing can be sent;
                }
                try
                {
                    this.rawOut.write(byt);
                    this.rawOut.flush();
                }
                catch (java.io.IOException e)
                {
                    this.errorNumber=10;
                    return Types.ERR_TRNS;
                }
                catch (Exception e)
                {
                    this.errorNumber=8;
                    return Types.ERR_TRNS;
                }
                break;
                
            case IPTypes.TYPE_SCK:  
                this.errorNumber=2;
                return Types.ERR_ICHN;
                
            default:
                this.errorNumber=1;
                return Types.ERR_ICHN;
        }
        return 0;
    }

    /**
     * Read bytes from memory and puts them into the byte array.
     * 
     * @param cpu the cpu with the memeory (as array of shorts).
     * @param byt the array to be filled with the bytes read from memory.
     * @param A1 where to start reading memory content from.
     */
    private void fastRead(smsqmulator.cpu.MC68000Cpu cpu,byte []byt,int A1,int size)
    {
        int w;
        short[]mainMemory=cpu.getMemory();
        
        A1/=2;                                                  // index into memory array
        int i;
        for (i=0;i<size-1;i+=2)
        {
            w=mainMemory[A1++]&0xffff;                          // get the word                          
            byt[i]=(byte)(w>>>8);                               // set upper byte
            byt[i+1]=(byte)(w&0xff);                            // set lower byte
        }
        if ((size&1)!=0)                                        // handle possible trailing byte
        {
            byt[i]=(byte)cpu.readMemoryByte(A1*2);
        }
    }
    
    /**
     * Receive bytes from the port and copy them to (A1), updating A1.
     *  
     * on entry, D1 may hold a flag, which could be as follows
     *                  0x0             // no flag
	MSG_OOB		0x1		// process out-of-band data 
	MSG_PEEK	0x2		/* peek at incoming message 
	MSG_DONTROUTE	0x4		/* send without using routing tables 
	MSG_EOR		0x8		/* data completes record 
	MSG_TRUNC	0x10		/* data discarded before delivery 
	MSG_CTRUNC	0x20		/* control data lost before delivery 
	MSG_WAITALL	0x40		/* wait for full request or error 
     * 
     * for the time being, only cases 0 and 2 are honoured.
     * 
     * @param cpu the CPU used.
     * @param size nbr of bytes to read
     * @param checkD1 if true, should check D1 for flags.
     * @param byt byt array into which to receive, if null, a temporary one will be used here.
     * 
     * @return SMSQ/E negative error code or the number of bytes I got
     */
    public int receive(smsqmulator.cpu.MC68000Cpu cpu,int size,boolean checkD1,byte[]byt)
    {
        this.errorNumber=0;
        switch (this.type)
        {    
            case IPTypes.TYPE_TCP:
                if (this.status!=IPTypes.CONN_STATUS.CONNECTED)
                {
                    this.errorNumber=9;
                    return Types.ERR_EOF;                       // if we're not connected, nothing can be received
                }
       
                if (byt==null)
                    byt = new byte[size];                       // buffer size
                int A1 = cpu.addr_regs[1];                      // where to put result
                try
                {
                    int received;
                    if (checkD1)
                        received = this.inBuffer.read(byt, (cpu.data_regs[1]&2)!=0); 
                    else
                        received = this.inBuffer.read(byt, false); 
                    
                    if (received>0 )
                    {
                        if ((A1&1)==0)
                        {
                            fastWrite(cpu,byt,A1,received);
                        }
                        else
                        {
                            for (int i=0;i<received;i++)
                            {
                                cpu.writeMemoryByte(A1++, byt[i]);
                            } 
                        }
                        return received;
                    }
                    return 0;                                   // a read of -1 means I got nothing
                }
                catch (java.net.SocketTimeoutException e)       // the socket timed out before I got anything
                {
                    this.errorNumber=11;                        // set specific error code
                    return Types.ERR_NC;                        // show that my call was not complete.
                   // return 0;
                }
                catch (java.io.IOException e)                   // some other I/O error
                {
                    this.errorNumber=11;
                    return Types.ERR_TRNS;                      // show it
                }
                catch (Exception e)                             // catch all
                {
                    this.errorNumber=8;
                    return Types.ERR_TRNS;
                }
                
            case IPTypes.TYPE_SCK:     
                this.errorNumber=2;
                return Types.ERR_ICHN;
                
            default:
                this.errorNumber=1;
                return Types.ERR_ICHN;
        }
    }
   
     /**
     * Gets bytes from the byte array and writes them into memory.
     * This presumes that A1 is even.
     * 
     * @param cpu the cpu with the memory (as array of shorts).
     * @param byt the array to be filled with the bytes read from memory.
     * @param A1 where to start writing memory content to.
     * @param nbr how many bytes to write
     * 
     * ///@return updated memory pointer
     */
    private void fastWrite(smsqmulator.cpu.MC68000Cpu cpu,byte []byt,int A1,int nbr)
    {
        int w;
        short[]mainMemory=cpu.getMemory();
        A1/=2;                                                  // index into memory array
        for (int i=0;i<nbr-1;i+=2)
        {
            w=byt[i]<<8;
            w|=(byt[i+1]&0xff);
            mainMemory[A1++]=(short)w;                          
        }
        if ((nbr&1)!=0)                            // handle possible trailing byte
        {
            cpu.writeMemoryByte(A1*2,byt[nbr-1]);
        }
    }
    
    /**
     * Receive one byte from the port and return it.
     *  
     * for the time being, only cases 0 and 2 are honoured.
     * 
     * @param cpu the CPU used.
     * @param byt a byte array with ONE ELEMENT ONLY into which the byte goes, may not be null.
     * 
     * @return SMSQ/E error code or 0 if all ok.
     */
    public int receive1Byte(smsqmulator.cpu.MC68000Cpu cpu,byte[]byt)
    {
        this.errorNumber=0;
        if (byt==null || byt.length!=1)
            return Types.ERR_EOF;                       // what????
        switch (this.type)
        {    
            case IPTypes.TYPE_TCP:
                if (this.status!=IPTypes.CONN_STATUS.CONNECTED)
                {
                    this.errorNumber=9;
                    return Types.ERR_EOF;                       // if we're not connected, nothing can be received
                }
                try
                {
                    int received = this.inBuffer.read(byt, false); // get one byte
                    if (received<1)
                    {
                        return Types.ERR_NC;
                    }
                }
                catch (java.net.SocketTimeoutException e)
                {
                    this.errorNumber=11;
                    return Types.ERR_NC;
                }
                catch (java.net.SocketException e)
                {
                    this.errorNumber=11;
                    return Types.ERR_EOF;
                }
                catch (java.io.IOException e)
                {
                    this.errorNumber=11;
                    return Types.ERR_TRNS;
                }
                catch (Exception e)
                {
                    this.errorNumber=8;
                    return Types.ERR_TRNS;
                }
                break;
                
            case IPTypes.TYPE_SCK:     
                this.errorNumber=2;
                return Types.ERR_ICHN;
                 
            default:
                this.errorNumber=1;
                return Types.ERR_ICHN;
                
        }
        return 0;
    }  
    
    /**
     * Get info on the remote host, put it at (a1)..
     * at A1 lie : family, port, ipv4 address. On return D1 = length of answer.
     * 
     * @param cpu the cpu used
     * 
     * @return SMSQ/E error code (0 if no error).
     */
    public int getRemote(smsqmulator.cpu.MC68000Cpu cpu)
    {
        if (cpu.data_regs[2]<2)
            return Types.ERR_ORNG;
        cpu.writeMemoryWord(cpu.addr_regs[1], this.family);
        
        if (cpu.data_regs[2]<4)
        {
            cpu.data_regs[1]=2;
            return 0;
        }
        java.net.InetSocketAddress temp =(java.net.InetSocketAddress)this.socket.getRemoteSocketAddress();
        cpu.writeMemoryWord(cpu.addr_regs[1]+2, temp.getPort());
        
        if (cpu.data_regs[2]<8)
        {
            cpu.data_regs[1]=4;
            return 0;
        }
        java.net.InetAddress iadr=temp.getAddress();
        byte[] addr=iadr.getAddress();
        if (addr.length!=4)
        {
            return Types.ERR_NIMP;
        }
        int ttemp=0;
        for (int i=0;i<3;i++)
        {
            ttemp |=(addr[i]&0xff);
            ttemp<<=8;
        }
        ttemp |=(addr[3]&0xff);
        cpu.writeMemoryLong(cpu.addr_regs[1]+4, ttemp);
        cpu.data_regs[1]=8;
        return 0;
    }
    
    public String getRemoteChanName()
    {
        if (this.status==IPTypes.CONN_STATUS.CONNECTED)
        {
             java.net.InetSocketAddress temp =(java.net.InetSocketAddress)this.socket.getRemoteSocketAddress();
             return temp.getHostString()+":"+temp.getPort();
        }
        else
        {
            return "";
        }
    }
    
      /**
     * Gets the name of the current socket, including address, port and connection status.
     * 
     * @return the full name, e.g "TCP_127.0.0.1:123 (l)" means a TCP connection to localhost, "listening" on port 123.
     */
    public String getName()
    {
        StringBuilder p=new StringBuilder (64);
        switch (this.type)
        {
            case IPTypes.TYPE_UDP:
                p.append("UDP_");
                break;
            case IPTypes.TYPE_SCK:
                p.append("SCK_");
                break;
            case IPTypes.TYPE_TCP:
                p.append("TCP_");
                break;
            default:
                p.append("???_IP_???_");
                break;
        }
        
        if (this.socket!=null)
        {
          //  p.setLength(0);
        //    p.append(this.socket.getLocalAddress());
            
          //  p.append(":").append(this.socket.getLocalPort());
            p.append(this.ina.getHostAddress());
            p.append(":").append(this.port);
        }
        else
        {
            if (this.ina!=null)
                p.append(this.ina.getHostAddress());
            p.append(":").append(this.port);
        }
        
        switch (this.status)
        {
            case NOT_CONNECTED:
                p.append(" (nc)");
                break;
            case CONNECTING:
                p.append(" (ci)");
                break;
            case CONNECTED:
                p.append(" (c)");
                break;
            case LISTENING:
                p.append(" (l)"); 
                break;
            
        }
        return p.toString();
    }
   
    private static String stripSite(String s)
    {
        if (s==null)
            return null;
        return (s.toLowerCase().endsWith(".site") && s.length()>5)?s.substring(0,s.length()-5):s;
    }
}