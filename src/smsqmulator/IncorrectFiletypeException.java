
package smsqmulator;

/**
 * This exception should get thrown when the WIN, MEM or FLP drivers are fed files that are not of the correct type.
 * 
 * @author and copyright (c)  wolfgang Lenerz 2012
 */
public class IncorrectFiletypeException extends Exception
{
    /**
     * This just calls super.
     */
    public IncorrectFiletypeException() 
    {
        super();
    } 
    
    /**
     * This just calls super.
     * @param message the message to be displayed.
     */
    public IncorrectFiletypeException(String message) 
    {
        super(message);
    }
}
