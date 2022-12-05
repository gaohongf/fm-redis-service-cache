## @xyz.fmcy.redis.cache.annotation.Cache
标记方法进行简单地缓存
```java
@Cache
public String hello(String name){
    return "Hello" + name;
}
```
执行以上方法后会将返回值存入缓存,下次再调用这个方法并且传入相同参数时将会返回缓存里的结果,而业务逻辑不会再被执行

---

```java
@Cache(sign = "hello",expire = 60)
public String hello(String name){
    return "Hello" + name;
}
```
可选参数 ``sign`` 缓存签名,为该处缓存命名最主要的作用是为它分组,能够获得更好的结构

---
可选参数 ``expire`` 给缓存设定一个过期时间,单位是秒,默认不过期

---
## @xyz.fmcy.redis.cache.annotation.UpdateCache
```java
@UpdateCache(signs = {"hello"})
public void update(){
    ...
}
```
更新签名下的缓存,只要签名相同,或者同级签名相同都会被清理

---
可选参数 ``signs`` 可以传入一个缓存签名的数组,数组内对应的全部缓存都会被清理

---
## @xyz.fmcy.redis.cache.annotation.CacheNode
```java
@UpdateCache(signs = {"hello"})
@CacheNode(branch = "${sign}:zhangsan")
public void update(){
    ...
}
```
缓存节点,通常和其他的注解共同使用,用于指示节点

---
必选参数 ``branch`` 指定节点,如代码所示,这段代码执行后会指定的将 ``hello:zhangsan``以及其下所有的缓存删除也包括 zhangsan0 等当然一般不推荐这样使用
```java
@UpdateCache(signs = {"hello"})
@CacheNode(branch = "${sign}@{0}")
public void update(String name){
    ...
}
```
表达式 ``@{index}`` 将会被替换为入参的对应下标所指的对象并且分级
同样的还有 ``#{index}`` 会换为入参的对应下标所指的对象的哈希值并且分级, ``&{index}`` 替换为入参的对应下标所指的对象不分级
``*`` 指示模糊找寻对应键,并且使用找到的第一个键

---
## @xyz.fmcy.redis.cache.annotation.TermUpdate

```java
@Cache(sign = "hello",expire = 60)
@TermUpdate(expire = 60)
public String hello(String name){
    return "Hello" + name;
}
```
该注解标记方法读取时会刷新过期时间

必选参数 ``expire`` 表示查询之后更新的过期时间

---
## @xyz.fmcy.redis.cache.annotation.CacheInclude
```java
@CacheInclude(sign = "hello",expire = 60,dataIndex = 1,node = @CacheNode(branch = "${sign}@{0}"))
public String set(String name,Object data){
    return "key:" + name;
}
```
该注解标注一个方法为缓存的输入口,只进不出.

可选参数 ``dataIndex`` 指示要被缓存的数据所在的入参下标 默认 ``0``

可选参数 ``node`` 指示缓存所存储的节点 默认 ``${sign}``

返回值是一个 ``key`` 可以使用这个 ``key`` 来取回数据

---

## @xyz.fmcy.redis.cache.annotation.CacheExport

```java
@CacheExport(sign = "hello",node = @CacheNode(branch = "${sign}@{0}@{1}"))
@TermUpdate(expire = 60)
public Object get(String name,String key){
    return null;
}
```
与入口相对应的还有出口,想要取出指定的对象需要节点的信息和 ``key``

如果回来取的时候无法得知对应节点但是 ``key`` 值保证是唯一也可以将表达式写为 ``${sign}:*@{key}``这样就可以找到对应的数据了
