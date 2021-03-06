package com.netease.nim.camellia.redis.proxy.hbase.util;

import com.netease.nim.camellia.core.util.MD5Util;
import com.netease.nim.camellia.redis.proxy.hbase.conf.RedisHBaseConfiguration;
import org.apache.hadoop.hbase.util.Bytes;
import redis.clients.util.SafeEncoder;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * Created by caojiajun on 2020/3/5.
 */
public class RedisHBaseUtil {

    public static final byte[] NULL_CACHE_YES = SafeEncoder.encode("Y");
    public static final byte[] NULL_CACHE_NO = SafeEncoder.encode("N");
    public static final byte[] NX = SafeEncoder.encode("NX");
    public static final byte[] EX = SafeEncoder.encode("EX");

    public static byte[] nullCacheKey(byte[] key) {
        byte[] prefix = RedisHBaseConfiguration.nullCachePrefix();
        if (prefix == null) return key;
        return Bytes.add(prefix, key);
    }

    public static byte[] redisKey(byte[] key) {
        byte[] redisKeyPrefix = RedisHBaseConfiguration.redisKeyPrefix();
        if (redisKeyPrefix == null) {
            return key;
        }
        return Bytes.add(redisKeyPrefix, key);
    }

    public static byte[][] redisKey(byte[][] keys) {
        List<byte[]> list = new ArrayList<>(keys.length);
        for (byte[] key : keys) {
            list.add(redisKey(key));
        }
        return list.toArray(new byte[0][0]);
    }

    //32字节
    public static byte[] buildValueRefKey(byte[] key, byte[] value) {
        byte[] part1 = MD5Util.md5(key);
        byte[] part2 = MD5Util.md5(value);
        return Bytes.add(part1, part2);
    }

    public static boolean isValueRefKey(byte[] key, byte[] value) {
        if (value.length != 32) return false;
        byte[] part1 = MD5Util.md5(key);
        if (value.length < part1.length) return false;
        for (int i=0; i<part1.length; i++) {
            if (part1[i] != value[i]) {
                return false;
            }
        }
        return true;
    }

    public static final byte[] CF_D = Bytes.toBytes("d");

    public static final byte[] M = Bytes.toBytes("m_");
    public static final byte[] D = Bytes.toBytes("d_");

    public static final byte[] COL_DATA = Bytes.add(D, Bytes.toBytes("data"));

    public static final byte[] COL_EXPIRE_TIME = Bytes.add(M, Bytes.toBytes("expireTime"));
    public static final byte[] COL_TYPE = Bytes.add(M, Bytes.toBytes("type"));

    public static byte[] dataQualifier(byte[] qualifier) {
        return Bytes.add(D, qualifier);
    }

    public static boolean isDataQualifier(byte[] qualifierOriginal) {
        for (int i=0; i<D.length; i++) {
           if (qualifierOriginal[i] != D[i]) {
               return false;
           }
        }
        return true;
    }

    public static byte[] data(byte[] qualifierOriginal) {
        byte[] data = new byte[qualifierOriginal.length - D.length];
        System.arraycopy(qualifierOriginal, D.length, data, 0, data.length);
        return data;
    }

    public static byte[] buildRowKey(byte[] key) {
        byte[] md5 = MD5Util.md5(key);
        byte[] prefix = new byte[8];
        System.arraycopy(md5, 0, prefix,0, 8);
        return Bytes.add(prefix, key);
    }
}
