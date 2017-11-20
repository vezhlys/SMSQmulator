package smsqmulator;

/**
 * A dialog that lets you choose on which monitor in a multi-monitor environment the full screen mode will be displayed.
 * 
 * @author and copyright (c) Wolfgang Lenerz 2016
 * @version
 * 1.00 initial version.
 */
public class MultiMonitorDialog extends javax.swing.JDialog implements javax.swing.event.ListSelectionListener
{
    private int choice=-1;                                      // number of monitor chosen by user, none yet.
    
    /**
     * Creates new form MultiMonitorDialog
     * @param parent standard
     * @param modal standard
     * @param gds the screen devices available
     * @param current the number of the monitor currently chosen, will be highlit in the jlist.
     */
    public MultiMonitorDialog(java.awt.Frame parent, boolean modal,java.awt.GraphicsDevice[] gds, int current)
    {
        super(parent, modal);
        
        javax.swing.JScrollPane jScrollPane1 = new javax.swing.JScrollPane();
        javax.swing.JList jList1 = new javax.swing.JList();
        javax.swing.JLabel jL = new javax.swing.JLabel(Localization.Texts[139]+".");
        javax.swing.JButton button = new javax.swing.JButton("OK");
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        jScrollPane1.setViewportView(jList1);
        
        getContentPane().setLayout(new javax.swing.BoxLayout(this.getContentPane(),javax.swing.BoxLayout.Y_AXIS));
        this.add(jL);
        this.add(jScrollPane1);
        this.add(button);

        this.setTitle(Localization.Texts[139]);
        javax.swing.DefaultListModel<String> listModel = new javax.swing.DefaultListModel<>();
        StringBuilder sb=new StringBuilder (200);
        for (int i=0;i<gds.length;i++)                          // show description of all monitors
        {
            sb.setLength(0);                                    // nothing yet
            java.awt.GraphicsDevice device = gds[i];
            java.awt.GraphicsConfiguration gd=device.getDefaultConfiguration();
          //  sb.append(count).append(" : ").append(device.getIDstring()).append ("  ");
            sb.append(i).append(" : ");
            java.awt.Rectangle bounds = gd.getBounds();
            sb.append(bounds.width).append(" X ").append(bounds.height).append(Localization.Texts[140]);
            sb.append(bounds.x).append(" X ").append(bounds.y);
            listModel.addElement(sb.toString());
        }            
        
        jList1.setModel(listModel);
        javax.swing.ListSelectionModel lsm = jList1.getSelectionModel();
        lsm.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jList1.setSelectedIndex(current);
        jList1.addListSelectionListener(this);
        
        pack();
        final javax.swing.AbstractAction escapeAction = new javax.swing.AbstractAction() 
        {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent ae) 
            {
                dispose();                                          // this is called when user hits ESC
            }
        };

        getRootPane().getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW)
                    .put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0), "ESCAPE_KEY");
        getRootPane().getActionMap().put("ESCAPE_KEY", escapeAction);
        
        button.addActionListener (new java.awt.event.ActionListener()
        {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                dispose();
            }
        });
    }
    
    /**
     * This overrides the normal setVisible method - and just does nothing at all.
     * Use getChoice() instead.
     * 
     * @param f ignored.
     */
    @Override
    public void setVisible (boolean f)
    {   
    }
    
    /**
     * Displays the dialog and gets the number of the monitor chosen by the user.
     * 
     * @return the number of the monitor chosen by the user, -1 if no choice made.
     */
    public int getChoice()
    {
        super.setVisible(true);
        return this.choice;
    }

    /**
     * Sets the currently chosen monitor0
     * 
     * @param e the event.
     */
    @Override
    public void valueChanged(javax.swing.event.ListSelectionEvent e)
    {
            this.choice = e.getFirstIndex();
    }
}
