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

import com.alibaba.druid.pool.DruidDataSource;

import java.util.concurrent.CountDownLatch;

/**
 * @author zicung
 */
public class Test {

    public static void main(String[] args) throws Exception {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:mysql://127.0.0.1:3306/test");
        dataSource.setUsername("root");
        dataSource.setPassword("12345678");
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");

        SequenceIdGenerator generator = new SequenceIdGenerator(dataSource);
        generator.init();

        long start = System.currentTimeMillis();
        int threadNum = 10, loopTime = 3000000;
        CountDownLatch finish = new CountDownLatch(threadNum);
        for (int i = 0; i < threadNum; i++) {
            Thread t = new Thread(() -> {
                for (int j = 0; j < loopTime; j++) {
                    generator.generate(1);
                }
                finish.countDown();
            });
            t.start();
        }

        finish.await();
        System.out.printf(
                "Sequence id count: %d, time: %d\n", threadNum * loopTime, (System.currentTimeMillis() - start) / 1000);
        generator.destroy();
    }
}
