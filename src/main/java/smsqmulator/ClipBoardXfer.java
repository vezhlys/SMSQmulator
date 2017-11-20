
package smsqmulator;

/**
 * An object accessing the system clipboard for transfer from it.
 * 
 * @author wolfhang lenerz (c) 2013-2014
 * @version 
 *  1.01    can also put content into clipboard.
 *  1.00    gets content of clipboard if string.
 */
public class ClipBoardXfer implements java.awt.datatransfer.ClipboardOwner
{
   /**
    * Not needed here.
    */
    @Override
    public void lostOwnership(java.awt.datatransfer.Clipboard clipboard,java.awt.datatransfer.Transferable contents) 
    {
     /*nop*/
    }

    /**
    * Puts a string into the clipboard with me as owner - unused as yet.
    * 
    * @param toSet the String to put into the clipboard.
    */
    public void setClipboardContents(String toSet)
    {
        java.awt.datatransfer.Clipboard clipboard = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
        java.awt.datatransfer.StringSelection stringSelection = new java.awt.datatransfer.StringSelection(toSet);
        clipboard.setContents(stringSelection,this);
    }

    /**
    * Gets the string from the clipboard.
    *
    * @return any string in the Clipboard; empty String if none.
    */
    public String getClipboardContents() 
    {
        String result="";
        java.awt.datatransfer.Clipboard clipboard = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
        java.awt.datatransfer.Transferable contents = clipboard.getContents(null);
        if (contents==null)
            return "";
        
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
        String p=getClipboardContents();
        if (!p.isEmpty())
        {
            cpu.writeSmsqeString(cpu.addr_regs[1], p, 32000);
        }
    }  /**
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
}
