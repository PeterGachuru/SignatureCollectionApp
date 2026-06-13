package ke.co.signature.MpesaIntegration;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
public class MpesaTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String merchantRequestId;
    private String checkoutRequestId;
    private String phoneNumber;
    private BigDecimal amount;
    private String status; // PENDING, SUCCESS, FAILED
    private String mpesaReceiptNumber;
    @CreationTimestamp
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private LocalDateTime timeCallbackReceived;
    private String callbackDescription;
    private Integer callbackResultCode;

    private String accountReference;
    private Long transactionReferenceId;
}