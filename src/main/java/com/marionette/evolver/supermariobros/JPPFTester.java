package com.marionette.evolver.supermariobros;

import org.jppf.JPPFException;
import org.jppf.client.JPPFClient;
import org.jppf.client.JPPFJob;
import org.jppf.node.protocol.AbstractTask;

/**
 * Created by Mitchell on 6/3/2016.
 */
public class JPPFTester {
    public static void main(String[] args) throws JPPFException {
        //System.out.println(Thread.currentThread().getContextClassLoader().getResource(".").getFile());
        //BasicConfigurator.configure();
        //Logger.getRootLogger().setLevel(Level.DEBUG);

        //startVisualization();

        JPPFClient client = new JPPFClient("Test Client");
        JPPFJob job = new JPPFJob("Test Job");

        for (int i = 0; i < 12; i++) {
            final int finalI = i;
            job.add(new AbstractTask<String>() {
                private int anInt = finalI;

                @Override
                public void run() {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        setThrowable(e);
                    }
                    setResult("Test " + finalI + " \"" + Thread.currentThread().getContextClassLoader().getResource(".").getFile() + "\"");
                }
            });
        }

        job.setBlocking(false);

        client.submitJob(job);
        job.awaitResults().stream().forEach(task -> System.out.println(task.getResult()));

        client.close();
    }
}
