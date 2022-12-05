package xyz.fmcy.redis.cache.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 使用这个注解可以让一个方法成为缓存更新的契机,每次调用方法都会将对应签名下的缓存全部清理
 *
 * @author 付高宏
 * @date 2022/11/10 0:14
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface UpdateCache {
    /**
     * 缓存签名
     */
    String[] signs() default {};
}
