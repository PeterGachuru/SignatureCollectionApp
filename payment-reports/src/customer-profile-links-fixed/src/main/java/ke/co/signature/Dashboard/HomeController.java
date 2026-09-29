package ke.co.signature.Dashboard;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final Dashboardservice dashboardservice;


    @GetMapping("/")
    public String home(Model model) {

        DashboardSummaryDTO summary =
                dashboardservice.getDashboardSummary();

        model.addAttribute(
                "summary",
                summary
        );

        model.addAttribute(
                "message",
                "Welcome to Signature Care Collection App"
        );

        return "home";
    }
}