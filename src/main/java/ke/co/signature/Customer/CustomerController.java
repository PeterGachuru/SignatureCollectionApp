package ke.co.signature.Customer;

import ke.co.signature.Auth.User.UserRepository;
import ke.co.signature.Auth.User.UserService;
import ke.co.signature.Configs.ConfigurationService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.ArrayList;

@Controller
@RequestMapping("/admin/customers")
public class CustomerController {
    private final CustomerService customerService;
    private final UserRepository userRepository; // reps
    private final ConfigurationService configService; // reps

    public CustomerController(CustomerService customerService,
                              UserRepository userRepository,
                              ConfigurationService configService) {
        this.customerService = customerService;
        this.userRepository = userRepository;
        this.configService = configService;
    }

    // Show form
    @GetMapping("/new")
    public String newCustomerForm(Model model) {
        model.addAttribute("customer", new Customer());
//        model.addAttribute("reps", userRepository.findByRole("REP"));
        model.addAttribute("reps", new ArrayList<>());
        model.addAttribute("units", configService.listUnits());
        model.addAttribute("towns", configService.listTowns());
        return "customers/customer-form";
    }

    // Submit form
    @PostMapping("/save")
    public String saveCustomer(@ModelAttribute Customer customer,
                               RedirectAttributes redirectAttributes) {

        customerService.createCustomer(customer);
        redirectAttributes.addFlashAttribute("success",
                "Customer created successfully");

        return "redirect:/admin/customers/list";
    }


    @GetMapping("/list")
    public String listCustomers(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            Model model
    ) {

        Page<CustomerCreditDTO> customersPage =
                customerService.listCustomersWithTotalCredit(
                        search,
                        page,
                        size
                );

        model.addAttribute("customers", customersPage.getContent());

        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", customersPage.getTotalPages());

        model.addAttribute("search", search);

        return "customers/customer-list";
    }

    @PostMapping("/upload")
    public String uploadCustomers(@RequestParam("file") MultipartFile file,
                                  RedirectAttributes redirectAttributes) {

        try {

            customerService.uploadCustomersFromExcel(file);

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Customers uploaded successfully"
            );

        } catch (Exception e) {

            e.printStackTrace();

            redirectAttributes.addFlashAttribute(
                    "error",
                    "Failed to upload customers: " + e.getMessage()
            );
        }

        return "redirect:/admin/customers/list";
    }
}
