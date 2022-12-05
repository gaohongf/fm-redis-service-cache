package ex;

import xyz.fmcy.redis.cache.core.ServiceCacheHandler;

import java.util.Arrays;

/**
 * @author 付高宏
 * @date 2022/11/30 10:32
 */
public class CacheTest {
    @org.junit.Test
    public void test01(){
        System.out.println(ServiceCacheHandler.signBranchParse("token", "${token}@{0}_&{1}#{3}:*", new Object[]{"zhangsan", "lisi", "wangwu", "zhaoliu"}));
    }
}
