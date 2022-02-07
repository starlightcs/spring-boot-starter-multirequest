package io.github.starlightcs.annotation;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Controller中方法接收多个JSON对象
 *
 * @author Allen starlightcs@foxmail.com
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface MultiRequestBody {
    /**
     * 别名为 {@link #name}.
     */
    @AliasFor("name")
    String value() default "";

    /**
     * 绑定请求参数的名称
     */
    @AliasFor("value")
    String name() default "";

    /**
     * 是否是必要的 body param 参数
     * <p>默认为 {@code true} ，参数为 NULL 时抛出异常
     * 如果允许 body param 为 NULL，请设置为 {@code false}
     */
    boolean required() default true;
}