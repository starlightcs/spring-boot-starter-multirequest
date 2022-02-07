package io.github.starlightcs.filter;

import org.springframework.http.HttpMethod;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * 可反复获取body数据
 *
 * @author Allen starlightcs@foxmail.com
 */
public class BodyFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) {
        // do nothing
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        ServletRequest requestWrapper = null;
        if (request instanceof HttpServletRequest) {
            // 该方法处理 POST请求并且contentType为application/json格式以及contentType为null的
            HttpServletRequest httpServletRequest = (HttpServletRequest)request;
            if (HttpMethod.POST.toString().equals(httpServletRequest.getMethod())
                    && (httpServletRequest.getContentType() == null || httpServletRequest.getContentType().contains("json"))) {
                requestWrapper = new BodyRequestWrapper(httpServletRequest);
            }
        }
        if (requestWrapper == null) {
            chain.doFilter(request, response);
        } else {
            chain.doFilter(requestWrapper, response);
        }
    }

    @Override
    public void destroy() {
        // do nothing
    }
}
