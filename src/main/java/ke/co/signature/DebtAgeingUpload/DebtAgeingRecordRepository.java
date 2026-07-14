package ke.co.signature.DebtAgeingUpload;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DebtAgeingRecordRepository
        extends JpaRepository<DebtAgeingRecord, Long> {

    List<DebtAgeingRecord> findByUploadIdOrderByTotalDebtDesc(Long uploadId);
}