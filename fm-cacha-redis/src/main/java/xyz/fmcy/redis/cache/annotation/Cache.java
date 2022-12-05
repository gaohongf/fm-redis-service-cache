package xyz.fmcy.redis.cache.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 创建一个简易的缓存
 * @author 付高宏
 * @date 2022/11/9 22:21
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Cache {
    /**
     * 过期时间,单位秒 小于等于0时表示不过期
     */
    long expire() default 0;

    /**
     * 1.不建议多个缓存服务使用相同签名,尤其是在参数有概率完全相同的情况下。<br/>
     * 2.多个缓存服务同时使用相同签名时,在更新的时候将会清除拥有相同签名的所有缓存。<br/>
     * 3.缓存签名,注意和更新缓存时签名一致,默认格式为 完整类路径名#触发方法名。<br/><br/>
     * 分支概念:<br/>
     * 以传入参数的Hash值与签名拼接后作为 key 分支储存
     */
    String sign() default "";

}
