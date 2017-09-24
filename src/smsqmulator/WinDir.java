
package smsqmulator;

/**
 * This is an object containing one directory within a qxl.win drive.
 * 
 * A directory is just a file containing "entries" for files.
 * Each entry in the directory is <code>WinDriver.HEADER_LENGTH</code> bytes long. The first entry in the directory contains rubbish,
 * as that would be the file header of this directory (file).
 * <P> The content (structure) of the file entry/header is explained below but it contains at least the length of the file and its name.
 * The length of the file in the directory entry comprises that of the file header.
 * <p>
 * In a directory entry, if a file has a length of 0, then this is an empty entry in the directory, since a valid file always has a length of, at least, 
 * <code>WinDriver.HEADER_LENGTH</code> bytes (length of the header).
 * When the fileheader is returned to SMSQE, <code>WinDriver.HEADER_LENGTH</code> bytes are deducted from the length of the file as contained in the 
 * directory header to make the "real" length of the file as SMSQE sees it.
 * <p>
 * Creating a directory should go as follows : open file with opentype #2. call trap #3 with $4d, close file.
 * <p>
 * Directories may grow, but they never shrink (unless, if entirely empty, they are totally deleted).

 * @author and copyright (c) wolfgang lenerz 2013 -2015
 * 
 * @version
 * 1.05 deleteFile: if file to be deleted is a subdir of mine, and if it is deleted, rebuid subdir list.
 * 1.04 checkForFile, findInDirs, optimized ; fileIsDir uses better index, openFile sets error in D0, setFileHeader sets the length passed to it.
 * 1.03 correct handling when a subdir is created and files should be moved into it.
 * 1.02 sets date of newly created dir to 0 ; writes out dir and Fat after file creation, deletion.
 * 1.01 correctly copy files from old dir to new dir when new dir is created in the old dir and old dir contained files that must now be in new dir.
 * 1.00 initial version
 */
public class WinDir extends WinFile
{
    /*
     * Structure of a QLX.WIN file header in a directory entry
     */
    public  static final int HDR_FLEN = 0x00;               // long    File LENgth
    public  static final int HDR_ACCS = 0x04;               // byte    access control byte
    public  static final int HDR_TYPE = 0x05;               // byte    file TYPE
    public  static final int hdrtExe = 1  ;                 //          executable
    public  static final int hdrtRel = 2  ;                 //          relocatable
    public  static final int hdrtLdr = 3  ;                 //          loader re-locatable file
    public  static final int hdrtDir = 0xff ;               //          directory
    public  static final int hdr_info = 0x06;               // 8*bytes additional information
    public  static final int hdr_data = 0x06;               // long    program DATA space
    public  static final int hdr_xtra = 0x0a;               // long    extra info
    public  static final int HDR_NAME = 0x0e;               // string  file name length word(up to 36 characters long)
    
    public  static final int HDRNAMEL= 0x0f;                // byte length of file up to 36 characters long)
    public  static final int HDRNAME = 0x10;                // string  file name chars (up to 36 characters)
    
    public  static final int HDRNMLN = 36 ;                 //  max name length (excludes the Device part)
    public  static final int HDR_DATE = 0x34;               // long    update date
    public  static final int HDR_VERS = 0x38;               // word    version number
    public  static final int HDR_FLID = 0x3a;               // word    File ID = 1rst cluster for file if this is a dir
    public  static final int HDR_BKUP = 0x3c;               // long    backup date
    public  static final int hdr_end  = 0x40;               // end of header
    public  static final int hdrSet  = 0x0e;                // length of header set
    public  static final int HDRLEN  = 0x40;                // length of header

    private int[]openChannels;                              // contains the number of channels open to a file
    private final java.util.BitSet fileAccess;              // is access to a file forbidden - the bit is set only if file has a read/write channel open to it. This bitset grows a s is necessary.
    private final java.util.ArrayList<WinDir>dirList =new java.util.ArrayList<>();
    private boolean listDone=false;                         // become true once this dir has read all of its subdirs
    private byte[] name;                                    // the name of this subdir
    private byte[] normalizedName;                          // and the name in SMSQE lower case
    
    /**
     * This creates the directory object.
     * 
     * @param drive the drive on which this dir lies.
     * @param parentDir the dir containing this dir, will be null if this is the root directory.
     * @param index index into the parent dir (offset from start of parent dir buffer to my header). Will be 0 isf this is the root directory.
     * @param buf the ByteBuffer containg this entire dir.
     * @param cchain the clusterchain for this directory.
     * @param name the name for this dir.
     * @param normalizedName the name of this dir in SMSQE lower case.
     */
    public WinDir (WinDrive drive,WinDir parentDir, int index,java.nio.ByteBuffer buf,java.util.ArrayList<Integer> cchain,
                   byte[] name,byte[] normalizedName)
    {
        super (drive,parentDir,index,true,buf,cchain);
        if (this.dir==null)
            this.fileSize=this.drive.getIntFromFAT(WinDrive.QWA_RLEN);
        this.isDir=true;
        this.name=name;
        this.normalizedName=normalizedName;
        if (this.normalizedName==null)
            this.normalizedName=new byte[0];
        if (this.name==null)
            this.name=new byte[0];
        this.openChannels=new int[this.buffer.capacity()/WinDriver.HEADER_LENGTH];// there can be that many files in the buffer...
                            //...each arry element hold nbr of channels open to the file at that index in the buffer
        this.fileAccess=new java.util.BitSet(this.buffer.capacity()/WinDriver.HEADER_LENGTH);// one file access element per possible file
        
      //  makeDirList();                                  // make sure the list of subdirs is done
    }
    
    /************************* File management : file opening/closing/deletion/renaming/making into a subdir  **********************************/
     /**
     * Opens a file.
     * The caller has checked that the type of open requested is possible.
     * 
     * @param entry the index into the buffer of the file to open - 0 if the file doesn't exist yet.
     * @param filename the name of the file as it appears in the open call. The length will be 0 for the main directory.
     * @param openType the type of open wished.
     * @param cpu the cpu to be used.
     * 
     * @return  the file if file open went ok, else null.
     */
    public WinFile openFile(int entry,byte[]filename,int openType,smsqmulator.cpu.MC68000Cpu cpu)
    {
        int eindex=entry/WinDriver.HEADER_LENGTH;            // index into arrays
        switch (openType)
        {
            case 0:                                         // open old exclusive (read/write)
                java.util.ArrayList<Integer>cchain=new java.util.ArrayList<>();
                java.nio.ByteBuffer buf = this.drive.readFile(this.buffer.getInt(entry), this.buffer.getShort(entry+WinDir.HDR_FLID)&0xffff, cchain);
                WinFile wf = new WinFile(this.drive,this,entry,false,buf,cchain);  
                this.openChannels[eindex]++;                // show one more channel open to that file
                this.fileAccess.set(eindex);                // channel has read/write access now
                return wf;
               // break;
            case 1:                                         // open existing file for read only
            case 4:                                         // open a directory
                if (filename== null || filename.length==0)  // I'm trying to open the main dir
                {
                    cchain = (java.util.ArrayList<Integer>)(this.clusterchain.clone());// in that case this  MUST be the main dir.
                    buf=java.nio.ByteBuffer.allocate(this.buffer.capacity());
                    this.buffer.position(0);
                    buf.put(this.buffer);
                    this.buffer.position(this.filePosition);
                    openType=4;                             // always open a directory
                    wf = new WinFile(this.drive,null,0,true,buf,cchain); 
                }
                else
                {
                    cchain=new java.util.ArrayList<Integer>();
                    buf = this.drive.readFile(this.buffer.getInt(entry), this.buffer.getShort(entry+WinDir.HDR_FLID)&0xffff, cchain);
                    wf = new WinFile(this.drive,this,entry,true,buf,cchain);  
                }
                if (openType==4)
                    wf.setDirStatus(true);
                this.openChannels[eindex]++;                // show one more channel open to that file
                return wf;   
            case 3:                                         // this case should never happen!!!
            case 2:                                         // open new non existing file - in this case entry is bogus
                // first find free entry in this dir
                entry=findFreeEntryInDir();
                if (entry==-1)
                {
                    cpu.data_regs[0]= Types.ERR_DRFL;           // no newentry possible : drive must be full
                    return null;
                }
                cchain=this.drive.allocateClusters(1);          // make new clusterchain
                if (cchain==null)
                {
                    cpu.data_regs[0]= Types.ERR_DRFL;
                    return null;
                }
                buf = java.nio.ByteBuffer.allocate(this.drive.getClusterSize());
                if (buf==null)
                {
                    cpu.data_regs[0]= Types.ERR_IMEM;
                    return null;
                }
                
              //  makeNewEntry(entry,filename);      
                
          //     
                
                this.buffer.putInt(entry,WinDriver.HEADER_LENGTH);
                for (int indexx=entry+4 ;indexx<entry+WinDriver.HEADER_LENGTH;indexx+=4)
                {
                    this.buffer.putInt(indexx,0);               // make sure entry is zeroed out
                }
                this.buffer.put(entry+WinDir.HDRNAMEL,(byte)filename.length);
                int i=0;
                int p = entry+WinDir.HDRNAME;
                if (this.dir!=null)
                {
                    for (;i<this.name.length;i++)
                    {
                        this.buffer.put(p+i,this.name[i]);// copy the part the name of my subdir.
                    }
                }
                
                for (;i<filename.length;i++)
                {
                    this.buffer.put(p+i,filename[i]);// copy rest of name
                }
              
            //    
                
                
                this.buffer.putShort(entry+WinDir.HDR_FLID,(short)(cchain.get(0)&0xffff));// put in first cluster
                wf= new WinFile(this.drive,this,entry,false,buf,cchain);  
                wf.setDirAndFatChanged();
              //  this.writeFile(entry, WinDir.HDRLEN);           // write out 
         /*       try
                {
                    this.drive.writePartOfFile(this.buffer, this.clusterchain,entry,WinDir.HDRLEN);
                }
                catch (Exception e)
                {}
                this.drive.flush();                             // and new fat
                */
                return wf;
        }
        return null;
    }
    
    /**
     * Does what is necessary when a file is closed: set file size, date etc...
     * 
     * @param entry entry in this buffer
     * @param dirChanged = <code>true</code> if this dir should save itself to disk since something in it has changed.
     * @param setDate set to <code>true</code> if the file date should be set in the file's header (which it shouldn't if the file is read only). 
     */
    public void closeFile(int entry,boolean dirChanged,boolean setDate)
    {
        if (dirChanged)
        {   
            if (setDate)
            {
                int l= (int)((System.currentTimeMillis()/1000)+Monitor.TIME_OFFSET); // ** magic offset  file date = now                   
                this.buffer.putInt(entry+WinDir.HDR_DATE,l);// set update date in file header   
            }
            try
            {
                this.drive.writeFile(this.buffer,this.clusterchain); // write this dir entry to disk. The disk is now synchronized as far as the direcory is concerned
            }
            catch (Exception e)
            {
                /*nop*/
            }
        }
        entry/=WinDriver.HEADER_LENGTH;
        this.openChannels[entry]--;                         // one less channel open for the file
        if (this.openChannels[entry]<1)
        {
            this.openChannels[entry]=0;                     // just to make sure....
            this.fileAccess.set(entry, false);              // no reason this file couldn't be opened again
        }
    }
    
    /**
     * Deletes a file from this directory (and thus from the disk).
     * The drive object itself handles whether or not its map should be saved.
     * 
     * @param entry the entry to delete. If this is 0, the dir is supposed to delete itself.
     * 
     * @return either Types.ERR_xxxx values if unsuccessful (all smaller than 0) or the 1st cluster of the file to be deleted.
     */
    public int deleteFile(int entry)
    {
        // check whether I should delete myself - IF YES PREMATURE EXIT
        if (entry==0)
        {
            if (this.dir==null)
                return Types.ERR_FDIU;                      // this is the main dir, which you can't delete.
            // now I've got to find myself in the mainDir and delete myself from there.
            return this.dir.deleteFile(this.index);         // ***** PREMATURE EXIT *****
        }
        
        // check whether file has open channels, if yes, refuse to let the file die
        if (this.openChannels[entry/WinDriver.HEADER_LENGTH]>0)
            return Types.ERR_FDIU;                          // there are still channels open for this file
        if (this.openChannels[entry/WinDriver.HEADER_LENGTH]!=0)//if this is !0, then we have a negative nbr of channels open??????
        {
            Helper.reportError(Localization.Texts[45], Localization.Texts[57]+this.openChannels[entry/WinDriver.HEADER_LENGTH]+
                   Localization.Texts[58]+"\n"+Localization.Texts[59] , null);
            return Types.ERR_FDIU;                          // there are still channels open for this file
        }
        
        int cluster=this.buffer.getShort(entry+WinDir.HDR_FLID)&0xffff;// 1rst cluster of file to be deleted
        
        // check whether file is a subdir, and if so, whether it is empty. If it isn't, don't let file be deleted
        boolean isSubdir=this.buffer.get(entry+WinDir.HDR_TYPE)==-1;
        if (isSubdir)         
        {
            int dirsize=this.buffer.getInt(entry);      
            java.util.ArrayList<Integer> cchain=new java.util.ArrayList<>();
            java.nio.ByteBuffer dbuffer=this.drive.readFile(dirsize, cluster, cchain);// read the content of the dir into a buffer
            if (dbuffer==null || dbuffer.capacity()<dirsize)
            {                                               // this is a serious error, I couldn't read the content of this dir.
                Helper.reportError(Localization.Texts[45],Localization.Texts[60]+"\n"+Localization.Texts[59], null);
                return Types.ERR_FDIU;                      // pretend file is in use
            }
            for (int i=WinDriver.HEADER_LENGTH;i<dirsize;i+=WinDriver.HEADER_LENGTH)// now go through all entries in the dir, check that they are empty.
            {
                if (dbuffer.getInt(i)!=0 || dbuffer.getShort(i+WinDir.HDR_NAME)!=0 )
                    return Types.ERR_FDIU;                  // entry is not "empty" : there are still files in this dir, refuse to let it be deleted
            }
        }
        // If we get here, this file may be deleted
        for (int i=entry;i<entry+WinDir.HDRLEN;i+=4)
            this.buffer.putInt(i,0);                        // delete entire entry in my buffer now
        
        if (isSubdir)   // this subdir is empty now delete this dir from my list of dirs
        {
            this.dirList.clear();                           // remove all subdirs
            makeDirList();                                  // and get them again   **** i should only remove the deleted one
        }
       
        writeFile(entry,WinDriver.HEADER_LENGTH);           // write this part of the dir back to disk
        return cluster;
    }
    
    /**
     * This converts an existing file contained in a parent directory (this object) into a subdirectory thereof.
     * The caller MUST already have checked that the file in question CAN legally be converted into a dir.
     * <p>
     * <b>This is NOT checked here again!!!!</b>
     * <p>
     * @param entry where this file header is in the parent dir's buffer.
     * @param cchain the file's clusterchain - this might have to be increased.
     * @param buf the file's buffer (i.e. the file's data on the disk) -I might have to increase this.
     * 
     * @return <code>false</code> if error, <code>true</code> if conversion file to dir went OK.
     */
    public boolean makeDirectory(int entry, java.util.ArrayList<Integer> cchain, java.nio.ByteBuffer buf)
    {
        boolean writeAll=false;                             // whether I have to write back the entire dir or not
        int fsize=WinDriver.HEADER_LENGTH;                  // size of new subdir  = just the header (for now)
        this.buffer.putInt(entry+4,0x00ff0000);             // make fileype
        this.buffer.putInt(entry+8,0);                      // no additional info
        this.buffer.putShort(entry+12,(short)0);            // no additional info   
        this.buffer.putInt(entry+WinDir.HDR_DATE,0);        // no date   
        this.buffer.putShort(entry+WinDir.HDR_VERS,(short)0);// no version  
        this.buffer.putInt(entry+WinDir.HDR_BKUP,0);        // no backup date  
        
        // now we must check whether any file that is in me (the parent directory) should be moved into this new subdirectory.
        byte nlength=this.buffer.get(entry+WinDir.HDR_NAME+1);// length of name of the new dir
        if (nlength==0)                                     // I can't make a root dir here!!!
            return false;
        while(this.buffer.get(entry+WinDir.HDRNAME+nlength-1)==Types.UNDERSCORE)// underscore at end of name?
        {
            nlength--;                                      // yes, delete it : new length of name
            if (nlength<=0)
                return false;                               // this wouldn't be valid
            this.buffer.put(entry+WinDir.HDRNAME+nlength,(byte)0);//delete underscore in header - this isn't STRICTLY necessary
        }
        this.buffer.put(entry+WinDir.HDR_NAME+1,nlength);   // set new length of new subdir
        byte[]fname=new byte[nlength];                      // for name of new subdir
        byte[]uncasedName=new byte[nlength];                // for name in lower case
        int counter;
        for (int i=0;i<nlength;i++)
        {
            counter=this.buffer.get(entry+WinDir.HDRNAME+i)&0xff;
            fname[i]=(byte)(counter);                       // name of new subdir
            uncasedName[i]=WinDrive.LOWER_CASE[counter];    // and uncased name for comparison
        }
        byte newl;
        int myentry=entry+WinDir.HDRNAME;                   // where name of the new dir lies in me
        
        // now create a new dir object, using the converted file's buffer and clusterchain (!!!)
        WinDir newDir=new WinDir(this.drive,this,entry,buf,cchain,fname,uncasedName);// this is the new subdir object
        
        // now check the anme of every file in this parent dir to see whether it should be moved to new subdir
        for (int loop=WinDriver.HEADER_LENGTH+WinDir.HDRNAME;loop<this.fileSize;loop+=WinDriver.HEADER_LENGTH)
        {
            if (loop==myentry)
                continue;                                   // no need to check the file that is being converted into a dir.
            newl=this.buffer.get(loop-1);                   // length of name we're comparing with
            if (newl>nlength+1)                             // name of file to check : must be bigger than the one for the subdir + underscore, else no neeeed to check it
            {
                boolean foundit=true;                       // pretend we have found a match
                for (int i=0;i<nlength;i++)
                {
                    counter=this.buffer.get(loop+i)&0xff;   // get byte from filename to compare with the new subdir's name
                    if (WinDrive.LOWER_CASE[counter]!=uncasedName[i])// check for match
                    {
                        foundit=false;                      // chars don't match, leave loop
                        break;
                    }
                }
               
                if (foundit && (this.buffer.get(loop+fname.length)==Types.UNDERSCORE))// up to here, the file name is a match for the dirname. Check that there is an underscrore
                {   
                    // here we found a file that is in me for now but must go into the new subdir. The header for that file starts in me at loop-WinDir.HDRNAME. 
                    // I must now:
                    //      - copy the found file's header from me to the new subdir 
                    //      - delete the header in me 
                    //      - (possibly) adjust filesize of new subdir
                    //      - check if that file is open and if yes, set the new dir in the corresponding WinFIle object
                    //      - check if that file is a subdir and if yes, set the new dir int he corresonding WInDir object.
                    
                    // the new subdir is empty at first, so there will not be any "holes" in it. This means that the entry for the new file will
                    // go at fsize (which will then be increased)
                   
                    // first make sure there is space for a new entry in the new subdir
                    if (fsize+WinDriver.HEADER_LENGTH > buf.capacity())
                    {                                       // if we get here, there is no more space in the buffer of the newly created subdir, increase it.
                        java.nio.ByteBuffer buf2=this.drive.increaseCapacity(buf,WinDriver.HEADER_LENGTH,cchain);
                        if (buf2==null)
                            return false;                   // something went seriously wrong; give up
                        buf=buf2;
                        newDir.setDirBuffer(buf);
                    }
                    int entryInParentDir=loop-WinDir.HDRNAME;  // this is the entry in me of that found file (don't confound it with entry of new subdir)
                    for (int j=0;j<WinDriver.HEADER_LENGTH;j+=4)
                    {
                        buf.putInt(fsize+j,this.buffer.getInt(entryInParentDir+j));// copy file header from parent dir (=me) to newly created subdir...
                        this.buffer.putInt(entryInParentDir+j,0);// ... and erase in me 
                    }
                    // the case of the newly created subdir into which this file is "copied" may be different from
                    // the case of the file name. To make sure the name of the file reflects that of the subdir, copy
                    // the name of the subdir into the name of the file.
                    for (int i=0,j=fsize+WinDir.HDRNAME; i<fname.length-1; i++,j++)
                    {
                        buf.put(j,fname[i]);            // write name of subdir into entry
                    }
                    
                    writeAll=true;                      // this (parent) dir has been changed elsewhere than just in the entry pointing to the new dir
                    this.drive.changeParentDir(this, entryInParentDir,newDir,fsize); // if a file object exists, change the parent dir and index
                    if (buf.get(fsize+HDR_TYPE)==(byte)-1)
                        changeParentDir(entryInParentDir,newDir,fsize);// same if that file was a subdir
                    fsize+=WinDriver.HEADER_LENGTH;         // new size of new dir (one entry was added). We can do this here, as 
                                                            // this is a newly created file, so it has no "holes" in it.
                }
            }
        }
        
        this.buffer.putInt(entry,fsize);                    // set length of new dir in parent dir me
        this.dirList.add(newDir);
        newDir.fileSize=fsize;                              // size of the new directory
        newDir.writeFile(0,0);                              // write out the entire new dir
        if (writeAll)
            writeFile(0,0);                                 // and all of myself (I'm the parent dir)
        else
            writeFile(entry,WinDriver.HEADER_LENGTH);       // or only part of myself if nothing else was changed in mes
        this.drive.flush();
        return true;
    }
 
    /**
     * Renames/moves a file from the old directory to this new one.
     * 
     * @param oldDir the directory currently holding this file.
     * @param wFile the file to be renamed.
     * @param fname the new name of the file.
     */
    public void renameFile(WinDir oldDir,WinFile wFile,byte[]fname)
    {
        int entry = wFile.getIndex();                       // index in old dir
        int mylength=this.name.length;                      // name length of (new) dir
        if (oldDir != this)                                 // am I renaming the file in the same dir?
        {                                                   // no, so copy from old to new, nuke entry in old dir
            int myentry=findFreeEntryInDir();               // this is where the file will go in the new dir
            java.nio.ByteBuffer oldBuffer=oldDir.getDirBuffer();// 
            for (int i=0;i<WinDriver.HEADER_LENGTH;i++)
            {
                this.buffer.put(myentry+i,oldBuffer.get(entry+i));//copy fleheader accross
                oldBuffer.put(entry+i,(byte)0);             // delete header in old dir
            }
            oldDir.writeFile(entry,WinDriver.HEADER_LENGTH);// save the old dir
            wFile.setDir(this);                             // set the new dir and index in the file
            wFile.setIndex(myentry);
            entry=myentry;                                  // file is now here in the new dir
            // now I want to make sure that the first part of the filename corresponds exactly to that of this directory
            for (int i=0;i<mylength;i++)
            {
                this.buffer.put(entry+WinDir.HDRNAME+i,this.name[i]);
            }
        }

        int fnlength=fname.length;                           // length of new filename
        this.buffer.putShort(entry+WinDir.HDR_NAME,(short)fnlength);//set new length of filename in new dir
        this.buffer.position(entry+WinDir.HDRNAME+mylength);
        for (int i=mylength;i<fnlength;i++)
        {
            this.buffer.put(fname[i]);                       // transfer rest of name
        }
        for (int i=fnlength;i<WinDir.HDRNMLN;i++)
        {
            this.buffer.put((byte)0);                        // nuke out the rest of the old name
        }
        writeFile(entry,WinDriver.HEADER_LENGTH);
   }
    
    /********************* Getting setting infos about a file in the directory from the file header*************************/  
    
    /**
     * Sets the header for a file (always 14 bytes).
     * 
     * @param entry where in the dir buffer is the header to be set.
     * @param cpu the SMSQ/E CPU - A1 points to the data for the file header. MUST BE AT AN EVEN ADDRESS!!!!!!
     * @param length the length of the file.
     */
    public void setFileHeader(int entry,smsqmulator.cpu.MC68000Cpu cpu,int length)
    {
        int A1=cpu.addr_regs[1]+4;
        this.buffer.putInt(entry,length);
        //this.buffer.putInt(entry,cpu.readMemoryLong(A1)+WinDriver.HEADER_LENGTH);
       // A1+=4;
        for (int i=entry+4;i<14+entry;i+=2,A1+=2)
        {
            this.buffer.putShort(i,(cpu.readMemoryShort(A1)));
        }
        cpu.addr_regs[1]=A1;
        cpu.data_regs[1]=14;
        cpu.data_regs[0]=0;
    }
    
    /**
     * Sets a long word in a header.
     * 
     * @param offset where to set the element (i.e. index+entry in header).
     * @param longword what to set.
     */
    public void setInHeader(int offset,int longword)
    {
        this.buffer.putInt(offset,longword);
    }
        
    
    /**
     * Checks whether the file at (index) is a directory.
     * 
     * @param index index into this buffer, where the file to check lies (should be multiple of Windriver.HEADER_LENGTH).
     * 
     * @return <code>true</code> if this is a (sub)directory, else <code>false</code>
     */
    public boolean fileIsDir(int index)
    {
       // was ;  if (index<this.fileSize)
        if (index+HDR_TYPE<this.fileSize)
            return (this.buffer.get(index+HDR_TYPE)==-1);
        return false;
    }
    
    /**
     * Gets the length of a file at (index).
     * 
     * @param index where the file lies in the directory.
     * 
     * @return the length of the file or 0 if the index would be beyond the end of the file.
     */
    public int getFileLength(int index)
    {
        if (index<this.fileSize)
            return this.buffer.getInt(index);
        return 0;
    }
   
     
    /**
     * Gets the version of the file.
     * 
     * @param entry where the file lies in the directory.
     * 
     * @return the file version.
     */
    public int getFileVersion (int entry)
    {
        return this.buffer.getShort(entry+WinDir.HDR_VERS)&0xffff;
    }
    
    /**
     * Sets the version of the file.
     * 
     * @param entry where the file lies in the directory.
     * @param version the file version to set.
     */
    public void setFileVersion(int entry,int version)
    {
        this.buffer.putShort(entry+WinDir.HDR_VERS,(short)(version&0xffff));
    } 
    
    /**
     * Gets the date of the file.
     * 
     * @param whatDate what date to get (i.e. index+date to get -upate or backup date).
     * 
     * @return the date.
     */
    public int getFileDate (int whatDate)
    {
        return this.buffer.getInt(whatDate);
    }
    
    
    /**
     * Sets the date of the file.
     * 
     * @param entry what date to set (i.e. index+date to get -upate or backup date).
     * @param mdate the date in QL format.
     */
    public void setFileDate(int entry,int mdate)
    {
        this.buffer.putInt(entry,mdate);
    }
    
    /**************************** Various subroutines for the directory object itself *******************************************/
       
    /**
     * Makes a list with all of my subdirs, creating the corresponding WinDir objects.
     */
    private void makeDirList()
    {
        for (int entry=WinDriver.HEADER_LENGTH;entry<this.fileSize;entry+=WinDriver.HEADER_LENGTH)
        {
            if (this.buffer.get(entry+WinDir.HDR_TYPE)==-1)
            {
                int fsize=this.buffer.getInt(entry);
                int fcluster=this.buffer.getShort(entry+WinDir.HDR_FLID)&0xffff;
                java.util.ArrayList<Integer> cchain= new java.util.ArrayList<>();
                java.nio.ByteBuffer buf=this.drive.readFile(fsize, fcluster,cchain);
                int namelength=this.buffer.get(entry+WinDir.HDRNAMEL)&0xff;
                if (this.buffer.get(entry+namelength-1+WinDir.HDRNAME)==Types.UNDERSCORE)
                    namelength--;
                byte []nm=new byte[namelength];
                byte []uncasednm=new byte[namelength];
                int p,offs=entry+WinDir.HDRNAME;
                for (int i=0;i<namelength;i++)
                {
                    nm[i]=this.buffer.get(i+offs);
                    p=nm[i]&0xff;
                    uncasednm[i]=WinDrive.LOWER_CASE[p];    
                }
                if (buf!=null)
                {
                    WinDir newDir=new WinDir(this.drive,this,entry,buf,cchain,nm,uncasednm);
                    this.dirList.add(newDir);
                }
            }
        }
        this.listDone=true;
    }
    
    /**
     * Finds out whether a file belongs or would belong in this dir or any of its subdirs.
     * This does not mean that the file ACTUALLY EXISTS in this dir, but if it were to exists on the drive it would be here.
     * 
     * @param fname the lower cased name of the file.
     * 
     * @return the dir this file belongs in, null if not in this dir or any of its subdirs.
     */
    public WinDir findInDirs(byte[] fname)
    {
        
        if (!this.listDone)
        {
            makeDirList();                                  // make sure the list of subdirs is done
        }
        
        int dl=this.name.length;
        // check whether the file belongs in THIS dir
        if (this.dir!=null && (dl!=0))                      // if this is main dir, file belongs here!
        {
            if (dl >= fname.length)                             // name of dir is bigger than filename - file can't belong here
                return null;
        
            int i;
            for (i=0;i<dl;i++)
            {
                if (this.normalizedName[i]!=fname[i]) 
                return null;
            }
            if (fname[i]!=Types.UNDERSCORE)
                return null;                                    // to belong here, after the subdir name there must be a path separator
        }
        
        WinDir found;                                       // if we get here, file would at least belong in this dir
        for (WinDir toSearch : this.dirList)                // now check whethr it would belong in any subdir
        {
            if ((found=toSearch.findInDirs(fname))!=null)
                return found;                               // it would be in this subdir
        }
        return this;                                        // it wouldn't be in any subdir, so it's in me.
    }
    
    /**
     * This checks whether a file of that name exists in this dir.
     * 
     * @param fname : the uncased name of the file (SMSQE lower case)
     * @param flags a (return) array of booleans which will be filled in this method :
     *      [0] true if file exists in this dir.
     *      [1] true if file is a dir.
     *      [2] true if file can't be written to.
     *      [3] true if file has channels open to it.
     * 
     * @return the entry in this dir (=offset from start of buffer) or 0 if file is not found in this directory.
     */
    public int checkFile(boolean[] flags,byte[]fname)
    {
        int namelength=fname.length;
        outerloop:
        for (int entry=WinDriver.HEADER_LENGTH;entry<this.fileSize;entry+=WinDriver.HEADER_LENGTH)
        {
            if ((this.buffer.get(entry+WinDir.HDRNAMEL)&0xff)!=namelength)
                continue;                                   // name lengths don't match so names don't match
            for (int i=0,j=entry+WinDir.HDRNAME;i<namelength;i++,j++)
            {
               int n=this.buffer.get(j)&0xff;                   // char of file in dir
               if (WinDrive.LOWER_CASE[n]!=fname[i])            // now uncased, must be same as char in filename
               {
                   continue outerloop;                          // but it isn't : no match
               }
            }

            // if we get here, this is a match;
            flags[0]=true;                                  // file was found
            flags[1]=this.buffer.get(entry+WinDir.HDR_TYPE)==-1;// maybe a dir
            flags[2]=this.fileAccess.get(entry/WinDriver.HEADER_LENGTH);// file may be read only
            flags[3]=this.openChannels[entry/WinDriver.HEADER_LENGTH]!=0;// file may have channels open to it
            return entry;
        }          
        // if we get here name wasn't found in this dir  
        for (int i=0;i<flags.length; i++)
        {
           flags[i]=false;
        }
        return 0;
    }
    
   /**
    * Finds the first free entry in my buffer.
    * 
    * An entry is considered to be free if : length of file=0 and length of filename=0.
    * If no entry is free, the fileSize may grow either by using the remaining space in the buffer, or by adding 
    * a new cluster to the dir.
    * 
    * @return the index into the buffer where the first free entry lies, or -1 if no free entry could be found.
    */
    public int findFreeEntryInDir()
    {
        for (int indexx=WinDriver.HEADER_LENGTH; indexx<this.fileSize ; indexx+=WinDriver.HEADER_LENGTH)
        {
            try
            {
                if ((this.buffer.getInt(indexx)==0) && (this.buffer.getShort(indexx+WinDir.HDR_NAME)==0))
                    return indexx;                              // there was a "hole" in the dir, fill it
            }
            catch (Exception e)
            {
                return -1;
            }
        }
        
        // if we get here, we need to expand the (sub)dir. This may simple: there is still space in the buffer, or more complex:
        // we're also at the end of the buffer
        if (this.fileSize == this.buffer.capacity())        // we are at end of buffer, need more buffer space
        {
            java.nio.ByteBuffer buf=this.drive.increaseCapacity(this.buffer,WinDriver.HEADER_LENGTH,this.clusterchain);
            if (buf==null)
                return -1;                                  // something went seriously wrong
            this.buffer=buf;
            this.openChannels=java.util.Arrays.copyOf(this.openChannels,this.buffer.capacity()/WinDriver.HEADER_LENGTH);// new array with nbr of channels open per file
        }
        
        this.fileSize+=WinDriver.HEADER_LENGTH;             // we've expanded the filesize of this dir
        if (this.dir==null)                                 // this is the main dir
        {
            this.drive.setRootDirLength(this.fileSize);     // so set new filesize in map
        }
        else                                                // this isn'the main dir, set my new filesize in dir containing me
        {
            this.dir.setInHeader(this.index, this.fileSize);// set new length in the dir that contains me
            this.dir.writeFile(this.index,WinDriver.HEADER_LENGTH); // write that dir out
        }
        return this.fileSize-WinDriver.HEADER_LENGTH;       // next free place
    }
    
    /**
     * This calculates the SMSQE "size" of a directory.
     * The size of an SMSQE dir corresponds to the last occupied entry in the dir.
     * 
     * @param buf the dir's buffer
     * 
     * @return the SMSQE length of the dir.
     */
    private int calcDirSize(java.nio.ByteBuffer buf)
    {
        for (int i=buf.capacity()-WinDriver.HEADER_LENGTH;i>0;i-=WinDriver.HEADER_LENGTH)
        {
            if((buf.getInt(i)!=0) || (buf.get(i)!=(byte)0))
                return i-WinDriver.HEADER_LENGTH;
        }
        return 0;
    }
       
    /**
     * Makes a new entry for a file.
     * Sets the filename. File size is set to "0" (for smsqe), ie. Windriver.HEADER_LENGTH; length of the header.
     * 
     * @param entry pointer into the buffer for the header.
     * @param fname SMSQE name of file.
     */
    private void makeNewEntry(int entry,byte[] fname)
    {
        this.buffer.putInt(entry,WinDriver.HEADER_LENGTH);
        for (int indexx=entry+4 ;indexx<entry+WinDriver.HEADER_LENGTH;indexx+=4)
        {
            this.buffer.putInt(indexx,0);
        }
        this.buffer.put(entry+WinDir.HDRNAMEL,(byte)fname.length);
        for (int i=0;i<fname.length;i++)
        {
            this.buffer.put(entry+i+WinDir.HDRNAME,fname[i]);
        }
        if (this.dir!=null)
        {
            for (int i=0;i<this.name.length;i++)
            {
                this.buffer.put(entry+i+WinDir.HDRNAME,this.name[i]);//overwrite the part of the name with the name of my subdir.
            }
        }
    }
    
    /**
     * Add a dir to my dir list. This means that that dir is a subdir of mine.
     * 
     * @param toAdd the WinDir to add.
     */
    public void addToList(WinDir toAdd)
    {
        this.dirList.add(toAdd);
    }
    
    /**
     * Remove a dir from my dir list. This means that that dir is no longer a subdir of mine.
     * 
     * @param toRemove the WinDir to remove.
     */
    public void removeFromList(WinDir toRemove)
    {
        this.dirList.remove(toRemove);
    }
    
    /**
     * Checks whether the entry is a dir of mine, and if so: removes it from my dir list, sets it as subdir of the new dir
     * @param entry entry in me
     * @param newDir the newdir this shuld be a subdir of
     * @param newEntry entry in that new dir
     */
    private void changeParentDir(int entry, WinDir newDir, int newEntry)
    {
        java.util.ArrayList<WinDir> avoidComodification=new java.util.ArrayList<>();
        for (WinDir f: this.dirList)
        {
            if (f.getIndex()==entry)
            {
                avoidComodification.add(f);                 // this must be removed later on
                newDir.addToList(f);                        // but it is a subdir of the new dir
                f.setDir(newDir);                           // also set this in that subdir
                f.setIndex(newEntry);
            }
        }
        for (WinDir f: avoidComodification)
        {
            this.dirList.remove(f);                         // this is no nonger a subdir of mine
        }
    }
    
  
    /**
     * Gets the name of this directory.
     * 
     * @return the name as an array of bytes.
     */
    public byte[] getName()
    {
        return this.name;
    }
    
    /**
     * Gets the lower cased name of this file.
     * 
     * @return the name as an array of SMSQE lower cased bytes.
     */
    public byte[] getUncasedName()
    {
        return this.normalizedName;
    }
   
       
     /**
     * Gets the ByteBuffer containing the data in this dir.
     * Be careful what you do with this.
     * 
     * @return this directory's ByteBuffer.
     */
    public java.nio.ByteBuffer getDirBuffer()
    {
        return this.buffer;
    }
    
    /**
     * Sets the ByteBuffer containing the data in this dir.
     * Be careful what you do with this.
     * 
     * @param buf this directory's new ByteBuffer.
     */
    private void setDirBuffer(java.nio.ByteBuffer buf)
    {
        this.buffer=buf;
    }
}
