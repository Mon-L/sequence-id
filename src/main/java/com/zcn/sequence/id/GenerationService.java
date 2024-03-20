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

package com.zcn.sequence.id;

import com.zcn.sequence.id.model.IdBuffer;
import com.zcn.sequence.id.model.IdSlot;
import com.zcn.sequence.id.model.Segment;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zicung
 */
public class GenerationService {

    private static final Logger LOG = LoggerFactory.getLogger(GenerationService.class);

    private final ExecutorService fillSegmentExecutor = new ThreadPoolExecutor(
            5, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<>(), new ThreadFactory() {
                private final AtomicInteger i = new AtomicInteger(0);

                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, "Update-SequenceId-segment-thread-" + i.incrementAndGet());
                }
            });

    private final IdSlotDao idSlotDao;
    private volatile boolean inited = false;
    private final Map<Integer, IdBuffer> idBuffers = new ConcurrentHashMap<>();

    public GenerationService(DataSource dataSource) {
        this.idSlotDao = new IdSlotDao(dataSource);
    }

    public void init() {
        if (inited) {
            return;
        }

        updateIdBuffers();
        inited = true;
        startUpdateIdBufferInterval();
    }

    private void startUpdateIdBufferInterval() {
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setName("Update-SequenceId-IdBuffer-thread");
            t.setDaemon(true);
            return t;
        });

        executorService.scheduleAtFixedRate(this::updateIdBuffers, 5, 5, TimeUnit.MINUTES);
    }

    private void updateIdBuffers() {
        try {
            List<IdSlot> idSlots = idSlotDao.loadAll();
            if (idSlots == null || idSlots.isEmpty()) {
                return;
            }

            Set<Integer> cached = new HashSet<>(idBuffers.keySet());
            List<IdSlot> newIdSlots = new ArrayList<>();
            for (IdSlot slot : idSlots) {
                if (!cached.contains(slot.getType())) {
                    newIdSlots.add(slot);
                } else {
                    cached.remove(slot.getType());
                }
            }

            for (Integer t : cached) {
                idBuffers.remove(t);
                LOG.info("Remove unused IdBuffer. SequenceId Type:" + t);
            }

            for (IdSlot idSlot : newIdSlots) {
                IdBuffer idBuffer = new IdBuffer(idSlot);
                idBuffers.put(idSlot.getType(), idBuffer);
                LOG.info("Add IdBuffer. SequenceId Type :" + idSlot.getType());
            }
        } catch (Exception e) {
            LOG.error("Update IdBuffer error.", e);
        }
    }

    public long generate(int type) throws SequenceIdException {
        if (!inited) {
            throw new SequenceIdException("GenerationService was not initialized.");
        }

        IdBuffer idBuffer = idBuffers.get(type);
        if (idBuffer == null) {
            throw new SequenceIdException("Unknown SequenceId type :" + type);
        }

        if (!idBuffer.isReady()) {
            synchronized (idBuffer) {
                if (!idBuffer.isReady()) {
                    try {
                        fillSegment(type, idBuffer, idBuffer.getCurrentSegment());
                        idBuffer.changeToReady();
                    } catch (Exception e) {
                        LOG.error("Failed to init IdBuffer. SequenceId Type: " + type);
                    }
                }
            }
        }

        return getValue(idBuffer);
    }

    private void fillSegment(int type, IdBuffer idBuffer, Segment segment) throws SequenceIdException, SQLException {
        int step = idBuffer.getNextStep();
        IdSlot idSlot = idSlotDao.updateIdAllocAndGet(type, step);
        if (idSlot == null) {
            throw new SequenceIdException(
                    "No SequenceId Type, Please check table sequence_id. SequenceId Type: " + type);
        }
        segment.refresh(idSlot.getMax(), step);
    }

    private void fillNextSegmentAsync(IdBuffer idBuffer) {
        fillSegmentExecutor.execute(() -> {
            boolean ok = false;
            try {
                fillSegment(idBuffer.getType(), idBuffer, idBuffer.getNextSegment());
                ok = true;
            } catch (Exception e) {
                LOG.error("Failed to refresh sequenceId segment.", e);
            } finally {
                if (ok) {
                    idBuffer.getWriteLock().lock();
                    idBuffer.setNextReady(true);
                    idBuffer.isFillingNext().set(false);
                    idBuffer.getWriteLock().unlock();
                } else {
                    idBuffer.isFillingNext().set(false);
                }
            }
        });
    }

    private boolean shouldFillNextSegment(IdBuffer idBuffer) {
        return !idBuffer.isNextReady() && idBuffer.isFillingNext().compareAndSet(false, true);
    }

    private long getValue(IdBuffer idBuffer) {
        while (true) {
            try {
                idBuffer.getReadLock().lock();
                Segment segment = idBuffer.getCurrentSegment();
                if (segment.reachThreshold() && shouldFillNextSegment(idBuffer)) {
                    fillNextSegmentAsync(idBuffer);
                }

                Long val = segment.next();
                if (val != null) {
                    return val;
                }
            } finally {
                idBuffer.getReadLock().unlock();
            }

            if (!idBuffer.isNextReady() && shouldFillNextSegment(idBuffer)) {
                fillNextSegmentAsync(idBuffer);
            }

            waitMoment(idBuffer);

            try {
                idBuffer.getWriteLock().lock();
                Segment segment = idBuffer.getCurrentSegment();
                Long value = segment.next();
                if (value != null) {
                    return value;
                }
                if (idBuffer.isNextReady()) {
                    idBuffer.switchSegment();
                    idBuffer.setNextReady(false);
                } else {
                    throw new SequenceIdException("Both two segments are not ready. Type :" + idBuffer.getType());
                }
            } finally {
                idBuffer.getWriteLock().unlock();
            }
        }
    }

    private void waitMoment(IdBuffer idBuffer) {
        int count = 0;
        while (count < 1000) {
            if (!idBuffer.isFillingNext().get()) {
                return;
            }
            count++;
        }

        for (int i = 1; i <= 3 && idBuffer.isFillingNext().get(); i++) {
            try {
                TimeUnit.MILLISECONDS.sleep(i * 10L);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    public void destroy() {
        fillSegmentExecutor.shutdown();
    }
}
