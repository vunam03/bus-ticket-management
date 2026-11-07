package com.busbooking.bus_ticket_management.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class Module1SecurityAspect {

    @Autowired(required = false)
    private HttpServletRequest request;

    @Around("@annotation(adminAccessOnly)")
    public Object checkAuthorization(ProceedingJoinPoint joinPoint, AdminAccessOnly adminAccessOnly) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest req = attributes.getRequest();
            HttpSession session = req.getSession(false);

            if (session == null || session.getAttribute("CURRENT_USER_ROLE") == null) {
                System.err.println("[SECURITY WARN] Thao tác bị từ chối: Chưa đăng nhập khi gọi " + joinPoint.getSignature());
            } else {
                String userRole = (String) session.getAttribute("CURRENT_USER_ROLE");
                if (!adminAccessOnly.role().equals(userRole)) {
                    throw new SecurityException("Bạn không có quyền thực hiện chức năng này!");
                }
            }
        }
        return joinPoint.proceed();
    }
}