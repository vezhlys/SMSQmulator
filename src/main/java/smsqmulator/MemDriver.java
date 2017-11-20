package smsqmulator;

/**
 * This is the device driver for the MEM device.
 * This device copies a QXL.WIN drive into memory and accesses it from there instead of the physical disk.<p>
 * No speed enhancement are to be expected compared with normal QXL.WIN drives operation, since this emulates such a drive in memory.
 * The purpose of this is to have a drive that can be loaded over the Internet (e.g. in an embedded applet).<p>
 * If SMSQmulator is run locally, the drive can be written back to the disk later.<p>
 * As usual for SMSQE, the device can have up to 8 drives BUT : 
 * each drive is limited to a max of 500 MiB! Bigger drives will be refused.
 * 
 * @author and copyright (c) wolfgang Lenerz 2014.
 * 
 * @version  
 * 1.02 setNames adjusted.
 * 1.01 use StringBuilder in setNames.
 * 1.00 a rewrite of the WinDriver.
 * 
 */
public class MemDriver extends WinDriver implements DeviceDriver
{
  /**
   * Creates the driver object.
   * 
   * @param cpu the cpu to use.
   * @param warn object with warning flags.
   */
    public MemDriver(smsqmulator.cpu.MC68000Cpu cpu,Warnings warn)
    {
        super(cpu,warn);
        this.deviceID=Types.MEMDriver;                 //'MEM0';
        this.drives=new MemDrive[8]; 
    }
    
   /**
     * Formats a medium.
     * 
     * @param formatName the name to give to the formatted drive.
     * @param inifile the ".ini" file with initialized values.
     * 
     * @return <code>true</code> if deviceID corresponded to this device, <code>false</code> if not.
     * <p> A format for this device always fails,this drive CANNOT be formatted : error <code>Types.ERR_FMTF</code>..
     */
    @Override
    public boolean formatMedium(String formatName,inifile.IniFile inifile)
    {
        this.cpu.data_regs[0]=Types.ERR_FMTF;
        return false;
    }
  
    /************************** Getting/setting the names of the native files containing the drives *************************************/

    /**
     * Sets the names for the native files containing the drives and creates the drive objects.
     * 
     * @param names an 8 element string array containing the native names of the container files for the drives.
     * @param inifile the initialization object.
     * @param forceRemoval true if files/drives should removed before being remounted.
     * 
     * @return <code>true</code> if this was the device concerned.
     */
    @Override
    public boolean setNames(String[] names,inifile.IniFile inifile,boolean forceRemoval, boolean suppressWarnings) 
    {
        if (names.length==8)
        {
            if (inifile.getTrueOrFalse("DISABLE-MEM-DEVICE"))
                return true;
            StringBuilder errStr=new StringBuilder();
            for (int i=0;i<8;i++)
            {
                // first of all, unset all drives that aren't equal to the one being set now
                if (this.nativeDir[i]!=null && !this.nativeDir[i].isEmpty())
                {   
                    if (forceRemoval || names[i]==null || names[i].isEmpty())// if new name is null, unset drive
                    {
                        if(this.drives[i]!=null)
                        {
                            this.drives[i].unuse();
                            this.drives[i]=null;
                        }
                    }
                    else if(!this.nativeDir[i].toLowerCase().equals(names[i].toLowerCase()))//if new names != old name, unset drive
                    {
                        if(this.drives[i]!=null)
                        {
                            this.drives[i].unuse();
                            this.drives[i]=null;
                        }
                    }
                }
            }
            this.nativeDir=names.clone();                   // keep a local copy of the names
            
            for (int i=0;i<8;i++)
            {
                try
                {
                    if (this.nativeDir[i]!=null && !this.nativeDir[i].isEmpty())
                    {
                        if (this.drives[i]!=null)           // if the drive already exists and the file is the same, don't do anything...
                        {
                            if(!this.nativeDir[i].equals(this.drives[i].getDrivename()))
                            {
                                this.drives[i].unuse();
                                this.drives[i]=new MemDrive(this.cpu,this.nativeDir[i],this,i+0x31,this.warnings);
                            }
                        }
                        else
                        {
                            this.drives[i]=new MemDrive(this.cpu,this.nativeDir[i],this,i+0x31,this.warnings);// drive doesn't exist yet
                        }
                        if (this.drives[i].isReadOnly() && this.warnings.warnIfQXLDriveIsReadOnly)
                        {
                            errStr.append(this.nativeDir[i]).append("\n");
                            errStr.append(Localization.Texts[105]).append("\n");// possibly warn if drive is read only
                        }
                    }
                }
                catch (java.io.FileNotFoundException e)     // the native file wasn't found at all
                {
                    if (this.warnings.warnIfNonexistingQXLDrive)
                    {
                        errStr.append(this.nativeDir[i]).append("\n");
                        errStr.append(Localization.Texts[64]).append("\n");
                    }
                    this.drives[i]=null;
                }
                catch (java.net.ProtocolException e)        // the main dir couldn't be read
                {
                    errStr.append(this.nativeDir[i]).append("\n");
                    errStr.append(e.getMessage()).append("\n");
                    this.drives[i]=null;
                }
                catch (ArrayIndexOutOfBoundsException e)    // file had seriously wrong clusters for FAT
                {
                    errStr.append(this.nativeDir[i]).append("\n");
                    errStr.append(Localization.Texts[65]).append("\n");
                    this.drives[i]=null;
                }
                catch (DoNothingException e)                // file had non compliant clusters and user didn't want to continue using it
                {
                    this.drives[i]=null;
                }
               
                catch (IncorrectFiletypeException e)        // file is NOT a valid qxl.win file
                {
                    errStr.append(this.nativeDir[i]).append("\n");
                    errStr.append(Localization.Texts[111]).append("\n");
                    this.drives[i]=null;
                }
                catch (Exception e)                         // any other exception
                {
                    errStr.append(this.nativeDir[i]).append("\n");
                    errStr.append(e.getMessage()).append("\n");
                    this.drives[i]=null;
                }
            }
            if (errStr.length()!=0 && !suppressWarnings)
            {
                Helper.reportError(Localization.Texts[45], Localization.Texts[49]+"\n"+errStr.toString(), null);
            }
            return true;
        }
        else
            return false;
    }
    
    /**
     * Writes the drive back to a native file
     * 
     * @param driveNbr the drive number (1...8)
     */
    @Override
    public void writeBack (int driveNbr)
    {
        if (this.drives[driveNbr]!=null)
        {
            MemDrive m =(MemDrive)this.drives[driveNbr];
            m.writeBack(this.nativeDir[driveNbr]);
        }
        else
            this.cpu.data_regs[0]=Types.ERR_FDNF;
    }
}

