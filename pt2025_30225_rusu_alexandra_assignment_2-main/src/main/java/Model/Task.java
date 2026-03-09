package Model;

public class Task {
    private int id;
    private int arrivalTime;
    private int serviceTime;
    private int remainingServiceTime;
    private int finishTime;

    public Task(int id, int arrivalTime, int serviceTime) {
        this.id = id;
        this.arrivalTime = arrivalTime;
        this.serviceTime = serviceTime;
        this.remainingServiceTime = serviceTime;
        this.finishTime = -1; // Not finished yet
    }

    public int getId() {
        return id;
    }

    public int getArrivalTime() {
        return arrivalTime;
    }

    public int getServiceTime() {
        return serviceTime;
    }

    public int getRemainingServiceTime() {
        return remainingServiceTime;
    }

    public void decrementServiceTime() {
        if (remainingServiceTime > 0) {
            remainingServiceTime--;
        }
    }

    public void setFinishTime(int finishTime) {
        this.finishTime = finishTime;
    }

    public int getFinishTime() {
        return finishTime;
    }

    public int getWaitingTime() {
        if (finishTime == -1) {
            return -1; // Task not finished
        }
        return finishTime - arrivalTime - serviceTime;
    }

    @Override
    public String toString() {
        return "(" + id + "," + arrivalTime + "," + serviceTime + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Task task = (Task) obj;
        return id == task.id;
    }

    @Override
    public int hashCode() {
        return id;
    }
}