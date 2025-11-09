package com.queuectl;

import java.util.ArrayList;
import java.util.List;

public class WorkerManager {
    private final JobStorage storage;
    private final List<Thread> workers = new ArrayList<>();
    private volatile boolean running = false;

    public WorkerManager(JobStorage storage) {
        this.storage = storage;
    }

    public synchronized void start(int count) {
        if (running) return;
        running = true;
        for (int i = 0; i < count; i++) {
            Thread t = new Thread(() -> {
                while (running && !Thread.currentThread().isInterrupted()) {
                    Job job = storage.claimNextPendingJob();
                    if (job == null) {
                        try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
                        continue;
                    }
                    // Process job in this thread
                    new Worker(storage, job).run();
                }
            }, "queuectl-worker-" + i);
            t.start();
            workers.add(t);
        }
    }

    public synchronized void stop() {
        running = false;
        for (Thread t : workers) {
            t.interrupt();
        }
        workers.clear();
    }

    public int activeWorkers() {
        return workers.size();
    }
}
