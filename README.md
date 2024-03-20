# SequenceId

基于号段模式的 SequenceId，无需部署额外的服务。

# Quick Start
1. 导入数据表定义
``` sql
CREATE TABLE `sequence_id` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `type` int NOT NULL COMMENT '业务类型，用于区分不同业务场景下的sequence id。',
  `step` int NOT NULL COMMENT '每次获取sequence id的步长。',
  `step_duration` int NOT NULL DEFAULT '30000' COMMENT '指每次消耗完长度为step的sequence id所需的预计时间。使用该值判断是否有突发流量流入，自适应的增大step。',
  `max_step` int NOT NULL DEFAULT '500000' COMMENT '获取sequence id的最大步长，自适应增大step不会超过该值。',
  `max` bigint NOT NULL COMMENT '当前已分配的sequence id的最大值。',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_type` (`type`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
```

2. 引入 SequenceId 依赖

``` xml
<dependency>
    <groupId>com.zcn</groupId>
    <artifactId>sequence-id</artifactId>
    <version>1.0</version>
</dependency>
```

3. 调用 API
``` java
//构建DateSource
DataSource ds = ...;

//创建SequenceIdGenerator并初始化
SequenceIdGenerator generator = new SequenceIdGenerator(ds);
generator.init();

//获取一个sequence id
long id = generator.generate(1);
```