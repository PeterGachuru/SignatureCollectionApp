package ke.co.signature.Payment;

import ke.co.signature.Configs.Bank.Bank;
import ke.co.signature.Customer.Customer;
import ke.co.signature.Customer.CustomerRepository;
import ke.co.signature.DebtAgeingUpload.DebtAgeingRecord;
import ke.co.signature.DebtAgeingUpload.DebtAgeingRecordRepository;
import ke.co.signature.DebtAgeingUpload.DebtAgeingUpload;
import ke.co.signature.DebtAgeingUpload.DebtAgeingUploadRepository;
import ke.co.signature.MpesaIntegration.MpesaTransaction;
import ke.co.signature.Payment.PaymentInProgress.PaymentInProgress;
import ke.co.signature.Payment.PaymentInProgress.PaymentInProgressRepository;
import ke.co.signature.Payment.PaymentSplit.PaymentSplit;
import ke.co.signature.Payment.PaymentSplit.PaymentSplitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import ke.co.signature.Auth.User.User;
import ke.co.signature.Utils.SecurityUtils;

@Service
@RequiredArgsConstructor
public class PaymentPostingService {

    private final PaymentRepository paymentRepository;
    private final PaymentInProgressRepository inProgressRepository;

    private final PaymentSplitRepository paymentSplitRepository;

    private final CustomerRepository customerRepository;

    private final DebtAgeingUploadRepository debtAgeingUploadRepository;
    private final DebtAgeingRecordRepository debtAgeingRecordRepository;


    /**
     * Posts a manually captured payment.
     *
     * Payment is allocated against the customer's latest
     * DebtAgeingRecord, starting with the oldest debt bucket.
     */
    @Transactional
    public void postPayment(Long paymentInProgressId) {

        PaymentInProgress pip =
                inProgressRepository.findById(paymentInProgressId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Payment not found."
                                ));

        if (pip.getStatus() != PaymentStatus.READY_TO_POST) {

            throw new IllegalStateException(
                    "Payment is not ready to post."
            );
        }

        validatePayment(pip);

        Customer customer = pip.getCustomer();

        /*
         * Find the latest debt ageing upload containing
         * this customer.
         */
        DebtAgeingUpload latestUpload =
                debtAgeingUploadRepository
                        .findLatestUploadForCustomer(customer)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "No debt ageing upload exists " +
                                                "for customer " +
                                                customer.getBusinessName()
                                )
                        );

        /*
         * A customer has exactly one record in an upload.
         */
        DebtAgeingRecord ageingRecord =
                debtAgeingRecordRepository
                        .findByUploadIdAndCustomer(
                                latestUpload.getId(),
                                customer
                        )
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "No debt ageing record found " +
                                                "for customer in the latest upload."
                                )
                        );

        /*
         * Create the posted payment.
         */
        PostedPayment postedPayment =
                new PostedPayment();

        postedPayment.setCustomer(customer);
        postedPayment.setAmount(pip.getAmount());
        postedPayment.setReference(pip.getReference());
        postedPayment.setPhoneNumber(pip.getPhoneNumber());
        postedPayment.setPaymentMode(pip.getPaymentMode());
        postedPayment.setPaymentDate(pip.getPaymentDate());
        postedPayment.setEntrySource(pip.getEntrySource());

        postedPayment.setBank(pip.getBank());
        postedPayment.setChequeNumber(pip.getChequeNumber());
        postedPayment.setChequeDate(pip.getChequeDate());

        postedPayment.setUnallocatedAmount(
                BigDecimal.ZERO
        );

        postedPayment =
                paymentRepository.saveAndFlush(postedPayment);


        /*
         * Allocate the payment against the ageing buckets.
         *
         * OLDEST FIRST:
         *
         * 1. Over 120 days
         * 2. 120 days
         * 3. 90 days
         * 4. 60 days
         * 5. 30 days
         * 6. Current
         */
        BigDecimal remaining =
                pip.getAmount();


        remaining = applyToBucket(
                postedPayment,
                ageingRecord,
                remaining,
                DebtBucket.OVER_120
        );

        remaining = applyToBucket(
                postedPayment,
                ageingRecord,
                remaining,
                DebtBucket.DAYS_120
        );

        remaining = applyToBucket(
                postedPayment,
                ageingRecord,
                remaining,
                DebtBucket.DAYS_90
        );

        remaining = applyToBucket(
                postedPayment,
                ageingRecord,
                remaining,
                DebtBucket.DAYS_60
        );

        remaining = applyToBucket(
                postedPayment,
                ageingRecord,
                remaining,
                DebtBucket.DAYS_30
        );

        remaining = applyToBucket(
                postedPayment,
                ageingRecord,
                remaining,
                DebtBucket.CURRENT
        );


        /*
         * Anything left after clearing all debt
         * becomes unallocated.
         */
        postedPayment.setUnallocatedAmount(remaining);

        paymentRepository.saveAndFlush(postedPayment);


        /*
         * Update the upload total.
         *
         * The amount actually cleared can never exceed
         * the original debt.
         */
        BigDecimal amountCleared =
                pip.getAmount().subtract(remaining);

        BigDecimal uploadTotal =
                latestUpload.getTotalDebt() == null
                        ? BigDecimal.ZERO
                        : latestUpload.getTotalDebt();

        latestUpload.setTotalDebt(
                uploadTotal.subtract(amountCleared)
                        .max(BigDecimal.ZERO)
        );

        debtAgeingUploadRepository.saveAndFlush(latestUpload);


        /*
         * Payment has now been posted.
         * Flush the delete so a database constraint/error is surfaced
         * inside this transaction instead of appearing as a silent redirect.
         */
        inProgressRepository.delete(pip);
        inProgressRepository.flush();
    }


    /**
     * Reverses a posted payment without deleting the audit record.
     * The original allocation splits are restored to their original buckets
     * when bucket metadata is available.
     */
    @Transactional
    public void reversePayment(Long paymentId, String reason) {

        PostedPayment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Posted payment not found."));

        if (payment.getStatus() == PostedPaymentStatus.REVERSED) {
            throw new IllegalStateException("This payment has already been reversed.");
        }

        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A reversal reason is required.");
        }

        List<PaymentSplit> splits = paymentSplitRepository.findByPostedPaymentId(paymentId);
        if (splits.isEmpty()
                && safe(payment.getAmount()).compareTo(BigDecimal.ZERO) > 0
                && safe(payment.getUnallocatedAmount()).compareTo(safe(payment.getAmount())) != 0) {
            throw new IllegalStateException("This payment has no allocation history and cannot be safely reversed.");
        }

        BigDecimal allocated = BigDecimal.ZERO;
        boolean legacyAllocation = false;
        for (PaymentSplit split : splits) {
            BigDecimal applied = safe(split.getAmountApplied());
            allocated = allocated.add(applied);

            DebtAgeingRecord record = split.getDebtAgeingRecord();
            if (split.getBucket() == null) {
                // Legacy postings did not persist the bucket used for each split.
                // Restore the balance to Current so the debt totals remain internally consistent.
                legacyAllocation = true;
                BigDecimal current = safe(record.getCurrentAmount());
                record.setCurrentAmount(current.add(applied));
            } else {
                BigDecimal bucketAmount = getBucketAmount(record, split.getBucket());
                setBucketAmount(record, split.getBucket(), safe(bucketAmount).add(applied));
            }
            record.setTotalDebt(safe(record.getTotalDebt()).add(applied));
            debtAgeingRecordRepository.save(record);

            DebtAgeingUpload upload = record.getUpload();
            if (upload != null) {
                upload.setTotalDebt(safe(upload.getTotalDebt()).add(applied));
                debtAgeingUploadRepository.save(upload);
            }
        }

        BigDecimal balanceImpact = allocated;
        payment.setStatus(PostedPaymentStatus.REVERSED);
        payment.setReversalReason(reason.trim());
        payment.setReversedAt(LocalDateTime.now());
        User currentUser = SecurityUtils.getCurrentUser();
        payment.setReversedBy(currentUser);
        payment.setReversalBalanceImpact(balanceImpact);
        if (legacyAllocation) {
            payment.setReversalReason(reason.trim() + " [Legacy allocation restored to Current bucket]");
        }
        paymentRepository.save(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentSplit> getSplits(Long paymentId) {
        return paymentSplitRepository.findByPostedPaymentId(paymentId);
    }

    /**
     * Applies a payment to one ageing bucket.
     */
    private BigDecimal applyToBucket(
            PostedPayment postedPayment,
            DebtAgeingRecord record,
            BigDecimal remaining,
            DebtBucket bucket) {

        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal bucketAmount =
                getBucketAmount(record, bucket);

        if (bucketAmount == null ||
                bucketAmount.compareTo(BigDecimal.ZERO) <= 0) {

            return remaining;
        }

        BigDecimal applied =
                remaining.min(bucketAmount);

        /*
         * Reduce the selected ageing bucket.
         */
        setBucketAmount(
                record,
                bucket,
                bucketAmount.subtract(applied)
        );


        /*
         * Reduce total debt.
         */
        BigDecimal totalDebt =
                record.getTotalDebt() == null
                        ? BigDecimal.ZERO
                        : record.getTotalDebt();

        record.setTotalDebt(
                totalDebt.subtract(applied)
                        .max(BigDecimal.ZERO)
        );


        debtAgeingRecordRepository.save(record);


        /*
         * Create payment allocation record.
         */
        PaymentSplit split =
                new PaymentSplit();

        split.setPostedPayment(postedPayment);
        split.setDebtAgeingRecord(record);
        split.setAmountApplied(applied);
        split.setBucket(bucket);

        paymentSplitRepository.save(split);


        return remaining.subtract(applied);
    }


    private BigDecimal getBucketAmount(
            DebtAgeingRecord record,
            DebtBucket bucket) {

        return switch (bucket) {

            case OVER_120 ->
                    safe(record.getOver120());

            case DAYS_120 ->
                    safe(record.getDays120());

            case DAYS_90 ->
                    safe(record.getDays90());

            case DAYS_60 ->
                    safe(record.getDays60());

            case DAYS_30 ->
                    safe(record.getDays30());

            case CURRENT ->
                    safe(record.getCurrentAmount());
        };
    }


    private void setBucketAmount(
            DebtAgeingRecord record,
            DebtBucket bucket,
            BigDecimal amount) {

        amount = safe(amount);

        switch (bucket) {

            case OVER_120 ->
                    record.setOver120(amount);

            case DAYS_120 ->
                    record.setDays120(amount);

            case DAYS_90 ->
                    record.setDays90(amount);

            case DAYS_60 ->
                    record.setDays60(amount);

            case DAYS_30 ->
                    record.setDays30(amount);

            case CURRENT ->
                    record.setCurrentAmount(amount);
        }
    }


    private BigDecimal safe(BigDecimal value) {

        return value == null
                ? BigDecimal.ZERO
                : value;
    }


    private void validatePayment(
            PaymentInProgress payment) {

        if (payment.getAmount() == null ||
                payment.getAmount()
                        .compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "Payment amount must be greater than zero."
            );
        }

        if (payment.getPaymentMode() == null) {

            throw new IllegalArgumentException(
                    "Payment mode is required."
            );
        }

        /*
         * Cheque-specific validation.
         */
        if (payment.getPaymentMode() == PaymentMode.CHEQUE) {

            if (payment.getBank() == null) {

                throw new IllegalArgumentException(
                        "Bank is required for cheque payments."
                );
            }

            if (payment.getChequeNumber() == null ||
                    payment.getChequeNumber().isBlank()) {

                throw new IllegalArgumentException(
                        "Cheque number is required."
                );
            }

            if (payment.getChequeDate() == null) {
                throw new IllegalStateException(
                        "Cheque date is required."
                );
            }

            LocalDate today = LocalDate.now();

            if (payment.getChequeDate().isAfter(today)) {

                throw new IllegalStateException(
                        "This cheque cannot be posted yet. " +
                                "The cheque date is " +
                                payment.getChequeDate() +
                                ", while today is " +
                                today +
                                "."
                );
            }


            if (payment.getChequeDate() == null) {

                throw new IllegalArgumentException(
                        "Cheque date is required."
                );
            }
        }
    }


    /**
     * M-Pesa callback posting.
     */
    @Transactional
    public void postMpesaPayment(
            MpesaTransaction tx) {

        Customer customer =
                customerRepository
                        .findByCustomerCode(
                                tx.getCustomerCode()
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Customer not found."
                                ));


        DebtAgeingUpload latestUpload =
                debtAgeingUploadRepository
                        .findLatestUploadForCustomer(customer)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "No debt ageing upload exists " +
                                                "for customer."
                                ));


        DebtAgeingRecord ageingRecord =
                debtAgeingRecordRepository
                        .findByUploadIdAndCustomer(
                                latestUpload.getId(),
                                customer
                        )
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "No debt ageing record found."
                                ));


        PostedPayment postedPayment =
                new PostedPayment();

        postedPayment.setCustomer(customer);
        postedPayment.setAmount(tx.getAmount());
        postedPayment.setReference(
                tx.getMpesaReceiptNumber()
        );
        postedPayment.setPhoneNumber(
                tx.getPhoneNumber()
        );
        postedPayment.setPaymentMode(
                PaymentMode.MPESA
        );
        postedPayment.setPaymentDate(
                LocalDate.now()
        );
        postedPayment.setEntrySource(PaymentEntrySource.MPESA_CALLBACK);
        postedPayment.setUnallocatedAmount(
                BigDecimal.ZERO
        );

        postedPayment =
                paymentRepository.save(postedPayment);


        BigDecimal remaining =
                tx.getAmount();


        remaining = applyToBucket(
                postedPayment,
                ageingRecord,
                remaining,
                DebtBucket.OVER_120
        );

        remaining = applyToBucket(
                postedPayment,
                ageingRecord,
                remaining,
                DebtBucket.DAYS_120
        );

        remaining = applyToBucket(
                postedPayment,
                ageingRecord,
                remaining,
                DebtBucket.DAYS_90
        );

        remaining = applyToBucket(
                postedPayment,
                ageingRecord,
                remaining,
                DebtBucket.DAYS_60
        );

        remaining = applyToBucket(
                postedPayment,
                ageingRecord,
                remaining,
                DebtBucket.DAYS_30
        );

        remaining = applyToBucket(
                postedPayment,
                ageingRecord,
                remaining,
                DebtBucket.CURRENT
        );


        postedPayment.setUnallocatedAmount(
                remaining
        );

        paymentRepository.save(postedPayment);


        BigDecimal amountCleared =
                tx.getAmount().subtract(remaining);

        BigDecimal uploadTotal =
                safe(latestUpload.getTotalDebt());

        latestUpload.setTotalDebt(
                uploadTotal.subtract(amountCleared)
                        .max(BigDecimal.ZERO)
        );

        debtAgeingUploadRepository.save(
                latestUpload
        );
    }


}