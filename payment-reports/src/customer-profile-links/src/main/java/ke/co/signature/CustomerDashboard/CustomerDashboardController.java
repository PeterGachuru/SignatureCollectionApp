package ke.co.signature.CustomerDashboard;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
@RequestMapping("/customer")
public class CustomerDashboardController {
    private final CustomerDashboardService dashboardService;

    @GetMapping("/dashboard")
    public String dashboard(Model model, Principal principal) {
        String username = principal.getName();

        CustomerDashboardDTO dashboard =
                dashboardService.getDashboard(username);

        model.addAttribute("dashboard", dashboard);
        return "customer/dashboard";
    }
}
