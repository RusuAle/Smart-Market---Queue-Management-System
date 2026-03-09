package BusinessLogic;

import GUI.SimulationFrame;
import Model.Server;
import Model.Task;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SimulationManager implements Runnable {
    // Input data
    private int numTasks;
    private int numServers;
    private int timeLimit;
    private int minArrivalTime;
    private int maxArrivalTime;
    private int minServiceTime;
    private int maxServiceTime;
    private Scheduler scheduler;
    private SimulationFrame frame;
    private List<Task> generatedTasks;
    private List<Task> finishedTasks;
    private List<Task> manualClients;

    // Output file
    private PrintWriter logWriter;
    private String logFilePath = "simulation_log.txt";
    private String historyLogPath = "simulation_history.txt";

    public SimulationManager(SimulationFrame frame, int numTasks, int numServers, int timeLimit,
                             int minArrivalTime, int maxArrivalTime,
                             int minServiceTime, int maxServiceTime,
                             SelectionPolicy policy, List<Task> manualClients) {
        this.frame = frame;
        this.numTasks = numTasks;
        this.numServers = numServers;
        this.timeLimit = timeLimit;
        this.minArrivalTime = minArrivalTime;
        this.maxArrivalTime = maxArrivalTime;
        this.minServiceTime = minServiceTime;
        this.maxServiceTime = maxServiceTime;
        this.manualClients = manualClients;

        this.scheduler = new Scheduler(numServers);
        this.scheduler.changeStrategy(policy);

        this.generatedTasks = new ArrayList<>();
        this.finishedTasks = new ArrayList<>(); // Initialize the list

        try {
            this.logWriter = new PrintWriter(new FileWriter(logFilePath));
        } catch (IOException e) {
            e.printStackTrace();
        }

        generateTasks();


        displayAllGeneratedTasks();

        try {
            FileWriter historyWriter = new FileWriter(historyLogPath, true); // append mode
            historyWriter.write("\n\n========================================\n");
            historyWriter.write("NEW SIMULATION RUN - " + java.time.LocalDateTime.now() + "\n");
            historyWriter.write("========================================\n\n");
            historyWriter.write("CONFIGURATION:\n");
            historyWriter.write("- Clients: " + numTasks + "\n");
            historyWriter.write("- Queues: " + numServers + "\n");
            historyWriter.write("- Simulation Time: " + timeLimit + "\n");
            historyWriter.write("- Arrival Time Range: " + minArrivalTime + " - " + maxArrivalTime + "\n");
            historyWriter.write("- Service Time Range: " + minServiceTime + " - " + maxServiceTime + "\n");
            historyWriter.write("- Strategy: " + policy + "\n");

            if (!manualClients.isEmpty()) {
                historyWriter.write("- Manual clients added: " + manualClients.size() + "\n");
            }

            historyWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void generateTasks() {
        // Clear existing generated tasks
        generatedTasks = new ArrayList<>();

        // Add manual clients if any
        if (manualClients != null && !manualClients.isEmpty()) {
            generatedTasks.addAll(manualClients);
        }

        // Generate exactly numTasks random tasks
        Random random = new Random();
        int startId = 1;

        for (int i = 0; i < numTasks; i++) {
            int arrivalTime = random.nextInt(maxArrivalTime - minArrivalTime + 1) + minArrivalTime;
            int serviceTime = random.nextInt(maxServiceTime - minServiceTime + 1) + minServiceTime;
            generatedTasks.add(new Task(startId + i, arrivalTime, serviceTime));
        }

        generatedTasks.sort((t1, t2) -> Integer.compare(t1.getArrivalTime(), t2.getArrivalTime()));
    }


    private void displayAllGeneratedTasks() {
        StringBuilder sb = new StringBuilder();
        sb.append("\nALL GENERATED CLIENTS (").append(generatedTasks.size()).append(" total):\n");
        sb.append("-----------------------------------------------------\n");
        sb.append("| Client ID | Arrival Time | Service Time |\n");
        sb.append("-----------------------------------------------------\n");

        for (Task task : generatedTasks) {
            sb.append(String.format("| %-9d | %-12d | %-12d |\n",
                    task.getId(), task.getArrivalTime(), task.getServiceTime()));
        }
        sb.append("-----------------------------------------------------\n\n");

        frame.appendLog(sb.toString());
        logWriter.println(sb.toString());
        logWriter.flush();

        // Adăugă lista de clienți generați în fișierul de istoric
        try {
            FileWriter historyWriter = new FileWriter(historyLogPath, true);
            historyWriter.write(sb.toString());
            historyWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        int currentTime = 0;
        int peakHour = -1;
        int maxClientsInQueues = -1;

        // StringBuffer pentru a colecta toate log-urile pe parcursul simulării
        StringBuilder simulationLog = new StringBuilder();

        while (currentTime < timeLimit && (!generatedTasks.isEmpty() || hasClientsInQueues())) {
            // Wait for user input if in manual mode
            if (frame.isManualMode()) {
                frame.waitForUserInput();
            } else {
                // Calculate delay based on speed slider
                int speed = frame.getSimulationSpeed();
                int delay;
                if (speed < 5) {
                    // Slower: 1000ms to 2000ms
                    delay = 1000 + (5 - speed) * 250;
                } else if (speed > 5) {
                    // Faster: 100ms to 500ms
                    delay = 500 - (speed - 5) * 100;
                    if (delay < 100) delay = 100;
                } else {
                    // Normal speed: 1000ms
                    delay = 1000;
                }

                // Wait for the calculated delay
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            // Move clients arriving at this time to queues
            List<Task> arrivedTasks = new ArrayList<>();
            for (Task task : generatedTasks) {
                if (task.getArrivalTime() == currentTime) {
                    scheduler.dispatchTask(task);
                    arrivedTasks.add(task);
                }
            }
            generatedTasks.removeAll(arrivedTasks);

            // Update servers current time
            scheduler.setCurrentTimeForServers(currentTime);

            // Check if this is the peak hour
            int currentClientsInQueues = getTotalClientsInQueues();
            if (currentClientsInQueues > maxClientsInQueues) {
                maxClientsInQueues = currentClientsInQueues;
                peakHour = currentTime;
            }

            // Collect finished tasks from servers
            for (Server server : scheduler.getServers()) {
                List<Task> serverFinishedTasks = server.getFinishedTasks();
                if (serverFinishedTasks != null && !serverFinishedTasks.isEmpty()) {
                    finishedTasks.addAll(serverFinishedTasks);
                    serverFinishedTasks.clear(); // Clear the server's finished tasks after collecting
                }
            }

            // Log the current state
            String logMessage = generateLogMessage(currentTime);
            frame.appendLog(logMessage);
            logWriter.println(logMessage);
            logWriter.flush();

            // Adaugă mesajul de log la buffer-ul de simulare
            simulationLog.append(logMessage);

            // Update visual representation
            updateVisualization(currentTime);

            currentTime++;
        }

        // Make sure we collect any remaining finished tasks
        for (Server server : scheduler.getServers()) {
            List<Task> serverFinishedTasks = server.getFinishedTasks();
            if (serverFinishedTasks != null && !serverFinishedTasks.isEmpty()) {
                finishedTasks.addAll(serverFinishedTasks);
            }
        }

        // Calculate statistics
        double avgWaitingTime = calculateAvgWaitingTime();
        double avgServiceTime = calculateAvgServiceTime();

        String finalStats = "\nSimulation finished!\n" +
                "Average waiting time: " + String.format("%.2f", avgWaitingTime) + "\n" +
                "Average service time: " + String.format("%.2f", avgServiceTime) + "\n" +
                "Peak hour: " + peakHour;

        frame.appendLog(finalStats);
        logWriter.println(finalStats);
        logWriter.flush();
        logWriter.close();
        simulationLog.append(finalStats);

        try {
            FileWriter historyWriter = new FileWriter(historyLogPath, true);
            historyWriter.write("\n\n--- SIMULATION LOG ---\n");
            historyWriter.write(simulationLog.toString());
            historyWriter.write("\n--- END OF SIMULATION LOG ---\n");
            historyWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        frame.displaySimulationEnd(currentTime, scheduler.getServers(), avgWaitingTime, avgServiceTime, peakHour);
        scheduler.stopServers();
    }

    private void updateVisualization(int currentTime) {
        List<Server> servers = scheduler.getServers();
        frame.updateVisualRepresentation(currentTime, servers, generatedTasks.size());
    }

    private boolean hasClientsInQueues() {
        for (Server server : scheduler.getServers()) {
            if (server.getQueueSize() > 0) {
                return true;
            }
        }
        return false;
    }

    private int getTotalClientsInQueues() {
        int total = 0;
        for (Server server : scheduler.getServers()) {
            total += server.getQueueSize();
        }
        return total;
    }

    private double calculateAvgWaitingTime() {
        if (finishedTasks.isEmpty()) {
            return 0;
        }

        double totalWaitingTime = 0;
        for (Task task : finishedTasks) {

            int waitingTime = task.getFinishTime() - task.getArrivalTime() - task.getServiceTime();
            if (waitingTime >= 0) {
                totalWaitingTime += waitingTime;
            }
        }

        return totalWaitingTime / finishedTasks.size();
    }

    private double calculateAvgServiceTime() {
        if (finishedTasks.isEmpty() && generatedTasks.isEmpty()) {
            return 0;
        }

        double totalServiceTime = 0;
        int count = 0;

        // Add service times of finished tasks
        for (Task task : finishedTasks) {
            totalServiceTime += task.getServiceTime();
            count++;
        }
        for (Task task : generatedTasks) {
            totalServiceTime += task.getServiceTime();
            count++;
        }

        for (Server server : scheduler.getServers()) {
            for (Task task : server.getTasks()) {
                if (!finishedTasks.contains(task)) {
                    totalServiceTime += task.getServiceTime();
                    count++;
                }
            }
        }

        return count > 0 ? totalServiceTime / count : 0;
    }

    private String generateLogMessage(int currentTime) {
        StringBuilder sb = new StringBuilder();
        sb.append("Time ").append(currentTime).append("\n");

        // Waiting clients
        sb.append("Waiting clients: ");
        if (generatedTasks.isEmpty()) {
            sb.append("none");
        } else {
            for (Task task : generatedTasks) {
                sb.append(task).append("; ");
            }
        }
        sb.append("\n");

        // Queues status
        for (Server server : scheduler.getServers()) {
            if (server.getQueueSize() == 0) {
                sb.append("Queue ").append(server.getQueueId()).append(": closed\n");
            } else {
                sb.append("Queue ").append(server.getQueueId()).append(": ");
                Task processingTask = server.getProcessingTask();
                if (processingTask != null) {
                    sb.append("(").append(processingTask.getId()).append(",")
                            .append(processingTask.getArrivalTime()).append(",")
                            .append(processingTask.getRemainingServiceTime()).append("/")
                            .append(processingTask.getServiceTime()).append(")").append("; ");
                }
                for (Task task : server.getTasks()) {
                    if (task != processingTask) {
                        sb.append(task).append("; ");
                    }
                }
                sb.append("\n");
            }
        }

        return sb.toString();
    }
    public void stopServers() {
        if (scheduler != null) {
            scheduler.stopServers();
        }
    }
}