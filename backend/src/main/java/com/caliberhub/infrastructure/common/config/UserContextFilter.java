package com.caliberhub.infrastructure.common.config;

import com.caliberhub.infrastructure.common.context.UserContextHolder;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;

/**
 * 用户上下文过滤器
 * 从请求头 X-User 中提取用户身份
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class UserContextFilter implements Filter {
    
    private static final String USER_HEADER = "X-User";
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            if (request instanceof HttpServletRequest httpRequest) {
                String user = httpRequest.getHeader(USER_HEADER);
                if (StringUtils.hasText(user)) {
                    UserContextHolder.setCurrentUser(user);
                }
            }
            chain.doFilter(request, response);
        } finally {
            UserContextHolder.clear();
        }
    }
}
