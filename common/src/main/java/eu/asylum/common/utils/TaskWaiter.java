package eu.asylum.common.utils;

public class TaskWaiter {

    private volatile boolean finished = false;

    public TaskWaiter() {
    }

    public final void finish() {
        this.finished = true;
    }

    public final void await(long timeoutMillis) {
        long start = System.currentTimeMillis();
        while (!this.finished) {
            if (System.currentTimeMillis() - start > timeoutMillis) {
                break;
            }
        }
    }

}
