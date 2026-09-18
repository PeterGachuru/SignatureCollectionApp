package ke.co.signature.Payment;

/** Identifies who/what entered the payment into the collection workflow. */
public enum PaymentEntrySource {
    ADMIN,
    CUSTOMER,
    MPESA_CALLBACK
}
