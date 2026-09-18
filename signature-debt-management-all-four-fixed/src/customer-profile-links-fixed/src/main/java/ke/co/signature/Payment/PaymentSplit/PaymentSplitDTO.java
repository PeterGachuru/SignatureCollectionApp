package ke.co.signature.Payment.PaymentSplit;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PaymentSplitDTO {
    private Long id;

    private BigDecimal amountApplied;

    private Long paymentId;
    private BigDecimal paymentAmount;
    private String paymentReference;
    private String phoneNumber;
    private String paymentMode;
    private LocalDate paymentDate;
}
