package ke.co.signature.DebtAgeingUpload;

import jakarta.persistence.*;
import ke.co.signature.BaseEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
public class DebtAgeingUpload extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;

    private LocalDate reportDate;

    private LocalDateTime uploadedAt;

    private String uploadedBy;

    private Integer totalRecords;

    private BigDecimal totalDebt;
}