package ke.co.signature.Auth.User;

import ke.co.signature.Utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // View own profile
    @GetMapping
    public String viewProfile(Model model) {
        User user = SecurityUtils.getCurrentUser();

        model.addAttribute("user", user);
        return "profile";
    }

    // Show change password form
    @GetMapping("/change-password")
    public String showChangePasswordForm() {
        return "change-password";
    }

    // Handle password change
    @PostMapping("/change-password")
    public String changePassword(
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            Model model
    ) {
        User user = userRepository.findByUsername( SecurityUtils.getCurrentUser().getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check current password
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            model.addAttribute("error", "Current password is incorrect");
            return "change-password";
        }

        // Check new password match
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "New passwords do not match");
            return "change-password";
        }

        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        model.addAttribute("success", "Password updated successfully");
        return "change-password";
    }
}
