package BusinessLogic;

import Model.Server;
import Model.Task;

import java.util.ArrayList;
import java.util.List;

public class Scheduler {
    private List<Server> servers;
    private Strategy strategy;
    private List<Thread> threads;

    public Scheduler(int numServers) {
        this.servers = new ArrayList<>();
        this.threads = new ArrayList<>();
        this.strategy = new TimeStrategy(); // Default strategy

        for (int i = 0; i < numServers; i++) {
            Server server = new Server(i + 1);
            servers.add(server);
            Thread thread = new Thread(server);
            threads.add(thread);
            thread.start();
        }
    }


    public void dispatchTask(Task task) {
        strategy.addTask(servers, task);
    }

    public List<Server> getServers() {
        return servers;
    }

    public void setCurrentTimeForServers(int currentTime) {
        for (Server server : servers) {
            server.setCurrentTime(currentTime);
        }
    }

    public void stopServers() {
        for (Server server : servers) {
            server.stopServer();
        }

        for (Thread thread : threads) {
            thread.interrupt();
        }
    }
    public void changeStrategy(SelectionPolicy policy) {
        if (policy == SelectionPolicy.SHORTEST_QUEUE) {
            strategy = new ShortestQueueStrategy();
        } else if (policy == SelectionPolicy.ADAPTIVE) {
            strategy = new AdaptiveStrategy();
        } else {
            strategy = new TimeStrategy();
        }
    }
}