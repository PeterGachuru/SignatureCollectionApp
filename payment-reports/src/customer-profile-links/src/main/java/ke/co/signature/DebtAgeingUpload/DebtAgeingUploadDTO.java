package ke.co.signature.DebtAgeingUpload;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class DebtAgeingUploadDTO {

    private Long id;

    private String fileName;

    private LocalDate reportDate;

    private LocalDateTime uploadedAt;

    private String uploadedBy;

    private Integer totalRecords;

    private BigDecimal totalDebt;
}