package io.github.starlightcs.annotation;

import io.github.starlightcs.config.MultiRequestBodyConfigurer;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 启动注解
 *
 * @author Allen starlightcs@foxmail.com
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Configuration
@Import(MultiRequestBodyConfigurer.class)
public @interface EnableMultiRequestBody {
}
