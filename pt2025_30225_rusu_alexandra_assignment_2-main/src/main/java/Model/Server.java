package Model;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class Server implements Runnable {
    private BlockingQueue<Task> tasks;
    private AtomicInteger waitingPeriod;
    private boolean running;
    private int queueId;
    private int currentTime;
    private Task processingTask;
    private List<Task> finishedTasks; // Track finished tasks

    public Server(int id) {
        this.queueId = id;
        this.tasks = new LinkedBlockingQueue<>();
        this.waitingPeriod = new AtomicInteger(0);
        this.running = true;
        this.currentTime = 0;
        this.processingTask = null;
        this.finishedTasks = new ArrayList<>(); // Initialize finished tasks list
    }

    public void addTask(Task task) {
        tasks.add(task);
        waitingPeriod.addAndGet(task.getServiceTime());
    }

    public BlockingQueue<Task> getTasks() {
        return tasks;
    }

    public int getQueueSize() {
        return tasks.size() + (processingTask != null ? 1 : 0);
    }

    public int getWaitingPeriod() {
        return waitingPeriod.get();
    }

    public int getQueueId() {
        return queueId;
    }

    public Task getProcessingTask() {
        return processingTask;
    }

    public void setCurrentTime(int time) {
        this.currentTime = time;
    }

    // Method to get and clear finished tasks
    public List<Task> getFinishedTasks() {
        return new ArrayList<>(finishedTasks);
    }

    @Override
    public void run() {
        while (running) {
            try {
                if (processingTask == null && !tasks.isEmpty()) {
                    processingTask = tasks.take();
                }

                if (processingTask != null) {

                    Thread.sleep(1000);
                    processingTask.decrementServiceTime();


                    if (waitingPeriod.get() > 0) {
                        waitingPeriod.decrementAndGet();
                    }

                    if (processingTask.getRemainingServiceTime() <= 0) {
                        processingTask.setFinishTime(currentTime);
                        finishedTasks.add(processingTask);
                        processingTask = null;
                    }
                } else {

                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void stopServer() {
        running = false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Queue ").append(queueId).append(": ");

        if (getQueueSize() == 0) {
            sb.append("closed");
        } else {
            for (Task t : tasks) {
                sb.append(t.toString()).append(" ");
            }
            if (processingTask != null) {
                sb.append("Processing: ").append(processingTask.toString());
            }
        }

        return sb.toString();
    }
}