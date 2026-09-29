package ke.co.signature.Auth.User;

import ke.co.signature.Auth.Role.Role;
import ke.co.signature.Auth.Role.RoleRepository;
import ke.co.signature.Configs.Region.Region;
import ke.co.signature.Configs.Region.RegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static ke.co.signature.Auth.Role.RoleValue.ROLE_REGIONAL_REP;

@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final RegionRepository regionRepository;

    /* =========================
       LIST USERS (already done)
       ========================= */
    @GetMapping
    public String listUsers(Model model) {
        model.addAttribute("users", userRepository.findAll());
        return "users";
    }

    @GetMapping("new")
    public String newUserForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("roles", roleRepository.findAll());
        model.addAttribute("regions", regionRepository.findAll());

        return "user-form";
    }

    @PostMapping("save")
    public String saveUser(
            @ModelAttribute User user,
            Long roleId,
            @RequestParam(required = false) List<Long> regionIds) {

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        if (ROLE_REGIONAL_REP.equals(role.getName())
                && (regionIds == null || regionIds.isEmpty())) {
            throw new RuntimeException(
                    "Regional Representative must have at least one region assigned");
        }

        user.setRoles(Set.of(role));

        if ("REGIONAL_REP".equals(role.getName())) {

            if (regionIds == null || regionIds.isEmpty()) {
                throw new RuntimeException("At least one region must be selected");
            }

            Set<Region> regions = new HashSet<>(
                    regionRepository.findAllById(regionIds)
            );

            user.setRegions(regions);
        } else {
            user.setRegions(Collections.emptySet());
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setEnabled(true);

        userRepository.save(user);

        return "redirect:/admin/users";
    }

    @GetMapping("/edit/{id}")
    public String editUserForm(@PathVariable Long id, Model model) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        model.addAttribute("user", user);
        model.addAttribute("roles", roleRepository.findAll());
        model.addAttribute("regions", regionRepository.findAll());

        return "user-edit";
    }

    /* =========================
       UPDATE USER (NO PASSWORD)
       ========================= */
    @PostMapping("/edit/{id}")
    public String updateUser(
            @PathVariable Long id,
            @RequestParam String username,
            @RequestParam Long roleId,
            @RequestParam(required = false) List<Long> regionIds,
            @RequestParam(defaultValue = "false") boolean enabled) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        user.setUsername(username);
        user.setEnabled(enabled);
        user.setRoles(new HashSet<>(Collections.singleton(role)));

        if (ROLE_REGIONAL_REP.equals(role.getName())) {

            if (regionIds == null || regionIds.isEmpty()) {
                throw new RuntimeException(
                        "Regional Representative must have at least one region assigned");
            }

            Set<Region> regions =
                    new HashSet<>(regionRepository.findAllById(regionIds));

            user.setRegions(regions);

        } else {
            user.getRegions().clear();
        }

        System.out.println("User before save: "+user);

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