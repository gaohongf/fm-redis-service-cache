package xyz.fmcy.redis.cache.core;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * @author 付高宏
 * @date 2022/11/23 19:48
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(ServiceCacheHandler.class)
public @interface EnableServiceCache {
}
