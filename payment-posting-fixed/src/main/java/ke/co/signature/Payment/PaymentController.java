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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentRepository paymentRepository;

    private final PaymentInProgressRepository
            paymentInProgressRepository;

    private final CustomerRepository customerRepository;

    private final PaymentPostingService
            paymentPostingService;

    private final PaymentService paymentService;

    private final BankRepository bankRepository;

    private final PaymentCodeValidationService paymentCodeValidationService;

    private final DebtAgeingUploadRepository
            debtAgeingUploadRepository;

    private final DebtAgeingRecordRepository
            debtAgeingRecordRepository;


    @GetMapping
    public String listPayments(
            @RequestParam(defaultValue = "in-progress")
            String tab,

            @RequestParam(defaultValue = "")
            String search,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size,

            Model model) {

        /*
         * Prevent invalid page sizes.
         */
        if (size != 10 &&
                size != 20 &&
                size != 50 &&
                size != 100) {

            size = 20;
        }

        /*
         * Prevent negative page numbers.
         */
        if (page < 0) {
            page = 0;
        }

        Pageable pageable;

        if ("posted".equals(tab)) {

            pageable =
                    PageRequest.of(
                            page,
                            size,
                            Sort.by(
                                    Sort.Direction.DESC,
                                    "paymentDate"
                            )
                    );

            Page<PostedPayment> postedPayments;

            if (search == null ||
                    search.trim().isEmpty()) {

                postedPayments =
                        paymentRepository
                                .findAll(pageable);

            } else {

                String searchTerm =
                        search.trim();

                postedPayments =
                        paymentRepository
                                .findByCustomer_BusinessNameContainingIgnoreCaseOrReferenceContainingIgnoreCase(
                                        searchTerm,
                                        searchTerm,
                                        pageable
                                );
            }

            model.addAttribute(
                    "payments",
                    postedPayments
            );

        } else {

            /*
             * Default tab = IN PROGRESS
             */
            tab = "in-progress";

            pageable =
                    PageRequest.of(
                            page,
                            size,
                            Sort.by(
                                    Sort.Direction.DESC,
                                    "createdAt"
                            )
                    );

            Page<PaymentInProgress> inProgressPayments;

            if (search == null ||
                    search.trim().isEmpty()) {

                inProgressPayments =
                        paymentInProgressRepository
                                .findAllByOrderByCreatedAtDesc(
                                        pageable
                                );

            } else {

                String searchTerm =
                        search.trim();

                inProgressPayments =
                        paymentInProgressRepository
                                .findByCustomer_BusinessNameContainingIgnoreCaseOrReferenceContainingIgnoreCase(
                                        searchTerm,
                                        searchTerm,
                                        pageable
                                );
            }

            model.addAttribute(
                    "inProgressPayments",
                    inProgressPayments
            );
        }

        model.addAttribute(
                "activeTab",
                tab
        );

        model.addAttribute(
                "search",
                search
        );

        model.addAttribute(
                "size",
                size
        );

        return "payments/payment-list";
    }


    @GetMapping("/in-progress")
    public String listPaymentsInProgress(
            Model model) {

        model.addAttribute(
                "payments",
                paymentInProgressRepository
                        .findAll()
        );

        return "payments/payment-in-progress-list";
    }


    /**
     * Opens the full review page for a posted transaction.
     * Reversal is intentionally available only from this page, not the list.
     */
    @GetMapping("/view/{id}")
    public String viewPostedPayment(
            @PathVariable Long id,
            Model model) {

        PostedPayment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found."));

        List<ke.co.signature.Payment.PaymentSplit.PaymentSplit> splits = paymentPostingService.getSplits(id);
        model.addAttribute("payment", payment);
        model.addAttribute("splits", splits);
        model.addAttribute("legacyAllocation", splits.stream().anyMatch(split -> split.getBucket() == null));
        return "payments/payment-view";
    }

    /**
     * Reversal is deliberately hidden from the posted-payment list.
     * It is available after reviewing the transaction.
     */
    @PostMapping("/reverse/{id}")
    public String reversePayment(
            @PathVariable Long id,
            @RequestParam String reason,
            RedirectAttributes redirectAttributes) {

        try {
            paymentPostingService.reversePayment(id, reason);
            redirectAttributes.addFlashAttribute("success", "Payment reversed successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }

        return "redirect:/admin/payments/view/" + id;
    }

    @GetMapping("/new")
    public String newPayment(
            Model model) {

        PaymentInProgress pip =
                new PaymentInProgress();

        pip.setPaymentDate(
                LocalDate.now()
        );

        model.addAttribute(
                "paymentInProgress",
                pip
        );

        model.addAttribute(
                "customers",
                customerRepository.findAll()
        );

        model.addAttribute(
                "paymentModes",
                PaymentMode.values()
        );

        model.addAttribute(
                "banks",
                bankRepository.findByActiveTrueOrderByNameAsc()
        );

        model.addAttribute(
                "unallocatedAmount",
                BigDecimal.ZERO
        );

        return "payments/payment-form";
    }


    /**
     * Optional endpoint used by the payment form
     * to retrieve the customer's latest debt.
     */
    @GetMapping("/customer/{customerId}/debt")
    @ResponseBody
    public CustomerDebtResponse
    getCustomerDebt(
            @PathVariable Long customerId) {

        Customer customer =
                customerRepository.findById(
                        customerId
                ).orElseThrow(() ->
                        new RuntimeException(
                                "Customer not found."
                        )
                );


        DebtAgeingUpload latestUpload =
                debtAgeingUploadRepository
                        .findLatestUploadForCustomer(
                                customer
                        )
                        .orElse(null);


        if (latestUpload == null) {

            return new CustomerDebtResponse(
                    null,
                    BigDecimal.ZERO,
                    null
            );
        }


        DebtAgeingRecord record =
                debtAgeingRecordRepository
                        .findByUploadIdAndCustomer(
                                latestUpload.getId(),
                                customer
                        )
                        .orElse(null);


        if (record == null) {

            return new CustomerDebtResponse(
                    latestUpload.getId(),
                    BigDecimal.ZERO,
                    null
            );
        }


        return new CustomerDebtResponse(
                latestUpload.getId(),
                record.getTotalDebt(),
                record.getId()
        );
    }


    @PostMapping("/save")
    public String savePayment(
            @RequestParam Long customerId,

            @RequestParam BigDecimal amount,

            @RequestParam PaymentMode paymentMode,

            @RequestParam(required = false)
            String reference,

            @RequestParam(required = false)
            String phoneNumber,

            @RequestParam(required = false)
            Long bankId,

            @RequestParam(required = false)
            String chequeNumber,

            @RequestParam(required = false)
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate chequeDate,

            @RequestParam(required = false)
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate paymentDate,

            RedirectAttributes redirectAttributes) {


        try {

            Customer customer =
                    customerRepository.findById(
                            customerId
                    ).orElseThrow(() ->
                            new RuntimeException(
                                    "Customer not found."
                            )
                    );


            /*
             * Ensure the customer has an ageing record
             * in the latest upload.
             */
            DebtAgeingUpload latestUpload =
                    debtAgeingUploadRepository
                            .findLatestUploadForCustomer(
                                    customer
                            )
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "This customer does not " +
                                                    "have a debt ageing record " +
                                                    "in the latest upload."
                                    )
                            );


            DebtAgeingRecord record =
                    debtAgeingRecordRepository
                            .findByUploadIdAndCustomer(
                                    latestUpload.getId(),
                                    customer
                            )
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Debt ageing record not found " +
                                                    "for this customer."
                                    )
                            );


            if (amount == null ||
                    amount.compareTo(
                            BigDecimal.ZERO
                    ) <= 0) {

                throw new RuntimeException(
                        "Payment amount must be greater than zero."
                );
            }


            PaymentInProgress pip =
                    new PaymentInProgress();

            pip.setCustomer(customer);
            pip.setAmount(amount);
            pip.setPaymentMode(paymentMode);
            pip.setReference(reference);

            /*
             * Phone only for M-Pesa.
             */
            if (paymentMode == PaymentMode.MPESA) {

                pip.setPhoneNumber(
                        phoneNumber
                );

            } else {

                pip.setPhoneNumber(null);
            }


            /*
             * Cheque information.
             */
            if (paymentMode == PaymentMode.CHEQUE) {

                if (bankId == null) {

                    throw new RuntimeException(
                            "Please select a bank."
                    );
                }

                if (chequeNumber == null ||
                        chequeNumber.isBlank()) {

                    throw new RuntimeException(
                            "Cheque number is required."
                    );
                }

                if (chequeDate == null) {

                    throw new RuntimeException(
                            "Cheque date is required."
                    );
                }


                pip.setBank(
                        bankRepository.findById(
                                bankId
                        ).orElseThrow(() ->
                                new RuntimeException(
                                        "Bank not found."
                                )
                        )
                );

                pip.setChequeNumber(
                        chequeNumber.trim()
                );

                pip.setChequeDate(
                        chequeDate
                );

            } else {

                pip.setBank(null);
                pip.setChequeNumber(null);
                pip.setChequeDate(null);
            }


            pip.setPaymentDate(
                    paymentDate == null
                            ? LocalDate.now()
                            : paymentDate
            );

            paymentCodeValidationService.validateNewPaymentCode(
                    paymentMode,
                    reference,
                    chequeNumber
            );

            pip.setEntrySource(PaymentEntrySource.ADMIN);

            pip.setStatus(
                    PaymentStatus.READY_TO_POST
            );


            paymentInProgressRepository.save(
                    pip
            );


            redirectAttributes.addFlashAttribute(
                    "success",
                    "Payment captured and awaiting posting."
            );


        } catch (Exception ex) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    ex.getMessage()
            );
        }


        return "redirect:/admin/payments/in-progress";
    }


    @PostMapping("/post/{id}")
    public String postPayment(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        try {

            paymentPostingService.postPayment(id);

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Payment posted successfully."
            );

        } catch (Exception ex) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    ex.getMessage()
            );
        }

        return "redirect:/admin/payments?tab=in-progress&page=0";
    }

    public record CustomerDebtResponse(
            Long uploadId,
            BigDecimal totalDebt,
            Long debtAgeingRecordId
    ) {
    }
}