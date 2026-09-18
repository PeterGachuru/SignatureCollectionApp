package ke.co.signature.Payment;

import ke.co.signature.Configs.Bank.BankRepository;
import ke.co.signature.Customer.Customer;
import ke.co.signature.Customer.CustomerRepository;
import ke.co.signature.DebtAgeingUpload.DebtAgeingRecord;
import ke.co.signature.DebtAgeingUpload.DebtAgeingRecordRepository;
import ke.co.signature.DebtAgeingUpload.DebtAgeingUpload;
import ke.co.signature.DebtAgeingUpload.DebtAgeingUploadRepository;
import ke.co.signature.Payment.PaymentInProgress.PaymentInProgress;
import ke.co.signature.Payment.PaymentInProgress.PaymentInProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/customer/payments")
@RequiredArgsConstructor
public class CustomerPaymentController {

    private final CustomerRepository customerRepository;
    private final PaymentInProgressRepository paymentInProgressRepository;
    private final BankRepository bankRepository;
    private final DebtAgeingUploadRepository debtAgeingUploadRepository;
    private final DebtAgeingRecordRepository debtAgeingRecordRepository;

    @GetMapping
    public String list(Principal principal, Model model) {
        Customer customer = currentCustomer(principal);
        model.addAttribute("customer", customer);
        model.addAttribute("payments", paymentInProgressRepository
                .findByCustomer_UsernameOrderByCreatedAtDesc(customer.getUsername()));
        return "customer/payments";
    }

    @GetMapping("/new")
    public String form(Principal principal, Model model) {
        Customer customer = currentCustomer(principal);
        PaymentInProgress payment = new PaymentInProgress();
        payment.setCustomer(customer);
        payment.setPaymentDate(LocalDate.now());
        payment.setEnteredByCustomer(true);

        model.addAttribute("paymentInProgress", payment);
        model.addAttribute("customer", customer);
        model.addAttribute("paymentModes", PaymentMode.values());
        model.addAttribute("banks", bankRepository.findByActiveTrueOrderByNameAsc());
        return "customer/payment-form";
    }

    @PostMapping("/save")
    public String save(Principal principal,
                       @RequestParam BigDecimal amount,
                       @RequestParam PaymentMode paymentMode,
                       @RequestParam(required = false) String reference,
                       @RequestParam(required = false) String phoneNumber,
                       @RequestParam(required = false) Long bankId,
                       @RequestParam(required = false) String chequeNumber,
                       @RequestParam(required = false)
                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate chequeDate,
                       @RequestParam(required = false)
                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate paymentDate,
                       RedirectAttributes redirectAttributes) {
        try {
            Customer customer = currentCustomer(principal);

            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Payment amount must be greater than zero.");
            }

            if (paymentMode == null) {
                throw new IllegalArgumentException("Payment mode is required.");
            }

            // Keep the same business rule as the admin capture process:
            // the customer must have a current ageing record.
            DebtAgeingUpload latestUpload = debtAgeingUploadRepository
                    .findLatestUploadForCustomer(customer)
                    .orElseThrow(() -> new IllegalStateException(
                            "This customer does not have a debt ageing record in the latest upload."));

            DebtAgeingRecord record = debtAgeingRecordRepository
                    .findByUploadIdAndCustomer(latestUpload.getId(), customer)
                    .orElseThrow(() -> new IllegalStateException(
                            "Debt ageing record not found for this customer."));

            PaymentInProgress pip = new PaymentInProgress();
            pip.setCustomer(customer);
            pip.setAmount(amount);
            pip.setPaymentMode(paymentMode);
            pip.setReference(reference);
            pip.setEnteredByCustomer(true);
            pip.setPaymentDate(paymentDate == null ? LocalDate.now() : paymentDate);
            pip.setStatus(PaymentStatus.READY_TO_POST);

            if (paymentMode == PaymentMode.MPESA) {
                pip.setPhoneNumber(phoneNumber);
            } else {
                pip.setPhoneNumber(null);
            }

            if (paymentMode == PaymentMode.CHEQUE) {
                if (bankId == null) throw new IllegalArgumentException("Please select a bank.");
                if (chequeNumber == null || chequeNumber.isBlank()) {
                    throw new IllegalArgumentException("Cheque number is required.");
                }
                if (chequeDate == null) {
                    throw new IllegalArgumentException("Cheque date is required.");
                }
                pip.setBank(bankRepository.findById(bankId)
                        .orElseThrow(() -> new IllegalArgumentException("Bank not found.")));
                pip.setChequeNumber(chequeNumber.trim());
                pip.setChequeDate(chequeDate);
            } else {
                pip.setBank(null);
                pip.setChequeNumber(null);
                pip.setChequeDate(null);
            }

            paymentInProgressRepository.save(pip);

            redirectAttributes.addFlashAttribute("success",
                    "Payment captured successfully and is awaiting admin posting.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/customer/payments";
    }

    private Customer currentCustomer(Principal principal) {
        Customer customer = customerRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new AccessDeniedException("Customer account not found."));
        return customer;
    }
}
