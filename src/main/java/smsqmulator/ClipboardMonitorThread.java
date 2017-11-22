package smsqmulator;

/**
 * A thread to monitor changes from the clipboard, by comparing current content with old content.
 * 
 * @version  1.00
 * 
 * @author and copyright (c) wolfgang Lenerz 2017
 * 
 */
public class ClipboardMonitorThread extends Thread
{
    private final ClipboardXfer clipboardXfer;
    private String old = null;
    private volatile boolean isCancelled = false;
    
    public ClipboardMonitorThread(ClipboardXfer cbxfer,String m)
    {
        this.clipboardXfer=cbxfer;
        if (m!=null)
            this.old=m.toLowerCase();
    }
     
    /**
     * Stop this thread.
     */
    public void cancel()
    {
        this.isCancelled=true;
    }
    
    @Override
    /**
     * This checks every half second if the text in the clipboard has changed.
     */
    public void run() 
    {
        while (!this.isCancelled)
        {
            try 
            {
                this.sleep(500);
                String current = this.clipboardXfer.getClipboardContents();
                String lc=current.toLowerCase();
                if (!current.isEmpty() && (this.old == null || !(this.old.equals(lc))))
                {
                    this.clipboardXfer.setNewContent(current);
                    this.old=lc;
                }
            }
            catch (InterruptedException e) 
            {
                return;                 
            }      
        }
    }
}
   