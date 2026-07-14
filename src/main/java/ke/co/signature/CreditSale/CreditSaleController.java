package ke.co.signature.CreditSale;

import ke.co.signature.Customer.CustomerRepository;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
@RequestMapping("/credit-sales")
public class CreditSaleController {

    private final CreditSaleService creditSaleService;
    private final CustomerRepository customerRepository;

    public CreditSaleController(CreditSaleService creditSaleService,
                                CustomerRepository customerRepository) {
        this.creditSaleService = creditSaleService;
        this.customerRepository = customerRepository;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/new")
    public String newCreditSale(Model model) {
        model.addAttribute("customers", customerRepository.findAll());
        model.addAttribute("today", LocalDate.now());
        return "credit-sales/credit-sale-form";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/save")
    public String saveCreditSale(@RequestParam Long customerId,
                                 @RequestParam BigDecimal grossAmount,
                                 @RequestParam BigDecimal discount,
                                 @RequestParam BigDecimal invoiceAmount,
                                 @RequestParam(required = false) LocalDate saleDate,
                                 @RequestParam(required = false) String description,
                                 RedirectAttributes redirectAttributes) {

        creditSaleService.createCreditSale(customerId, grossAmount, discount, invoiceAmount, saleDate, description);
        redirectAttributes.addFlashAttribute("success", "Credit Sale recorded successfully");

        return "redirect:/credit-sales/list";
    }

    @GetMapping("/list")
    public String listCreditSales(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Model model) {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String username = authentication.getName();

        boolean regionalRep = authentication.getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_REGIONAL_REP"));

        Page<CreditSaleDTO> creditSalePage =
                creditSaleService.listCreditSalesPaginated(
                        username,
                        page,
                        size);

        model.addAttribute("creditSales", creditSalePage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", creditSalePage.getTotalPages());
        model.addAttribute("regionalRep", regionalRep);

        return "credit-sales/credit-sales-list";
    }

    @GetMapping("/{id}")
    public String viewCreditSale(@PathVariable Long id, Model model) {

        CreditSaleDetailsDTO creditSale = creditSaleService.getCreditSaleDetails(id);

        model.addAttribute("cs", creditSale);

        return "credit-sales/credit-sale-profile";
    }

    @PostMapping("/upload")
    public String uploadCreditSales(
            @RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes) {

        try {

            int records = creditSaleService.importCreditSales(file);

            redirectAttributes.addFlashAttribute(
                    "success",
                    records + " credit sales uploaded successfully");

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    "Upload failed: ");
            e.printStackTrace();
        }

        return "redirect:/credit-sales/list";
    }
}
