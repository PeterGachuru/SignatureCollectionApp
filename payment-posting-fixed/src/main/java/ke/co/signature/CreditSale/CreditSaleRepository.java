package ke.co.signature.CreditSale;

import ke.co.signature.Customer.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.math.BigDecimal;

public interface CreditSaleRepository extends JpaRepository<CreditSale, Long> {

    List<CreditSale> findByCustomer(Customer customer);

    @Query("SELECT cs FROM CreditSale cs WHERE cs.balance > 0")
    List<CreditSale> findAllActive();

    List<CreditSale> findByCreatedAtAfter(Date date);


    List<CreditSale> findByCustomerAndBalanceGreaterThanOrderByCreatedAtAsc(
            Customer customer, BigDecimal zero);



    @Query("""
        SELECT COALESCE(SUM(cs.balance), 0)
        FROM CreditSale cs
        WHERE cs.customer.username = :username
        AND cs.balance > 0
    """)
    BigDecimal totalPendingBalance(String username);

    long countByCustomer_UsernameAndBalanceGreaterThan(String username, BigDecimal zero);


    // ✅ NEW: recent credit sales
    List<CreditSale> findTop5ByCustomer_UsernameOrderByIdDesc(String username);

    @Query("""
        SELECT 
            dc.name,
            dc.minDaysLate,
            COUNT(cs),
            COALESCE(SUM(cs.balance), 0)
        FROM DebtClassification dc
        LEFT JOIN CreditSale cs 
            ON cs.classification.id = dc.id
            AND cs.balance > 0
        WHERE dc.active = true
        GROUP BY dc.name
        ORDER BY dc.minDaysLate
    """)
    List<Object[]> getClassificationSummary();

    boolean existsBySaleCode(String saleCode);

    @Query("""
    SELECT cs
    FROM CreditSale cs
    JOIN cs.customer c
    JOIN c.town t
    JOIN t.region r
    WHERE r.id IN :regionIds
""")
    Page<CreditSale> findByRegions(
            Collection<Long> regionIds,
            Pageable pageable
    );
}

