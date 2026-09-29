package ke.co.signature.MpesaIntegration;


import lombok.Data;

import java.math.BigDecimal;

@Data
public class StkPushRequestDto {
    private String phoneNumber;
    private BigDecimal amount;
    private String accountReference;
    private Long creditSale;
    private String customerCode;
    private Long accountReferenceId;
    private String transactionDesc;

}
