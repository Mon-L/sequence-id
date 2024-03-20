/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zcn.sequence.id.model;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author zicung
 */
public class IdBuffer {

    /**
     * IdBuffer是否初始化完成
     */
    private volatile boolean ready;

    /**
     * 下一个Segment是否准备完毕
     */
    private volatile boolean nextReady;

    /**
     * 是否正在填充下一个Segment
     */
    private final AtomicBoolean isFillingNext = new AtomicBoolean(false);

    /**
     * 双Segment，当前Segment消耗完时使用下一个Segment，不断循环
     */
    private final Segment[] segments = new Segment[] {new Segment(), new Segment()};

    /**
     * 当前正在使用的Segment的索引
     */
    private int segmentPos = 0;

    /**
     * 上一次切换Segment的时间
     */
    private long lateSwitchMillis = 0;

    /**
     * 当前Step
     */
    private int step;

    private final IdSlot idSlot;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public IdBuffer(IdSlot idSlot) {
        this.idSlot = idSlot;
        this.step = idSlot.getStep();
    }

    public Segment getCurrentSegment() {
        return segments[segmentPos];
    }

    public Segment getNextSegment() {
        return segments[(segmentPos + 1) % 2];
    }

    public void switchSegment() {
        this.segmentPos = (segmentPos + 1) % 2;
        this.lateSwitchMillis = System.currentTimeMillis();
    }

    public int getType() {
        return idSlot.getType();
    }

    public boolean isReady() {
        return ready;
    }

    public void changeToReady() {
        this.ready = true;
        this.lateSwitchMillis = System.currentTimeMillis();
    }

    public AtomicBoolean isFillingNext() {
        return isFillingNext;
    }

    public boolean isNextReady() {
        return nextReady;
    }

    public void setNextReady(boolean nextReady) {
        this.nextReady = nextReady;
    }

    public int getNextStep() {
        if (!isReady()) {
            return idSlot.getStep();
        }

        long elapsed = System.currentTimeMillis() - lateSwitchMillis;
        int prevStep = this.step;
        if (elapsed < idSlot.getStepDuration()) {
            this.step = Math.min(idSlot.getMaxStep(), prevStep * 2);
        } else if (elapsed < idSlot.getStepDuration() * 2L) {
            // do nothing
        } else {
            this.step = prevStep / 2 >= idSlot.getStep() ? prevStep / 2 : prevStep;
        }

        return this.step;
    }

    public Lock getReadLock() {
        return this.lock.readLock();
    }

    public Lock getWriteLock() {
        return this.lock.writeLock();
    }
}
