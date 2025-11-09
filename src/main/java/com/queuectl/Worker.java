package com.queuectl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Worker implements Runnable {
    private final JobStorage storage;
    private final Job job;

    public Worker(JobStorage storage, Job job) {
        this.storage = storage;
        this.job = job;
    }

    @Override
    public void run() {
        // Use a loop to implement retries instead of recursion. This avoids
        // stack growth and makes retry flow clearer.
        while (true) {
            job.state = "processing";
            storage.updateJob(job);

            try {
                Process process;
                if (System.getProperty("os.name").toLowerCase().contains("win")) {
                    // Windows
                    process = new ProcessBuilder("cmd.exe", "/c", job.command)
                            .redirectErrorStream(true)
                            .start();
                } else {
                    // Linux / macOS
                    process = new ProcessBuilder("/bin/sh", "-c", job.command)
                            .redirectErrorStream(true)
                            .start();
                }

                // Capture output
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("  " + line);
                    }
                }

                int exitCode = process.waitFor();

                if (exitCode == 0) {
                    job.state = "completed";
                    System.out.println("✅ Job " + job.id + " completed successfully.");
                    storage.updateJob(job);
                    break; // done
                } else {
                    // failed attempt
                    job.attempts++;
                    if (job.attempts > job.maxRetries) {
                        job.state = "dead";
                        System.out.println("💀 Job " + job.id + " moved to DLQ after max retries.");
                        storage.updateJob(job);
                        break;
                    } else {
                        job.state = "failed";
                        storage.updateJob(job);
                        int delay = (int) Math.pow(2, job.attempts);
                        System.out.println("⚠️ Job " + job.id + " failed. Retrying in " + delay + " seconds...");
                        try { Thread.sleep(delay * 1000L); } catch (InterruptedException ignored) {}
                        // loop to retry
                    }
                }
            } catch (IOException e) {
                // treat IO errors as a failed attempt
                job.attempts++;
                if (job.attempts > job.maxRetries) {
                    job.state = "dead";
                    System.out.println("💀 Job " + job.id + " moved to DLQ after max retries (IO error).");
                    storage.updateJob(job);
                    break;
                } else {
                    job.state = "failed";
                    storage.updateJob(job);
                    int delay = (int) Math.pow(2, job.attempts);
                    System.out.println("⚠️ Job " + job.id + " encountered IO error. Retrying in " + delay + " seconds...");
                    try { Thread.sleep(delay * 1000L); } catch (InterruptedException ignored) {}
                    // loop to retry
                }
            } catch (InterruptedException e) {
                // Restore interrupted status and stop retrying
                Thread.currentThread().interrupt();
                job.state = "failed";
                System.out.println("⚠️ Job " + job.id + " interrupted. Marking as failed.");
                storage.updateJob(job);
                break;
            }
        }
    }
}