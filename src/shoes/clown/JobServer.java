package shoes.clown;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Server which takes job requests and updates progress of tasks
 *
 * TODO:
 *  - Allow multiple jobs
 *  - Limit on multiple jobs and either
 *    + Respond busy
 *    + Put into a queue to be worked though
 */
public class JobServer {

    private volatile static ProcessingState sharedState = ProcessingState.NEW;
    // percentage should only go from 0 to 100. As it's only a demo -1 is the default
    private volatile static byte percentageComplete = -1;
    private static Logger logger = Logger.getGlobal();

    private ProcessingRunnable processor;
    private Thread processorThread;
    private HttpServer jobServer;
    private int port;

    public JobServer(int port) {
        this.processor = new ProcessingRunnable();
        this.port = port;
    }

    public void start()  throws IOException {
        this.processor = new ProcessingRunnable();
        this.processorThread = new Thread(processor);
        this.processorThread.start();

        this.jobServer = HttpServer.create(new InetSocketAddress(this.port), 0);
        this.jobServer.createContext("/", new JobHandler());
        this.jobServer.setExecutor(null);
        this.jobServer.start();
    }

    /**
     * Stops HTTP server and processing Runnable.
     *
     * The order being
     * 1. Stop processor
     * 2. Stop HTTP Server
     */
    public void stop() {
        try {
            logger.info("shutting down processing thread");
            this.processor.stop();
            this.processorThread.join();

            logger.info("shutting down http server");
            this.jobServer.stop(0);

        } catch (Exception e) {
            logger.log(Level.SEVERE, "shutdown ran into exception", e);
        }
    }

    static class JobHandler implements HttpHandler {
        /**
         * class constructor
         */
        public JobHandler() {}

        @Override
        public void handle(HttpExchange t) throws IOException {

            if ((percentageComplete == 100 || percentageComplete < 0) &&
                    t.getRequestMethod().equalsIgnoreCase("POST")) {

                logger.info("requesting new job");
                percentageComplete = 0;
            }

            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            String response = String.format(
                    "%s - processing state: %s (%s%% complete)\n",
                    dateFormat.format(new Date()),
                    sharedState,
                    percentageComplete
            );

            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    /**
     * Processes imaginary job
     */
    private static class ProcessingRunnable implements Runnable {

        private boolean doStop = false;

        /**
         * class constructor
         */
        public ProcessingRunnable() {
            logger.info(String.format("starting processing runnable with state: %s", sharedState));
        }

        /**
         * Marks runnable for stop state and sets shared state to FINISHED
         * Will allow any current running jobs to complete
         */
        public synchronized void stop() {
            this.doStop = true;
            logger.info(String.format("process state = %s", sharedState));
        }

        private synchronized boolean keepRunning() {
            return this.doStop == false;
        }


        private void doSleep(int seconds) {
            try {
                Thread.sleep(seconds * 1000L);
            } catch (InterruptedException e) {
                logger.log(Level.SEVERE, "processing sleep interrupted", e);
            }
        }
        @Override
        public void run() {
            while(keepRunning()) {

                if (percentageComplete >= 0 && percentageComplete < 100) {
                    if (percentageComplete == 0) {
                        sharedState = ProcessingState.WORKING;
                    }
                    percentageComplete += 1;
                }

                if (percentageComplete == 100) {
                    sharedState = ProcessingState.IDLE;
                }

                doSleep(1);
                logger.info(String.format("state: %s pct complete: %s", sharedState, percentageComplete));
            }
            // Stop has been requested so we'll closing out
            sharedState = ProcessingState.FINISHED;
        }
    }
}
