package BusinessLogic;

import Model.Server;
import Model.Task;
import java.util.List;

public class ShortestQueueStrategy implements Strategy {
    @Override
    public void addTask(List<Server> servers, Task task) {
        Server bestServer = servers.get(0);
        int minQueueSize = bestServer.getQueueSize();

        for (Server server : servers) {
            if (server.getQueueSize() < minQueueSize) {
                minQueueSize = server.getQueueSize();
                bestServer = server;
            }
        }

        bestServer.addTask(task);
    }
}