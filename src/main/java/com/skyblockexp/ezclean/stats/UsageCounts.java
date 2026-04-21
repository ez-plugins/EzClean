package com.skyblockexp.ezclean.stats;

public class UsageCounts {
    private int pendingSync;
    private int pendingAsync;
    private int activeAsyncWorkers;
    private int threadCount;
    private long cpuTime;
    private long jarSize;

    public int getPendingSync() {
        return pendingSync;
    }

    public int getPendingAsync() {
        return pendingAsync;
    }

    public int getActiveAsyncWorkers() {
        return activeAsyncWorkers;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public long getCpuTime() {
        return cpuTime;
    }

    public long getJarSize() {
        return jarSize;
    }

    public int getTotal() {
        return pendingSync + pendingAsync + activeAsyncWorkers;
    }

    public void incrementPendingSync() {
        pendingSync++;
    }

    public void incrementPendingAsync() {
        pendingAsync++;
    }

    public void incrementActiveAsyncWorkers() {
        activeAsyncWorkers++;
    }

    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
    }

    public void setCpuTime(long cpuTime) {
        this.cpuTime = cpuTime;
    }

    public void setJarSize(long jarSize) {
        this.jarSize = jarSize;
    }
}
