
package smsqmulator;

/**
 * An object accessing the system clipboard for transfer to/from it.
 * 
 * @author wolfgang lenerz (c) 2013-2017
 * @version 
 *  1.01    can also put content into clipboard.
 *  1.00    gets content of clipboard if string.
 */
public class ClipboardXfer implements java.awt.datatransfer.ClipboardOwner
{
    private static final java.awt.datatransfer.Clipboard clipboard = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
    private int changeCounter;                                  // incremented each time the clipboard changes
    private  String content=null;                                // String content of clipboard
    private ClipboardMonitorThread clipboardMonitorThread =null;                // thread monitoring the 
    
    
   /**
    * Not needed here.
    */
    @Override
    public void lostOwnership(java.awt.datatransfer.Clipboard clipboard,java.awt.datatransfer.Transferable contents) 
    {
     /*nop*/
    }

    /**
    * Puts a string into the clipboard with me as owner.
    * 
    * @param toSet the String to put into the clipboard.
    */
    public void setClipboardContents(String toSet)
    {
        java.awt.datatransfer.StringSelection stringSelection = new java.awt.datatransfer.StringSelection(toSet);
        this.clipboard.setContents(stringSelection,this);
    }

    /**
    * Gets the string from the clipboard.
    *
    * @return any string in the Clipboard; empty String (not NULL!!!) if none.
    */
    public final String getClipboardContents()
    {     
        String result="";
        java.awt.datatransfer.Transferable contents = this.clipboard.getContents(null);
        if (contents==null)
            return result;
        boolean hasTransferableText =contents.isDataFlavorSupported(java.awt.datatransfer.DataFlavor.stringFlavor);
        if (hasTransferableText) 
        {
            try 
            {
                result = (String)contents.getTransferData(java.awt.datatransfer.DataFlavor.stringFlavor);
            }
            catch (java.awt.datatransfer.UnsupportedFlavorException | java.io.IOException e)
            {
               /*nop*/
            }
        }
        return result;
    }
    
    /**
     * Transfers a max of 32000 chars from the clipboard to the area pointed to by A1.
     * 
     * @param cpu the cpu (memory) to transfer to.
     */
    public void transferClipboardContentsToScrap(smsqmulator.cpu.MC68000Cpu cpu)
    {
        if (isMonitoring())
        {
            synchronized (this.content)
            { 
                if (!this.content.isEmpty())
                {
                    cpu.writeSmsqeString(cpu.addr_regs[1], this.content, 32000);
                }
            }
        }
        else
        {
            String p=getClipboardContents();
            if (!p.isEmpty())
            {
                cpu.writeSmsqeString(cpu.addr_regs[1],p, 32000);
            }
        }
    }  
    
    /**
     * Transfers chars from the area pointed to by A1 to the clipboard.
     * 
     * @param cpu the cpu (memory) to transfer to.
     */
    public void transferScrapToClipboard(smsqmulator.cpu.MC68000Cpu cpu)
    {
        String p=cpu.readSmsqeString(cpu.addr_regs[1]);
        if (!p.isEmpty())
        {
            setClipboardContents(p);
        }
    }
    
    /**
     * Sets the new content and increases the change counter when content has changed.
     * 
     * @param s the new content.
     */
    public final void setNewContent(String s)
    {
        synchronized (this.content)
        {
            this.content=s;
            this.changeCounter++;
        }
    }
    
    /**
     * Checks whether the clipboard content has changed.
     * If yes, D0 is set to the length of the data, else it is set to 0. If length of data were > 32000, it is limited to 32000.
     * 
     * @param cpu the cpu used to query this.
     * (on entry here, D1 = smsqe stored clipboard counter).
     */
    public final synchronized void getChangeCounter(smsqmulator.cpu.MC68000Cpu cpu)
    {
        if (cpu.data_regs[1]==this.changeCounter)
        {
            cpu.data_regs[0]=0;
            cpu.reg_sr|=4;                                      // signal that there is no change
        }
        else
        {
            cpu.data_regs[1]=this.content.length()>32000?32000:this.content.length();// limit nbr of bytes to 32000
            cpu.data_regs[0]=this.changeCounter;                // D0 = nbr of bytes in clipboard
            cpu.reg_sr&=~4;                                     // set status flag that D0 != 0.
        }
    }
   
    /*** ---------- routines added for syncscrap -----------***/
    
    /**
     * Syncscrap job is activated, start monitoring the clipbord.
     */
    public void startMonitoring() 
    {
        if (isMonitoring()) return;                             // clipboard monitor is already running
        this.content=getClipboardContents();                    // current clipboard content
        this.changeCounter=0;                                   // this ensures that original content will be copied to scrap
        this.clipboardMonitorThread = new ClipboardMonitorThread(this,getClipboardContents()) ;
        this.clipboardMonitorThread.setName("ClipboardMonitor");
        this.clipboardMonitorThread.setDaemon(true);
        this.clipboardMonitorThread.start();
    }
    
    /**
     * Syncscrap job is stopped, stop monitoring the clipbord..
     */
    public void stopMonitoring() 
    {
        if (this.clipboardMonitorThread != null) 
        {
            this.clipboardMonitorThread.cancel();
            this.clipboardMonitorThread.interrupt();
    //        this.clipboardMonitorThread.stop();
        }
        this.clipboardMonitorThread = null;
    }

    public synchronized boolean isMonitoring() 
    {
        return this.clipboardMonitorThread != null && this.clipboardMonitorThread.isAlive();
    }    
}
