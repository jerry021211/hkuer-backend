package com.hkuer.hkuer.utils;

@org.springframework.stereotype.Component
public class SnowflakeIdGenerator {

    // 起始时间戳（2025-10-28 00:00:00）
    private final long twepoch = 1751049600000L;

    // 机器ID所占的位数（从3位减少到2位）
    private final long workerIdBits = 2L;

    // 数据中心ID所占的位数（从3位减少到2位）
    private final long datacenterIdBits = 2L;

    // 支持的最大机器ID，结果是3
    private final long maxWorkerId = -1L ^ (-1L << workerIdBits);

    // 支持的最大数据中心ID，结果是3
    private final long maxDatacenterId = -1L ^ (-1L << datacenterIdBits);

    // 序列在ID中占的位数（从12位减少到10位）
    private final long sequenceBits = 10L;

    // 机器ID向左移10位
    private final long workerIdShift = sequenceBits;

    // 数据中心ID向左移12位
    private final long datacenterIdShift = sequenceBits + workerIdBits;

    // 时间戳向左移14位
    private final long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits;

    // 生成序列的掩码，这里为1023 (0b1111111111=0x3FF=1023)
    private final long sequenceMask = -1L ^ (-1L << sequenceBits);

    // 工作机器ID(0~3)
    private long workerId;

    // 数据中心ID(0~3)
    private long datacenterId;

    // 毫秒内序列(0~1023)
    private long sequence = 0L;

    // 上次生成ID的时间戳
    private long lastTimestamp = -1L;

    public SnowflakeIdGenerator(@org.springframework.beans.factory.annotation.Value("${mesh.worker-id:1}") long workerId,
            @org.springframework.beans.factory.annotation.Value("${mesh.datacenter-id:1}") long datacenterId) {
        if (workerId > maxWorkerId || workerId < 0) {
            throw new IllegalArgumentException(String.format("worker Id can't be greater than %d or less than 0", maxWorkerId));
        }
        if (datacenterId > maxDatacenterId || datacenterId < 0) {
            throw new IllegalArgumentException(String.format("datacenter Id can't be greater than %d or less than 0", maxDatacenterId));
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }

    public synchronized long nextId() {
        long timestamp = timeGen();

        if (timestamp < lastTimestamp) {
            throw new RuntimeException(String.format("Clock moved backwards. Refusing to generate id for %d milliseconds", lastTimestamp - timestamp));
        }

        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & sequenceMask;
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        return ((timestamp - twepoch) << timestampLeftShift)
                | (datacenterId << datacenterIdShift)
                | (workerId << workerIdShift)
                | sequence;
    }

    protected long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    protected long timeGen() {
        return System.currentTimeMillis();
    }
}

