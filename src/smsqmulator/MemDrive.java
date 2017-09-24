package smsqmulator;

/**
 * This class handles one "MEM" drive - a copy in memory of a qxl.win drive.
 * <p>
 * Except for specific methods, this is a clone on the WinDrive class.
 * @author and copyright (c) wolfgang lenerz 2013
 * 
 * @version  
 * 1.00 initial version
 */
public class MemDrive extends WinDrive
{
    private java.nio.ByteBuffer totalFile;
    public static final int MAX_FILESIZE= 500*1024*1024;    // the totally artificial file size limit for such a file is 500MiB.
    private int fileSize;                                   // size of file
    private boolean isLocal=false;                          // <code> true </code> if thiq file is local, not over the internet.    
    
    /**
     * Creates the object.
     * 
     * @param cpu the cpu used.
     * @param driveName the name of the qxl.win file on the native file system.
     * @param driver the driver having created this object.
     * @param number my driveNbr as ASCII!.
     * @param warn object with warning flags.
     * 
     * @throws java.io.FileNotFoundException the qxl.win file isn't found
     * @throws java.io.IOException any IO exception reading the FAT / main dir
     * @throws ArrayIndexOutOfBoundsException a wrong FAT cluster number was given
     * @throws java.net.ProtocolException if the main dir couldn't be read.
     * @throws DoNothingException if this is a drive with a wrong FAT and user doesn't want me to (try to) fix it.
     * @throws IncorrectFiletypeException if this is not a valid qlwa file.
     */
    public MemDrive(smsqmulator.cpu.MC68000Cpu cpu,String driveName,MemDriver driver,int number,Warnings warn) 
           throws java.io.FileNotFoundException,java.io.IOException,ArrayIndexOutOfBoundsException,java.net.ProtocolException,
                  DoNothingException,IncorrectFiletypeException
    {
        super(cpu,warn);
        this.driveName=driveName;
        this.driver=driver;
        this.driveNumber=number;
        readFAT(this.driveName);                            // read the header and the FAT, will generate an exception if error
        readMainDir();                                      // read the main directory
        if (this.mainDir==null)
        {
            throw new java.net.ProtocolException(Localization.Texts[56]+"\n("+driveName+")");
        }
    }
    
    /**
     * Reads the drive into the a buffer and reads the drive's fat.
     * The FAT starts at offset 0x40 of the very first sector. So the fist cluster (cluster 0) already is part of the FAT.
     * 
     * @param nativeFilename the native name of the file that is the QXL.WIN type file. This my also be a URL passed as a string
     * 
     * @throws java.io.FileNotFoundException the qxl.win file isn't found.
     * @throws java.io.IOException any IO exception reading the map / main dir / file itqelf.
     * @throws ArrayIndexOutOfBoundsException a wrong map cluster number was given
     * @throws IncorrectFiletypeException if this is not a valid qlwa file.
     * @throws IllegalStateException if the raFile isn't null when this object is created (how could that be?????).
     */
    private void readFAT(String nativeFilename) throws java.io.FileNotFoundException,java.io.IOException,ArrayIndexOutOfBoundsException,
                                                            DoNothingException,IncorrectFiletypeException
    {
        int fSize;
        java.io.InputStream is=null;
        long fl=0;
        try
        {
            
            java.io.File  f =new java.io.File (nativeFilename);
            is= new java.io.FileInputStream(f);             // try to open file as file
            fl=f.length();
            if (fl>MemDrive.MAX_FILESIZE)
            {
                try 
                {
                    is.close();
                }
                catch (Exception e)
                {/*nop*/}
                fSize=-1;                                // signal error
            }
            else
                fSize=(int)fl + 2;
            this.isLocal=true;                              // this is a local fil, may be written back
        }
        catch (Exception noFile)                            // problem opening file? Perhaps it's a URL      
        {
            try
            {
                java.net.URL url=new java.net.URL(nativeFilename);
                is=url.openStream();
                fSize=1024*1024;                            // fictitious filesize
            }        
            catch (Exception e)
            {
         //       e.printStackTrace();
                throw new java.io.FileNotFoundException();
            }
        }
        if (fSize==-1)
            throw new java.io.IOException (Localization.Texts[115]+":\n"+nativeFilename+"\n("+fl+Localization.Texts[116]);
     //±   fSize+=2;                                           // get around a bug if the file is just 1024*1024 bytes long
        byte[] b=new byte[fSize];                           // presume file will fit into here
        this.fileSize=0;
        // read the file chunk by chunk, maybe increase the array size
        try
        {
            int p=0;                                        // how many bytes we get at each read
            while (p!=-1)
            {
                p=is.read(b, this.fileSize, fSize-this.fileSize);
                this.fileSize+=p;
                if ((fSize-this.fileSize)<=0)
                {
                    fSize+=1024*1024;                       // increase in 1 MiB chunks
                    if (fSize>MemDrive.MAX_FILESIZE)        // but not more than accepted max size
                    {
                        fSize=-1;
                        break;
                    }
                    b = java.util.Arrays.copyOf(b, fSize);
                }
            }
            try
            {
                is.close();
            }
            catch (Exception e)
            {/* nop */}
        }
        catch (Exception e)
        {
            throw new java.io.IOException(Localization.Texts[117]);
        }
        if (fSize==-1)
            throw new java.io.IOException (Localization.Texts[115]+":\n"+nativeFilename+"\n("+fl+Localization.Texts[116]);
        this.fileSize+=1;
        this.totalFile=java.nio.ByteBuffer.wrap(b);     
        if (this.totalFile.getInt(0)!=WinDriver.QLWA)       // marker must check
        {
            throw new IncorrectFiletypeException();         // else this can't be a true QLWA file
        }
      
        this.clusterSize=512*(this.totalFile.getShort(WinDrive.QWA_SCTG)&0xffff); // size of cluster in bytes
        this.fatClusterChain= new java.util.ArrayList <>();//cluster hain for FAT
        this.fatClusterChain.add(0);                        // first cluster of map is always cluster 0
        // now find out the cluster numbers for the FAT.
        // for convenience, we'll hold the entire FAT + header in memory - we first need to find out how many clusters the FAT occupies
        // the FATs clusterchain lies at the every beginnng of the FAT.
        int cluster=this.totalFile.getShort(WinDrive.QWA_GMAP)&0xffff;// next cluster for map
        try                                                 // we assume that the entire FAT clusterchain fits into the first sector...
        {
            while (cluster!=0)
            {       
                this.fatClusterChain.add(cluster);          // one more cluster
                cluster=this.totalFile.getShort(WinDrive.QWA_GMAP+cluster*2)&0xffff;//get pointer to next cluster
            }
        }
        catch (Exception e)  
        {
            throw new ArrayIndexOutOfBoundsException();     // Huh????
        }
        // patch for badly constructed drives, like those built by qxltools. These drive do NOT have
        // cluster entries for the FAT(!!!!). They just indicate the number of sectors taken by the FAT.
        // I presume that it can then be presumed that the FAT's sectors are all contiguous and start at sector 0.
        int nbrOfSectorsInMap=this.totalFile.getShort(WinDrive.QWA_SCTM)&0xffff;// that many sectors are supposed to be in the fat
        int temp=nbrOfSectorsInMap/(this.totalFile.getShort(WinDrive.QWA_SCTG)&0xffff);
        if (nbrOfSectorsInMap % (this.totalFile.getShort(WinDrive.QWA_SCTG)&0xffff)!=0)
            temp++;                                         // these are the number of clusters needed to hold the map
        if (temp!=this.fatClusterChain.size())              // if they both agree, all is ok, I have a valid drive map
        {   
            if (this.warnings.warnIfQXLDriveNotCompliant)   // but here it isn't and I don't
            {
                java.awt.Toolkit.getDefaultToolkit().beep();
                int reply=javax.swing.JOptionPane.showConfirmDialog
                (
                    null,
                    Localization.Texts[74]+this.driver.getName(this.driveNumber-49)+Localization.Texts[75]+"\n"+
                    Localization.Texts[76]+"\n"+
                    Localization.Texts[77]+"WIN"+(this.driveNumber-48)+"_ "+Localization.Texts[78]+"\n",
                    Localization.Texts[73],
                    javax.swing.JOptionPane.WARNING_MESSAGE,
                    javax.swing.JOptionPane.YES_NO_OPTION
                );
                if (reply!=javax.swing.JOptionPane.YES_OPTION) 
                {
                    throw new DoNothingException();         // this signals that we should abandon trying to fix the drive
                }
            }
            for (int i = 1;i<temp;i++)
                this.fatClusterChain.add(i);                // make a clusterchain representing xxx first clusters
        }
        // if we get here, we have the number of clusters occupied by the FAT
        this.driveFAT=java.nio.ByteBuffer.allocate(this.clusterSize*this.fatClusterChain.size());// get space for all clusters
        this.driveFAT.position(0);
        this.driveFAT.limit(this.driveFAT.capacity());
        this.totalFile.position(0);
        this.totalFile.limit(this.driveFAT.capacity());
        this.driveFAT.put(this.totalFile);
        this.totalFile.limit(this.totalFile.capacity());
        this.driveFAT.position(0);
        this.totalFile.position(0);
    }
    
    /**
     * Creates and reads the main directory.
     */
    private void readMainDir()
    {
        int rootDirCluster=this.driveFAT.getShort(QWA_ROOT)&0xffff;         // first cluster of root dir
        int rootLength=(this.driveFAT.getInt(QWA_RLEN));                    // length of root dir
        java.util.ArrayList<Integer> cchain= new java.util.ArrayList<>();
        java.nio.ByteBuffer buf=readFile(rootLength, rootDirCluster,cchain);
        if (buf!=null)
        {
            this.mainDir=new WinDir(this,null,0,buf,cchain,null,null);
        }
    }
    
    /**
     * This writes the FAT + drive header back to the drive.
     * 
     * @throws java.io.IOException 
     */
    private void writeFAT() throws java.io.IOException
    {
        writeFile(this.driveFAT,this.fatClusterChain);
        this.driveFAT.limit(this.driveFAT.capacity());
        this.driveFAT.position(0);
    }
  
  
    /*********** ACTUAL READING/WRITING TO/FROM THE DRIVE ****************************/
     /**
     * Creates a ByteBuffer (for a file) and reads the file into it.
     * This also sets the clusters in the clusterchain
     * 
     * @param fileLength the length of the file (should include the length of the file header).
     * @param cluster the first cluster of the file (index into the FAT).
     * @param clusterchain - the empty cluster chain, will be filled in here.
     * 
     * @return the newly created buffer with the content of the file, or <code>null</code> if problem. Attention : the size of the buffer will most
     * likely be bigger than the size of the file : the size of the buffer is increased to be a multiple of the clusterSize.
     * This should correspond to the number of clusters occupied by the file.
     */
    @Override
    public java.nio.ByteBuffer readFile(int fileLength,int cluster,java.util.ArrayList<Integer> clusterchain)
    {
        if (fileLength<1 || clusterchain==null)             // null or negative length files don't make sense, clusterchain must exist.
            return null;
        
        int fsize=fileLength/this.clusterSize;
        if ((fileLength%this.clusterSize)!=0)
            fsize++;                                        // number of clusters needed for this file
        
        clusterchain.clear();
        java.nio.ByteBuffer buffer=java.nio.ByteBuffer.allocate(fsize*this.clusterSize);
        if (readFile(fileLength,cluster,buffer,clusterchain))            // read file into buffer
            return buffer;
        else
            return null;
    }
       
    /**
     * Reads a file from the drive into the given ByteBuffer.
     * The file is read cluster by cluster.
     * The general contract here is that after each read from the total file buffer, the position is reset to 0 and the limit to the end of the buffer
     * 
     * @param fileLength the length of the file to be read | the remaining bytes to be read (will be decreased during read).
     * @param cluster the first cluster of the file (index into the FAT).
     * @param buffer the ByteBuffer into which the file will be read. This must be long enough for the entire file, else
     *        the file will not be read.
     * 
     * @return <code>true</code> if read was OK, <code>false</code> if not.
     */
    private boolean readFile(int fileLength,int cluster,java.nio.ByteBuffer buffer,java.util.ArrayList<Integer> clusterchain)
    {
        if (buffer.capacity()<fileLength)                   // buffer must be this long to hold the file
            return false;
        int bufferPosition=0;                               // start in buffer where bytes will be read to
        int bytesRead;                                      // nbr of bytes read
        int filePosition;                                  // position in qxl.win file to read from
        try
        {
            do 
            {
                clusterchain.add(cluster);
                bytesRead=Math.min(this.clusterSize,fileLength);//read this mlany bytes
                buffer.limit(bufferPosition+bytesRead);     // end of where the bytes to read go
                buffer.position(bufferPosition);            // start of where the bytes to read go
                filePosition=cluster*this.clusterSize;      // where the cluster lies in the QXL.Win file
                this.totalFile.limit(filePosition+bytesRead);// nbr of bytes to read
                this.totalFile.position(filePosition);
                buffer.put(this.totalFile);
                fileLength-=bytesRead;
                if (fileLength<=0  || bytesRead!=this.clusterSize)
                    cluster=0;                              // either end of file, or error, in both cases stop reading
                else
                {
                    bufferPosition+=this.clusterSize;
                    cluster=this.driveFAT.getShort(cluster*2+WinDrive.QWA_GMAP)&0xffff;
                }
            } while (cluster!=0);
            buffer.position(0);
            buffer.limit(buffer.capacity());
            this.totalFile.position(0);
            this.totalFile.limit(this.totalFile.capacity());
            return true;
        }
        catch (Exception e)
        {
            this.totalFile.position(0);
            this.totalFile.limit(this.totalFile.capacity());
            return false;
        }
    }
    
    /**
     * This writes an entire file in a byte buffer back to the drive.
     * 
     * @param fileBuffer the ByteBuffer containing the file. The capacity of the buffer may be bigger than the true filesize.
     * @param clusterchain the first cluster of the file.
     * @throws java.io.IOException  any i/o exception from the java i/o operations.
     * 
     */
    @Override
    public void writeFile(java.nio.ByteBuffer fileBuffer,java.util.ArrayList<Integer> clusterchain) throws java.io.IOException
    {       
        int buffpos=0,bufflim=this.clusterSize;             // max write size for one cluster
        int oldbuffpos=fileBuffer.position();
        int oldbufflim=fileBuffer.limit();
        int filePosition;
        for (int cluster:clusterchain)                      // cluster to write
        {
            fileBuffer.limit(bufflim);                      // position - limit = nbr of bytes read from buffer & written to drive
            fileBuffer.position(buffpos);                   // where to read bytes from
            filePosition=cluster*this.clusterSize;          // where we write them to
            this.totalFile.limit(filePosition+this.clusterSize);// max nbr of bytes to write
            this.totalFile.position(filePosition);
            this.totalFile.put(fileBuffer);
            {
                bufflim+=this.clusterSize;
                buffpos+=this.clusterSize;
                if (bufflim>fileBuffer.capacity())
                    bufflim=fileBuffer.capacity();
            }
        }
        fileBuffer.limit(oldbufflim);
        fileBuffer.position(oldbuffpos);
        this.totalFile.position(0);
        this.totalFile.limit(this.totalFile.capacity());
    }
    /**
     * This writes a part of byte buffer back to the drive.
     * 
     * @param fileBuffer the ByteBuffer containing the file. The capacity of the buffer may be bigger than the true filesize.
     * @param clusterchain the first cluster of the file.
     * @param start where in the buffer to start writing.
     * @param bytesToWrite how many bytes must be written.
     * 
     * @throws java.io.IOException  any i/o exception from the java i/o operations.
     * 
     */
    @Override
    public void writePartOfFile(java.nio.ByteBuffer fileBuffer,java.util.ArrayList<Integer> clusterchain,int start,int bytesToWrite) throws java.io.IOException
    {
        int filePosition;                                   // how many bytes were written
        int oldbuffpos=fileBuffer.position();
        int oldbufflim=fileBuffer.limit();
        int startCluster=start/this.clusterSize;
        int endCluster=(start+bytesToWrite)/this.clusterSize;
        for (int i=startCluster;i<endCluster+1;i++)         // cluster to write
        {
            int cluster=clusterchain.get(i);
            fileBuffer.limit((i+1)*this.clusterSize);                     
            fileBuffer.position(i*this.clusterSize);        
            filePosition=cluster*this.clusterSize;          // where we write them to
            this.totalFile.limit(filePosition+this.clusterSize);// max nbr of bytes to write
            this.totalFile.position(filePosition);
            this.totalFile.put(fileBuffer);
        }
        fileBuffer.limit(oldbufflim);
        fileBuffer.position(oldbuffpos);
        this.totalFile.position(0);
        this.totalFile.limit(this.totalFile.capacity());
    }
    
      /**
     * Reads bytes at a certain position in the drive to a buffer.
     * Used by the special file.
     * 
     * @param nbrOfBytes how many bytes to read.
     * @param buffer where to read the bytes to - this buffer must be at least nbrOfBytes long. The bytes are read to the start of the buffer.
     * @param position where to read from in the drive.
     * 
     * @return the number of bytes read, -1 if error.
     */
    @Override
    public int readBytes(int nbrOfBytes,java.nio.ByteBuffer buffer,long position)
    {
        if (buffer.capacity()<nbrOfBytes)
            return -1;
        try
        {
            buffer.limit(nbrOfBytes);
            buffer.position (0);
            this.totalFile.position((int) position);
            this.totalFile.limit((int) position + nbrOfBytes);// nbr of bytes to read
            buffer.put(this.totalFile);
            this.totalFile.limit(this.totalFile.capacity());
            this.totalFile.position(0);
            return nbrOfBytes;
        }
        catch (Exception e)
        {
            this.totalFile.position(0);
            this.totalFile.limit(this.totalFile.capacity());
            return -1;
        }
    }
    
    /**
     * Writes bytes from a certain position in a buffer to a certain position in the drive.
     * 
     * @param nbrOfBytes how many bytes to write.
     * @param buffer where to read the bytes to - this buffer must be at least nbrOfBytes long. The bytes are taken from the start of the buffer
     * @param position where to write to in the drive.
     * @param startInBuffer where in the buffer to start taking the bytes to wirite from.
     * 
     * @return the number of bytes read, -1 if error.
     */
    @Override
    public int writeBytes(int nbrOfBytes,java.nio.ByteBuffer buffer,long position,int startInBuffer)
    {
        if (buffer.capacity()<nbrOfBytes+startInBuffer)
            return -1;
        try
        {
            int bufpos=buffer.position();
            int buflim=buffer.limit();
            buffer.limit(nbrOfBytes+startInBuffer);
            buffer.position (startInBuffer);
            this.totalFile.limit((int) position + nbrOfBytes);// nbr of bytes to read
            this.totalFile.position((int) position);
            this.totalFile.put(buffer);
            this.totalFile.position(0);
            this.totalFile.limit(this.totalFile.capacity());
            buffer.limit(buflim);
            buffer.position (bufpos);
            return nbrOfBytes;
        }
        catch (Exception e)
        {
            return -1;
        }
    }
    
    /**
     * Writes the drive back to a native file
     * 
     * @param driveName the name of the native file.
     */
    public void writeBack(String driveName)
    {
        if (!this.isLocal)
            this.cpu.data_regs[0]=Types.ERR_RDO;
        else
        {
            try
            {
                java.io.File device=new java.io.File(driveName);// the native file used as qxl.win file
                this.raFile=new java.io.RandomAccessFile(device,"rw");// read/write access to this file
                this.ioChannel = raFile.getChannel();           // the filechannel
                this.totalFile.limit(this.totalFile.capacity());
                this.totalFile.position(0);
                this.ioChannel.write(this.totalFile);
                this.raFile.close();
                this.cpu.data_regs[0]=0;
                this.totalFile.position(0);
            }
            catch (java.io.FileNotFoundException e)
            {
                this.cpu.data_regs[0]=Types.ERR_FDNF;
            }
            catch (java.io.IOException e)
            {
                this.cpu.data_regs[0]=Types.ERR_RDO;
            }
        }
    }
}
