package com.zcn.sequence.id.model;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author zicung
 */
public class Segment {

    /**
     * 已使用的最大的ID
     */
    private final AtomicLong value = new AtomicLong(0);

    /**
     * 最大可使用的ID
     */
    private volatile long max = 0L;

    /**
     * 当前步长
     */
    private volatile int step = 0;

    public void refresh(long max, int step) {
        this.value.set(max - step);
        this.step = step;
        this.max = max;
    }

    public Long next() {
        if (value.get() >= max) {
            return null;
        }

        long val = value.incrementAndGet();
        return val <= max ? val : null;
    }

    public long getRemaining() {
        long r = max - value.get();
        return r > 0 ? r : 0;
    }

    public boolean reachThreshold() {
        return getRemaining() < step * 0.8;
    }
}
