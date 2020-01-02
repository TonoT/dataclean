package com.epoint.cleaning.thread;

import java.util.concurrent.CountDownLatch;

public abstract class BasicThread extends Thread
{
    protected CountDownLatch countDownLatch;

    public CountDownLatch getCountDownLatch() {
        return countDownLatch;
    }

    public void setCountDownLatch(CountDownLatch countDownLatch) {
        this.countDownLatch = countDownLatch;
    }
}
