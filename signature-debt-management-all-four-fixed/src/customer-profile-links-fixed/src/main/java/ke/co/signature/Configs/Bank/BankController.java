package ke.co.signature.Configs.Bank;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/config/banks")
@RequiredArgsConstructor
//@PreAuthorize("hasRole('ADMIN')")
public class BankController {

    private final BankRepository bankRepository;

    @GetMapping
    public String list(Model model) {
        model.addAttribute(
                "banks",
                bankRepository.findAll(
                        org.springframework.data.domain.Sort.by(
                                org.springframework.data.domain.Sort.Direction.ASC,
                                "name"
                        )
                )
        );

        model.addAttribute("bank", new Bank());

        return "config/banks";
    }

    @PostMapping("/save")
    public String save(
            @ModelAttribute Bank bank,
            RedirectAttributes redirectAttributes) {

        if (bank.getCode() == null || bank.getCode().isBlank()) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Bank code is required."
            );

            return "redirect:/config/banks";
        }

        if (bank.getName() == null || bank.getName().isBlank()) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Bank name is required."
            );

            return "redirect:/config/banks";
        }

        bank.setCode(bank.getCode().trim().toUpperCase());
        bank.setName(bank.getName().trim());

        bankRepository.save(bank);

        redirectAttributes.addFlashAttribute(
                "success",
                "Bank saved successfully."
        );

        return "redirect:/config/banks";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        Bank bank = bankRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Bank not found."));

        bank.setActive(!bank.isActive());

        bankRepository.save(bank);

        redirectAttributes.addFlashAttribute(
                "success",
                "Bank status updated."
        );

        return "redirect:/config/banks";
    }
}