package ke.co.signature.Payment;

import ke.co.signature.MpesaIntegration.MpesaTransactionRepository;
import ke.co.signature.Payment.PaymentInProgress.PaymentInProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentCodeValidationService {

    private final PaymentRepository paymentRepository;
    private final PaymentInProgressRepository paymentInProgressRepository;
    private final MpesaTransactionRepository mpesaTransactionRepository;

    @Transactional(readOnly = true)
    public void validateNewPaymentCode(PaymentMode mode, String reference, String chequeNumber) {
        String normalizedReference = normalize(reference);
        String normalizedCheque = normalize(chequeNumber);

        if (mode == PaymentMode.CHEQUE) {
            if (normalizedCheque == null) {
                throw new IllegalArgumentException("Cheque number is required.");
            }

            if (paymentRepository.existsByChequeNumberIgnoreCase(normalizedCheque)
                    || paymentInProgressRepository.existsByChequeNumberIgnoreCase(normalizedCheque)) {
                throw new IllegalArgumentException(
                        "Cheque number '" + normalizedCheque + "' has already been entered and cannot be reused."
                );
            }
            return;
        }

        if (mode == PaymentMode.MPESA || mode == PaymentMode.BANK_TRANSFER) {
            if (normalizedReference == null) {
                throw new IllegalArgumentException(
                        mode == PaymentMode.MPESA
                                ? "M-Pesa transaction code/reference is required."
                                : "Bank transaction reference is required."
                );
            }

            if (paymentRepository.existsByReferenceIgnoreCase(normalizedReference)
                    || paymentInProgressRepository.existsByReferenceIgnoreCase(normalizedReference)
                    || mpesaTransactionRepository.existsByMpesaReceiptNumberIgnoreCase(normalizedReference)) {
                throw new IllegalArgumentException(
                        "Payment code/reference '" + normalizedReference + "' has already been entered and cannot be reused."
                );
            }
        }
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
