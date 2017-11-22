package smsqmulator;

/**
 * Popup window to ask for drive assignments (e.g. NFA1_ is what directory?)
 * 
 * @author and copyright (c) 2012-2017 Wolfgang Lenerz
 * @version 
 *  1.05 show to what directory "./" will be expanded.
 *  1.04 removed localization
 *  1.03 ESc closes the window without alterations.
 *  1.02 window pops up at a nicer location (less mouse movment).
 *  1.01 make sure all double file separators are replaced with single ones.
 *  1.00 set better JTextField sizes : min 200 or max string length.
 *  0.02 Really take into account text typed in directly in the TextFields.
 *  0.01 take into account text typed in directly in the TextFields.
 *  0.00 initial version
 */
public class DriveAssignmentsDialog extends javax.swing.JDialog 
{
    private String []options=new String[8];
    private javax.swing.JTextField[]tf1;                            // will contain the text fields
    private javax.swing.JFileChooser fc;
    private boolean acceptChanges=false;
  //  private final java.awt.Frame parent;
   
   /**
    * Creates new modal dialog DriveAssignmentsDialog
    * 
    * @param parent my main Frame
    * @param modal is dialog modal or not (it should be).
    * @param presets a string array with exactly 8 elements containing the current values of the options. 
    * @param deviceName name of device to get drive assignments for.
    * @param mustBeDir flag whether files to select must be directories or not. 
    * @param expandTo name to which "./" dir will be expanded to
    */
    public DriveAssignmentsDialog(java.awt.Frame parent, boolean modal,String []presets,String deviceName,boolean mustBeDir,String expandTo) 
    {
        super(parent, modal);
      //  this.parent=parent;
        if (presets==null ||presets.length!=8)
            return;                         
        initComponents();
        javax.swing.JButton button;
        this.setTitle(Localization.Texts[35]);
        System.arraycopy(presets, 0,this.options,0,8);          // copy old info
        this.tf1=new javax.swing.JTextField[8];                 // there will be one textfield for each dir
        java.awt.Container contentPane = this.getContentPane();
        contentPane.setLayout(new javax.swing.BoxLayout(contentPane,1));
        javax.swing.JPanel contentPanel = new javax.swing.JPanel();
        contentPanel.setLayout(new javax.swing.SpringLayout());;
        java.awt.Dimension size;
        for (int i=0;i<8;i++)                                   // build the textfields
        {
            if (this.options[i]==null)
                this.options[i]="";                             // no null strings allowed
            
            contentPanel.add(new javax.swing.JLabel(deviceName+(1+i)));
            
            this.tf1[i]=new javax.swing.JTextField(this.options[i]);// create the textfield with the right parameters
            size=this.tf1[i].getPreferredSize();
            if (size.width<200)
            {
                size.width=200;
                this.tf1[i].setPreferredSize(size);         // no need to create new dimension
            }
            contentPanel.add(this.tf1[i]);
            
            button=new javax.swing.JButton ("...");         // create the button 
            button.setMnemonic(i);                          // LEAVE THIS, is needed in dirButtonsActionPerformed
            button.addActionListener (new java.awt.event.ActionListener()
            {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent evt)
                {
                    dirButtonsActionPerformed(evt);
                }
            });
            size=button.getPreferredSize();
            size.width=50;
            button.setPreferredSize(new java.awt.Dimension (size));
            contentPanel.add(button);            
        }
        button=new javax.swing.JButton (Localization.Texts[46]);// cancel button...
        button.addActionListener (new java.awt.event.ActionListener()
        {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                OKButtonActionPerformed(false);                 // ... with an action routine
            }
        });
        
        contentPanel.add(button); 
        
        button=new javax.swing.JButton ("OK");                  // and an OK button...
        button.addActionListener (new java.awt.event.ActionListener()
        {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                OKButtonActionPerformed(true);                         
            }
        });
        contentPanel.add(button);
        
        contentPanel.add(new javax.swing.JLabel(""));// bogus component to fill the last row
        
        SpringUtilities.makeCompactGrid(contentPanel,   // use the spring utilities to put everything in a grid
                                 8+1, 3,                    //rows, cols
                                 5, 5,                      //initialX, initialY
                                 5, 5);                     //xPad, yPad
        contentPane.add(contentPanel);
        contentPane.add(new javax.swing.JLabel(Localization.Texts[151]+expandTo));// bogus component to fill the last row
        
        pack();
        this.fc=new javax.swing.JFileChooser();
        if (mustBeDir)
            this.fc.setFileSelectionMode(javax.swing.JFileChooser.DIRECTORIES_ONLY);
        else
            this.fc.setFileSelectionMode(javax.swing.JFileChooser.FILES_AND_DIRECTORIES);
        java.awt.event.ActionListener actionListener = new java.awt.event. ActionListener() // create action routine for esc keystroke in wdw
        {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent actionEvent) 
            {
                OKButtonActionPerformed(false);
            }
        };
        javax.swing.KeyStroke stroke = javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0);
        this.rootPane.registerKeyboardAction(actionListener, stroke, javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW);       
    }
    
    /**
     * Called when the directory choice buttons are actioned, opens a FileChooser dialog to let the user choose drives.
     * 
     * @param evt ignored.
     */
    private void  dirButtonsActionPerformed(java.awt.event.ActionEvent evt)
    {
         javax.swing.JButton button=(javax.swing.JButton)evt.getSource();
         int which=button.getMnemonic();                        // the mnemonic is used to find out which button was clicked           
         String currentDir=this.options[which];
         if (!currentDir.isEmpty())
            fc.setCurrentDirectory(new java.io.File(currentDir));
         if (fc.showOpenDialog(this)==javax.swing.JFileChooser.APPROVE_OPTION)
         {
             this.options[which]=fc.getSelectedFile().getAbsolutePath();
             this.tf1[which].setText(this.options[which]);
         }
    }
    
     /**
     * Accept or cancel buttons pressed.
     * If accept : the new options are copied to the old array.
     * 
     * @param state =<code>true</code> if OK pressed, else false.
     */
    private void OKButtonActionPerformed(boolean state)
    {
        this.acceptChanges=state;                           // are changes accepted or not?
        if (state)
        {
            for (int i=0;i<8;i++)
            {
                this.options[i]=this.tf1[i].getText().replaceAll( java.io.File.separator+"{2,}",java.io.File.separator);
            }
        }
        this.setVisible(false);
    }
    
    /**
     * This is called to show this dialog (do NOT use set.Visible directly).
     * 
     * @param oldnames a String array with 8 elements. If there aren't 8 elements : premature exit!
     * @param x and...
     * @param y ... where to show the dialogue
     * 
     * @return <code>true</code> if changes accepted, <code>false</code> if not.
     */
    public boolean adoptChanges(String []oldnames, int x,int y)
    {
        this.acceptChanges=false;                           // nothing accepted yet
        if (oldnames.length!=8)
        {
            return false;
        }
        this.setLocation(x,y);
        this.setVisible(true);                              // this will freeze this thread until the dialog is closed again
        return this.acceptChanges;
    }
    
    /**
     * Returns the current options array.
     * 
     * @return the current options array.
     */
    public String[]getOptions()
    {
        return this.options.clone();
    }
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
