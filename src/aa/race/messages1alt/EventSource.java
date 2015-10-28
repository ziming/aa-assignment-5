package aa.race.messages1alt;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Properties;
import java.util.Random;

// This is the main class to run
public class EventSource {

    // refer to eventsource.properties file to understand what these attributes are for.
    // Their values will be read from the property file
    private String mode;
    private int period;
    private double eventFrequency;
    private int bufferSize;
    private int msgSize;
    private String ipOfEventListener;
    private int portOfServer;
    private boolean dropNewCharWhenBufferFull;
    private int numPullClients;

    // main method. Life begins here.
    public static void main(String[] args) throws IOException {
        new EventSource().startExperiment();
    }

    // Constructor.
    public EventSource() {
        readPptyFile();
    }

    // Everything is done here
    public void startExperiment() {
        // delayIntervals is an ArrayList of Integers representing the intervals (in seconds) between event fires
        ArrayList<Integer> delayIntervals = new ArrayList<Integer>();
        getDelayIntervals(delayIntervals, eventFrequency, period);

        // create Message Buffer object
        MessageBuffer msgBuffer = new MessageBuffer(bufferSize, dropNewCharWhenBufferFull);

        // This is the Event Generator which uses delayIntervals to fire events. Each event is basically a message of msgSize characters in length.
        // Every time an event fires, the new message is inserted into the message buffer
        EventGenerator eventGenerator = new EventGenerator(delayIntervals, msgBuffer, msgSize);
        eventGenerator.start();

        // push mode
        if (mode.equalsIgnoreCase("push")) {
            System.out.println("PUSH mode no long supported...");
            System.exit(1);

//      System.out.println("PUSH mode starting...");
//      System.out.println("  IMPORTANT: aa.race.messages.PushServer should already be running, else you will get an error!");
//      System.out.println("---");
//
//      aa.race.messages.PushClient pushClient = new aa.race.messages.PushClient (msgBuffer, ipOfEventListener, portOfServer, period);
//      pushClient.start();
        }
        // pull mode
        else {
            System.out.println("PULL mode starting...");
            System.out.println("  INSTRUCTION: Run aa.race.messages.PullClient in separate DOS window.");
            System.out.println("---");

            for (int i = 0; i < numPullClients; i++) {
                PullServer pullServer = new PullServer(msgBuffer, portOfServer + i, period);
                pullServer.start();
                // System.out.println("started on " +(portOfServer+i);
            }
        }
    }

    // -------------------------------------------------------------------
    // This method calculates the delay intervals based on the experimental period & event frequency (i.e. how many events are to be fired within 60 secs)
    // period is in seconds, not msec
    private void getDelayIntervals(ArrayList<Integer> delayIntervals, double eventFrequency, int period) {
        int noOfEvents = (int) (eventFrequency / 60 * period);
        System.out.println("Number of events to be generated over period of " + period + " seconds: " + noOfEvents);

        // randomly generate numbers within the period
        ArrayList<Integer> temp = new ArrayList<Integer>();
        Random r = new Random();
        while (temp.size() <= noOfEvents) {
            Integer rand = new Integer(r.nextInt(period));
            if (!temp.contains(rand))
                temp.add(new Integer(rand));
        }
        Collections.sort(temp);

        // create new ArrayList for delay intervals
        delayIntervals.clear();
        for (int i = 0; i < temp.size() - 1; i++) {
            int smaller = temp.get(i).intValue();
            int bigger = temp.get(i + 1).intValue();
            delayIntervals.add(new Integer(bigger - smaller));
        }
        return;
    }

    // -------------------------------------------------------------------
    private void readPptyFile() {
        // Read properties file.
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream("eventsource.properties"));

            mode = properties.getProperty("mode");
            if (!(mode.equalsIgnoreCase("push") || mode.equalsIgnoreCase("pull"))) {
                System.err.println("*** FATAL ERROR: mode property in eventsource.properties must be set to pull or push only");
                System.exit(1);
            }
            period = Integer.parseInt(properties.getProperty("period"));
            eventFrequency = Double.parseDouble(properties.getProperty("eventFrequency"));
            if (eventFrequency > 60) {
                System.out.println("*** FATAL ERROR: eventFrequency property in eventsource.properites should not be set to more than 60");
                System.exit(1);
            }
            bufferSize = Integer.parseInt(properties.getProperty("bufferSize"));
            msgSize = Integer.parseInt(properties.getProperty("msgSize"));
            ipOfEventListener = properties.getProperty("ipOfEventListener");
            portOfServer = Integer.parseInt(properties.getProperty("portOfServer"));
            dropNewCharWhenBufferFull = Boolean.parseBoolean(properties.getProperty("dropNewCharWhenBufferFull"));
            numPullClients = Integer.parseInt(properties.getProperty("numPullClients"));

            System.out.println("Read the following attributes from the property file:");
            System.out.println("  Mode.....................: " + mode);
            System.out.println("  Period...................: " + period);
            System.out.println("  Event Frequency..........: " + eventFrequency);
            System.out.println("  Buffer Size..............: " + bufferSize);
            System.out.println("  Message Size.............: " + msgSize);
            System.out.println("  IP of Event Listener.....: " + ipOfEventListener);
            System.out.println("  Port of Server...........: " + portOfServer);
            System.out.println("  DropNewCharWhenBufferFull: " + dropNewCharWhenBufferFull);
            System.out.println("---");
        } catch (IOException e) {
            System.err.println("*** FATAL ERROR: " + e.getMessage());
            System.err.println("*** LIKELY REASON: Ensure that eventsource.properties is found in the same directory as aa.race.messages.EventSource.class and that it is readable");
            System.exit(1);
        } catch (Exception e) {
            System.err.println("*** FATAL ERROR: " + e.getMessage());
            System.err.println("*** LIKELY REASON: Ensure that eventsource.properties is correctly configured");
            System.exit(1);
        }
    }
}


