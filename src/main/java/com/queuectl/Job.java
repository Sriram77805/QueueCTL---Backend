package com.queuectl;

import java.time.LocalDateTime;

public class Job {
    public String id;
    public String command;
    public String state;
    public int attempts;
    public int maxRetries;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;

    public Job(String id, String command, int maxRetries) {
        this.id = id;
        this.command = command;
        this.state = "pending";
        this.attempts = 0;
        this.maxRetries = maxRetries;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return String.format("Job{id='%s', state='%s', attempts=%d}", id, state, attempts);
    }
}
