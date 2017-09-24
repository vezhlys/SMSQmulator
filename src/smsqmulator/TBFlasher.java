package smsqmulator;

/**
 * A class to flash the taskbar icon, if it is not focused.
 * 
 * @author W. Lenerz 2016 Copyright (c) W. Lenerz 2016
 * @version 1.00
 **/
public class TBFlasher  extends javax.swing.SwingWorker<Void,Void>
{
    private final javax.swing.JFrame f;
    private final java.awt.Image ic;
    private boolean x;
    private final java.awt.Image inv;
    private final String title;

    /** 
     * Creates a window flasher object which can flash the window's taskbar text.
     * @param fr the frame which which this is to flash
     * @param in0v the normal srite for this
     **/
    public TBFlasher(javax.swing.JFrame fr,java.awt.Image in0v) 
    {
        this.f=fr;
        this.inv=in0v;
        this.ic = f.getIconImage();
        this.title=f.getTitle();
    }
    
    @Override
    public void done()
    {
        this.f.setIconImage(this.ic); 
        this.f.setTitle(this.title);
    }
    
    private void sleep()
    {
        try
        {
            Thread.sleep(600);
        }
        catch (Exception e)
        {}
    }
    
    @Override
    /**
     * Called from the EDT - set the text to the original text or to an empty String.
     * This creates the illusion of flashing.
     */
    public void process(java.util.List<Void> n)
    {
         this.x=!this.x;
         this.f.setIconImage(this.x?this.inv:this.ic);
         this.f.setTitle(this.x?"":this.title);
    }

    /** 
     * "Flash" the window's taskbar text if the window is not focused. 
     * Stop when the window becomes focused.
     * 
     * @return nothing
     **/
    @Override
    protected Void doInBackground()
    {
        while(!f.isFocused())
        {
            sleep();     
            publish ();
        } 
        return null;
    }
}
