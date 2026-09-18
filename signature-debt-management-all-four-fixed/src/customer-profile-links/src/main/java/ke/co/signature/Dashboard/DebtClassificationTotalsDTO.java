package ke.co.signature.Dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class DebtClassificationTotalsDTO {

    private BigDecimal current;

    private BigDecimal days30;

    private BigDecimal days60;

    private BigDecimal days90;

    private BigDecimal days120;

    private BigDecimal over120;
}