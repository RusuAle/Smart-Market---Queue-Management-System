package BusinessLogic;

import Model.Server;
import Model.Task;
import java.util.List;

public class TimeStrategy implements Strategy {
    @Override
    public void addTask(List<Server> servers, Task task) {
        Server bestServer = servers.get(0);
        int minWaitingTime = bestServer.getWaitingPeriod();

        for (Server server : servers) {
            if (server.getWaitingPeriod() < minWaitingTime) {
                minWaitingTime = server.getWaitingPeriod();
                bestServer = server;
            }
        }

        bestServer.addTask(task);
    }
}