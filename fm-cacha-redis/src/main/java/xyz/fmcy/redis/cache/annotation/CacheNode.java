package xyz.fmcy.redis.cache.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author 付高宏
 * @date 2022/11/17 14:26
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CacheNode {
    /**
     * * 分支规则:<br/>
     * 表达式 ${sign}表示签名<br/>
     * &#064;{0}  表示以字符串形式表示的分支分布在参数值下标0的位置<br/>
     * #{0} 表示以hash值形式表示的分支分布在参数值下标0的位置
     * &{0} 表示用于替换的值分布在参数值下标0的位置
     */
    String branch();
}
