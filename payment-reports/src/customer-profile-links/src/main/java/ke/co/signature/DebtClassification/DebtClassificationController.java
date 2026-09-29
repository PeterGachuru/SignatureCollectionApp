package ke.co.signature.DebtClassification;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/config/debt-classifications")
public class DebtClassificationController {
    private final DebtClassificationService service;

    public DebtClassificationController(DebtClassificationService service) {
        this.service = service;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("classifications", service.findAll());
        return "config/debt/debt-classification-list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("classification", new DebtClassification());
        return "config/debt/debt-classification-form";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("classification", service.getById(id));
        return "config/debt/debt-classification-form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute DebtClassification classification,
                       RedirectAttributes redirect) {

        service.save(classification);
        redirect.addFlashAttribute("success", "Debt classification saved");
        return "redirect:/config/debt-classifications";
    }
}

