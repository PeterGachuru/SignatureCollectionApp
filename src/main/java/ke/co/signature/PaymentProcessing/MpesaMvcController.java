package ke.co.signature.PaymentProcessing;

import ke.co.signature.Customer.Customer;
import ke.co.signature.Customer.CustomerService;
import ke.co.signature.MpesaIntegration.MpesaService;
import ke.co.signature.MpesaIntegration.StkPushRequestDto;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequestMapping("/admin/mpesa")
public class MpesaMvcController {

    private final MpesaService mpesaService;
    private final CustomerService customerService;

    public MpesaMvcController(MpesaService mpesaService,
                              CustomerService customerService) {
        this.mpesaService = mpesaService;
        this.customerService = customerService;
    }



    /**
     * Load MPESA Push Page
     */
    @GetMapping("/initiate/{customerId}")
    public String initiate(@PathVariable Long customerId, Model model) {

        Customer customer = customerService.findById(customerId);
        BigDecimal outstanding = customerService.getTotalOutstandingCredit(customerId);

        StkPushRequestDto dto = new StkPushRequestDto();
        dto.setPhoneNumber(customer.getPhone());
        dto.setAmount(outstanding);
        dto.setAccountReference("CREDIT_PAYMENT");
        dto.setAccountReferenceId(customerId);
        dto.setTransactionDesc("Credit repayment - " + customer.getBusinessName());

        model.addAttribute("stk", dto);
        model.addAttribute("customerName", customer.getBusinessName());

        return "mpesa/mpesa-initiate";
    }

    /**
     * Submit MPESA Push
     */
    @PostMapping("/submit")
    public String submit(@ModelAttribute("stk") StkPushRequestDto request,
                         RedirectAttributes redirectAttributes) {
        System.out.println("==============================Submitted skt request=======================");
        try {
            mpesaService.initiateStkPush(
                    request.getPhoneNumber(),
                    request.getAmount(),
                    request.getAccountReference(),
                    request.getAccountReferenceId(),
                    request.getTransactionDesc()
            );

            redirectAttributes.addFlashAttribute(
                    "success",
                    "MPESA prompt sent successfully."
            );

            System.out.println("==================MPESA prompt sent successfully.=================================");

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Failed to send MPESA prompt: " + e.getMessage()
            );
            System.out.println("========================Failed to send MPESA prompt=====================================");
        }

        return "redirect:/admin/customers/list";
    }
}
