package ke.co.signature;


import ke.co.signature.Dashboard.DashboardSummaryDTO;
import ke.co.signature.Dashboard.Dashboardservice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    @Autowired
    Dashboardservice dashboardservice;

    @GetMapping("/")
    public String home(Model model) {
        // You can pass some info to the template if you want
        model.addAttribute("message", "Welcome to the Signature Collection App");

        DashboardSummaryDTO summary = dashboardservice.getDashboardSummary();

        model.addAttribute("summary", summary);
        model.addAttribute("message", "Welcome to Signature Care Collection App");

        return "home"; // This corresponds to src/main/resources/templates/home.html
    }


//    @RequestMapping("error")
//    public String handleError(HttpServletRequest request, Model model) {
//        Integer statusCode = (Integer) request.getAttribute("javax.servlet.error.status_code");
//        String errorMessage = (String) request.getAttribute("javax.servlet.error.message");
//        String requestUri = (String) request.getAttribute("javax.servlet.error.request_uri");
//
//        model.addAttribute("status", statusCode);
//        model.addAttribute("error", "Oops! Something went wrong");
//        model.addAttribute("message", errorMessage);
//        model.addAttribute("path", requestUri);
//        model.addAttribute("timestamp", new java.util.Date());
//
//        return "error/error"; // Thymeleaf template path
//    }
}