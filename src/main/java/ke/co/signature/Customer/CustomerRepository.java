package ke.co.signature.Customer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByCustomerCode(String customerCode);

    boolean existsByCustomerCode(String customerCode);
    boolean existsByUsername(String username);

    @Query("""
        SELECT c FROM Customer c
        LEFT JOIN c.town t
        LEFT JOIN t.region r
        LEFT JOIN c.unit u
        WHERE
            LOWER(c.businessName) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(c.customerCode) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(c.username) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(c.phone) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(t.name) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(r.name) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%'))
    """)
    Page<Customer> searchCustomers(String search, Pageable pageable);


    @Query("""
    SELECT c FROM Customer c
    LEFT JOIN c.town t
    LEFT JOIN t.region r
    LEFT JOIN c.unit u
    WHERE
        r.id IN :regionIds
    AND
    (
        LOWER(c.businessName) LIKE LOWER(CONCAT('%', :search, '%'))
        OR LOWER(c.customerCode) LIKE LOWER(CONCAT('%', :search, '%'))
        OR LOWER(c.username) LIKE LOWER(CONCAT('%', :search, '%'))
        OR LOWER(c.phone) LIKE LOWER(CONCAT('%', :search, '%'))
        OR LOWER(t.name) LIKE LOWER(CONCAT('%', :search, '%'))
        OR LOWER(r.name) LIKE LOWER(CONCAT('%', :search, '%'))
        OR LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%'))
    )
""")
    Page<Customer> searchCustomersByRegions(
            String search,
            Collection<Long> regionIds,
            Pageable pageable
    );

}

