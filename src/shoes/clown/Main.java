package shoes.clown;

import java.lang.System;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Main {

    private static Logger logger = Logger.getGlobal();

    public static void main(String[] args) {
        // Only a demo so don't need Java sized log lines
        // or separate config files.
        System.setProperty(
                "java.util.logging.SimpleFormatter.format",
                "[%1$tF %1$tT] [%4$-7s] %5$s %n"
        );
        logger.setLevel(Level.INFO);

        JobServer jobServer = new JobServer(8080);
        try {
            jobServer.start();
        } catch (Exception e) {
            logger.log(Level.SEVERE,"failed to start up job server due to exception: %s", e);
            System.exit(500);
        }

        // Capture a shutdown request and wrap up before exiting
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                logger.info("shutdown initiated...");
                System.out.println("shutdown initiated");
                try {
                    jobServer.stop();
                } catch (Exception e) {
                    logger.log(Level.SEVERE,"shutdown ran into exception: %s", e);
                }
            }
        });
    }

}

