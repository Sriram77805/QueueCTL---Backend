package com.queuectl;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class JobStorage {
    private static final String DB_URL = "jdbc:sqlite:jobs.db";

    public JobStorage() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS jobs (
                    id TEXT PRIMARY KEY,
                    command TEXT,
                    state TEXT,
                    attempts INTEGER,
                    maxRetries INTEGER
                )
            """);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addJob(Job job) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement("INSERT INTO jobs VALUES (?,?,?,?,?)")) {
            ps.setString(1, job.id);
            ps.setString(2, job.command);
            ps.setString(3, job.state);
            ps.setInt(4, job.attempts);
            ps.setInt(5, job.maxRetries);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Job> getPendingJobs() {
        List<Job> jobs = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM jobs WHERE state='pending'");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Job job = new Job(
                    rs.getString("id"),
                    rs.getString("command"),
                    rs.getInt("maxRetries")
                );
                job.state = rs.getString("state");
                job.attempts = rs.getInt("attempts");
                jobs.add(job);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return jobs;
    }

    /**
     * Atomically claim a single pending job. Returns the claimed Job or null if none.
     * Implementation: select a pending job id, then update it to 'processing' only if
     * it is still pending. This avoids race conditions across multiple workers.
     */
    public Job claimNextPendingJob() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            conn.setAutoCommit(false);
            String selSql = "SELECT id, command, attempts, maxRetries FROM jobs WHERE state='pending' LIMIT 1";
            try (PreparedStatement sel = conn.prepareStatement(selSql);
                 ResultSet rs = sel.executeQuery()) {
                if (!rs.next()) {
                    conn.commit();
                    return null;
                }

                String id = rs.getString("id");
                String cmd = rs.getString("command");
                int attempts = rs.getInt("attempts");
                int maxRetries = rs.getInt("maxRetries");

                // Try to atomically claim
                try (PreparedStatement upd = conn.prepareStatement("UPDATE jobs SET state='processing' WHERE id=? AND state='pending'")) {
                    upd.setString(1, id);
                    int updated = upd.executeUpdate();
                    if (updated == 1) {
                        conn.commit();
                        Job job = new Job(id, cmd, maxRetries);
                        job.attempts = attempts;
                        job.state = "processing";
                        return job;
                    } else {
                        conn.rollback();
                        return null; // someone else claimed it
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<Job> listJobsByState(String state) {
        List<Job> jobs = new ArrayList<>();
        String sql = "SELECT * FROM jobs WHERE state=?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, state);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Job job = new Job(rs.getString("id"), rs.getString("command"), rs.getInt("maxRetries"));
                    job.state = rs.getString("state");
                    job.attempts = rs.getInt("attempts");
                    jobs.add(job);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return jobs;
    }

    public List<Job> listDLQ() {
        return listJobsByState("dead");
    }

    public boolean retryDLQJob(String id) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement("UPDATE jobs SET state='pending', attempts=0 WHERE id=? AND state='dead'")) {
            ps.setString(1, id);
            int updated = ps.executeUpdate();
            return updated == 1;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void updateJob(Job job) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement("UPDATE jobs SET state=?, attempts=? WHERE id=?")) {
            ps.setString(1, job.state);
            ps.setInt(2, job.attempts);
            ps.setString(3, job.id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
