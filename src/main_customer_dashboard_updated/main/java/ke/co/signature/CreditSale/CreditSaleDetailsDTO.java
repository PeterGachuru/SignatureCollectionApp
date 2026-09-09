package ke.co.signature.CreditSale;

import ke.co.signature.Payment.PaymentSplit.PaymentSplitDTO;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

@Data
public class CreditSaleDetailsDTO {
    private Long id;

    private String customerName;
    private String customerCode;

    private BigDecimal amount;
    private BigDecimal invoiceAmount;
    private BigDecimal balance;

    private LocalDate saleDate;
    private String description;
    private String status;
    private String classificationName;

    private Date createdAt;
    private String createdBy;

    private List<PaymentSplitDTO> paymentSplits;
}