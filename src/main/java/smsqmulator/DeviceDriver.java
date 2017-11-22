package smsqmulator;

/**
 * This is the interface template for device drivers.
 * <P> Device drivers are responsible for handling all file I/O. A device driver must, of course, have a corresponding SMSQE device driver
 *     which is correctly linked into SMSQE. The NFA driver shows how this is done.
 * <p> Device drivers must implement all of the methods set out herein some of which return a <code>boolean</code> value.
 * <p> These methods MUST set the D0 register to signal success (or not) of the operation. In many cases (notably OPEN), the 
 *     <code>boolean</code> returned by a method does <b> <i> NOT </i>  </b> signal that an I/O operation succeeded (or not), 
 *     but has some other meaning : See the individual methods for more information on the returned <code>boolean</code>.
 * 
 * <p> ATM, device driver operations will always seem atomic to SMSQE, i.e. when the TRAP call returns, the operation in its entirety will
 *     have succeeded (or not). In other words, the timeout for the trap #3 calls is totally ignored. This has the unfortunate side effect
 *     that SMSQE will appear to be frozen whilst file i/o takes place.
 * 
 * <p> In keeping with standard SMSQE practice, it is presumed throughout that each device may have 8 drives.
 *
 * <p> Each device driver must have a deviceID, which consists of one Java <code>int</code>. This device ID must also be the one that
 *     the SMSQE driver uses. The deviceID is normally a long word, containing the upper cased three letter word of the device, followed by ASCII 0.
 *     Thus, for the NFA driver, this is 0x0x4e464130, which translates to 'NFA0'.
 * 
 * @author and copyright (c) 2012 -2014 Wolfgang Lenerz. See the licence in the licence.txt file.
 * @version 
 *  1.01 setNames takes additional parameter to force removal of existing drive before resetting it.
 *  1.00 added writeBack (for MEM device)
 *  0.00 initial version
 */
public interface DeviceDriver 
{
   /**
     * Opens a file.
     * <p> If the file is opened OK, D0 in the cpu is set to 0.
     * <p> If the file isn't OK, D0 is set to an error code.
     * <p> The device must first of all check whether the file is for itself (i.e. for this device). If the file isn't for this device, 
     *     immediately return <code>false</code>. In that case, the value of register D0 doesn't matter.
     *     In all other cases (i.e. the file was, indeed, for this device) always return <code>true</code>, but previously set D0 to signal
     *     whether the open routine was successful or not.
     *     If the operation succeeds, the Driver should place a word sized file ID at offset 0x1e of the channel definition block (eg.
     *     <code>this.cpu.writeMemoryWord(channelDefinitionBlock+0x1e, fileID)</code>), or use any other convenient method to make
     *     size it can later identify the file when the file's I/O routines are called.
     * 
     * @param devDriverLinkageBlock pointer to the SMSQE device driver linkage block (a3).
     * @param channelDefinitionBlock pointer to the SMSQE channel defintion block (a0).
     * @param openType  which kind of open is requested? 
     * <ul>
     * <li> 0 - old exclusive (r/w access to an already existing file)
     * <li> 1 - old shared (read only access to an already existing file)
     * <li> 2 - new exclusive (r/w access to a file to be created and which doesn't exist yet)
     * <li> 3 - new overwrite (r/w access to a file to be created, will overwrite any previously existing file of the same name)
     * <li> 4 - open directory
     * </ul>
     * @param driveNumber number of drive on which to open the file.
     * @param filename the name of the file to open.
     * @param uncased the name of the file to open in all lower case.
     * 
     * @return <code>true</code> if file was opened OK, <code>false</code> if not.  
     * 
     */
    public boolean openFile(int devDriverLinkageBlock,int channelDefinitionBlock,int openType,int driveNumber,byte []filename,byte[]uncased);   // return true if device opened
    
    
    /**
     * This closes an open file.
     * 
     * @param driveNbr the number of the drive (0..7).
     * @param fileID the fileID.
     * 
     * @return <code>true</code> if file was for this device, else <code>false</code>.
     * <p>If this returns true, this method MUST have set the D0 register to signal success (or not) of the operation to SMSQE.
     */  
    public boolean closeFile(int driveNbr,int fileID);
    
    
    /**
     * Formats a medium. This will probably fail in most cases as, e.g. the NFA driver does not allow formatting.
     * 
     * @param formatName the name to give to the formatted drive.
     * @param inifile the ".ini" file object.
     * 
     * @return <code>true</code> if deviceID corresponded to this device, <code>false</code> if not.
     * <p>  This method MUST set the D0 register to signal success (or not) of the operation to SMSQE.
     */
    public boolean formatMedium(String formatName,inifile.IniFile inifile);
    
    /**
     * Deletes a file.
     * 
     * @param driveNumber number of drive on which to delete the file.
     * @param filename the name of the file to open.
     * @param file the file to delete.
     * 
     * @return <code>true</code> if the file deleted.
     * <p>  This method MUST set the D0 register to signal success (or not) of the operation to SMSQE.
     */
    public boolean deleteFile(int driveNumber,byte[] filename,java.io.File file);
    
    
    /**
     * Sets the names of the native directories to be used for each drive of the device.
     * <p> In keeping with standard SMSQE practice, it is presumed throughout that each device may have 8 drives.
     *
     * @param names an 8 element String array with names for the drives.
     * @param inifile the initialization object.
     * @param forceRemoval true if files/drives should removed before being remounted.
     * @param suppressWarnings if true, don't show missing drives etc warning.
     * 
     * @return <code>true</code> if names were set, else <code>false</code>.
     */
    public boolean setNames(String[]names,inifile.IniFile inifile,boolean forceRemoval,boolean suppressWarnings);
    
    /**
     * Gets the names of the native directories/files used for each drive of the device.
     * <p> In keeping with standard SMSQE practice, it is presumed throughout that each device may have 8 drives.
     *
     * @return An 8 elements string array with the names, or <code>null</code> if the device ID wasn't mine. 
     * Individual elements may be <code>null</code> or empty if the corresponding drive isn't assigned.
     */
    public String[] getNames();
    
    /**
     * Gets the name of the native directory/file used for one drive of the device, or the current usage name of the device.
     * <p> In keeping with standard SMSQE practice, it is presumed throughout that each device may have 8 drives.
     *
     * @param drive the drive (from 1 to 8) for which the name is to be obtained, or 0 if the current usage name is to be returned.
     * 
     * @return A string array with the name, or <code>null</code> if the device ID wasn't mine, or "" if the device ID was mine but the drive didn't exist..
     */
    public String getName(int drive);
   
    /**
     * Handles trap#3 calls (but should check first whether the trap#3 call is, indeed, for this device).
     * <p> All device drivers get called in turn for this operation, until one signals that it has handled the trap call. "Handling" the trap call
     * does not mean that the trap call was completed successfully, it only means that the device driver signals that this trap
     * call concerned a file for which it was,indeed, the appropriate driver.
     * 
     * @param driveNumber number of drive on which to open the file
     * @param trapKey what kind of trap #3 is it?
     * @param channelDefinitionBlock  pointer to the SMSQE channel definition block (a0)
     * @param fileNbr the file number given by the ddd when file was opened (A0+0x1e)
     * 
     * @return <code>true</code> if file was for this device, else <code>false</code>. This says nothing about the success of the trap #3 
     * operation in itself, it only shows that the device driver recognized that this I/O operation was for a file on one of its drives.
     * <p>  This method MUST set the D0 register to signal success (or not) of the operation to SMSQE.
     */
    public boolean trap3OK(int driveNumber,int trapKey,int channelDefinitionBlock,int fileNbr);
    
    
    /**
     * This sets the usage name of the device, eg. "NFA_USE WIN"
     * 
     * @param usage the usage name as an int
     * 
     */
    public void setUsage(int usage);
    
    
    /**
     * This gets the usage name of the device.
     * 
     * @return the usage name as an int, or 0 if this wasn't for this device
     */
    public int getUsage();
    
    
    /**
     * Gets the device ID for this device, eg. SFA0.
     * 
     * @return the device ID as int.
     */
    public int getDeviceID();
    
    
    /**
     * Sets whether a filename's case should be changed (0 = unchanged, 1=all upper case, 2=all lower case.
     * 
     * @param change 0 = unchanged, 1=all upper case, 2=all lower case.
     */
    public void setFilenameChange(int change);
    
    
    /**
     * Closes all files opened by all drives of this device driver. 
     */
    public void closeAllFiles();
    
    /**
     * Sets the cpu used by the device driver.
     * 
     * @param cpu the cpu to be set.
     */
    public void setCpu (smsqmulator.cpu.MC68000Cpu cpu);
    
    
    /**
     * Writes the drive back to a native file
     * 
     * @param driveNbr the drive number (1...8)
     * 
     */
    public void writeBack (int driveNbr);
}
