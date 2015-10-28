package aa.race.messagesOriginal;

import aa.race.messages.MessageBuffer;

import java.util.*;

// This thread class is responsible for generating the events given the delay intervals, message buffer & message length of new messages
// An event is manifested by the creation of a new message (of fixed length) & writing of that message to the message buffer.
public class EventGenerator extends Thread
{

    // The values for these private attributes will be sent in through the constructor
    private ArrayList<Integer> delayIntervals;    // Array of delay intervals in seconds. for example: if the array is 3, 4, 1... , the first event will be fired, followed by a 3 sec pause, followed by a 2nd event, a 4 sec pause, the 3rd event, a 1 sec pause & so on...
    private int msgLength;                            // length of new messages
    private MessageBuffer msgBuffer;                // reference to message buffer object passed in through the constructor

    // Constructor
    public EventGenerator(ArrayList<Integer> delayIntervals, MessageBuffer msgBuffer, int msgLength)
    {
        this.delayIntervals = delayIntervals;
        this.msgBuffer = msgBuffer;
        this.msgLength = msgLength;
    }

    // Run method. Fires events periodically & ends when all events are fired.
    public void run()
    {
        int pointer = 0;    // pointer used in delayIntervals array

        // repeat until all the events have been fired
        while (pointer < delayIntervals.size())
        {
            // extract delay interval
            int nextDelayInterval = delayIntervals.get(pointer++).intValue();

            // delay for nextDelayInternal number of seconds
            delay(nextDelayInterval * 1000); // remember that delay intervals are given in seconds. so multiply by 1000 before passing into the delay method

            // An event happens here
            Date rightNow = new Date();
            System.out.println("Event " + pointer + " fired at time " + rightNow);
            System.out.println("---");

            // Create event message & append it to message buffer
            msgBuffer.appendToBack(createNewMessage(rightNow, '~', msgLength, '*'));
        }
        System.out.println("Exiting EventGenerator thread...");
    }

    // Creates & returns a new message of the prescribed format
    // Format of message: [timestamp]~[message]~  (tilde is used as separator). Whole length of message should be msgSize (including separators & timestamp)
    private String createNewMessage(Date rightNow, char separator, int msgLength, char contentFiller)
    {
        StringBuffer newMsg = new StringBuffer(((Long) rightNow.getTime()).toString());
        newMsg.append(separator);
        for (int i = 0; i < msgLength; i++)
            newMsg.append(contentFiller);
        newMsg.append(separator);
        return newMsg.toString();
    }

    // Pauses for a number of msec.
    private void delay(int msec)
    {
        try
        {
            Thread.sleep(msec);
        } catch (InterruptedException e)
        {
            //hoooo boy. swallowing exceptions is bad practice.
        }
    }
}
