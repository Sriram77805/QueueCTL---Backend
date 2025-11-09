# QueueCTL - A CLI-Based Job Queue System

![Python Version](https://img.shields.io/badge/python-3.10+-blue.svg)
![Build Status](https://img.shields.io/badge/build-passing-brightgreen)
![License](https://img.shields.io/badge/license-MIT-lightgrey)

QueueCTL is a minimal, production-grade, CLI-based background job queue system built in Python. It supports enqueuing jobs, parallel workers, automatic retries with exponential backoff, and a Dead Letter Queue (DLQ) for failed jobs, all backed by a persistent SQLite database.

**➡️ [Link to CLI Demo Video]** (Insert your demo link here)

---

## 🚀 Setup and Installation

Follow these steps to get `queuectl` running on your local machine.

1.  **Clone the repository:**
    ```bash
    git clone [https://github.com/](https://github.com/)[YOUR_USERNAME]/queuectl.git
    cd queuectl
    ```

2.  **Create a virtual environment and install dependencies:**
    *(Requires Python 3.10+)*
    ```bash
    python3 -m venv venv
    source venv/bin/activate
    pip install -r requirements.txt
    ```

3.  **Install the CLI (in editable mode):**
    This project uses `click` for the CLI. Installing in editable mode links the `queuectl` command to your shell.
    ```bash
    pip install -e .
    ```

4.  **Initialize the Database:**
    The system uses SQLite, which needs to be initialized.
    ```bash
    queuectl config init-db
    ```
    *Output:*
    ```text
    [queuectl] 💾 Database initialized at: /path/to/queuectl/db/queue.db
    ```

5.  **Verify Installation:**
    You should now have access to the `queuectl` command.
    ```bash
    queuectl --help
    ```
    *Output:*
    ```text
    Usage: queuectl [OPTIONS] COMMAND [ARGS]...

      Welcome to QueueCTL - A CLI-based job queue system.

    Options:
      --help  Show this message and exit.

    Commands:
      config   Manage configuration.
      dlq      Manage the Dead Letter Queue (DLQ).
      enqueue  Add a new job to the queue.
      list     List jobs by state.
      status   Show a summary of all job states & active workers.
      worker   Manage worker processes.
    ```

---

## Usage Examples

Here are the primary commands to interact with `queuectl`.

### 1. Enqueueing Jobs
Jobs are enqueued as a JSON string. The `id` and `command` fields are required. `max_retries` is optional and defaults to the system config.

```bash
# Enqueue a simple job that will succeed
queuectl enqueue '{"id":"job-echo","command":"echo Hello World from Job 1"}'

# Enqueue a job that will fail (and retry)
queuectl enqueue '{"id":"job-fail","command":"ls /invalid-path", "max_retries": 2}'

# Enqueue a job that will take time
queuectl enqueue '{"id":"job-sleep","command":"sleep 5"}'
