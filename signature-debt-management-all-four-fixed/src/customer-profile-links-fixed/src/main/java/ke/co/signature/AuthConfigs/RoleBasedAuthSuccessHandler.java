package ke.co.signature.AuthConfigs;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;
import java.util.Collection;

import static ke.co.signature.Auth.Role.RoleValue.*;

public class RoleBasedAuthSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        String redirectUrl = "/";

        if (authorities.stream().anyMatch(a -> a.getAuthority().equals(ROLE_ADMIN.name()))) {
            System.out.println("redirect to /");
            redirectUrl = "/";
        } else if (authorities.stream().anyMatch(a -> a.getAuthority().equals(ROLE_CUSTOMER_ADMIN.name()))) {
            System.out.println("redirect to /customer/dashboard");
            redirectUrl = "/customer/dashboard";
        } else if (authorities.stream().anyMatch(a -> a.getAuthority().equals(ROLE_REGIONAL_REP.name()))) {
            System.out.println("redirect to /manager/dashboard");
            redirectUrl = "/";
//            redirectUrl = "/manager/dashboard";
        }

        response.sendRedirect(redirectUrl);
    }
}

