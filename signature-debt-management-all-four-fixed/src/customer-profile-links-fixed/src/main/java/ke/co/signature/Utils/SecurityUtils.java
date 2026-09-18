package ke.co.signature.Utils;

import ke.co.signature.Auth.User.MyUserDetails;
import ke.co.signature.Auth.User.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {

    public static User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;

        Object principal = auth.getPrincipal();
        if (principal instanceof MyUserDetails) { // use your class here
            return ((MyUserDetails) principal).getUser();
        }

        return null;
    }
}
