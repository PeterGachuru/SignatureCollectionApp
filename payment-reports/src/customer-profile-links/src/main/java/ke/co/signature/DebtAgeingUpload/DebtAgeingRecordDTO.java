package ke.co.signature.DebtAgeingUpload;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DebtAgeingRecordDTO {

    private Long id;

    private Long customerId;

    private String customerCode;

    private String customerName;

    private String town;

    private String unit;

    private BigDecimal currentAmount = BigDecimal.ZERO;

    private BigDecimal days30 = BigDecimal.ZERO;

    private BigDecimal days60 = BigDecimal.ZERO;

    private BigDecimal days90 = BigDecimal.ZERO;

    private BigDecimal days120 = BigDecimal.ZERO;

    private BigDecimal over120 = BigDecimal.ZERO;

    private BigDecimal totalDebt = BigDecimal.ZERO;

    private String remarks;
}