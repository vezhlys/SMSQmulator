/**
 * These are replacement MOVE instructions for SMSQmulator.
 * 
 * They are (supposed to be) a little faster than those from the m68k package by Tony Headford.
 * 
 * There are 12 groups, corresponding to the 12 source effective addresses, and each group has 8 classes corresponding to the destination addresses.
 * <ul>
 *  <li> MOVEDnx MOVE where source is Dn.</li>
 *  <li> MOVEAnYx MOVE where source is d16(Pc).</li>
 *  <li> MOVEAnXx MOVE where source is d8(Pc,Xn).</li>
 *  <li> MOVEDAnWx MOVE where source is absolute .W.</li>
 *  <li> MOVEDAnLx MOVE where source is absolute .L.</li>
 *  <li> MOVEAnTx MOVE where source is  immediate DATA.</li>
 *  <li> MOVEAnPx MOVE where source is (AN)+ .</li>
 *  <li> MOVEAnMx MOVE where source is -(AN).</li>
 *  <li> MOVEAnIx MOVE where source is d8(An,Xn).</li>
 *  <li> MOVEAnDx MOVE where source is d16(An).</li>
 *  <li> MOVEAnCx MOVE where source is (An).</li>
 *  <li> MOVEAnx MOVE where source is An.</li>
 * </ul>
 * 
 * 
 */
package smsqmulator.cpu.instructions.MOVE;
