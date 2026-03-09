package BusinessLogic;

import Model.Server;
import Model.Task;
import java.util.List;

public class AdaptiveStrategy implements Strategy {
    private TimeStrategy timeStrategy = new TimeStrategy();
    private ShortestQueueStrategy queueStrategy = new ShortestQueueStrategy();

    @Override
    public void addTask(List<Server> servers, Task task) {



        double avgQueueSize = calculateAvgQueueSize(servers);
        double avgWaitingTime = calculateAvgWaitingTime(servers);
        int maxQueueSize = findMaxQueueSize(servers);

        if (hasHighQueueVariance(servers, avgQueueSize) || maxQueueSize > 5) {

            queueStrategy.addTask(servers, task);
        } else {

            timeStrategy.addTask(servers, task);
        }
    }

    private double calculateAvgQueueSize(List<Server> servers) {
        int totalSize = 0;
        for (Server server : servers) {
            totalSize += server.getQueueSize();
        }
        return servers.isEmpty() ? 0 : (double) totalSize / servers.size();
    }

    private double calculateAvgWaitingTime(List<Server> servers) {
        int totalWaitingTime = 0;
        for (Server server : servers) {
            totalWaitingTime += server.getWaitingPeriod();
        }
        return servers.isEmpty() ? 0 : (double) totalWaitingTime / servers.size();
    }

    private int findMaxQueueSize(List<Server> servers) {
        int max = 0;
        for (Server server : servers) {
            max = Math.max(max, server.getQueueSize());
        }
        return max;
    }

    private boolean hasHighQueueVariance(List<Server> servers, double avgQueueSize) {

        double sumSquaredDiff = 0;
        for (Server server : servers) {
            double diff = server.getQueueSize() - avgQueueSize;
            sumSquaredDiff += diff * diff;
        }
        double variance = servers.size() > 1 ? sumSquaredDiff / (servers.size() - 1) : 0;

        return variance > 2.0;
    }
}