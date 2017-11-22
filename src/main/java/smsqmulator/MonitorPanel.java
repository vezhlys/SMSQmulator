package smsqmulator;

/**
 *  This is the JPanel containing the monitor windows and command line.
 *  There are two windows : one (the one to the right) displays the instructions, e.g. when doing a "di" command.
 *  The other (to the left) displays data, e.g. when doing an "d" command.
 *  There is also a command line beneath these two windows, with a primitive 50 element command line history accessible through
 *  the up/down keystrokes.
 * 
 *  @author and copyright (c) Wolfgang Lenerz 2014-2016.
 * 
 *  @version
 *  1.02 implement MouseWheelListener to increase font size with CTRL+mousewheel
 *  1.01 history also implements down keystroke.
 *  1.00 creation (copied from the old panel in the old MonitorGui).
 */
public class MonitorPanel extends javax.swing.JPanel
{
    private int commandlineHistoryCounter=0;                    // monitor command line history stores up to 50 elements 
    private final int commandlineHistoryMaxSize=50;
    private CircularBuffer <String>commandlineHistory;          // monitor command line history stores up to 50 elements 
    private final javax.swing.JTextArea registerTextArea = new javax.swing.JTextArea();
    private final javax.swing.JTextArea dataTextArea = new javax.swing.JTextArea();
    private final javax.swing.JTextField monitorCommandsTextField = new javax.swing.JTextField();
    private final javax.swing.JSplitPane jSplitPane1 = new javax.swing.JSplitPane();
    
    /**
     * Creates the object.
     * 
     * @param handler the enclosing panel.
     */
    public MonitorPanel(final MonitorHandler handler)
    {
        super();
        final javax.swing.JScrollPane jScrollPane1 = new javax.swing.JScrollPane();
        final javax.swing.JPanel dataPanel = new javax.swing.JPanel();
        final javax.swing.JScrollPane jScrollPane2 = new javax.swing.JScrollPane();
        final javax.swing.JPanel registerPanel = new javax.swing.JPanel();
        
        this.jSplitPane1.setDividerLocation(400);
        this.jSplitPane1.addPropertyChangeListener(new java.beans.PropertyChangeListener() 
        {
            @Override
            public void propertyChange(java.beans.PropertyChangeEvent evt) 
            {
                javax.swing.JSplitPane sourceSplitPane = (javax.swing.JSplitPane) evt.getSource();
                if (evt.getPropertyName().equals(javax.swing.JSplitPane.LAST_DIVIDER_LOCATION_PROPERTY)) 
                    handler.dividerLocationChanged(sourceSplitPane.getDividerLocation());
            }
        });
        
        registerPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        this.registerTextArea.setEditable(false);
        this.registerTextArea.setColumns(20);
        this.registerTextArea.setFont(new java.awt.Font("Monospaced", 0, 10)); 
        this.registerTextArea.setRows(5);
        jScrollPane1.setViewportView(this.registerTextArea);
        MWListener mwlR=new MWListener(this.registerTextArea,jScrollPane1);        
        this.registerTextArea.addMouseWheelListener(mwlR);
        
        javax.swing.GroupLayout registerPanelLayout = new javax.swing.GroupLayout(registerPanel);
        registerPanel.setLayout(registerPanelLayout);
        registerPanelLayout.setHorizontalGroup(
            registerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 395, Short.MAX_VALUE)
        );
        registerPanelLayout.setVerticalGroup(
            registerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 257, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        
        this.jSplitPane1.setLeftComponent(registerPanel);

        dataPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        this.dataTextArea.setEditable(false);
        this.dataTextArea.setColumns(20);
        this.dataTextArea.setFont(new java.awt.Font("Monospaced", 0, 10)); // NOI18N
        this.dataTextArea.setRows(5);
        jScrollPane2.setViewportView(this.dataTextArea);
        MWListener mwlD=new MWListener(this.dataTextArea,jScrollPane2);        
        this.dataTextArea.addMouseWheelListener(mwlD);
        
         javax.swing.GroupLayout dataPanelLayout = new javax.swing.GroupLayout(dataPanel);
        dataPanel.setLayout(dataPanelLayout);
        dataPanelLayout.setHorizontalGroup(
            dataPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 521, Short.MAX_VALUE)
        );
        dataPanelLayout.setVerticalGroup(
            dataPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 257, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        this.jSplitPane1.setRightComponent(dataPanel);

        this.monitorCommandsTextField.setFont(new java.awt.Font("Courier New", 0, 12));
        this. monitorCommandsTextField.addActionListener(new java.awt.event.ActionListener()
        {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) 
            {
                String command=monitorCommandsTextField.getText();
                handler.monitorCommand(command);
                monitorCommandsTextField.setText("");
                commandlineHistory.addNoDoubles(command);
                commandlineHistoryCounter=0;
            }
        });
        
        // listen to up/down keystrokes to scroll thgough command line history
        this.monitorCommandsTextField.addKeyListener(new java.awt.event.KeyAdapter() 
        {
            @Override
            public void keyPressed(java.awt.event.KeyEvent evt) 
            {
                if (evt.getKeyCode()==java.awt.event.KeyEvent.VK_UP && commandlineHistory!=null)
                {
                    monitorCommandsTextField.setText(commandlineHistory.get (commandlineHistoryCounter));
                    commandlineHistoryCounter++;
                    if (commandlineHistoryCounter>commandlineHistoryMaxSize)
                        commandlineHistoryCounter=0;                    
                }
                
                if (evt.getKeyCode()==java.awt.event.KeyEvent.VK_DOWN && commandlineHistory!=null)
                {
                    monitorCommandsTextField.setText(commandlineHistory.get (commandlineHistoryCounter));
                    commandlineHistoryCounter--;
                    if (commandlineHistoryCounter<0)
                        commandlineHistoryCounter=commandlineHistoryMaxSize-1;                    
                }
            }
        });

        javax.swing.GroupLayout monitorPanelLayout = new javax.swing.GroupLayout(this);
        this.setLayout(monitorPanelLayout);
        monitorPanelLayout.setHorizontalGroup(
            monitorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(monitorPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(this.monitorCommandsTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 936, Short.MAX_VALUE)
                .addContainerGap())
            .addGroup(monitorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(monitorPanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(this.jSplitPane1)
                    .addContainerGap()))
        );
        monitorPanelLayout.setVerticalGroup(
            monitorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, monitorPanelLayout.createSequentialGroup()
                .addContainerGap(281, Short.MAX_VALUE)
                .addComponent(this.monitorCommandsTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
            .addGroup(monitorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(monitorPanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(this.jSplitPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(36, Short.MAX_VALUE)))
        );
        
        
        dataPanel.setFocusable(false);
        this.dataTextArea.setFocusable(false);
        this.registerTextArea.setFocusable(false);
        
        try
        {
            this.commandlineHistory = new CircularBuffer <String>(this.commandlineHistoryMaxSize);
        }
        catch (IllegalArgumentException e)
        { 
            this.commandlineHistory = null;
        }
    }
    
     /**
     * Gets the left monitor "wdw".
     * 
     * @return the <code>javax.swing.JTextArea</code> corresponds to the left wdw.
     */
    public javax.swing.JTextArea getDataLogger()
    {
        return this.dataTextArea;
    } 
    
    /**
     * Gets the right monitor wdw.
     * 
     * @return the <code>javax.swing.JTextArea</code> corresponds to the right wdw.
     */
    public javax.swing.JTextArea getRegLogger()
    {
        return this.registerTextArea;
    }
    
    /**
     * Gets the command line wdw.
     * 
     * @return the command line "wdw", a <code>javax.swing.JTextField</code>.
     */
    public javax.swing.JTextField getInputWindow()
    {
        return this.monitorCommandsTextField;
    }
    
    /**
     * Sets the divider location in the the split panel.
     * 
     * @param temp the new divider location in the split panel.
     */
    public void setDividerLocation(int temp)
    {
        this.jSplitPane1.setDividerLocation(temp);
    }
    
    
    private class MWListener implements java.awt.event.MouseWheelListener
    {
        private final javax.swing.JTextArea ta;
        private final javax.swing.JScrollPane jScrollPane;
      
        private MWListener (javax.swing.JTextArea t,final javax.swing.JScrollPane myScrollPane)
        {
            this.ta=t;
            this.jScrollPane=myScrollPane;
        }

        @Override
        public void mouseWheelMoved(java.awt.event.MouseWheelEvent e)
        {
            if (e.isControlDown()) 
            {    
                java.awt.Font f=this.ta.getFont();
                float s=f.getSize();
                if (e.getWheelRotation() < 0) 
                {            
                    s++;
                    if (s>60)
                       s=60;
                } 
                else 
                {   
                    s--;
                    if (s<4)
                       s=4;          
                }
                java.awt.Font newFont = f.deriveFont(s);
                this.ta.setFont(newFont);
                e.consume();
            }    
            else
            {
                this.jScrollPane.dispatchEvent(e);
            }
        }
    }    
}
