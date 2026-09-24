package ke.co.signature.Payment;

import ke.co.signature.Customer.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<PostedPayment, Long> {

    // ✅ For dashboard: payments in last N days
    @Query("""
        SELECT p FROM PostedPayment p
        WHERE p.paymentDate > :date
        AND p.status = ke.co.signature.Payment.PostedPaymentStatus.POSTED
    """)
    List<PostedPayment> findByPaymentDateAfter(@Param("date") LocalDate date);

    // ✅ Payments by customer
    @Query("""
        SELECT p FROM PostedPayment p
        WHERE p.customer = :customer
        AND p.status = ke.co.signature.Payment.PostedPaymentStatus.POSTED
        ORDER BY p.paymentDate DESC
    """)
    List<PostedPayment> findByCustomer(@Param("customer") Customer customer);

    // ✅ Payments for a specific credit sale
//    List<PostedPayment> findByCreditSaleId(Long creditSaleId);

    @Query("""
        SELECT p FROM PostedPayment p
        WHERE p.status = ke.co.signature.Payment.PostedPaymentStatus.POSTED
        ORDER BY p.paymentDate DESC
    """)
    List<PostedPayment> findAllByOrderByPaymentDateDesc();

    @Query("""
        SELECT p FROM PostedPayment p
        WHERE (LOWER(p.customer.businessName) LIKE LOWER(CONCAT('%', :customerName, '%'))
             OR LOWER(p.reference) LIKE LOWER(CONCAT('%', :reference, '%')))
    """)
    Page<PostedPayment> findByCustomer_BusinessNameContainingIgnoreCaseOrReferenceContainingIgnoreCase(
            @Param("customerName") String customerName,
            @Param("reference") String reference,
            Pageable pageable
    );

    @Query("""
        SELECT COALESCE(SUM(p.unallocatedAmount), 0)
        FROM PostedPayment p
        WHERE p.status = ke.co.signature.Payment.PostedPaymentStatus.POSTED
        AND p.unallocatedAmount > 0
    """)
    BigDecimal getTotalOverpayments();

    @Query("""
    select coalesce(sum(p.unallocatedAmount), 0)
    from PostedPayment p
    where p.customer.id = :customerId
      and p.status = ke.co.signature.Payment.PostedPaymentStatus.POSTED
""")
    Optional<BigDecimal> sumUnallocatedByCustomer(@Param("customerId") Long customerId);

    @Query("""
        SELECT COALESCE(SUM(p.unallocatedAmount), 0)
        FROM PostedPayment p
        WHERE p.customer.username = :username
        AND p.status = ke.co.signature.Payment.PostedPaymentStatus.POSTED
        AND p.unallocatedAmount > 0
    """)
    BigDecimal totalOverpayments(String username);

    @Query("""
        SELECT p FROM PostedPayment p
        WHERE p.customer.username = :username
        AND p.status = ke.co.signature.Payment.PostedPaymentStatus.POSTED
        ORDER BY p.paymentDate DESC
    """)
    List<PostedPayment> findTop5ActiveByCustomerUsername(@Param("username") String username, org.springframework.data.domain.Pageable pageable);

    @Query("""
        SELECT p FROM PostedPayment p
        WHERE p.customer.username = :username
        AND p.status = ke.co.signature.Payment.PostedPaymentStatus.POSTED
        ORDER BY p.paymentDate DESC
    """)
    List<PostedPayment> findTop10ActiveByCustomerUsername(@Param("username") String username, org.springframework.data.domain.Pageable pageable);

    @Query("""
        SELECT COUNT(p) FROM PostedPayment p
        WHERE p.customer.id = :customerId
        AND p.status = ke.co.signature.Payment.PostedPaymentStatus.POSTED
    """)
    long countByCustomerId(@Param("customerId") Long customerId);
    @Query("""
        SELECT p FROM PostedPayment p
        WHERE p.customer.username = :username
        ORDER BY p.paymentDate DESC
    """)
    List<PostedPayment> findByCustomer_UsernameOrderByPaymentDateDesc(String username);

    @Query("""
        SELECT COUNT(p) FROM PostedPayment p
        WHERE p.customer.id IN :customerIds
        AND p.status = ke.co.signature.Payment.PostedPaymentStatus.POSTED
    """)
    long countByCustomerIdIn(@Param("customerIds") List<Long> customerIds);

    @Query("""
        SELECT COALESCE(SUM(p.amount), 0)
        FROM PostedPayment p
        WHERE p.customer.id IN :customerIds
        AND p.status = ke.co.signature.Payment.PostedPaymentStatus.POSTED
    """)
    BigDecimal sumAmountByCustomerIds(@Param("customerIds") List<Long> customerIds);


    @Query("""
        SELECT COALESCE(SUM(p.amount), 0)
        FROM PostedPayment p
        WHERE p.customer.id = :customerId
        AND p.status = ke.co.signature.Payment.PostedPaymentStatus.POSTED
    """)
    BigDecimal sumAmountByCustomerId(@Param("customerId") Long customerId);

    /**
     * Number of posted payments.
     */
    @Override
    long count();


    /**
     * Total amount of all posted payments.
     */
    @Query("""
        SELECT COALESCE(SUM(p.amount), 0)
        FROM PostedPayment p
        WHERE p.status = ke.co.signature.Payment.PostedPaymentStatus.POSTED
    """)
    BigDecimal getTotalPostedAmount();


    /**
     * Count distinct customers from a supplied
     * customer list who have made a posted payment.
     *
     * This is used for the dashboard's latest ageing upload.
     */
    @Query("""
        SELECT COUNT(DISTINCT p.customer.id)
        FROM PostedPayment p
        WHERE p.customer.id IN :customerIds
        AND p.status = ke.co.signature.Payment.PostedPaymentStatus.POSTED
    """)
    long countDistinctCustomersWhoHavePaid(
            @Param("customerIds")
            List<Long> customerIds
    );


    /**
     * Get customer IDs that have made posted payments.
     *
     * Only customers contained in the latest ageing upload
     * are considered.
     */
    @Query("""
        SELECT DISTINCT p.customer.id
        FROM PostedPayment p
        WHERE p.customer.id IN :customerIds
        AND p.status = ke.co.signature.Payment.PostedPaymentStatus.POSTED
    """)
    List<Long> findCustomersWhoHavePaid(
            @Param("customerIds")
            List<Long> customerIds
    );

    boolean existsByChequeNumberIgnoreCase(String normalizedCheque);

    boolean existsByReferenceIgnoreCase(String normalizedReference);
}