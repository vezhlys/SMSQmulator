package smsqmulator;

/**
 * A simple interface to make sure that components including a MonitorPanel have the required methods to communicate with it.
 * 
 * @author and copyright (c) Wolfgang Lenerz 2014
 * 
 * @version 1.00 creation.
 */
public interface MonitorHandler 
{
    /**
     * Send a Command to the monitor.
     * 
     * @param command the command sent
     */
    public void monitorCommand(String command);
    
    /**
     * Act when the divider location changes.
     * 
     * @param newLocation the new x position of the divider.
     */
    public void dividerLocationChanged(int newLocation);
}
