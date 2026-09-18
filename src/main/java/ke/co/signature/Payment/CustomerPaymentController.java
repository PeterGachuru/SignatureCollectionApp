package ke.co.signature.Payment;

import ke.co.signature.Configs.Bank.BankRepository;
import ke.co.signature.Customer.Customer;
import ke.co.signature.Customer.CustomerRepository;
import ke.co.signature.DebtAgeingUpload.DebtAgeingRecordRepository;
import ke.co.signature.DebtAgeingUpload.DebtAgeingUpload;
import ke.co.signature.DebtAgeingUpload.DebtAgeingUploadRepository;
import ke.co.signature.Payment.PaymentInProgress.PaymentInProgress;
import ke.co.signature.Payment.PaymentInProgress.PaymentInProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;

@Controller
@RequestMapping("/customer/payments")
@RequiredArgsConstructor
public class CustomerPaymentController {
    private final CustomerRepository customerRepository;
    private final PaymentInProgressRepository paymentInProgressRepository;
    private final BankRepository bankRepository;
    private final DebtAgeingUploadRepository debtAgeingUploadRepository;
    private final DebtAgeingRecordRepository debtAgeingRecordRepository;
    private final PaymentRepository paymentRepository;

    @GetMapping
    public String list(Principal principal, Model model) {
        Customer customer = currentCustomer(principal);
        model.addAttribute("customer", customer);
        model.addAttribute("postedPayments", paymentRepository.findByCustomer_UsernameOrderByPaymentDateDesc(customer.getUsername()));
        model.addAttribute("pendingPayments", paymentInProgressRepository.findByCustomer_UsernameOrderByCreatedAtDesc(customer.getUsername()));
        return "customer/payments";
    }

    @GetMapping("/new")
    public String form(Principal principal, Model model) {
        Customer customer = currentCustomer(principal);
        PaymentInProgress payment = new PaymentInProgress();
        payment.setCustomer(customer);
        payment.setPaymentDate(LocalDate.now());
        model.addAttribute("paymentInProgress", payment);
        model.addAttribute("paymentModes", PaymentMode.values());
        model.addAttribute("banks", bankRepository.findByActiveTrueOrderByNameAsc());
        return "customer/payment-form";
    }

    @PostMapping("/save")
    public String save(Principal principal,
                       @RequestParam BigDecimal amount,
                       @RequestParam PaymentMode paymentMode,
                       @RequestParam(required=false) String reference,
                       @RequestParam(required=false) String phoneNumber,
                       @RequestParam(required=false) Long bankId,
                       @RequestParam(required=false) String chequeNumber,
                       @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate chequeDate,
                       @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate paymentDate,
                       RedirectAttributes redirectAttributes) {
        try {
            Customer customer = currentCustomer(principal);
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("Payment amount must be greater than zero.");
            if (paymentMode == null) throw new IllegalArgumentException("Payment mode is required.");
            if (paymentDate == null) paymentDate = LocalDate.now();

            DebtAgeingUpload latestUpload = debtAgeingUploadRepository.findLatestUploadForCustomer(customer)
                    .orElseThrow(() -> new IllegalStateException("This customer does not have a debt ageing record in the latest upload."));
            debtAgeingRecordRepository.findByUploadIdAndCustomer(latestUpload.getId(), customer)
                    .orElseThrow(() -> new IllegalStateException("Debt ageing record not found for this customer."));

            PaymentInProgress pip = new PaymentInProgress();
            pip.setCustomer(customer);
            pip.setAmount(amount);
            pip.setPaymentMode(paymentMode);
            pip.setReference(reference == null ? null : reference.trim());
            pip.setPaymentDate(paymentDate);
            pip.setEntrySource(PaymentEntrySource.CUSTOMER);
            pip.setStatus(PaymentStatus.READY_TO_POST);

            if (paymentMode == PaymentMode.MPESA) {
                if (phoneNumber == null || phoneNumber.isBlank()) throw new IllegalArgumentException("M-Pesa phone number is required.");
                if (reference == null || reference.isBlank()) throw new IllegalArgumentException("M-Pesa transaction reference is required.");
                pip.setPhoneNumber(phoneNumber.trim());
            } else {
                pip.setPhoneNumber(null);
            }

            if (paymentMode == PaymentMode.CHEQUE) {
                if (bankId == null) throw new IllegalArgumentException("Please select a bank.");
                if (chequeNumber == null || chequeNumber.isBlank()) throw new IllegalArgumentException("Cheque number is required.");
                if (chequeDate == null) throw new IllegalArgumentException("Cheque date is required.");
                pip.setBank(bankRepository.findById(bankId).orElseThrow(() -> new IllegalArgumentException("Bank not found.")));
                pip.setChequeNumber(chequeNumber.trim());
                pip.setChequeDate(chequeDate);
            }

            paymentInProgressRepository.save(pip);
            redirectAttributes.addFlashAttribute("success", "Payment captured successfully and is awaiting administrator posting.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/customer/payments";
    }

    private Customer currentCustomer(Principal principal) {
        return customerRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalStateException("Customer profile not found."));
    }
}
