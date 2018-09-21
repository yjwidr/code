package com.netbrain.xf.flowengine.metric;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

@Component
public class Metrics {
    public long getAndResetStartCount() {
        return startCount.getAndSet(0);
    }

    public long getAndResetStopCount() {
        return stopCount.getAndSet(0);
    }

    public long getAndResetStepdownCount() {
        return stepdownCount.getAndSet(0);
    }

    public long getAndResetStepupCount() {
        return stepupCount.getAndSet(0);
    }

    public long getAndResetDtgStopCount() {
        return dtgStopCount.getAndSet(0);
    }

    public long getAndResetDtgQueryCount() {
        return dtgQueryCount.getAndSet(0);
    }

    public long getAndResetTriggerCount() {
        return triggerCount.getAndSet(0);
    }

    public long getAndResetTaskflowStartCount() {
        return taskflowStartCount.getAndSet(0);
    }

    public long getAndResetTaskflowEndCount() {
        return taskflowEndCount.getAndSet(0);
    }

    public long getAndResetTaskCompletedCount() {
        return taskCompletedCount.getAndSet(0);
    }

    public long getAndResetTaskCrashedCount() {
        return taskCrashedCount.getAndSet(0);
    }

    public long getAndResetTaskCancelledCount() {
        return taskCancelledCount.getAndSet(0);
    }

    public long getAndResetTaskPendingSnapshot() {
        if(taskPendingSnapshotCallback != null){
            return taskPendingSnapshotCallback.apply(0L);
        }
        return 0L;
    }

    public long getAndResetTaskInmemorySnapshot() {
        if(taskInmemorySnapshotCallback != null){
            return taskInmemorySnapshotCallback.apply(0L);
        }
        return 0L;
    }

    public long getAndResetTaskUnackSnapshot() {
        if(taskUnackSnapshotCallback != null){
            return taskUnackSnapshotCallback.apply(0L);
        }
        return 0L;
    }

    public long getAndResetSchedulerSkippedCount() {
        return schedulerSkippedCount.getAndSet(0);
    }

    public void addStartCount(long startCount) {
        this.startCount.addAndGet(startCount);
    }

    public void addStopCount(long stopCount) {
        this.stopCount.addAndGet(stopCount);
    }

    public void addStepdownCount(long stepdownCount) {
        this.stepdownCount.addAndGet(stepdownCount);
    }

    public void addStepupCount(long stepupCount) {
        this.stepupCount.addAndGet(stepupCount);
    }

    public void addDtgStopCount(long dtgStopCount) {
        this.dtgStopCount.addAndGet(dtgStopCount);
    }

    public void addDtgQueryCount(long dtgQueryCount) {
        this.dtgQueryCount.addAndGet(dtgQueryCount);
    }

    public void addTriggerCount(long triggerCount) {
        this.triggerCount.addAndGet(triggerCount);
    }

    public void addTaskflowStartCount(long taskflowStartCount) {
        this.taskflowStartCount.addAndGet(taskflowStartCount);
    }

    public void addTaskflowEndCount(long taskflowEndCount) {
        this.taskflowEndCount.addAndGet(taskflowEndCount);
    }

    public void addTaskCompletedCount(long taskCompletedCount) {
        this.taskCompletedCount.addAndGet(taskCompletedCount);
    }

    public void addTaskCrashedCount(long taskCrashedCount) {
        this.taskCrashedCount.addAndGet(taskCrashedCount);
    }

    public void addTaskCancelledCount(long taskCancelledCount) {
        this.taskCancelledCount.addAndGet(taskCancelledCount);
    }

    public void addSchedulerSkippedCount(long schedulerSkippedCount) {
        this.schedulerSkippedCount.addAndGet(schedulerSkippedCount);
    }

    public void setTaskPendingSnapshotCallback(Function<Long, Long> taskPendingSnapshotCallback) {
        this.taskPendingSnapshotCallback = taskPendingSnapshotCallback;
    }

    public void setTaskInmemorySnapshotCallback(Function<Long, Long> taskInmemorySnapshotCallback) {
        this.taskInmemorySnapshotCallback = taskInmemorySnapshotCallback;
    }

    public void setTaskUnackSnapshot(Function<Long, Long> taskUnackSnapshotCallback) {
        this.taskUnackSnapshotCallback = taskUnackSnapshotCallback;
    }

    //How many times taskengine starts in a period
    private AtomicLong startCount = new AtomicLong(0L);

    //How many times taskengine stops in a period
    private AtomicLong stopCount = new AtomicLong(0L);

    //How many times taskengine steps down from leader role
    private AtomicLong stepdownCount = new AtomicLong(0L);

    //How many times taskengine steps up to leader role
    private AtomicLong stepupCount = new AtomicLong(0L);

    //How many stop DTG requests are sent in a period
    private AtomicLong dtgStopCount = new AtomicLong(0L);

    //How many query DTG requests are sent in a period
    private AtomicLong dtgQueryCount = new AtomicLong(0L);

    //How many trigger events are received in a period
    private AtomicLong triggerCount = new AtomicLong(0L);

    //How many task flows are started in a period
    private AtomicLong taskflowStartCount = new AtomicLong(0L);

    //How many task flows are finished in a period
    private AtomicLong taskflowEndCount = new AtomicLong(0L);

    //How many tasks are completed in a period
    private AtomicLong taskCompletedCount = new AtomicLong(0L);

    //How many tasks are crashed in a period
    private AtomicLong taskCrashedCount = new AtomicLong(0L);

    //How many tasks are cancelled in a period
    private AtomicLong taskCancelledCount = new AtomicLong(0L);

    //How many tasks are pending in the task queue
    private Function<Long, Long> taskPendingSnapshotCallback;

    //How many tasks are in memory repository
    private Function<Long, Long> taskInmemorySnapshotCallback;

    //How many tasks are in un-ack queue
    private Function<Long, Long> taskUnackSnapshotCallback;

    //How many schedules are skipped in a period
    private AtomicLong schedulerSkippedCount = new AtomicLong(0L);
}
