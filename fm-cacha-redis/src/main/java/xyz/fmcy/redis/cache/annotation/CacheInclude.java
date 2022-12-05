package xyz.fmcy.redis.cache.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author 付高宏
 * @date 2022/11/23 10:14
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CacheInclude {

    /**
     * 过期时间,单位秒 小于等于0时表示不过期
     * 作为出口时如果使用了@{@link TermUpdate}那么将会刷新缓存时间
     */
    long expire() default 0;

    /**
     * 1.不建议多个缓存服务使用相同签名,尤其是在参数有概率完全相同的情况下。<br/>
     * 2.多个缓存服务同时使用相同签名时,在更新的时候将会清除拥有相同签名的所有缓存。<br/>
     * 3.缓存签名,注意和更新缓存时签名一致,指定出入口缓存必须指定签名。<br/><br/>
     * 分支概念:<br/>
     * 将签名与返回值用":"拼接
     */
    String sign();

    int dataIndex() default 0;

    CacheNode node() default @CacheNode(branch = "${sign}");
}
