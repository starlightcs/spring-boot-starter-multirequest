# spring-boot-starter-multirequest

MultiRequestBody 是为了解决使用 @RequestBody 时，必须定义 Dto （或 String 接收解析） 的问题，基于 RequestBody 的逻辑，写了 MultiRequestBody 以支持读取 body 下的参数

## I.spring-boot 快速使用

#### 1.maven 引入jar包

**注意：** MultiRequestBody需要Java 8或更高版本。

如果您的应用程序是在maven中构建的，只需在pom.xml中添加以下代码即可。

```xml

    <dependencies>
        ...
        <dependency>
            <groupId>io.github.starlightcs</groupId>
            <artifactId>spring-boot-starter-multirequest</artifactId>
            <version>1.0.0.RELEASE</version>
        </dependency>
        ...
    </dependencies>


```

#### 2.建立 Configurer

添加 @EnableMultiRequestBody 标签注入服务

```java

@Component
@EnableMultiRequestBody
public class MultiRequestBodyConfigurer {

}

```

#### 3.Controller层使用

接口入参添加 @MultiRequestBody 标签
```java

@RestController
@RequestMapping
public class TestController {

    @PostMapping(value = "test1", produces = MediaType.APPLICATION_JSON_VALUE)
    public Object test1(@MultiRequestBody Integer value1 , @MultiRequestBody String value2 ) {
        Map<String, Object> date = new HashMap<>(2);
        date.put("value1", v1);
        date.put("value2", value2);
        return date;
    }

}
```

#### 4.调用
启动项目调用

```shell
[root@centos ~]# curl -H "Content-type:application/json" -X POST -d '{"value1":123,"value2":"test"}' http://127.0.0.1:8080/test1
{"value2":"test","value1":123}
```

## II.进阶使用

### MultiRequestBody 标签

@@MultiRequestBody(value="name",required=false) 提供额外的三个标签

参数名|作用
---|---
value|别名
name|同value，别名
required|是否必须要 body param 参数。默认为 true ，参数为空时抛出异常；如果允许 body param 为空，请设置为 false
