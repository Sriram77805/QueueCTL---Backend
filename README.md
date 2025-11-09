# QueueCTL - A CLI-Based Job Queue System

![Java Version](https://img.shields.io/badge/java-17+-blue.svg)
![Maven Version](https://img.shields.io/badge/maven-3.8+-blue.svg)
![Build Status](https://img.shields.io/badge/build-passing-brightgreen)
![License](https://img.shields.io/badge/license-MIT-lightgrey)

QueueCTL is a minimal, production-grade, CLI-based background job queue system built in **Java** using **Maven**. It supports enqueuing jobs, parallel workers (via threads), automatic retries with exponential backoff, and a Dead Letter Queue (DLQ) for failed jobs, all backed by a persistent **SQLite** database.

This project features a user-friendly interactive prompt (REPL) for all commands.

---

## 🚀 Setup and Installation

Follow these steps to get `queuectl` running on your local machine.

1.  **Prerequisites:**
    * **JDK 17** (or newer)
    * **Apache Maven** 3.8+

2.  **Clone the repository:**
    ```bash
    git clone https://github.com/Sriram77805/QueueCTL---Backend.git
    cd QueueCTL---Backend
    ```

3.  **Build the Project:**
    This command will compile the code, run tests, and package the application into an executable "fat" JAR.
    ```bash
    mvn clean package
    ```
    This will create an executable JAR in the `target/` directory (e.g., `queuectl-1.0.0-jar-with-dependencies.jar`).

4.  **Initialize the Database:**
    The system uses SQLite, which needs to be initialized. 
    

5.  **Run the Application:**
    To start the main interactive prompt, just run the JAR file:
    ```bash
     mvn exec:java
    ```

---

## Usage Examples

Once you run the application, you'll be in the interactive `queuectl` prompt.

```text
QueueCTL CLI Ready. Type 'help' for commands.
>
<img width="326" height="162" alt="image" src="https://github.com/user-attachments/assets/7584d320-7cf5-4403-8021-b0340e4b9d0c" />
> enqueue
Enter job id: job-echo
Enter command: echo hello world from job1
Enter maxRetries (enter for default):
? Job enqueued: job-echo

> enqueue
Enter job id: job-fail
Enter command: ls /invalid-path
Enter maxRetries (enter for default): 2
? Job enqueued: job-fail

