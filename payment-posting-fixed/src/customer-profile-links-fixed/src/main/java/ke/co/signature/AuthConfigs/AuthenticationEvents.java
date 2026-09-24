package ke.co.signature.AuthConfigs;

import ke.co.signature.Audits.LoginAudit;
import ke.co.signature.Audits.LoginAuditRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

@Component
public class AuthenticationEvents {

    // ⚡ Declare the logger here
    private static final Logger log = LoggerFactory.getLogger(AuthenticationEvents.class);

    @Autowired
    private HttpServletRequest request;
    @Autowired
    private LoginAuditRepository loginAuditRepository;

    // Successful logins
    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        String username = event.getAuthentication().getName();
        String ip = request.getRemoteAddr();
        log.info("Login SUCCESS for {} from IP {}", username, ip);
        LoginAudit loginAudit = new LoginAudit();
        loginAudit.setLoginTime(new Date());
        loginAudit.setUsername(username);
        loginAudit.setStatus("SUCCESS");
        loginAudit.setRemoteAddress(ip);
        loginAuditRepository.save(loginAudit);
    }

    // Failed logins
    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent event) {
        String username = event.getAuthentication().getName();
        String ip = request.getRemoteAddr();
        String reason = event.getException().getMessage();
        log.warn("Login FAILURE for {} from IP {}. Reason: {}", username, ip, reason);
        LoginAudit loginAudit = new LoginAudit();
        loginAudit.setLoginTime(new Date());
        loginAudit.setUsername(username);
        loginAudit.setStatus("FAILURE");
        loginAudit.setRemoteAddress(ip);
        loginAudit.setReason(reason);
        loginAuditRepository.save(loginAudit);
    }
}