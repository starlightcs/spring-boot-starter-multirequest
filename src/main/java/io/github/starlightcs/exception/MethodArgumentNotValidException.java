package io.github.starlightcs.exception;

import org.springframework.core.MethodParameter;
import org.springframework.validation.*;

/**
 * 方法参数无效异常
 *
 * @author Allen starlightcs@foxmail.com
 */
public class MethodArgumentNotValidException extends org.springframework.web.bind.MethodArgumentNotValidException {

    private final MethodParameter parameter;

    private final String errorMsg;

    /**
     * Constructor for {@link MethodArgumentNotValidException}.
     *
     * @param parameter the parameter that failed validation
     * @param bindingResult  异常信息
     */
    public MethodArgumentNotValidException(MethodParameter parameter, BindingResult bindingResult) {
        super(parameter, bindingResult);
        this.parameter = parameter;
        this.errorMsg = bindingResult.getFieldErrors().get(0).getDefaultMessage();
    }

    /**
     * 自定义父类异常
     */
    private static BindingResult save(MethodParameter parameter, String errorMsg) {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(parameter, errorMsg);
        bindingResult.addError(new ObjectError(parameter.toString(), errorMsg));
        return bindingResult;
    }

    /**
     * Constructor for {@link MethodArgumentNotValidException}.
     *  @param parameter the parameter that failed validation
     * @param errorMsg  异常信息
     */
    public MethodArgumentNotValidException(MethodParameter parameter, String errorMsg) {
        super(parameter, save(parameter, errorMsg));
        this.parameter = parameter;
        this.errorMsg = errorMsg;
    }

    /**
     * Return the method parameter that failed validation.
     */
    public MethodParameter getParameter() {
        return this.parameter;
    }

    /**
     * 返回异常信息.
     */
    public String getErrorMsg() {
        return this.errorMsg;
    }

    @Override
    public String getMessage() {
        return "Validation failed for argument at index " +
                this.parameter.getParameterIndex() + " in method: " +
                this.parameter.getMethod().toGenericString() +
                ", " + this.errorMsg;
    }

}