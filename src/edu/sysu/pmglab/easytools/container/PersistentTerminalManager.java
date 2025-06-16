package edu.sysu.pmglab.easytools.container;

import ch.qos.logback.classic.Logger;
import edu.sysu.pmglab.LogBackOptions;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class PersistentTerminalManager {
    private static boolean silent = false;
    private volatile boolean isShuttingDown = false;
    private LinkedList<TerminalSession> terminals = new LinkedList<>();
    private static final Logger logger = LogBackOptions.getRootLogger();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    // internal class represents a live terminal
    private class TerminalSession {
        private final int id;
        private final Process process;
        private final Thread outputThread;
        private final BufferedWriter writer;
        private final BufferedReader reader;
        private volatile boolean isActive = true;
        private volatile boolean isReady = false;
        private final CountDownLatch readyLatch = new CountDownLatch(1);

        public TerminalSession(int id) throws IOException {
            this.id = id;

            ProcessBuilder pb = new ProcessBuilder();
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                pb.command("cmd.exe");
            } else {
                pb.command("bash");
            }

            // Redirect output to null in silent mode
            if (silent) {
                if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                    pb.redirectOutput(ProcessBuilder.Redirect.to(new File("NUL")));
                    pb.redirectError(ProcessBuilder.Redirect.to(new File("NUL")));
                } else {
                    pb.redirectOutput(ProcessBuilder.Redirect.to(new File("/dev/null")));
                    pb.redirectError(ProcessBuilder.Redirect.to(new File("/dev/null")));
                }
                this.process = pb.start();
                this.writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
                this.reader = null;
                this.outputThread = null;
                // In silent mode, assume ready immediately
                this.isReady = true;
                this.readyLatch.countDown();
            } else {
                pb.redirectErrorStream(true);
                this.process = pb.start();
                this.writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
                this.reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                // Start output monitoring thread
                this.outputThread = new Thread(this::monitorOutput);
                this.outputThread.setDaemon(true);
                this.outputThread.start();
            }

            // Send a test command to check if terminal is ready
            executor.submit(this::checkReadiness);

            if (!silent) {
                logger.info("Terminal {} created and initializing...", id);
            }
        }

        private void checkReadiness() {
            try {
                // Wait a bit for process to fully start
                Thread.sleep(100);

                // Send a simple command to test readiness
                String testCommand = System.getProperty("os.name").toLowerCase().contains("windows")
                        ? "echo READY" : "echo READY";

                writer.write(testCommand);
                writer.newLine();
                writer.flush();

                // In silent mode, we assume it's ready after sending command
                if (silent) {
                    Thread.sleep(200); // Small delay to ensure command is processed
                    isReady = true;
                    readyLatch.countDown();
                }
            } catch (Exception e) {
                if (!silent) {
                    logger.error("Terminal {} readiness check failed: {}", id, e.getMessage());
                }
            }
        }

        private void monitorOutput() {
            if (silent || reader == null) return;

            try {
                String line;
                while (isActive && (line = reader.readLine()) != null) {
                    // Check for readiness indicator
                    if (!isReady && line.contains("READY")) {
                        isReady = true;
                        readyLatch.countDown();
                        if (!silent) {
                            logger.info("Terminal {} is ready", id);
                        }
                        continue;
                    }

                    // Skip the READY echo output
                    if (line.trim().equals("READY")) {
                        continue;
                    }

                    logger.info("[Terminal{}] {}", id, line);
                }
            } catch (IOException e) {
                if (isActive && !silent) {
                    logger.error("Terminal {} output monitoring error: {}", id, e.getMessage());
                }
            }
        }

        public boolean waitForReady(long timeout, TimeUnit unit) {
            try {
                return readyLatch.await(timeout, unit);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        public boolean isReady() {
            return isReady;
        }

        public synchronized void executeCommand(String command) throws IOException {
            if (!isActive) {
                throw new IllegalStateException("Terminal " + id + " is closed");
            }

            if (!silent) {
                logger.info("Terminal {} executing command: {}", id, command);
            }
            writer.write(command);
            writer.newLine();
            writer.flush();
        }

        public synchronized void close() {
            if (!isActive) return;

            isActive = false;

            try {
                // Send exit command
                if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                    writer.write("exit");
                } else {
                    writer.write("exit");
                }
                writer.newLine();
                writer.flush();

                // Wait for process to end
                if (!process.waitFor(3, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                }

            } catch (Exception e) {
                process.destroyForcibly();
            } finally {
                try {
                    if (writer != null) writer.close();
                    if (reader != null) reader.close();
                } catch (IOException e) {
                    // Ignore close exceptions
                }

                if (outputThread != null && outputThread.isAlive()) {
                    outputThread.interrupt();
                }

                if (!silent) {
                    logger.info("Terminal {} closed", id);
                }
            }
        }

        public boolean isAlive() {
            return isActive && process.isAlive();
        }

        public int getId() {
            return id;
        }
    }

    /**
     * Set silent mode
     *
     * @param silent true for silent mode, false for normal mode
     */
    public static void silent(boolean silent) {
        PersistentTerminalManager.silent = silent;
    }

    /**
     * Get current silent mode status
     */
    public static boolean isSilent() {
        return silent;
    }

    /**
     * Create specified number of terminals
     */
    public void createTerminals(int count) {
        for (int i = 0; i < count; i++) {
            try {
                TerminalSession terminal = new TerminalSession(i);
                terminals.add(i, terminal);
            } catch (IOException e) {
                if (!silent) {
                    logger.error("Failed to create terminal {}: {}", (i + 1), e.getMessage());
                }
            }
        }

        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(this::closeAllTerminals));
    }

    /**
     * Wait for all terminals to be ready
     */
    public boolean waitForAllTerminalsReady(long timeout, TimeUnit unit) {
        long startTime = System.nanoTime();
        long timeoutNanos = unit.toNanos(timeout);

        for (TerminalSession terminal : terminals) {
            if (!terminal.isAlive()) {
                continue;
            }

            long remainingNanos = timeoutNanos - (System.nanoTime() - startTime);
            if (remainingNanos <= 0) {
                return false;
            }

            if (!terminal.waitForReady(remainingNanos, TimeUnit.NANOSECONDS)) {
                return false;
            }
        }

        if (!silent) {
            logger.info("All terminals are ready");
        }
        return true;
    }

    /**
     * Check if all terminals are ready
     */
    public boolean areAllTerminalsReady() {
        return terminals.stream()
                .filter(TerminalSession::isAlive)
                .allMatch(TerminalSession::isReady);
    }

    /**
     * Execute command on specified terminal
     */
    public void executeCommand(int terminalId, String command) {
        TerminalSession terminal = terminals.get(terminalId);
        if (terminal == null) {
            if (!silent) {
                logger.error("Terminal {} does not exist", terminalId);
            }
            return;
        }

        if (!terminal.isAlive()) {
            if (!silent) {
                logger.error("Terminal {} is closed", terminalId);
            }
            terminals.remove(terminalId);
            return;
        }

        if (!terminal.isReady()) {
            if (!silent) {
                logger.error("Terminal {} is not ready yet", terminalId);
            }
            return;
        }

        try {
            terminal.executeCommand(command);
        } catch (IOException e) {
            if (!silent) {
                logger.error("Terminal {} command execution failed: {}", terminalId, e.getMessage());
            }
        }
    }

    /**
     * Execute same command on all terminals
     */
    public void executeCommandOnAll(String command) {
        List<Integer> deadTerminals = new ArrayList<>();

        for (TerminalSession terminal : terminals) {
            if (!terminal.isAlive()) {
                deadTerminals.add(terminal.getId());
                continue;
            }

            if (!terminal.isReady()) {
                if (!silent) {
                    logger.error("Terminal {} is not ready yet", terminal.getId());
                }
                continue;
            }

            try {
                terminal.executeCommand(command);
            } catch (IOException e) {
                if (!silent) {
                    logger.error("Terminal {} command execution failed: {}", terminal.getId(), e.getMessage());
                }
                deadTerminals.add(terminal.getId());
            }
        }

        // Clean up dead terminals
        deadTerminals.forEach(terminals::remove);
    }

    /**
     * Execute command on specified terminal asynchronously
     */
    public CompletableFuture<Void> executeCommandAsync(int terminalId, String command) {
        return CompletableFuture.runAsync(() -> executeCommand(terminalId, command), executor);
    }

    /**
     * Execute command on all terminals asynchronously
     */
    public CompletableFuture<Void> executeCommandOnAllAsync(String command) {
        return CompletableFuture.runAsync(() -> executeCommandOnAll(command), executor);
    }

    /**
     * Close specified terminal
     */
    public void closeTerminal(int terminalId) {
        TerminalSession terminal = terminals.remove(terminalId);
        if (terminal != null) {
            terminal.close();
        }
    }

    /**
     * Close all terminals
     */
    public void closeAllTerminals() {
        if (isShuttingDown) return;
        isShuttingDown = true;

        if (!silent) {
            logger.info("Closing all terminals...");
        }

        List<CompletableFuture<Void>> closeTasks = new ArrayList<>();

        for (TerminalSession terminal : terminals) {
            closeTasks.add(CompletableFuture.runAsync(terminal::close, executor));
        }

        // Wait for all terminals to close
        CompletableFuture<Void> allCloseTasks = CompletableFuture.allOf(closeTasks.toArray(new CompletableFuture[0]));

        try {
            allCloseTasks.get(10, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            if (!silent) {
                logger.error("Terminal closing timeout, forcing shutdown");
            }
            closeTasks.forEach(task -> task.cancel(true));
        } catch (Exception e) {
            if (!silent) {
                logger.error("Exception occurred while closing terminals: {}", e.getMessage());
            }
        }

        terminals.clear();
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        if (!silent) {
            logger.info("All terminals closed");
        }
    }

    /**
     * Get active terminal list
     */
    public List<Integer> getActiveTerminals() {
        return terminals.stream()
                .filter(TerminalSession::isAlive)
                .map(TerminalSession::getId)
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Get ready terminal list
     */
    public List<Integer> getReadyTerminals() {
        return terminals.stream()
                .filter(terminal -> terminal.isAlive() && terminal.isReady())
                .map(TerminalSession::getId)
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Get terminal count
     */
    public int getTerminalCount() {
        return (int) terminals.stream().filter(TerminalSession::isAlive).count();
    }

    /**
     * Get ready terminal count
     */
    public int getReadyTerminalCount() {
        return (int) terminals.stream().filter(terminal -> terminal.isAlive() && terminal.isReady()).count();
    }

    // Example usage
    public static void main(String[] args) throws InterruptedException {
        // Set silent mode example
        PersistentTerminalManager.silent(false); // Normal mode, show all output and process info
        // PersistentTerminalManager.silent(true); // Silent mode, no output and process info

        PersistentTerminalManager manager = new PersistentTerminalManager();

        // Create 3 terminals
        manager.createTerminals(3);

        // Wait for terminals to be ready (with timeout)
        if (manager.waitForAllTerminalsReady(5, TimeUnit.SECONDS)) {
            logger.info("All terminals initialized successfully");
        } else {
            logger.error("Some terminals failed to initialize within timeout");
        }

        // Execute different commands on different terminals
        manager.executeCommand(1, "echo 'Terminal 1: Starting work'");
        manager.executeCommand(2, "echo 'Terminal 2: Starting work'");
        manager.executeCommand(3, "echo 'Terminal 3: Starting work'");

        Thread.sleep(1000);

        // Demonstrate dynamic mode switching
        logger.info("=== Switching to silent mode ===");
        PersistentTerminalManager.silent(true);

        // Execute same command on all terminals (silent mode, no process info displayed)
        manager.executeCommandOnAll("pwd");

        Thread.sleep(1000);

        // Switch back to normal mode
        logger.info("=== Switching back to normal mode ===");
        PersistentTerminalManager.silent(false);

        // Execute asynchronous commands
        List<CompletableFuture<Void>> futures = Arrays.asList(
                manager.executeCommandAsync(1, "sleep 2 && echo 'Terminal 1: Task completed'"),
                manager.executeCommandAsync(2, "sleep 3 && echo 'Terminal 2: Task completed'"),
                manager.executeCommandAsync(3, "sleep 1 && echo 'Terminal 3: Task completed'")
        );

        // Wait for all async tasks to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        Thread.sleep(1000);

        // Show active terminals
        logger.info("Active terminals: {}", manager.getActiveTerminals());
        logger.info("Ready terminals: {}", manager.getReadyTerminals());

        // Close one terminal
        manager.closeTerminal(2);

        Thread.sleep(1000);

        logger.info("Remaining terminals: {}", manager.getActiveTerminals());

        // closeAllTerminals() will be called automatically when program ends
        // Can also call manually
        manager.closeAllTerminals();
    }
}