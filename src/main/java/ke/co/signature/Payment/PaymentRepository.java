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
    List<PostedPayment> findByPaymentDateAfter(LocalDate date);

    // ✅ Payments by customer
    List<PostedPayment> findByCustomer(Customer customer);

    // ✅ Payments for a specific credit sale
//    List<PostedPayment> findByCreditSaleId(Long creditSaleId);

    List<PostedPayment> findAllByOrderByPaymentDateDesc();

    Page<PostedPayment>
    findByCustomer_BusinessNameContainingIgnoreCaseOrReferenceContainingIgnoreCase(
            String customerName,
            String reference,
            Pageable pageable
    );

    @Query("""
        SELECT COALESCE(SUM(p.unallocatedAmount), 0)
        FROM PostedPayment p
        WHERE p.unallocatedAmount > 0
    """)
    BigDecimal getTotalOverpayments();

    @Query("""
    select coalesce(sum(p.unallocatedAmount), 0)
    from PostedPayment p
    where p.customer.id = :customerId
""")
    Optional<BigDecimal> sumUnallocatedByCustomer(@Param("customerId") Long customerId);

    @Query("""
        SELECT COALESCE(SUM(p.unallocatedAmount), 0)
        FROM PostedPayment p
        WHERE p.customer.username = :username
        AND p.unallocatedAmount > 0
    """)
    BigDecimal totalOverpayments(String username);

    List<PostedPayment> findTop5ByCustomer_UsernameOrderByPaymentDateDesc(String username);

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
    """)
    List<Long> findCustomersWhoHavePaid(
            @Param("customerIds")
            List<Long> customerIds
    );
}