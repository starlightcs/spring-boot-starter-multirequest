package io.github.starlightcs.multirequest;

import com.alibaba.fastjson.*;
import io.github.starlightcs.annotation.MultiRequestBody;
import io.github.starlightcs.exception.HttpMediaTypeOrHttpBodyException;
import io.github.starlightcs.exception.MethodArgumentNotValidException;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * RequestBodyParam 参数解析器
 *
 * @author Allen starlightcs@foxmail.com
 */
public class MultiRequestBodyArgumentResolver implements HandlerMethodArgumentResolver {

    private static boolean hasNestedIfOptional = true;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        // 支持带@MultiRequestBody注解的参数
        return parameter.hasParameterAnnotation(MultiRequestBody.class);
    }

    /**
     * 参数解析，注意：非基本类型返回null会报空指针异常，要通过反射或者JSON工具类创建一个空对象
     *
     * @param parameter     方法入参
     * @param mavContainer  mdoleAndView容器
     * @param webRequest    request
     * @param binderFactory 绑定工厂
     * @return
     * @throws Exception
     */
    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        Object arg = readWithMessage(webRequest, parameter, mavContainer);
        String name = parameter.getParameterName();
        if (binderFactory != null) {
            WebDataBinder binder = binderFactory.createBinder(webRequest, arg, name);
            if (arg != null) {
                validateIfApplicable(binder, parameter);
                if (binder.getBindingResult().hasErrors() && isBindExceptionRequired(binder, parameter)) {
                    throw new MethodArgumentNotValidException(parameter, binder.getBindingResult());
                }
            }
            if (mavContainer != null) {
                mavContainer.addAttribute(BindingResult.MODEL_KEY_PREFIX + name, binder.getBindingResult());
            }
        }
        return arg;
    }

    private Object readWithMessage(NativeWebRequest webRequest, MethodParameter parameter, ModelAndViewContainer mavContainer) throws Exception {
        parameter = nestedIfOptional(parameter);
        // 根据@MultiRequestBody注解value作为json解析的key
        MultiRequestBody multiRequestBody = parameter.getParameterAnnotation(MultiRequestBody.class);
        String json = ((ServletWebRequest) webRequest).getRequest().getReader().lines().collect(Collectors.joining(System.lineSeparator()));
        Map<String, Object> data = new HashMap<>(4);
        if (!StringUtils.isEmpty(json)) {
            try {
                data = JSON.parseObject(json, new TypeReference<Map<String, Object>>() {
                });
            } catch (JSONException e) {
                throw new HttpMediaTypeOrHttpBodyException(parameter, e);
            }
        }
        String name = StringUtils.isEmpty(multiRequestBody.value()) ? parameter.getParameterName() : multiRequestBody.value();
        Object param = data.get(name);

        check(parameter, param, name, multiRequestBody);
        mavContainer.addAttribute(BindingResult.MODEL_KEY_PREFIX + name, param);
        if (param instanceof Map || param instanceof List) {
            return JSON.parseObject(param.toString(), parameter.getParameterType());
        } else if (isDto(parameter.getParameterType())) {
            return parseBasicTypeWrapper(parameter, param);
        } else {
            return param;
        }
    }

    private MethodParameter nestedIfOptional(MethodParameter parameter) {
        // 判断是否 org.springframework.core.MethodParameter#nestedIfOptional() 方法是否存在
        // 建议使用大于或等于 spring 4.3
        if (hasNestedIfOptional) {
            try {
                return parameter.nestedIfOptional();
            } catch (NoSuchMethodError e) {
                hasNestedIfOptional = false;
                return nested(parameter);
            }
        } else {
            return nested(parameter);
        }
    }

    private MethodParameter nested(MethodParameter parameter) {
        parameter = new MethodParameter(parameter);
        parameter.increaseNestingLevel();
        return parameter;
    }

    /**
     * 检查参数
     *
     * @param parameter        方法入参
     * @param param            json参数对应的value
     * @param name             json参数的key
     * @param multiRequestBody 自定义注解
     * @throws MethodArgumentNotValidException
     */
    private void check(MethodParameter parameter, Object param, String name, MultiRequestBody multiRequestBody) throws Exception {
        // 如果value是空，并且注解为必填，抛出Valid异常
        if (Objects.isNull(param) && multiRequestBody.required()) {
            throw new MethodArgumentNotValidException(parameter, name + " is Null ");
        }
        // 如果value是空，但是注解为非必填，通过校验
        if (Objects.isNull(param)) {
            return;
        }
        // 如果value类型与方法入参类型一致，通过校验
        if (Objects.equals(param.getClass(), parameter.getParameterType())) {
            return;
        }
        // 如果value类型与方法入参类型不一致，但入参类型是基本包装类型，通过校验
        if (!Objects.equals(param.getClass(), parameter.getParameterType()) && isDto(parameter.getParameterType())) {
            return;
        }
        // value类型json数组对象，并且方法入参是数组，通过校验
        if (Objects.equals(param.getClass(), JSONArray.class) && parameter.getParameterType().isArray()) {
            return;
        }
        // value类型json对象，并且方法入参不是基本类型，通过校验
        if (Objects.equals(param.getClass(), JSONObject.class) && !isDto(parameter.getParameterType())) {
            return;
        }
        throw new MethodArgumentNotValidException(parameter, name + " argument type mismatch ");
    }

    /**
     * 检查是否基本包装类型
     */
    private boolean isDto(Class<?> classz) {
        return (Objects.equals(classz, Integer.class)
                || Objects.equals(classz, Float.class)
                || Objects.equals(classz, Double.class)
                || Objects.equals(classz, Byte.class)
                || Objects.equals(classz, Boolean.class)
                || Objects.equals(classz, Character.class)
                || Objects.equals(classz, Short.class)
                || Objects.equals(classz, Long.class)
                || Objects.equals(classz, String.class));
    }

    /**
     * 基本类型包装类解析
     */
    private Object parseBasicTypeWrapper(MethodParameter parameter, Object value) throws Exception {
        try {
            Class<?> parameterType = parameter.getParameterType();
            if (Number.class.isAssignableFrom(parameterType)) {
                Number number = (Number) value;
                if (parameterType == Integer.class) {
                    return number.intValue();
                } else if (parameterType == Short.class) {
                    return number.shortValue();
                } else if (parameterType == Long.class) {
                    return number.longValue();
                } else if (parameterType == Float.class) {
                    return number.floatValue();
                } else if (parameterType == Double.class) {
                    return number.doubleValue();
                } else if (parameterType == Byte.class) {
                    return number.byteValue();
                }
            } else if (parameterType == String.class) {
                return value;
            } else if (parameterType == Boolean.class) {
                return value.toString();
            } else if (parameterType == Character.class) {
                return value.toString().charAt(0);
            }
            return null;
        } catch (Exception e) {
            throw new MethodArgumentNotValidException(parameter, "parseBasicTypeWrapper: " + value + " argument type mismatch ");
        }
    }

    /**
     * Validate the binding target if applicable.
     * <p>The default implementation checks for {@code @javax.validation.Valid},
     * Spring's {@link org.springframework.validation.annotation.Validated},
     * and custom annotations whose name starts with "Valid".
     *
     * @param binder    the DataBinder to be used
     * @param parameter the method parameter descriptor
     * @see #isBindExceptionRequired
     * @since 4.1.5
     */
    private void validateIfApplicable(WebDataBinder binder, MethodParameter parameter) {
        Annotation[] annotations = parameter.getParameterAnnotations();
        for (Annotation ann : annotations) {
            Validated validatedAnn = AnnotationUtils.getAnnotation(ann, Validated.class);
            if (validatedAnn != null || ann.annotationType().getSimpleName().startsWith("Valid")) {
                Object hints = (validatedAnn != null ? validatedAnn.value() : AnnotationUtils.getValue(ann));
                Object[] validationHints = (hints instanceof Object[] ? (Object[]) hints : new Object[]{hints});
                binder.validate(validationHints);
                break;
            }
        }
    }

    /**
     * Whether to raise a fatal bind exception on validation errors.
     *
     * @param binder    the data binder used to perform data binding
     * @param parameter the method parameter descriptor
     * @return {@code true} if the next method argument is not of type {@link Errors}
     * @since 4.1.5
     */
    private boolean isBindExceptionRequired(WebDataBinder binder, MethodParameter parameter) {
        int i = parameter.getParameterIndex();
        Class<?>[] paramTypes = parameter.getExecutable().getParameterTypes();
        boolean hasBindingResult = (paramTypes.length > (i + 1) && Errors.class.isAssignableFrom(paramTypes[i + 1]));
        return !hasBindingResult;
    }

}