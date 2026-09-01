package ke.co.signature.DebtAgeingUpload;

import ke.co.signature.Customer.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DebtAgeingRecordRepository
        extends JpaRepository<DebtAgeingRecord, Long> {

    List<DebtAgeingRecord>
    findByUploadIdOrderByTotalDebtDesc(Long uploadId);

    List<DebtAgeingRecord>
    findByUploadIdAndCustomerTownRegionIdInOrderByTotalDebtDesc(
            Long uploadId,
            List<Long> regionIds
    );

    Optional<DebtAgeingRecord>
    findByUploadIdAndCustomer(
            Long uploadId,
            Customer customer
    );

    Optional<DebtAgeingRecord>
    findByUploadIdAndCustomerId(
            Long uploadId,
            Long customerId
    );
}