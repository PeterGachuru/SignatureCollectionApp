package ke.co.signature.Payment;

import ke.co.signature.CreditSale.CreditSale;
import ke.co.signature.CreditSale.CreditSaleRepository;
import ke.co.signature.Customer.Customer;
import ke.co.signature.Customer.CustomerRepository;
import ke.co.signature.Payment.PaymentInProgress.PaymentInProgress;
import ke.co.signature.Payment.PaymentInProgress.PaymentInProgressRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/admin/payments")
public class PaymentController {

    private final PaymentRepository paymentRepository;
    private final PaymentInProgressRepository paymentInProgressRepository;
    private final CustomerRepository customerRepository;
    private final CreditSaleRepository creditSaleRepository;
    private final PaymentPostingService paymentPostingService;
    private final PaymentService paymentService;

    public PaymentController(
            PaymentRepository paymentRepository,
            PaymentInProgressRepository paymentInProgressRepository,
            CustomerRepository customerRepository,
            CreditSaleRepository creditSaleRepository,
            PaymentPostingService paymentPostingService,
            PaymentService paymentService) {

        this.paymentRepository = paymentRepository;
        this.paymentInProgressRepository = paymentInProgressRepository;
        this.customerRepository = customerRepository;
        this.creditSaleRepository = creditSaleRepository;
        this.paymentPostingService = paymentPostingService;
        this.paymentService = paymentService;
    }

    // ===============================
    // ✅ LIST POSTED PAYMENTS
    // ===============================
    @GetMapping
    public String listPayments(Model model) {
        List<PaymentInProgress> inProgressPayments =
                paymentInProgressRepository.findAllByOrderByCreatedAtDesc();

        List<Payment> postedPayments =
                paymentRepository.findAllByOrderByPaymentDateDesc();

        model.addAttribute("inProgressPayments", inProgressPayments);
        model.addAttribute("payments", postedPayments);

        return "payments/payment-list";
    }

    // ===============================
    // ⏳ LIST PAYMENTS IN PROGRESS
    // ===============================
    @GetMapping("/in-progress")
    public String listPaymentsInProgress(Model model) {
        model.addAttribute(
                "payments",
                paymentInProgressRepository.findAll()
        );
        return "payments/payment-in-progress-list";
    }

    // ===============================
    // ➕ NEW PAYMENT (IN PROGRESS)
    // ===============================
    @GetMapping("/new")
    public String newPayment(
            @RequestParam(required = false) Long creditSaleId,
            Model model) {

        PaymentInProgress pip = new PaymentInProgress();
        pip.setPaymentDate(LocalDate.now());

        BigDecimal unallocatedAmount = BigDecimal.ZERO;

        if (creditSaleId != null) {
            CreditSale creditSale = creditSaleRepository.findById(creditSaleId)
                    .orElseThrow(() -> new RuntimeException("Credit sale not found"));

            pip.setCreditSale(creditSale);
            pip.setCustomer(creditSale.getCustomer());
            pip.setAmount(creditSale.getBalance()); // default suggestion

            // 🔥 Fetch unallocated funds
            unallocatedAmount = paymentService
                    .getCustomerUnallocatedAmount(creditSale.getCustomer());
        }

        model.addAttribute("paymentInProgress", pip);
        model.addAttribute("unallocatedAmount", unallocatedAmount);

        model.addAttribute("customers", customerRepository.findAll());
        model.addAttribute("creditSales", creditSaleRepository.findAll());
        model.addAttribute("paymentModes", PaymentMode.values());

        return "payments/payment-form";
    }



    // ===============================
    // 💾 SAVE PAYMENT (IN PROGRESS)
    // ===============================
    @PostMapping("/save")
    public String savePayment(
            @RequestParam Long customerId,
            @RequestParam BigDecimal amount,
            @RequestParam PaymentMode paymentMode,
            @RequestParam(required = false) String reference,
            @RequestParam(required = false) String phoneNumber,
            @RequestParam(required = false) Long creditSaleId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate paymentDate,
            RedirectAttributes redirectAttributes) {

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        CreditSale creditSale = null;
        if (creditSaleId != null) {
            creditSale = creditSaleRepository.findById(creditSaleId)
                    .orElseThrow(() -> new RuntimeException("Credit sale not found"));
        }

        PaymentInProgress pip = new PaymentInProgress();
        pip.setCustomer(customer);
        pip.setCreditSale(creditSale);
        pip.setAmount(amount);
        pip.setPaymentMode(paymentMode);
        pip.setReference(reference);
        pip.setPhoneNumber(phoneNumber);
        pip.setPaymentDate(paymentDate);
        pip.setStatus(PaymentStatus.READY_TO_POST);
        // manual payments are immediately postable

        System.out.println(pip);

        paymentInProgressRepository.save(pip);

        redirectAttributes.addFlashAttribute(
                "success",
                "Payment captured and awaiting posting"
        );

        return "redirect:/admin/payments/in-progress";
    }

    // ===============================
    // ✅ POST (FINALIZE) PAYMENT
    // ===============================
    @PostMapping("/post/{id}")
    public String postPayment(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        paymentPostingService.postPayment(id);

        redirectAttributes.addFlashAttribute(
                "success",
                "Payment posted successfully"
        );

        return "redirect:/admin/payments";
    }
}
