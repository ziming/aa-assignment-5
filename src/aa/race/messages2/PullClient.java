package aa.race.messages2;

import java.io.*;
import java.net.Socket;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

// This class is only used in PULL mode. It represents a pull client which contacts the pull server
// which should be listening at a port, waiting for the client to send a request for messages
public class PullClient {

    // keeps track of experiment's start time
    private long exptStartTime;

    // the following variables are used for statistical collection of msg delay values
    long totalDelayRecorded = 0;
    int noOfDelaysRecorded = 0;
    long maxDelayRecorded = 0;
    long minDelayRecorded = Long.MAX_VALUE;
    // the following variable keeps track of the network traffic costs in number of characters
    int noOfCharSentThruNetwork = 0;

    // these private attributes will have their values read from pullcient.properties
    private String ipOfPullServer;    // IP address of Pull Server
    private int portOfServer;            // port number used by Pull Server
    private int pullInterval;            // interval between pull requests sent to Pull Server
    private int period;                // period of experiment - how long does this experiment last?
    private int commOverhead;
    private int totalMsg = 0;

    private static Lock lock = new ReentrantLock();

    // main method. This is where life begins.
    public static void main(String args[]) {
        int id = Integer.parseInt(args[0]);
        new PullClient().startExperiment(id);
    }

    // Constructor
    public PullClient() {
        // populate private attributes with values from pullclient.properties
        readPptyFile();
    }

    // returns true when its time to stop this whole thing
    private boolean exptTimeUp() {
        return (new Date().getTime() - exptStartTime >= (period * 1000));
    }

    // does what the name implies. will print out to screen information about the message (inputLine) received
    private void processMsgAndCollectStats(String inputLine) {

        // inputLine is in this format: [timestamp of msg1]~[msg 1]~[timestamp of msg2]~[msg 2]~[timestamp of msg3]~[msg 3]~...etc
        // No error checking is done; assumption is that inputLine is in the correct format
        String[] temp = inputLine.split("~");
        int noOfMsg = temp.length / 2;
        totalMsg += noOfMsg;
        System.out.println("Number of messages received: " + noOfMsg);

        long currentTime = (new Date()).getTime();

        // retrieve time stamp & message text from received message
        for (int i = 0; i < temp.length; i += 2) {
            long msgTime = Long.parseLong(temp[i]);
            String msg = temp[i + 1];
            System.out.println("Message timestamp: " + msgTime);
            System.out.println("Message text: " + msg);
            long delay = currentTime - msgTime;
            System.out.println("Message is delayed by: " + delay + " msec");

            // Collect stats for delays
            totalDelayRecorded += delay;
            noOfDelaysRecorded++;
            maxDelayRecorded = Math.max(maxDelayRecorded, delay);
            minDelayRecorded = Math.min(minDelayRecorded, delay);
        }
    }

    // Everything is done here
    public void startExperiment(int id) {
        exptStartTime = (new Date()).getTime();

        // open sockets to connect to server (Pull Server)
        Socket socket = null;
        PrintWriter out = null;
        BufferedReader in = null;

        try {
            socket = new Socket(ipOfPullServer, portOfServer + id); // so 3 different pull server
            out = new PrintWriter(socket.getOutputStream(), true); // output stream is changed "print" to the "server"
            in = new BufferedReader(new InputStreamReader(socket.getInputStream())); // input comes from pull server

            BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in)); // read from pull server

            while (!exptTimeUp()) { // while time is not up yet. default 63 seconds
                System.out.println("Polling Pull Server now...");
                out.println("request"); // send something to the server socket. Doesn't have to be "request"; this is not checked at the Pull Server

                // update noOfCharSentThruNetwork for request sent
                noOfCharSentThruNetwork += commOverhead;

                // this statement retrieves data from the server socket
                String inputLine = in.readLine(); // each pull client read from different pull server, each pull server grab from a single MessageBuffer so ...

                // update noOfCharSentThruNetwork for reply received
                noOfCharSentThruNetwork += commOverhead;

                // update noOfCharSentThruNetwork for messages in reply received
                if (inputLine != null && !inputLine.equals("")) {
                    noOfCharSentThruNetwork += inputLine.length();
                    processMsgAndCollectStats(inputLine);
                } else {
                    System.out.println("No message retrieved");
                }
                System.out.println("---");

                // wait
                delay(pullInterval);
            }
            // Will arrive here when period for experiment is up.
            // Clean up
            out.close();
            in.close();
            stdIn.close();
            socket.close();
            System.out.println("aa.race.messages.PullClient exited normally...");
        } catch (IOException e) {
            System.err.println("*** ERROR: " + e.getMessage());
            System.err.println("*** LIKELY REASON: aa.race.messages.EventSource is not running, or has completed running");
        } catch (NumberFormatException e) {
            System.err.println("*** FATAL ERROR: " + e.getMessage());
            System.err.println("*** LIKELY REASON: Message format of message from server is incorrect.");
        } finally {
            // Show stats for delays
            if (noOfDelaysRecorded != 0) {

                System.out.println("Message delay stats:");
                System.out.println("  Average delay recorded: " + totalDelayRecorded / noOfDelaysRecorded + " msec");
                System.out.println("  Maximum delay recorded: " + maxDelayRecorded + " msec");
                System.out.println("  Minimum delay recorded: " + minDelayRecorded + " msec");
            }
            // Show stats for network traffic
            System.out.println("Events read by this client: " + totalMsg);
            System.out.println("Network traffic: " + noOfCharSentThruNetwork + " characters");
            System.out.println("---");
        }
    }

    // Reads pullclient.properties and populates private instance variables with values in property file
    private void readPptyFile() {

        // Read properties file.
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream("pullclient.properties"));
            portOfServer = Integer.parseInt(properties.getProperty("portOfServer"));
            ipOfPullServer = properties.getProperty("ipOfPullServer");
            pullInterval = Integer.parseInt(properties.getProperty("pullInterval"));
            period = Integer.parseInt(properties.getProperty("period"));
            commOverhead = Integer.parseInt(properties.getProperty("commOverhead"));

            System.out.println("Read the following attributes from pullclient.properties:");
            System.out.println("  portOfServer....: " + portOfServer);
            System.out.println("  ipOfPullServer..: " + ipOfPullServer);
            System.out.println("  pullInterval....: " + pullInterval);
            System.out.println("  period..........: " + period);
            System.out.println("  commOverhead....: " + commOverhead);
            System.out.println("---");
        } catch (IOException e) {
            System.err.println("*** ERROR: " + e.getMessage());
            System.err.println("*** LIKELY REASON: Ensure that pullcient.properties is found in the same directory as aa.race.messages.EventSource.class and that it is readable");
            System.exit(1);
        } catch (Exception e) {
            System.err.println("*** FATAL ERROR: " + e.getMessage());
            System.err.println("*** LIKELY REASON: Ensure that pullclient.properties is correctly configured");
            System.exit(1);
        }
    }

    // sleeps for a few msecs
    private static void delay(int msec) {
        try {
            Thread.sleep(msec);
        } catch (InterruptedException e) {
            //wah lau!  do not swallow exceptions!
        }
    }
}