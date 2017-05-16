package gts.example;


import org.junit.Test;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @since 5/15/17.
 */
public class SmokeTestClients {


    @Test
    public void fiveThreads() throws InterruptedException {
        ExecutorService serv = Executors.newFixedThreadPool(5);
        for (int i = 0; i++<5;) {
            serv.submit(new CliThread(2000000));
        }
        serv.awaitTermination(60, TimeUnit.SECONDS);
        serv.shutdown();
    }

    @Test
    public void someThreadsAndAShutdown() throws InterruptedException {
        ExecutorService serv = Executors.newFixedThreadPool(5);
        for (int i = 0; i++<4;) {
            serv.submit(new CliThread(2000000000));
        }
        serv.submit(new ShutdownCli(500));
        serv.awaitTermination(60, TimeUnit.SECONDS);
        serv.shutdown();

    }

    @Test
    public void gracefulConnectionHandling() {
        ExecutorService serv = Executors.newFixedThreadPool(5);
        for (int i = 0; i++<25;) {
            serv.submit(new CliThread(50000));
        }
        try {
            serv.awaitTermination(20, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        serv.shutdown();

    }


    public class CliThread implements Runnable {

        private int numMessages;
        Socket clientSocket = null;
        DataOutputStream outToServer = null;

        public CliThread(int numMessages) {
            this.numMessages = numMessages;
        }

        void shutdown() throws IOException {
            clientSocket.close();
        }

        @Override
        public void run() {
            try {
                clientSocket = new Socket("localhost", 4000);
                outToServer = new DataOutputStream(clientSocket.getOutputStream());
                for (int i=0; i++<numMessages;){
                    outToServer.write(String.format("%09d\n", new Double(Math.random() * 999999999).intValue()).getBytes());
                }
                shutdown();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public class ShutdownCli extends CliThread {

        public ShutdownCli(int numMessages) {
            super(numMessages);
        }

        @Override void shutdown() throws IOException {
            outToServer.write("terminate\n".getBytes());
            super.shutdown();
        }
    }

}

