package xyz.fmcy.redis.cache.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 *  指定方法为缓存出口
 *  作为出口的方法上指定的缓存签名必须与入口一致,出口的参数类型必须与入口的返回值类型相同,出口的返回值类型必须和入口传入的参数相同
 *
 * @author 付高宏
 * @date 2022/11/17 10:11
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CacheExport {

    /**
     * 1.不建议多个缓存服务使用相同签名,尤其是在参数有概率完全相同的情况下。<br/>
     * 2.多个缓存服务同时使用相同签名时,在更新的时候将会清除拥有相同签名的所有缓存。<br/>
     * 3.缓存签名,注意和更新缓存时签名一致,指定出入口缓存必须指定签名。<br/><br/>
     * 分支概念:<br/>
     * 将签名与返回值用":"拼接
     */
    String sign();

    CacheNode node() default @CacheNode(branch = "${sign}@{0}");
}
