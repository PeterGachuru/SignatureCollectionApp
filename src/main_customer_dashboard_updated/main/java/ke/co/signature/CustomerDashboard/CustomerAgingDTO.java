package ke.co.signature.CustomerDashboard;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CustomerAgingDTO {

    private Long uploadId;
    private String fileName;
    private String reportDate;

    private BigDecimal currentAmount;
    private BigDecimal days30;
    private BigDecimal days60;
    private BigDecimal days90;
    private BigDecimal days120;
    private BigDecimal over120;
    private BigDecimal totalDebt;

    private double currentPercentage;
    private double days30Percentage;
    private double days60Percentage;
    private double days90Percentage;
    private double days120Percentage;
    private double over120Percentage;
}
