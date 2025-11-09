package com.queuectl;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.Arrays;
import java.util.Scanner;

public class CLI {
    private final JobStorage storage = new JobStorage();
    private final WorkerManager manager = new WorkerManager(storage);
    private final Config config = new Config();
    private final Gson gson = new Gson();

    public void start() {
        Scanner sc = new Scanner(System.in);
        System.out.println("QueueCTL CLI ready. Type 'help' for commands.");

        while (true) {
            System.out.print("> ");
            String input = sc.nextLine().trim();
            if (input.isEmpty()) continue;
            String[] parts = input.split(" ", 2);
            String cmd = parts[0];
            String args = parts.length > 1 ? parts[1].trim() : "";

            try {
                switch (cmd) {
                    case "enqueue" -> {
                        if (args.isEmpty()) {
                            // interactive enqueue (backwards-compatible behaviour)
                            System.out.print("Enter job id: ");
                            String id = sc.nextLine().trim();
                            System.out.print("Enter command: ");
                            String command = sc.nextLine().trim();
                            System.out.print("Enter maxRetries (enter for default): ");
                            String mr = sc.nextLine().trim();
                            int maxRetries = mr.isEmpty() ? Integer.parseInt(config.get("max_retries", "3")) : Integer.parseInt(mr);
                            Job job = new Job(id, command, maxRetries);
                            storage.addJob(job);
                            System.out.println("✅ Job enqueued: " + id);
                        } else {
                            handleEnqueue(args);
                        }
                    }
                    case "worker" -> handleWorker(args);
                    case "status" -> handleStatus();
                    case "list" -> handleList(args);
                    case "dlq" -> handleDlq(args);
                    case "config" -> handleConfig(args);
                    case "exit" -> {
                        manager.stop();
                        System.exit(0);
                    }
                    case "help" -> printHelp();
                    default -> System.out.println("Unknown command. Try 'help'.");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private void handleEnqueue(String args) {
        if (args.startsWith("{")) {
            JsonObject obj = gson.fromJson(args, JsonObject.class);
            String id = obj.get("id").getAsString();
            String command = obj.get("command").getAsString();
            int maxRetries = obj.has("max_retries") ? obj.get("max_retries").getAsInt()
                    : obj.has("maxRetries") ? obj.get("maxRetries").getAsInt() : Integer.parseInt(config.get("max_retries", "3"));
            Job job = new Job(id, command, maxRetries);
            storage.addJob(job);
            System.out.println("✅ Job enqueued: " + id);
        } else {
            // simple: enqueue <id> <command...> [maxRetries]
            String[] toks = args.split(" ");
            if (toks.length < 2) { System.out.println("Usage: enqueue <id> <command> [maxRetries]"); return; }
            String id = toks[0];
            int maxRetries = Integer.parseInt(config.get("max_retries", "3"));
            String command;
            if (toks.length >= 3) {
                // if the last token is an integer, treat it as maxRetries
                String last = toks[toks.length - 1];
                try {
                    int mr = Integer.parseInt(last);
                    maxRetries = mr;
                    if (toks.length == 3) {
                        command = toks[1];
                    } else {
                        command = String.join(" ", Arrays.copyOfRange(toks, 1, toks.length - 1));
                    }
                } catch (NumberFormatException nfe) {
                    // last token not numeric -> part of command
                    command = String.join(" ", Arrays.copyOfRange(toks, 1, toks.length));
                }
            } else {
                command = toks[1];
            }

            Job job = new Job(id, command, maxRetries);
            storage.addJob(job);
            System.out.println("✅ Job enqueued: " + id);
        }
    }

    private void handleWorker(String args) {
        if (args.isEmpty() || args.startsWith("start")) {
            int count = 1;
            String[] p = args.split(" ");
            for (int i = 1; i < p.length; i++) {
                if (p[i].equals("--count") && i+1 < p.length) {
                    try { count = Integer.parseInt(p[i+1]); } catch (NumberFormatException ignored) {}
                }
            }
            manager.start(count);
            System.out.println("Started " + count + " worker(s)");
        } else if (args.startsWith("stop")) {
            manager.stop();
            System.out.println("Stopped workers");
        } else {
            System.out.println("worker start --count N | worker stop");
        }
    }

    private void handleStatus() {
        int pending = storage.listJobsByState("pending").size();
        int processing = storage.listJobsByState("processing").size();
        int completed = storage.listJobsByState("completed").size();
        int failed = storage.listJobsByState("failed").size();
        int dead = storage.listJobsByState("dead").size();
        System.out.println("Jobs: pending="+pending+" processing="+processing+" completed="+completed+" failed="+failed+" dead="+dead);
        System.out.println("Active workers: " + manager.activeWorkers());
    }

    private void handleList(String args) {
        String state = "pending";
        if (args.startsWith("--state") || args.startsWith("-s")) {
            String[] p = args.split(" ", 2);
            if (p.length==2) state = p[1].trim();
        }
        List<Job> jobs = storage.listJobsByState(state);
        jobs.forEach(System.out::println);
    }

    private void handleDlq(String args) {
        if (args.startsWith("list") || args.isEmpty()) {
            storage.listDLQ().forEach(System.out::println);
        } else if (args.startsWith("retry")) {
            String[] p = args.split(" ");
            if (p.length < 2) { System.out.println("Usage: dlq retry <job-id>"); return; }
            boolean ok = storage.retryDLQJob(p[1]);
            System.out.println(ok ? "Retried job " + p[1] : "Failed to retry job " + p[1]);
        } else {
            System.out.println("dlq list | dlq retry <id>");
        }
    }

    private void handleConfig(String args) {
        if (args.startsWith("set")) {
            String[] p = args.split(" ", 3);
            if (p.length < 3) { System.out.println("Usage: config set <key> <value>"); return; }
            config.set(p[1], p[2]);
            System.out.println("Config set: " + p[1] + "=" + p[2]);
        } else {
            System.out.println("config set <key> <value>");
        }
    }

    private void printHelp() {
        System.out.println("Commands:\n  enqueue <json|id command>  - Add new job\n  worker start --count N | worker stop\n  status\n  list --state <state>\n  dlq list | dlq retry <id>\n  config set <key> <value>\n  help\n  exit");
    }
}
