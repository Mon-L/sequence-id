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
        System.out.printf("Sequence id count: %d, time: %d\n", threadNum * loopTime, (System.currentTimeMillis() - start) / 1000);
        generator.destroy();
    }
}
