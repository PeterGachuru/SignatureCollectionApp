package ke.co.signature.DebtAgeingUpload;

import ke.co.signature.Customer.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DebtAgeingUploadRepository
        extends JpaRepository<DebtAgeingUpload, Long> {

    @Query("""
        SELECT u
        FROM DebtAgeingUpload u
        JOIN DebtAgeingRecord r
            ON r.upload = u
        WHERE r.customer = :customer
        ORDER BY u.uploadedAt DESC
    """)
    Optional<DebtAgeingUpload> findLatestUploadForCustomer(
            @Param("customer") Customer customer
    );
}