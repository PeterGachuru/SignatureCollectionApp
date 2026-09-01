package ke.co.signature.Payment;

import ke.co.signature.Customer.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}