package com.queuectl;

import java.util.List;

public class TestRunner {
    public static void main(String[] args) throws InterruptedException {
        JobStorage storage = new JobStorage();

        // Create a simple job that prints a message. On Windows this will be
        // executed via cmd.exe /c <command>, on *nix via /bin/sh -c <command>.
        String cmd = System.getProperty("os.name").toLowerCase().contains("win") ? "echo Hello from job" : "echo Hello from job";
    String id = "test-" + System.currentTimeMillis();
    Job job = new Job(id, cmd, 3);
        storage.addJob(job);

        List<Job> pending = storage.getPendingJobs();
        System.out.println("Starting " + pending.size() + " worker(s)");
        for (Job j : pending) {
            new Thread(new Worker(storage, j)).start();
        }

        // Wait for the worker(s) to run.
        Thread.sleep(5000);

        System.out.println("Done (sleep finished). Check DB or run CLI to inspect jobs");
    }
}
