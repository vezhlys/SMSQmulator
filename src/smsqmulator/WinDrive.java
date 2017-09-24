
package smsqmulator;


/**
 * This class handles one "QXL.WIN" drive.
 * <p>
 * A windrive is just a file in the native file system, upon which a certain structure is superimposed.
 * It mimics a physical hard drive, so there is a "physical" structure and a "logical" structure.<p>
 * 
 * The physical structure is as follows:<p>
 * The basic subdivision of a drive is a <b>sector</b>. Each sector is 512 bytes long.
 * Sectors are numbered from 0 upwards. Sector 0 is at the very beginning of the drive, sector 1 is the
 * following sector and so on.<p>
 * Sectors are nominally grouped into tracks - each track may contain a certain number of sectors. The 
 * number of sectors in a track is supposed to remain constant across the entire drive.
 * Tracks are grouped in cylinders (normally platters or read/write heads). A drive may have several cylinders.
 * Since a qxl.win drive is NOT a real hard disk, these physical details may be neglected, except for sector size.
 * <p>
 * The logical structure is as follows:<p>
 * Sectors are grouped together in clusters, or "groups" as the SMSQ/E documentation calls them.
 * A cluster is a grouping of consecutive and contiguous sectors and is the minimum size any file may have.
 * The cluster size is not necessarily the same for all drives, it may vary from drive to drive - but does not vary within one drive.
 * A cluster may be composed of 2,4,8 etc sectors. The cluster size is given in the header of the drive.
 * <p>
 * Each drive has a header which is 64 bytes long and is located at the very beginning of the drive, i.e. in sector 0.
 * The header contains information about the structure of the disk. The exact content of the header
 * is described below in the data structures. Most of the the words in the header are unsigned values.
 * <p>
 * The header of the drive is immediately followed by the file allocation table (FAT) or "map" as the SMSQ/E documentation calls it.
 * A physical drive might have several FATs, each FAT corresponding to a partition, but a qxl.win drive does not contain several partitions.
 * <p>
 * The FAT is composed of unsigned words (2 bytes). There can thus only be 65536 entries in the FAT.
 * Each entry in the FAT corresponds to a cluster and thus to a specific location within the drive.
 * If the cluster size is 4 sectors, then entry 0 in the FAT corresponds to sectors 0,1,2 and 3 of the drive, entry 1 in the
 * FAT corresponds to sectors 4,5,6 and 7 etc...
 * If the cluster size is 2 sectors, then entry 0 in the FAT corresponds to sectors 0 and 1 of the drive, entry 1 in the
 * FAT corresponds to sectors 2 and 3 etc...
 * So the physical location of a cluster in a file is determined by cluster number * cluster size.
 * <p>
 * Since the size of one entry in the FAT is a word, and there can  thus only be a maximum of 65536 entries in the FAT, the size of a drive
 * is this directly proportional to the number of sectors per cluster.
 * <p>
 * Each FAT entry points to the next cluster FOR THE SAME FILE, or is 0 to mark that this cluster is the of the end of the file.
 * The first entries in the FAT are for the FAT itself.
 * After that, there is the first entry for the root directory of the drive.
 * <p>
 * Any actual reading/writing from/to the drive is done through THIS object.
 * <p>
 * Implementation details:<p>
 * 
 * SMSQ/E doesn't open "files". It opens channels to files. this mens that a ('physical') file on the drive may have several
 * channels open to it, provided they are all read only, or one single read/write channel.
 * 
 * This object maintains a Hashmap of WinFiles. When a channel is opened an entry for the corresponding file is created.
 * Each file that is opened on a drive is given a file number.
 * <p>
 * There is a maximum of WinDrive.MAX_FILES_OPEN files that may be open at any one time.
 * <p>
 * The drive is responsible for opening, closing and deleting files. The files themselves are responsible for the actual I/O INTO THEIR BUFFER.
 * However any actual reading/writing from/to the drive is done through THIS object.
 * 
 * Files, dirs, even the FAT, have a clusterchain : just an arraylist with all of the cluster numbers of the file/dir/FAT.
 * 
 * <ul>
 *  <li>There is one <code>ByteBuffer</code> holding the entire header + the FAT of the drive</li>
 *  <li>Other directories are also kept in ByteBuffers and put into a hashmap as and when they are read.
 * </ul>
 *  <p> Each file has a WinDriver.HEADER_LENGTH bytes long header.<p>
 * Each (sub)directory contains the entire name of the file (stripped of the device part, though).
 * <p>
 * Note :
 * As far as SMSQE is concerned, each access to a file on the drive will be atomic - smsqe is suspended until the trzp rturns.
 * <p>
 * On a QXL.Win drive the fileheaders of files themselves contain rubbish. The fileheader is maintained in the directory and, when SMSQE tries
 * to read the header, this is copied from the directory.
 * <p>
 * When a file is created, it is allocated one cluster. New clusters are not allocated as the file grows, only when the file is
 * closed/flushed. the drawback of this is that a file may grow enormously without error and, when it is written back, it can't be saved any more.
 * 
 * @author and copyright (c) wolfgang lenerz 2013
 * 
 * @version  
 * 1.09 readFat : if non standard drive, the warning wdw referred to the wrong drivename.
 * 1.08 openfile : if dir open for an inexisting dir, copy filename of underlying dir correctly
 * 1.07 readFat : unlockable files may be made read only.
 * 1.06 small optimisations, files aren't passed chan defn blk.
 * 1.05 opening as dir a file that isn't a dir, opens the dir the file is in ; rename checks whether file exists on destination (!).
 * 1.04 implement closeAllFiles.
 * 1.03 if so configured (in ini file), will now ignore lock errors for qxl.win file.
 * 1.02 changeParentDir added, for correctly setting new dir of open files if a dir was created and an open file moved into it.
 * 1.01 trap #4f : name is 0 filled, not space filled
 * 1.00 initial version
 */
public class WinDrive 
{
    /**
     * DATA STRUCTURES
     */
    
    /**
     * The structure of the drive header.
     * Most of the the words in the header are unsigned values.
     */
    public static final int QWA_IDEN = 0x0000;              // word  QXL.win file identifier : "QLWA"
    public static final int QWA_NAME = 0x0004;              // string  up to 20 characters name of drive right padded with spaces
    public static final int QWA_SPR0 = 0x001a;              // word  2 bytes spare - set to zero
    public static final int QWA_UCHK = 0x001c;              // long  update check (removable media only)
    public static final int QWA_INTL = 0x0020;              // word  interleave factor (0 SCSI) - ignored here
    public static final int QWA_SCTG = 0x0022;              // word  sectors per cluster (1 sector= 512 bytes)
    public static final int QWA_SCTT = 0x0024;              // word  sectors per track (0 SCSI) - ignored here
    public static final int QWA_TRKC = 0x0026;              // word  tracks per cylinder (number of heads) (0 SCSI) - ignored here
    public static final int QWA_CYLD = 0x0028;              // word  cylinders per drive - ignored here
    public static final int QWA_NGRP = 0x002a;              // word  total number of clusters in drive
    public static final int QWA_FGRP = 0x002c;              // word  number of free clusters
    public static final int QWA_SCTM = 0x002e;              // word  sectors per FAT
    public static final int QWA_NMAP = 0x0030;              // word  number of FATs - ignored here / always 1 (?)
    public static final int QWA_FREE = 0x0032;              // word  first free cluster
    public static final int QWA_ROOT = 0x0034;              // word  first cluster of root directory  
    public static final int QWA_RLEN = 0x0036;              // long  root directory length
    public static final int QWA_FCYL = 0x003a;              // word  first cylinder number (ST506) - ignored here
    public static final int QWA_FSCT = 0x003a;              // long  first sector for this partition (SCSI) - ignored here
    public static final int QWA_PARK = 0x003e;              // word  park cylinder -ignored jere
    public static final int QWA_GMAP = 0x0040;              // words start of FAT ("group map"): each entry is the number of the next cluster or zero
    
    /**
     * Array for converting QL chars into lower case see : util_cv_locas_asm
     */
    public static final byte[]LOWER_CASE=
    {
	0x00,0x01,0x02,0x03,0x04,0x05,0x06,0x07,
	0x08,0x09,0x0A,0x0B,0x0C,0x0D,0x0E,0x0F,
	0x10,0x11,0x12,0x13,0x14,0x15,0x16,0x17,
	0x18,0x19,0x1A,0x1B,0x1C,0x1D,0x1E,0x1F,
        
	0x20,0x21,0x22,0x23,0x24,0x25,0x26,0x27,
	0x28,0x29,0x2A,0x2B,0x2C,0x2D,0x2E,0x2F,
	0x30,0x31,0x32,0x33,0x34,0x35,0x36,0x37,
	0x38,0x39,0x3A,0x3B,0x3C,0x3D,0x3E,0x3F,
        
	0x40,0x61,0x62,0x63,0x64,0x65,0x66,0x67,
	0x68,0x69,0x6A,0x6B,0x6C,0x6D,0x6E,0x6F,
	0x70,0x71,0x72,0x73,0x74,0x75,0x76,0x77,
	0x78,0x79,0x7A,0x5B,0x5C,0x5D,0x5E,0x5F,
        
	0x60,0x61,0x62,0x63,0x64,0x65,0x66,0x67,
	0x68,0x69,0x6A,0x6B,0x6C,0x6D,0x6E,0x6F,
	0x70,0x71,0x72,0x73,0x74,0x75,0x76,0x77,
	0x78,0x79,0x7A,0x7B,0x7C,0x7D,0x7E,0x7F,
       
	(256-0x80)*-1,(256-0x81)*-1,(256-0x82)*-1,(256-0x83)*-1,(256-0x84)*-1,(256-0x85)*-1,(256-0x86)*-1,(256-0x87)*-1,
	(256-0x88)*-1,(256-0x89)*-1,(256-0x8A)*-1,(256-0x8B)*-1,(256-0x8C)*-1,(256-0x8D)*-1,(256-0x8E)*-1,(256-0x8F)*-1,
	(256-0x90)*-1,(256-0x91)*-1,(256-0x92)*-1,(256-0x93)*-1,(256-0x94)*-1,(256-0x95)*-1,(256-0x96)*-1,(256-0x97)*-1,
	(256-0x98)*-1,(256-0x99)*-1,(256-0x9A)*-1,(256-0x9B)*-1,(256-0x9C)*-1,(256-0x9D)*-1,(256-0x9E)*-1,(256-0x9F)*-1,
        
	(256-0x80)*-1,(256-0x81)*-1,(256-0x82)*-1,(256-0x83)*-1,(256-0x84)*-1,(256-0x85)*-1,(256-0x86)*-1,(256-0x87)*-1,
	(256-0x88)*-1,(256-0x89)*-1,(256-0x8A)*-1,(256-0x8B)*-1,(256-0xAC)*-1,(256-0xAD)*-1,(256-0xAE)*-1,(256-0xAF)*-1,
	(256-0xB0)*-1,(256-0xB1)*-1,(256-0xB2)*-1,(256-0xB3)*-1,(256-0xB4)*-1,(256-0xB5)*-1,(256-0xB6)*-1,(256-0xB7)*-1,
	(256-0xB8)*-1,(256-0xB9)*-1,(256-0xBA)*-1,(256-0xBB)*-1,(256-0xBC)*-1,(256-0xBD)*-1,(256-0xBE)*-1,(256-0xBF)*-1,
        
	(256-0xC0)*-1,(256-0xC1)*-1,(256-0xC2)*-1,(256-0xC3)*-1,(256-0xC4)*-1,(256-0xC5)*-1,(256-0xC6)*-1,(256-0xC7)*-1,
	(256-0xC8)*-1,(256-0xC9)*-1,(256-0xCA)*-1,(256-0xCB)*-1,(256-0xCC)*-1,(256-0xCD)*-1,(256-0xCE)*-1,(256-0xCF)*-1,
	(256-0xD0)*-1,(256-0xD1)*-1,(256-0xD2)*-1,(256-0xD3)*-1,(256-0xD4)*-1,(256-0xD5)*-1,(256-0xD6)*-1,(256-0xD7)*-1,
	(256-0xD8)*-1,(256-0xD9)*-1,(256-0xDA)*-1,(256-0xDB)*-1,(256-0xDC)*-1,(256-0xDD)*-1,(256-0xDE)*-1,(256-0xDF)*-1,
        
	(256-0xE0)*-1,(256-0xE1)*-1,(256-0xE2)*-1,(256-0xE3)*-1,(256-0xE4)*-1,(256-0xE5)*-1,(256-0xE6)*-1,(256-0xE7)*-1,
	(256-0xE8)*-1,(256-0xE9)*-1,(256-0xEA)*-1,(256-0xEB)*-1,(256-0xEC)*-1,(256-0xED)*-1,(256-0xEE)*-1,(256-0xEF)*-1,
	(256-0xF0)*-1,(256-0xF1)*-1,(256-0xF2)*-1,(256-0xF3)*-1,(256-0xF4)*-1,(256-0xF5)*-1,(256-0xF6)*-1,(256-0xF7)*-1,
	(256-0xF8)*-1,(256-0xF9)*-1,(256-0xFA)*-1,(256-0xFB)*-1,(256-0xFC)*-1,(256-0xFD)*-1,(256-0xFE)*-1,-1
    };
    
    public static final int MAX_FILES_OPEN = 1000;              // max nbr of files open on one drive
    protected smsqmulator.cpu.MC68000Cpu cpu;                   // the cpu used
    protected String driveName;                                 // the name of the qxl win file in the native file system
    protected WinDriver driver;                                 // the device driver object
    protected int driveNumber;                                  // my drive number as ASCII text
    protected Warnings warnings;                                // warning object (to determine behaviour in case of read/write errors
    protected java.io.RandomAccessFile raFile=null;             // every access to this drive goes through this file...
    protected java.nio.channels.FileChannel ioChannel;          // ... and its associated channel
    protected boolean readOnly=false;                           // will be true if this entire drive is read only
    protected java.nio.channels.FileLock flock;                 // file lock acquired on the native file
    protected int clusterSize;                                  // size of one cluster in bytes, not in sectors
    protected java.util.ArrayList <Integer> fatClusterChain;    // all of the clusters occupied by the FAT.
    protected java.nio.ByteBuffer driveFAT;                     // the FAT (+ header) of the drive
    protected WinDir mainDir;                                   // the root direcory
    protected WinFile[]fileNumber=new WinFile [WinDrive.MAX_FILES_OPEN];  // a primitive way to identify a file
    protected boolean specialFileIsOpen=false;                  // is true when the special access file is open - no other file may be open then.

    /**
     * DO NOT USE THIS FORM OF CREATING THE OBJECT.
     */
    public WinDrive() throws UnsupportedOperationException
    {
        throw new UnsupportedOperationException();
    }   
    
    /**
     * Create object with a CPU and warnings. Use only for memdrive
     * @param cpu the cpu to be used
     * @param warn the warning object with info about what warnings should be suppressed or not.
     */
    public WinDrive(smsqmulator.cpu.MC68000Cpu cpu,Warnings warn)
    {
        this.cpu=cpu;
        this.warnings=warn;
    }
    
    /**
     * Creates the object.
     * 
     * @param cpu the cpu used.
     * @param driveName the name of the qxl.win file on the native file system.
     * @param driver the driver having created this object.
     * @param number my driveNbr as ASCII!.
     * @param warn object with warning flags.
     * @param inifile the ".ini" file object.
     * 
     * @throws java.io.FileNotFoundException the qxl.win file isn't found
     * @throws java.io.IOException any IO exception reading the FAT / main dir
     * @throws ArrayIndexOutOfBoundsException a wrong FAT cluster number was given
     * @throws java.net.ProtocolException if the main dir couldn't be read.
     * @throws DoNothingException if this is a drive with a wrong FAT and user doesn't want me to (try to) fix it.
     * @throws IncorrectFiletypeException if this is not a valid qlwa file.
     * @throws UnsupportedOperationException couldn't acquire a file lock.
     */
    public WinDrive(smsqmulator.cpu.MC68000Cpu cpu,String driveName,WinDriver driver,int number,Warnings warn,inifile.IniFile inifile) 
           throws java.io.FileNotFoundException,java.io.IOException,ArrayIndexOutOfBoundsException,java.net.ProtocolException,
                  DoNothingException,IncorrectFiletypeException,UnsupportedOperationException
    {
        this.cpu=cpu; 
        this.driveName=driveName;
        this.driver=driver;
        this.driveNumber=number;
        this.warnings=warn;
        readFAT(this.driveName,inifile.getTrueOrFalse("IGNORE-QXLWIN-LOCK-ERROR"),inifile.getTrueOrFalse("MAKE-UNLOCKABLE-QXLWIN-READONLY"));// read the header and the FAT, will generate an exception if error
        readMainDir();                                      // read the main directory
        if (this.mainDir==null)
        {
            closeRAFile();
            throw new java.net.ProtocolException(Localization.Texts[56]+"\n("+driveName+")");
        }
    }
    
    /**
     * Reads the drive's header and FAT.
     * The FAT starts at offset 0x40 of the very first sector. So the fist cluster (cluster 0) already is part of the FAT.
     * 
     * @param nativeFilename the native name of the file that is the QXL.WIN type file.
     * @param ignoreLockError true if one should ignore the fact that a file lock can't be acquired.
     * @param  akeNonLockableReadOnly if if an unlockable file should be made read only.
     * 
     * @throws java.io.FileNotFoundException the qxl.win file isn't found.
     * @throws java.io.IOException any IO exception reading the map / main dir.
     * @throws ArrayIndexOutOfBoundsException a wrong map cluster number was given.
     * @throws java.net.ProtocolException if the file lock couldn't be acquired and wasn't ot be ignored..
     * @throws java.nio.channels.FileLockInterruptionException if file lock couldn't be acquired
     * @throws smsqmulator.DoNothingException badly constructed qxl.win drive
     * @throws IncorrectFiletypeException if this is not a valid qlwa file.
     * @throws IllegalStateException if the raFile isn't null when this object is created (how could that be?????).
     */
    private void readFAT(String nativeFilename,boolean ignoreLockError, boolean makeNonLockableReadOnly) 
                    throws java.io.FileNotFoundException,java.io.IOException,
                     ArrayIndexOutOfBoundsException,java.net.ProtocolException,UnsupportedOperationException,
                     DoNothingException,IncorrectFiletypeException,IllegalStateException
    {
        java.io.File device=new java.io.File(nativeFilename);// the native file used as qxl.win file
        if (!device.exists())
            throw new java.io.FileNotFoundException();      // it doesn't exist at all
        if (this.raFile!=null)                              // the random access file aleady exists
        {
            throw new IllegalStateException();              // (how could that be?????).
        }
        try
        {
            this.raFile=new java.io.RandomAccessFile(device,"rw");// read/write access to this file
        }
        catch (java.io.FileNotFoundException e)             // this is returned when I try to access a read/write file on a read only device
        {
             this.raFile=new java.io.RandomAccessFile(device,"r");// read only access to this file
             this.readOnly=true;
        }
        this.ioChannel = raFile.getChannel();               // the filechannel
        
        if (!this.readOnly)
        {
            try
            {
                this.flock=this.ioChannel.tryLock();            // try to lock this file
            }
            catch (Exception e)
            {
                if (!ignoreLockError)
                    throw new UnsupportedOperationException() ;
            }
        
            if (this.flock==null) 
            {
                if (!ignoreLockError)
                {
                    closeRAFile();
                    throw new java.net.ProtocolException(Localization.Texts[66]);
                }
                else
                {
                    if (makeNonLockableReadOnly)
                    {
                        this.readOnly=true;
                    }
                }
            }
        }
        java.nio.ByteBuffer firstSector=java.nio.ByteBuffer.allocate(512);
        int bytesRead=this.ioChannel.read(firstSector);     // read the first sector (512 bytes - NOT the first cluster) of the drive
        if (bytesRead!=512 || firstSector.getInt(0)!=WinDriver.QLWA)// I must have read exactly this amount and marker must check
        {
            closeRAFile();
            throw new IncorrectFiletypeException();         // if this isn't possible, this can't be a true QLWA file
        }
        this.clusterSize=512*(firstSector.getShort(WinDrive.QWA_SCTG)&0xffff); // size of cluster in bytes
        this.fatClusterChain= new java.util.ArrayList <Integer>();//cluster chain for FAT
        this.fatClusterChain.add(0);                        // first cluster of map is always cluster 0
        // now find out the cluster numbers for the FAT.
        // for convenience, we'll hold the entire FAT + header in memory - we first need to find out how many clusters the FAT occupies
        // the FATs clusterchain lies at the every beginnng of the FAT.
        int cluster=firstSector.getShort(WinDrive.QWA_GMAP)&0xffff;// next cluster for map
        try                                                 // we assume that the entire FAT clusterchain fits into the first sector...
        {
            while (cluster!=0)
            {       
                this.fatClusterChain.add(cluster);          // one more cluster
                cluster=firstSector.getShort(WinDrive.QWA_GMAP+cluster*2)&0xffff;//get pointer to next cluster
            }
        }
        catch (Exception e)  
        {
            closeRAFile();
            throw new ArrayIndexOutOfBoundsException();     // Huh????
        }
        // patch for badly constructed drives, like those built by qxltools. These drive do NOT have
        // cluster entries for the FAT(!!!!). They just indicate the number of sectors taken by the FAT.
        // I presume that it can then be presumed that the FAT's sectors are all contiguous and start at sector 0.
        int nbrOfSectorsInMap=firstSector.getShort(WinDrive.QWA_SCTM)&0xffff;// that many sectors are supposed to be in the fat
        int temp=nbrOfSectorsInMap/(firstSector.getShort(WinDrive.QWA_SCTG)&0xffff);
        if (nbrOfSectorsInMap % (firstSector.getShort(WinDrive.QWA_SCTG)&0xffff)!=0)
            temp++;                                         // these are the number of clusters needed to hold the map
        if (temp!=this.fatClusterChain.size())              // if they both agree, all is ok, I have a valid drive map
        {   
            if (this.warnings.warnIfQXLDriveNotCompliant)   // but here I don't
            {
                java.awt.Toolkit.getDefaultToolkit().beep();
                int reply=javax.swing.JOptionPane.showConfirmDialog
                (
                    null,
                    Localization.Texts[74]+this.driver.getName(this.driveNumber-48)+Localization.Texts[75]+"\n"+
                    Localization.Texts[76]+"\n"+
                    Localization.Texts[77]+"WIN"+(this.driveNumber-48)+"_ "+Localization.Texts[78]+"\n",
                    Localization.Texts[73],
                    javax.swing.JOptionPane.WARNING_MESSAGE,
                    javax.swing.JOptionPane.YES_NO_OPTION
                );
                if (reply!=javax.swing.JOptionPane.YES_OPTION) 
                {
                    closeRAFile();
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
        for (int i:fatClusterChain)
        {
            this.driveFAT.limit(this.driveFAT.position()+this.clusterSize);
            int filePosition=i*this.clusterSize;
            this.ioChannel.position(filePosition);          // position in file
            this.ioChannel.read(this.driveFAT);             // read (part of) the file 
        }   
    }
    
    /**
     * CLoses the random access file, ignoring all close errors.
     */
    private void closeRAFile()
    {
        if (this.raFile!=null)
        {
            try
            {
                raFile.close();                             // if I haven't, this cannot be a qxl.win file.
            }
            catch (Exception e)
            {
                /*nop*/
            }
        }
        this.raFile=null;
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
     * Opens a file.
     * 
     * @param devDriverLinkageBlock pointer to the device driver linkage block (a3).
     * @param channelDefinitionBlock pointer to the channel definition block (a0).
     * @param openType  which kind of open is requested? (0 - old exclusive, 1 old shared,2 new exclusive,3 new overwrite,4 open dir).
     * @param driveNumber which drive number the file is to be opened on.
     * @param filename the (SMSQE) name of the file to open.
     * @param uncased the lower cased (SMSQE) name of the file to open.
     * 
     * @return true if open went ok, false if not.
     * 
     * @see DeviceDriver DeviceDriver for more information on file open operations.
     */
    public boolean openFile(int devDriverLinkageBlock, int channelDefinitionBlock, int openType, int driveNumber, byte[] filename,byte[]uncased) 
    {
        if (this.readOnly && openType!=1 && openType!=4)
        {
            this.cpu.data_regs[0]= Types.ERR_RDO;
            return false;
        }
        if (this.specialFileIsOpen)                         // if the special file is open, I can't open anything else any more
        {   
            this.cpu.data_regs[0]= Types.ERR_FDIU;
            return false;
        }
        
        // find a free space for a file in the array
        int fileNbr;
        for (fileNbr=0;fileNbr< WinDrive.MAX_FILES_OPEN;fileNbr++)
        {
            if (this.fileNumber[fileNbr]==null)
                break;
        }
        if (fileNbr>WinDrive.MAX_FILES_OPEN-1)
        {
            this.cpu.data_regs[0]= Types.ERR_DRFL;          // no more space in the table, signal drive full (!)
            return false;
        }
        
        this.cpu.data_regs[0]= Types.ERR_ITNF;              // signal not found (YET)
        
        // first find the dir that file will belong to. 
        WinDir fdir;
        boolean []flags=new boolean[5];
        int entry;
        if (filename.length==0)                             // I'm trying to open the main dir
        {
            fdir=this.mainDir;
            entry=0;
            flags[0]=true;
            flags[1]=true;
            flags[2]=false;
            flags[3]=false;
        }
        else
        { 
            if (filename.length==4 && uncased[0]=='*')
            {
                if (uncased[1]=='d' && uncased[2]=='2' & uncased[3]=='d')
                {
                    if (openType<4)
                        return makeSpecialAccessFile(fileNbr,channelDefinitionBlock);
                    else
                    {
                        cpu.data_regs[0]=Types.ERR_IPAR;
                        return false;
                    }
                }
            }
            fdir=this.mainDir.findInDirs(uncased);          // find dir this file is/will be in.
            if (fdir==null)
            {
                return false;                               // what???
            }
            entry=fdir.checkFile(flags,uncased);            // check whether file exists etc  
        }
        
        /*    flags:      
         *      [0] true if file exists in this dir.
         *      [1] true if file is a dir.
         *      [2] true if file can't be opened for another channel.
         *      [3] true if file has channels open to it.
         */
        switch (openType)                                   // make sure that this type of open is possible for this file
        {
            case 0:                                         // open old exclusive (read/write)
                if (!flags[0])                              // but it doesn't exist
                {
                    return false;
                }
                if(flags[1] ||flags[2] || flags[3])         // file is dir or can't be opened for read/write
                {
                    this.cpu.data_regs[0]= Types.ERR_FDIU;
                    return false;
                }
                break;
                
            case 1:                                         // open an existing file for read only access
                if (!flags[0])                              // but it doesn't exist
                {
                    return false;
                }
                if(flags[2])                               // file can't be opened for read/write
                {
                    this.cpu.data_regs[0]= Types.ERR_FDIU;
                    return false;
                }
                break;
                
            case 2:                                         // open new exclusive for read/write
                if (flags[0])                               // but file aleady exists
                {
                    this.cpu.data_regs[0]= Types.ERR_FEX;   // signal this error
                    return false;
                } 
                if(flags[1] ||flags[2] || flags[3])         // file is dir or can't be opened for read/write
                {
                    this.cpu.data_regs[0]= Types.ERR_FDIU;
                    return false;
                }
                break;
                
            case 3:                                         // open new for overwrite
                if(flags[1] ||flags[2] || flags[3])         // file is dir or can't be opened for read/write
                {
                    this.cpu.data_regs[0]= Types.ERR_FDIU;
                    return false;
                }
                if (flags[0])                               // if file aleady exists,delete it
                {
                    int cluster=fdir.deleteFile(entry);    // delete file in dir if (entry<0)
                    if (cluster<0)
                    {
                        this.cpu.data_regs[0]= cluster;     // couldn't                 
                        return false;
                    }
                    freeClusters(cluster);
                    flush();
                }
                openType=2;                                 // make open key new exclusive
                break;
                
            case 4:                                         // open directory
                if (!flags[0] || !flags[1])                 // but it doesn't exist or isn't a dir : this is a special case : if a dir to be opened doesn't exist, use the one containing it!
                {                                           
                    filename=fdir.getName();
                    int tmp=filename.length;
                    this.cpu.writeMemoryWord(channelDefinitionBlock+0x32, tmp);
                    for (int i=0;i<tmp;i++)
                        this.cpu.writeMemoryByte(channelDefinitionBlock+0x34+i, filename[i]);
                    entry=fdir.getIndex();
                    fdir=fdir.getDir();
                    if (fdir==null)
                        fdir=this.mainDir;
                    flags[1]=true;
                } 
           
                if (flags[2])                               // file can't be opened for read/write
                {
                    this.cpu.data_regs[0]= Types.ERR_FDIU;
                    return false;
                }
                break;
                
            case 255:                                       // delete the file
                if (!flags[0])
                { 
                    this.cpu.data_regs[0]= 0;               // file doesn't exist, so it was deleted sucessfully
                    return true;
                    
                }
                if(flags[2] || flags[3] || filename.length==0)
                {
                    this.cpu.data_regs[0]= Types.ERR_FDIU;  // file is still in use
                    return false;
                }
                
                entry=fdir.deleteFile(entry);               // try to delete file in dir
                if (entry<0)
                {
                    this.cpu.data_regs[0]= entry;                   
                    return false;
                }
                freeClusters(entry);                        // free clusters
                flush();                                    // and write FAT back to drive
                this.cpu.data_regs[0]= 0;                   // file was deleted sucessfully
                return true;
        }
        
        // if I come here, the open type wished should be possible.
        WinFile wf=fdir.openFile(entry,filename,openType,this.cpu);
        if (wf!=null)
        {
            this.fileNumber[fileNbr]=wf;
            this.cpu.writeMemoryWord(channelDefinitionBlock+0x1e, fileNbr);
            this.cpu.writeMemoryWord(channelDefinitionBlock+0x5e, fileNbr);
            this.cpu.data_regs[0]=0;
            if (openType==4)
            {
              //  this.cpu.writeMemoryWord(channelDefinitionBlock+0x62, fileNbr);
                this.cpu.writeMemoryWord(channelDefinitionBlock+0x62, fileNbr);
            }
//            this.nbrOfOpenFiles++;
        }
        return true;
    }
    
    /**
     * Sets the length of the root directory in the appropriate place in the FAT.
     * It then writes the FAT to the disk.
     * 
     * @param fsize the length of the root directory, in bytes.
     */
    public void setRootDirLength (int fsize)
    {
        this.driveFAT.putInt(WinDrive.QWA_RLEN,fsize);
        flush();
    }
    
    /*********** ACTUAL READING/WRITING TO/FROM THE DRIVE ****************************/
     
    /**
     * This writes the FAT + drive header back to the drive.
     * 
     * @throws java.io.IOException 
     */
    private void writeFAT() throws java.io.IOException
    {
        writeFile(this.driveFAT,this.fatClusterChain);
     //   this.ioChannel.force(false);
        this.driveFAT.limit(this.driveFAT.capacity());
        this.driveFAT.position(0);
    }
    
    /**
     * Writes the FAT  + drive header to the drive.
     */
    public void flush()
    {
        try
        { 
            writeFAT();
        }
        catch (Exception e)
        {
            Helper.reportError(Localization.Texts[45], Localization.Texts[71], null,e);
        }
    }
   
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
        long filePosition;                                  // position in qxl.win file to read from
        try
        {
            do 
            {
                clusterchain.add(cluster);
                if (fileLength>this.clusterSize)
                    buffer.limit(bufferPosition+this.clusterSize);// end of where the bytes to read go - read cluster by cluster
                else
                    buffer.limit(bufferPosition+fileLength);// end of where the bytes to read go
                buffer.position(bufferPosition);            // start of where the bytes to read go
                filePosition=cluster*this.clusterSize;      // where the cluster lies in the QXL.Win file
                this.ioChannel.position(filePosition);      // position in file
                bytesRead=this.ioChannel.read(buffer);      // read (part of) the file
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
            return true;
        }
        catch (Exception e)
        {
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
    public void writeFile(java.nio.ByteBuffer fileBuffer,java.util.ArrayList<Integer> clusterchain) throws java.io.IOException
    {
        if (this.ioChannel==null)
            return;                                         // no iochannel? can't write!        
        int bytesWritten;                                   // how many bytes were written
        int buffpos=0;
        int bufflim=this.clusterSize;                       // max write size for one cluster
        int oldbuffpos=fileBuffer.position();
        int oldbufflim=fileBuffer.limit();
        
        for (int cluster:clusterchain)                      // cluster to write
        {
            fileBuffer.limit(bufflim);                      // position - limit = nbr of bytes read from buffer & written to drive
            fileBuffer.position(buffpos);                   // where to read bytes from
            this.ioChannel.position((long)(cluster*this.clusterSize));// where in file we start to write
            bytesWritten=this.ioChannel.write(fileBuffer);  // write that many bytes onto the disk
            if (bytesWritten!=this.clusterSize)             // not enough bytes written, stop writing, perhaps eof
                break;
            else
            {
                bufflim+=bytesWritten;
                buffpos+=bytesWritten;
                if (bufflim>fileBuffer.capacity())
                    bufflim=fileBuffer.capacity();
            }
        }
        fileBuffer.limit(oldbufflim);
        fileBuffer.position(oldbuffpos);
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
    public void writePartOfFile(java.nio.ByteBuffer fileBuffer,java.util.ArrayList<Integer> clusterchain,int start,int bytesToWrite) throws java.io.IOException
    {
        if (this.ioChannel==null)
            return;                                         // no iochannel? can't write!        
        int bytesWritten;                                   // how many bytes were written
        int oldbuffpos=fileBuffer.position();
        int oldbufflim=fileBuffer.limit();
        int startCluster=start/this.clusterSize;
        int endCluster=(start+bytesToWrite)/this.clusterSize;
        for (int i=startCluster;i<endCluster+1;i++)         // cluster to write
        {
            int cluster=clusterchain.get(i);
            fileBuffer.limit((i+1)*this.clusterSize);                     
            fileBuffer.position(i*this.clusterSize);                   
            this.ioChannel.position((long)(cluster*this.clusterSize));// where in file we start to write
            bytesWritten=this.ioChannel.write(fileBuffer);  // write that many bytes onto the disk
            if (bytesWritten!=this.clusterSize)             // not enough bytes written, stop writing, perhaps eof
                break;
        }
        fileBuffer.limit(oldbufflim);
        fileBuffer.position(oldbuffpos);
    }
    
    /**
     * This "increases" a file's buffer by multiples of the clustersize.
     * It actually just creates a new, bigger buffer and copies the contents of the old buffer over.
     * This also increases the clusterchain.
     * 
     * @param buffer the buffer to increase. The limit and position of the buffer MUST have been put at the correct values before calling this.
     *               (position should be 0, limit set to the fileSize incl the header.
     * @param bytesToAdd the number of bytes to add to the file.
     * @param clusterchain the clusterchain for this file. New clusters will be added at the end.
     * 
     * @return the new buffer or NULL if no more space on the drive.
     */
    public java.nio.ByteBuffer increaseCapacity(java.nio.ByteBuffer buffer,int bytesToAdd,java.util.ArrayList<Integer> clusterchain)
    {
        int additionalSize=nbrOfClusters(bytesToAdd);       // additional number of clusters needed
        if (!addClusters(additionalSize,clusterchain))      // add new clusters to clusterchain...
            return null;                                    // .. but couldn't
        additionalSize*=this.clusterSize;                   // additional number of bytes needed
        if (buffer!=null)
            additionalSize+=buffer.capacity();              // plus original number of bytes in original buffer = new size   
        java.nio.ByteBuffer buf=java.nio.ByteBuffer.allocate(additionalSize);// make new buffer of this new size
        
        if (buffer!=null && buf!=null)
        {
            int pos=buffer.position();
            buffer.position(0);
            buffer.limit(buffer.capacity());
            buf.put(buffer);                                 // copy existing content from old buffer to new one
            buf.position(pos);
        }
        return buf;
    }
    
    /**
     * If a file is truncated : delete the clusterchain it occupied and make a new one with the lesser size, unless it would still
     * occupy the same number of clusters (in which case nothing is done).
     * 
     * @param oldsize old size of the file (=current file size).
     * @param newsize the file size once the file is truncated.
     * @param currentClusterchain the file's cluster chain.
     * 
     * @return  the file's new clusterchain.
     */
    public java.util.ArrayList<Integer> truncateFile(int oldsize,int newsize,java.util.ArrayList<Integer> currentClusterchain)
    {
        if (nbrOfClusters(oldsize)==nbrOfClusters(newsize))
            return currentClusterchain;                     // old and new sizes need the same number of clusters - nothing to do
        freeClusters(currentClusterchain.get(0));
        return allocateClusters(newsize);
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
    public int readBytes(int nbrOfBytes,java.nio.ByteBuffer buffer,long position)
    {
        if (buffer.capacity()<nbrOfBytes)
            return -1;
        try
        {
            buffer.limit(nbrOfBytes);
            buffer.position (0);
            return this.ioChannel.read(buffer,position);
        }
        catch (Exception e)
        {
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
            int bytesWritten= this.ioChannel.write(buffer,position);
            buffer.limit(buflim);
            buffer.position (bufpos);
            return bytesWritten;
        }
        catch (Exception e)
        {
            return -1;
        }
    }
    
    /****************************************   CLUSTER MANIPULATION ***************************************/
    
    
    /**
     * Adds one cluster to this clusterchain.
     * This marks everything necessary in the FAT but does NOT save the FAT back to the drive.
     * 
     * @param clusterchain the clusterchain where to add a cluster
     * 
     * @return true if operation successful, else false.
     */
    private boolean addCluster(java.util.ArrayList<Integer> clusterchain)
    {
        int cluster=clusterchain.get(clusterchain.size()-1);// get last cluster of clusterchain.
        if (this.driveFAT.getShort(WinDrive.QWA_GMAP+cluster*2)!=0)// check that this is inded the last cluster
            return false;                                   // there is smmething very wrong here!
        short fcluster=this.driveFAT.getShort(WinDrive.QWA_FGRP);// nbr of clusters still free on drive
        if (fcluster==0)
            return false;                                   // oops, nothing free on drive
        int freecluster=this.driveFAT.getShort(WinDrive.QWA_FREE)&0xffff;// pointer to 1rst free cluster, will become cluster I add to mY clusterchain
        if (freecluster==0)
            return false;                                   // there is no free cluster
        short newfirst=this.driveFAT.getShort(WinDrive.QWA_GMAP+freecluster*2);// pointer to next free cluster =  new first free cluster
        this.driveFAT.putShort(WinDrive.QWA_FREE,newfirst);          // set new first free cluster
        this.driveFAT.putShort(WinDrive.QWA_GMAP+cluster*2,(short)(freecluster&0xffff));// new last cluster in the clusterchain
        this.driveFAT.putShort(WinDrive.QWA_GMAP+freecluster*2,(short)0);// there is no next cluster in the clusterchain
        this.driveFAT.putShort(WinDrive.QWA_FGRP,--fcluster);        // new number of free clusters
        clusterchain.add(freecluster);                      // last element of clusterchain now
        return true;
    }
    
    /**
     * Adds a certain number of free clusters to an existing clusterchain.
     * The clusters are then occupied (no longer marked as free).
     * 
     * @param clustersNeeded number of free clusters needed.
     * @param currentClusterchain the clusterchain to which free clusters are to be added.
     * 
     * @return true if OK, false if there weren't enough free clusters. In that case, no new clusters are occupied. at all
     */
    private boolean addClusters (int clustersNeeded,java.util.ArrayList<Integer> currentClusterchain)
    {
        if ((this.driveFAT.getShort(WinDrive.QWA_FGRP)&0xffff)<clustersNeeded)
            return false;                                   // there are not enough free clusters
        for (int i=0;i<clustersNeeded;i++)
            addCluster(currentClusterchain);                // add the clusters
     //   flush();                                            // flush fat
        return true;
    }
    
    /**
     * This allocates a new clusterchain with as many clusters as needed.
     * The newly occupied clusters are marked as occupied in the FAT, but the FAT is not written to the disk.
     * 
     * @param clustersNeeded the number of clusters needed
     * 
     * @return the new clusterchain, or null if there weren't enough free clusters.
     */
    public java.util.ArrayList<Integer> allocateClusters(int clustersNeeded)
    { 
        int fcluster=this.driveFAT.getShort(WinDrive.QWA_FGRP)&0xffff;// nbr of clusters still free on drive
        if (fcluster < clustersNeeded)
            return null;                                   // there are not enough free clusters
        int freecluster=this.driveFAT.getShort(WinDrive.QWA_FREE)&0xffff;// pointer to 1rst free cluster, will become cluster I add to my clusterchain
        if (freecluster==0)
            return null;                                   // there is no free cluster, this would be a serious error and indicate a corrupted FAT/hreader
        short newfirst=this.driveFAT.getShort(WinDrive.QWA_GMAP+freecluster*2);// pointer to next free cluster =  new first free cluster
        this.driveFAT.putShort(WinDrive.QWA_FREE,newfirst);  // set new first free cluster in FAT
        fcluster--;
        this.driveFAT.putShort(WinDrive.QWA_FGRP,(short)(fcluster & 0xffff));// set new number of free clusters in FAT
        this.driveFAT.putShort(WinDrive.QWA_GMAP+freecluster*2,(short)0);// there is no next cluster in the new clusterchain
        java.util.ArrayList<Integer>clusterchain=new java.util.ArrayList<>();
        clusterchain.add(freecluster);                      // set the first element of the new clusterchain
        clustersNeeded--;                                   // one less cluster needed 
        if (clustersNeeded!=0)
            addClusters(clustersNeeded,clusterchain);       // add as many clusters as needed
        if (clusterchain.size()!=(clustersNeeded+1))        // check that clusters were indeed added
        {
            freeClusters(clusterchain.get(0));              // oops!
            return null;                                    // error
        }
        return clusterchain;                                // all went OK.
    }
    
    
    /**
     * Frees clusters in the FAT, freeing all clusters in a clusterchain.
     * The new number of free clusters IS set in the FAT.
     * The newly freed clusters are added to the free clusters list. 
     * The modified FAT is not written back to the drive here.
     * 
     * @param cluster the first cluster to be freed, all other clusters this cluster refers to will also be freed.
     */
    public void freeClusters(int cluster)
    {
        int count=0;                                        // at least one more free cluster : the one we start with
        int oldcluster;
        do
        {   
            oldcluster=cluster;                             // cluster currently being unlinked
            cluster=this.driveFAT.getShort(cluster*2+WinDrive.QWA_GMAP)&0xffff;// next cluster in list to be unlinked on next run through loop
            if (cluster==0)
                cluster=0;
            freeOneCluster(oldcluster);                     // but free current cluster first
            count++;
        } while(cluster!=0);
        
        count+=this.driveFAT.getShort(WinDrive.QWA_FGRP)&0xffff;// nbr of old free clusters + new free clusters = tot nbr of free clusters
        this.driveFAT.putShort(WinDrive.QWA_FGRP, (short)count);// put it in
    //    flush();
    }
    
    /**
     * This frees one cluster, making sure that it is inserted in the correct place in the free cluster chain.
     * SMSQE wants the free cluster list to be sorted in increasing order.
     * The FAT is not written back to the drive here.
     * 
     * @param cluster the cluster to be freed.
     */
    private void freeOneCluster(int cluster)
    {
        int oldFirst=this.driveFAT.getShort(WinDrive.QWA_FREE)&0xffff;   // first free cluster
        if (oldFirst==0)                                    // there is none, cluster in chain become the first next cluster                                                                              
        { 
            this.driveFAT.putShort(WinDrive.QWA_FREE,(short)(cluster&0xffff));
            this.driveFAT.putShort(cluster*2+WinDrive.QWA_GMAP,(short)0);   // there is no other free space pointer atter that
            return;
        }
        if (oldFirst>cluster)                               // cluster to free is smaller, so make that the first free cluster
        {
            this.driveFAT.putShort(WinDrive.QWA_FREE,(short)(cluster&0xffff));
            this.driveFAT.putShort(cluster*2+WinDrive.QWA_GMAP,(short)(oldFirst&0xffff));
            return;
        }
        int oldclust=oldFirst;
        while(oldFirst<cluster && oldFirst!=0)
        {
            oldclust=oldFirst;                                                      
            oldFirst=this.driveFAT.getShort(oldFirst*2+WinDrive.QWA_GMAP)&0xffff;
        }
        // here : oldclust = cluster before my cluster, oldfirst=cluster after my cluster
         this.driveFAT.putShort(oldclust*2+WinDrive.QWA_GMAP,(short)(cluster&0xffff));// cluster before points to me
         this.driveFAT.putShort(cluster*2+WinDrive.QWA_GMAP,(short)(oldFirst&0xffff));// I point to cluster after me
    }
    
    /**
     * Calculates how many clusters are needed for a certain file size.
     * 
     * @param size the file size in bytes.
     * 
     * @return the number of clusters needed.
     */
    private int nbrOfClusters(int size)
    {   
        int additionalSize=size/this.clusterSize;    
        if ((size%this.clusterSize)!=0)
            additionalSize++;                               // number of clusters needed
        return additionalSize;
    }
    
    /*************************************************************************/
    
    /**
     * Gets an int from the FAT or header.
     * 
     * @param index where to read in the map.
     * 
     * @return the int
     */
    public int getIntFromFAT(int index)
    {
        return this.driveFAT.getInt(index);
    }
    
    
    /**
     * This creates a new array of bytes, converting the case of the input array to SMSQE lower case.
     * 
     * @param in the array with the bytes to convert.
     * 
     * @return the new array with lower cased bytes.
     */
    public static final byte[] convert2SMSQELowerCase(byte[] in)
    {
        byte[] out = new byte[in.length];
        for (int i=0;i<in.length;i++)
        {
            int p=in[i]&0xff;
            out[i]=WinDrive.LOWER_CASE[p];
        }
        return out;
    }
    
    /**
     * Unuses (frees) the locked channel.
     */
    public void unuse()
    {
        try
        {
            this.ioChannel.force(true);
            this.raFile.close();
        }
        catch (Exception e)
        {
            /*NOP*/
        }
        this.cpu=null; 
        this.driveName=null;
        this.driver=null;
        this.warnings=null;
    }
    
    /**
     * Gets the name of the native file containing this drive.
     * 
     * @return the name of the native file containing this drive.
     */
    public String getDrivename()
    {
        return this.driveName;
    }
    
    /**
     * Checks whether the drive is read only.
     * 
     * @return true if drive is read only, else false.
     */
    public boolean isReadOnly()
    {
        return this.readOnly;
    }
    
    
    /**
     * Handles trap#3 calls.
     * 
     * @param driveNumber number of drive on which to open the file
     * @param trapKey what kind of trap #3 is it?
     * @param fileNbr the file number given by the ddd when file was opened (A0+0x1e)
     */
    public void trap3OK(int driveNumber,int trapKey, int fileNbr) 
    {
        if (this.readOnly)
        {
            switch (trapKey)
            {
                case 0x05:
                case 0x07:
                case 0x46:
                case 0x49:
                case 0x4a:
                case 0x4b:
                case 0x4c:
                case 0x4d:
                case 0x4e:                                  // io traps that require writing to the disk - not permitted
                    this.cpu.data_regs[0]=Types.ERR_RDO;
                    return;
                    
                case 0x41:                                  // flush file,  - let's say that this always succeeds on read only devices....
                    this.cpu.data_regs[0]=0;
                    return;
            }
        }
        
        int A1;
        WinFile nFile;
        switch (trapKey)
        {
           case 0x45:                                       // get (FAKE) info about medium
                A1 = this.cpu.addr_regs[1];
                this.cpu.writeSmsqeString(A1, "WIN DRIVER",false,-1);
                this.cpu.addr_regs[1]=A1+10;
                this.cpu.data_regs[1]=0x01001000; // This is totally fake info!
                this.cpu.data_regs[0]=0;                    // success.
                break;
               
           case 0x4a:                                       // rename file   
                A1=this.cpu.addr_regs[1];
                this.cpu.data_regs[0]=Types.ERR_ITNF;       // preset error
                int device=this.cpu.readMemoryLong(A1+2);
                if ((device&0xff)!=this.driveNumber)        // must be for this drive
                    return;
                device=device &0x5f5f5f00 + 0x30;
                if (device!=this.driver.getDeviceID() && device!=this.driver.getUsage())//  upper cased device name and '0' at end
                    return;                                 // premature exit with error
                if ( fileNbr>WinDrive.MAX_FILES_OPEN )      // ???? this file can't be open here
                {
                    this.cpu.data_regs[0]=Types.ERR_IPAR;
                    return;
                }
                device=this.cpu.readMemoryWord(A1)&0xffff;  // length of file name
                if (device<6 || device >40)
                    return;                                 // must be at least 6 chars long (device + underscore + 1 char for name at least ), not more that 40 long
                nFile=this.fileNumber[fileNbr];             // the file to rename
                if (nFile==null)                            // it doesn't exist?
                {
                    this.cpu.data_regs[0]=Types.ERR_FDNF;
                    return;
                }
                if (nFile.isDir)                            // a directory can't be renamed
                {
                    this.cpu.data_regs[0]=Types.ERR_RDO;
                    return;
                }
                
                byte [] nameUncased=new byte[device-5];
                byte [] name=new byte[device-5];
                int t;
                for (int i=0;i<device-5;i++)
                {
                    t= this.cpu.readMemoryByte(A1+7+i)&0xff;// +7 : skip over device & underscore and name length
                    name[i]=(byte) t;
                    nameUncased[i]=WinDrive.LOWER_CASE[t];  // lower cased name now
                } 
                WinDir newDir=this.mainDir.findInDirs(nameUncased);  // this will be the dir used by the renamed file
                if (newDir==null)
                {
                    this.cpu.data_regs[0]=Types.ERR_FDNF;
                    return;
                }
                boolean []flags=new boolean[6];
                newDir.checkFile(flags, nameUncased);
                if (flags[0])
                {
                    this.cpu.data_regs[0]=Types.ERR_FEX;
                    return;
                }
                WinDir thisDir=nFile.getDir();              // dir currently holding the file
                this.cpu.data_regs[0]=0;                    // as of here, let's presume the rename goes OK
                newDir.renameFile(thisDir,nFile,name);      // rename the file in the new dir
                break;
               
            case 0x4f:                                      //  get extended info  
                A1 = this.cpu.addr_regs[1];                 // pointer to block where extended info will be written
                int nlen=2+this.driveFAT.getShort(WinDrive.QWA_NAME)&0xffff;
                this.cpu.readFromBuffer(A1, nlen,this.driveFAT, 4);// name of device
                for (int i=nlen;i<22;i++)
                    this.cpu.writeMemoryByte(A1+i, 0);
                this.cpu.writeSmsqeString(A1+0x16, "WIN",-1);
                this.cpu.writeMemoryWord(A1+0x1c, driveNumber+1<<8);   // drive number + medium is read/write
                this.cpu.writeMemoryWord(A1+0x1e, this.clusterSize); // allocation unit size
                this.cpu.writeMemoryLong(A1+0x20, this.driveFAT.getShort(WinDrive.QWA_NGRP)&0xffff);       // total size
                this.cpu.writeMemoryLong(A1+0x24, this.driveFAT.getShort(WinDrive.QWA_FGRP)&0xffff);        // free size
                this.cpu.writeMemoryLong(A1+0x28, WinDriver.HEADER_LENGTH); // file header length
                this.cpu.writeMemoryWord(A1+0x2c, 0x0102);      // format
                this.cpu.writeMemoryWord(A1+0x2e, 0x0102);      // density/medium type
                this.cpu.writeMemoryLong(A1+0x30, 0x00ffffff);
                this.cpu.writeMemoryLong(A1+0x34, 0xffffffff);  
                this.cpu.writeMemoryLong(A1+0x38, 0xffffffff);  
                this.cpu.writeMemoryLong(A1+0x3c, 0xffffffff);     
                this.cpu.data_regs[0]=0;
                break;
                
            default:  
                nFile=this.fileNumber[fileNbr];
                if (nFile==null)
                    this.cpu.data_regs[0]=Types.ERR_FDNF;
                else
                    nFile.handleTrap3(trapKey,this.cpu);
                break;
        }
    }  
    
     /**
     * Sets the cpu for this Object.
     * 
     * @param cpu the cpu to be set.
     */
    public void setCpu(smsqmulator.cpu.MC68000Cpu cpu)
    {
        this.cpu=cpu;
    }
    
    /**
     * Closes a file.
     * 
     * @param fileNbr the number of the file to close.
     * 
     * @return <code>true</code> if closed ok, else <code>false</code>.
     */
    public boolean closeFile(int fileNbr)
    {
        WinFile winFile=this.fileNumber[fileNbr];
        if (winFile==null)
        {
            this.cpu.data_regs[0]=0;                        // this channel wasn't open, or file was already deleted, so close went ok
            return false;
        }
        winFile.close();                                    // call file close operation, may flush file, dir and FAT to disk
        this.fileNumber[fileNbr]=null;
        this.cpu.data_regs[0]=0;
        return true;
    }
    
   /************************************ Special access file handling *******************************/
    
    /**
     * This makes the special access file (devx_*d2d) for this drive
     * 
     * @param fileNbr the file number for this file.
     * 
     * @param channelDefinitionBlock 
     */
    private boolean makeSpecialAccessFile(int fileNbr,int channelDefinitionBlock)
    {
        int nbrOpen=cpu.readMemoryByte(cpu.addr_regs[1]+0x22);
        if (nbrOpen!=0)                                     // I can only open the special file if nothing else is open
        {
            this.cpu.data_regs[0]=Types.ERR_FDIU; 
            return false;
        }
        WinFile special=new WinSpecialFile(this); 
        this.fileNumber[fileNbr]=special;
        this.cpu.writeMemoryWord(channelDefinitionBlock+0x1e, fileNbr);
        this.cpu.writeMemoryWord(channelDefinitionBlock+0x5e, fileNbr);
        this.cpu.data_regs[0]=0;                            // show that open went OK
        this.specialFileIsOpen=true;
        return true;
    }
    
    /**
     * Marks special file as closed.
     */
    public void specialFileClosed()
    {
        this.specialFileIsOpen=false;
    }
    
   /**
     * Checks that the special file doesn't go beyond the end of the allowed sectors.
     * 
     * @param sector sector to check for EOF.
     * 
     * @return either this sector, or last allowed sector.
     */
    public int checkSector(int sector)
    {
        int nbrsect=this.driveFAT.getShort(WinDrive.QWA_SCTG)&0xffff;// nrb of secors par cluster
        int maxclust=this.driveFAT.getShort(WinDrive.QWA_NGRP)&0xffff;// nbr of clusters on drive
        int myclust=sector/nbrsect;                         // cluster this sector would be in.
        if (myclust>maxclust)
            return maxclust*nbrsect -1;
        return sector;
    }
 
    /**
     * Gets the size, in bytes, of one cluster on the drive.
     * 
     * @return the size, in bytes, of one cluster on the drive.
     */
    public int getClusterSize()
    {
        return this.clusterSize;
    }
    
    /**
     * Changes the parent dir of a file to the new dir.
     * 
     * @param oldDir the old parent dir
     * @param oldEntry where in the old dir the file is  : this identifies the file to treat
     * @param newDir the new parent dir
     * @param newEntry the new entry in that dir
     */
    public void changeParentDir(WinDir oldDir, int oldEntry,WinDir newDir, int newEntry)
    {
        for (WinFile f : this.fileNumber)
        {
            if ( f!=null && f.getDir()==oldDir && f.getIndex() == oldEntry)
            {
                f.setDir(newDir);
                f.setIndex(newEntry);
            }
        }
    }
    
    /**
     * "Closes" all files by just forgetting about them.
     */
    public void closeAllFiles()
    {
        this.fileNumber=new WinFile [WinDrive.MAX_FILES_OPEN];
        readMainDir();
    }
}
