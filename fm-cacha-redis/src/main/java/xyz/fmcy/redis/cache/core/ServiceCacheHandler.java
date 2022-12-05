package xyz.fmcy.redis.cache.core;

import org.apache.logging.log4j.util.Strings;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.CollectionUtils;
import xyz.fmcy.redis.cache.annotation.*;

import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 服务缓存处理器
 *
 * @author 付高宏
 * @date 2022/11/9 20:32
 */
@Configuration
@Aspect
public class ServiceCacheHandler {

    @Bean
    public RedisTemplate<Object, Object> redisStringTemplate(RedisTemplate<Object, Object> redisTemplate) {
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        redisTemplate.setKeySerializer(stringRedisSerializer);
        return redisTemplate;
    }

    final static List<Class<?>> stringKeyCacheLegitimateArgTypes;

    static {
        stringKeyCacheLegitimateArgTypes = new ArrayList<>();
        stringKeyCacheLegitimateArgTypes.add(byte.class);
        stringKeyCacheLegitimateArgTypes.add(short.class);
        stringKeyCacheLegitimateArgTypes.add(int.class);
        stringKeyCacheLegitimateArgTypes.add(long.class);
        stringKeyCacheLegitimateArgTypes.add(double.class);
        stringKeyCacheLegitimateArgTypes.add(float.class);
        stringKeyCacheLegitimateArgTypes.add(boolean.class);
        stringKeyCacheLegitimateArgTypes.add(char.class);
        stringKeyCacheLegitimateArgTypes.add(Byte.class);
        stringKeyCacheLegitimateArgTypes.add(Short.class);
        stringKeyCacheLegitimateArgTypes.add(Integer.class);
        stringKeyCacheLegitimateArgTypes.add(Long.class);
        stringKeyCacheLegitimateArgTypes.add(Double.class);
        stringKeyCacheLegitimateArgTypes.add(Float.class);
        stringKeyCacheLegitimateArgTypes.add(Boolean.class);
        stringKeyCacheLegitimateArgTypes.add(Character.class);
        stringKeyCacheLegitimateArgTypes.add(String.class);

    }

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @AfterReturning(value = "@within(org.springframework.stereotype.Service) && @annotation(xyz.fmcy.redis.cache.annotation.Cache)", returning = "ret")
    public void cache(JoinPoint point, Object ret) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        Cache cache = method.getAnnotation(Cache.class);
        String sign = cache.sign();
        String hashKey = hashKey(point.getArgs(), method, sign);
        if (cache.expire() > 0) {
            redisTemplate.opsForValue().set(hashKey, ret, cache.expire(), TimeUnit.SECONDS);
        } else {
            redisTemplate.opsForValue().set(hashKey, ret);
        }
    }

    @Around("@within(org.springframework.stereotype.Service) && @annotation(xyz.fmcy.redis.cache.annotation.Cache)")
    public Object readCache(ProceedingJoinPoint point) throws Throwable {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        Cache cache = method.getAnnotation(Cache.class);
        String sign = cache.sign();
        String hashKey = hashKey(point.getArgs(), method, sign);
        Object o = updateTerm(method, hashKey);
        return o != null ? o : point.proceed();
    }

    private Object updateTerm(Method method, String key) {
        Object o = redisTemplate.opsForValue().get(key);
        TermUpdate term = method.getAnnotation(TermUpdate.class);
        if (o != null && Objects.nonNull(term) && term.expire() > 0) {
            redisTemplate.expire(key, term.expire(), TimeUnit.SECONDS);
        }
        return o;
    }

    @AfterReturning(value = "@within(org.springframework.stereotype.Service)&& @annotation(xyz.fmcy.redis.cache.annotation.CacheInclude) && !@annotation(xyz.fmcy.redis.cache.annotation.CacheExport)", returning = "ret")
    public void unidirectionalIncludeCache(JoinPoint point, Object ret) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Object[] args = point.getArgs();
        CacheInclude include = signature.getMethod().getAnnotation(CacheInclude.class);
        int dataIndex;
        if (args.length == 0 || args.length <= (dataIndex = include.dataIndex())) {
            return;
        }
        if (isUnidirectionalCacheLegitimateArgType(ret)) {
            Object arg = args[dataIndex];
            String signNode = include.sign();
            CacheNode cacheNode = include.node();
            if (cacheNode != null && !Strings.isEmpty(cacheNode.branch())) {
                signNode = signBranchParse(signNode, cacheNode.branch(), args);
            }
            String key = signNode + ":" + ret;
            if (include.expire() > 0) {
                redisTemplate.opsForValue().set(key, arg, include.expire(), TimeUnit.SECONDS);
            } else {
                redisTemplate.opsForValue().set(key, arg);
            }
        } else {
            LoggerFactory.getLogger(this.getClass()).error(
                    "入口方法的返回值类型不允许超出合法的类型列表:" + stringKeyCacheLegitimateArgTypes
            );
        }
    }

    @Around("@within(org.springframework.stereotype.Service) && @annotation(xyz.fmcy.redis.cache.annotation.CacheExport) && !@annotation(xyz.fmcy.redis.cache.annotation.CacheInclude)")
    public Object readUnidirectionalExportCache(ProceedingJoinPoint point) throws Throwable {
        Method method = ((MethodSignature) point.getSignature()).getMethod();
        CacheExport export = method.getAnnotation(CacheExport.class);
        String signNode = export.sign();
        CacheNode cacheNode = export.node();
        if (cacheNode != null && !Strings.isEmpty(cacheNode.branch())) {
            signNode = signBranchParse(signNode, cacheNode.branch(), point.getArgs());
        }
        String key = signNode;
        if (key.contains("*")) {
            Set<String> keys = redisTemplate.keys(key);
            if (!CollectionUtils.isEmpty(keys)) {
                key = keys.toArray(new String[0])[0];
            }
        }
        Object o = updateTerm(method, key);
        return o != null ? o : point.proceed();
    }

    /**
     * 抹除一个签名,或者分支下的所有缓存
     */
    @After("@within(org.springframework.stereotype.Service) && @annotation(xyz.fmcy.redis.cache.annotation.UpdateCache) && !@annotation(xyz.fmcy.redis.cache.annotation.CacheNode)")
    public void clearCache(JoinPoint point) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        UpdateCache updateCache = method.getAnnotation(UpdateCache.class);
        String[] signs = updateCache.signs();
        for (String sign : signs) {
            Set<String> signKey = Optional.ofNullable(redisTemplate.keys(sign)).orElse(new HashSet<>());
            Set<String> keys = Optional.ofNullable(redisTemplate.keys(sign + ":*")).orElse(new HashSet<>());
            keys.addAll(signKey);
            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        }

    }

    /**
     * 抹除指定缓存以及分支下所有缓存
     */
    @After("@within(org.springframework.stereotype.Service) && @annotation(xyz.fmcy.redis.cache.annotation.UpdateCache) && @annotation(xyz.fmcy.redis.cache.annotation.CacheNode)")
    public void clearCacheByKey(JoinPoint point) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        UpdateCache updateCache = method.getAnnotation(UpdateCache.class);
        String[] signs = updateCache.signs();
        CacheNode cacheNode = method.getAnnotation(CacheNode.class);
        if (!Strings.isEmpty(cacheNode.branch())) {
            for (String sign : signs) {
                Set<String> keys = redisTemplate.keys(signBranchParse(sign, cacheNode.branch(), point.getArgs()) + "*");
                if (keys != null && !keys.isEmpty()) {
                    redisTemplate.delete(keys);
                }
            }
        }
    }

    public static String createDefaultSign(Method method) {
        Class<?> clazz = method.getDeclaringClass();
        String name = method.getName();
        StringJoiner joiner = new StringJoiner("&");
        Arrays.stream(method.getParameters()).map(
                Parameter::getType
        ).forEach(type -> joiner.add(type.getTypeName()));
        return clazz.getName() + "#" + name + "?" + joiner;
    }

    public static String signBranchParse(String sign, String branch, Object[] args) {
        final String regex1 = "([&@#])\\{([0-9]+)}";
        branch = branch.replace("${sign}", sign);
        String[] strings = branch.split(regex1);
        Pattern compile = Pattern.compile(regex1);
        Matcher matcher = compile.matcher(branch);
        List<Integer> indexList = new ArrayList<>();
        List<String> markList = new ArrayList<>();
        while (matcher.find()) {
            indexList.add(Integer.valueOf(matcher.group(2)));
            markList.add(matcher.group(1));
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < indexList.size(); i++) {
            if (strings.length > i) {
                stringBuilder.append(strings[i]);
            }
            try {
                String mark = markList.get(i);
                if ("&".equals(mark)) {
                    stringBuilder.append(args[indexList.get(i)]);
                } else {
                    stringBuilder.append(":").append("#".equals(mark) ? Arrays.hashCode(new Object[]{args[indexList.get(i)]}) : args[indexList.get(i)]);
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                LoggerFactory.getLogger(ServiceCacheHandler.class).warn(e.getMessage());
            }
        }
        if (strings.length > indexList.size()) {
            stringBuilder.append(strings[strings.length - 1]);
        }
        return stringBuilder.toString();
    }


    public static String hashKey(Object[] args, Method method, String signKey) {
        int argsHashCode = Arrays.hashCode(args);
        if ("".equals(signKey)) {
            String defaultSign = createDefaultSign(method);
            return defaultSign + ":" + argsHashCode;
        }
        return signKey + ":" + argsHashCode;
    }

    public static boolean isUnidirectionalCacheLegitimateArgType(Object o) {
        return stringKeyCacheLegitimateArgTypes.contains(o.getClass());
    }
}
