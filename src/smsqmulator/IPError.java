package smsqmulator;

/**
 * A data class containing entirely made up IP error strings.
 * 
 * The error strings remain in english.
 * 
 * @author Wolfgang Lenerz copyright (c) 2016
 * @version 0.00
 */
public class IPError
{
    private static final java.util.Map<Integer,String> map=new java.util.HashMap<>();
    
    public IPError()
    {   
        map.put(0,"NO ERROR");
        map.put(1,"UNTYPED SOCKET - can't do I/O");
        map.put(2,"GENERIC SOCKET - can't do I/O");
        map.put(3,"SOCKET IS ALREADY CONNECTED - can't connect again");
        map.put(4,"WRONG HOST ADDRESS :- The host address is wrong or malformed");
        map.put(5,"WRONG PORT NUMBER");
        map.put(6,"I/O EXCEPTION DURING SOCKET CREATION");
        map.put(7,"SECURITY MANAGER VIOLATION DURING SOCKET CREATION");
        map.put(8,"UNKNOWN ERROR");
        map.put(9,"SOCKET IS NOT CONNECTED");
        map.put(10,"I/O EXCEPTION WHEN SENDING BYTES");
        map.put(11,"I/O EXCEPTION WHEN RECEIVING BYTES");
        map.put(12,"ILLEGAL SOCKET CONVERSION");
        map.put(13,"SERVER CHANNEL CLOSED");
        map.put(14,"FUNCTION NOT IMPLEMENTED");
        map.put(15,"Connection refused");
        map.put(16,"Socket already bound");
    }
    /**
     * Gets the String corresponding to an error.
     * 
     * @param index the error number.
     * 
     * @return the String corresponding to the error or NULL if this is an unknown error number.
     */
    public static final String getErrorString(int index)
    {
        return map.get(index);
    }
}
