package Chambing.demo.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.HandlerInterceptor;
import jakarta.servlet.http.*;

/** El panel ya usaba sesión; sus APIs también deben comprobarla. */
@Configuration
public class AdminSessionConfig implements WebMvcConfigurer {
    @Override public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
                HttpSession session = request.getSession(false);
                if (session != null && Boolean.TRUE.equals(session.getAttribute("adminLogged"))) return true;
                response.setStatus(403);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"message\":\"Inicia sesión como administrador\"}");
                return false;
            }
        }).addPathPatterns("/admin/**").excludePathPatterns(
            "/admin", "/admin/", "/admin/login", "/admin/logout", "/admin/panel", "/admin/panel/");
    }
}
