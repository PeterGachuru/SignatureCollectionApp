package ke.co.signature.Auth.User;

import ke.co.signature.Auth.Role.Role;
import ke.co.signature.Auth.Role.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    /* =========================
       LIST USERS (already done)
       ========================= */
    @GetMapping
    public String listUsers(Model model) {
        model.addAttribute("users", userRepository.findAll());
        return "users";
    }

    /* =========================
       EDIT USER FORM
       ========================= */
    @GetMapping("/edit/{id}")
    public String editUserForm(@PathVariable Long id, Model model) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        model.addAttribute("user", user);
        model.addAttribute("roles", roleRepository.findAll());

        return "user-edit";
    }

    @GetMapping("new")
    public String newUserForm(Model model) {
        model.addAttribute("user", new User());
        List<Role> roles = roleRepository.findAll();
        model.addAttribute("roles", roles);
        return "user-form";
    }

    @PostMapping("save")
    public String saveUser(@ModelAttribute User user, Long roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        user.setRoles(Set.of(role));
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setEnabled(true);

        userRepository.save(user);
        return "redirect:/admin/users";
    }

    /* =========================
       UPDATE USER (NO PASSWORD)
       ========================= */
    @PostMapping("/edit/{id}")
    public String updateUser(
            @PathVariable Long id,
            @RequestParam String username,
            @RequestParam(required = false) List<Long> roleIds,
            @RequestParam(defaultValue = "false") boolean enabled
    ) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setUsername(username);
        user.setEnabled(enabled);

        if (roleIds != null) {
            Set<Role> roles = new HashSet<>(roleRepository.findAllById(roleIds));
            user.setRoles(roles);
        }

        userRepository.save(user);
        return "redirect:/admin/users";
    }

    /* =========================
       DELETE USER
       ========================= */
    @PostMapping("/delete/{id}")
    public String deleteUser(@PathVariable Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 🔒 Prevent deleting last ADMIN
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(r -> r.getName().equals("ROLE_ADMIN"));

        if (isAdmin) {
            long adminCount = userRepository.countUsersWithRole("ROLE_ADMIN");
            if (adminCount <= 1) {
                throw new RuntimeException("Cannot delete the last admin");
            }
        }

        userRepository.delete(user);
        return "redirect:/admin/users";
    }

    @GetMapping("/view/{id}")
    public String viewUser(@PathVariable Long id, Model model) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        model.addAttribute("user", user);
        return "user-view";
    }
}
