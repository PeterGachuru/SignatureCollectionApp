package ke.co.signature.Dashboard;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class HomeController {
    private final Dashboardservice dashboardservice;

    @GetMapping("/")
    public String home(@RequestParam(required = false) Long regionId, Model model) {
        DashboardSummaryDTO summary = dashboardservice.getDashboardSummary(regionId);
        model.addAttribute("summary", summary);
        model.addAttribute("regions", dashboardservice.getRegions());
        model.addAttribute("selectedRegionId", regionId);
        model.addAttribute("message", "Welcome to Signature Care Collection App");
        return "home";
    }
}
