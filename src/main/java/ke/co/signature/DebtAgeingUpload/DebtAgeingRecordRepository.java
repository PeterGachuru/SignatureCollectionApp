package ke.co.signature.DebtAgeingUpload;

import ke.co.signature.Customer.Customer;
import ke.co.signature.Dashboard.DebtClassificationTotalsDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface DebtAgeingRecordRepository
        extends JpaRepository<DebtAgeingRecord, Long> {

    List<DebtAgeingRecord>
    findByUploadIdOrderByTotalDebtDesc(
            Long uploadId
    );


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


    /**
     * Total outstanding debt in an ageing upload.
     */
    @Query("""
        SELECT COALESCE(SUM(r.totalDebt), 0)
        FROM DebtAgeingRecord r
        WHERE r.upload.id = :uploadId
    """)
    BigDecimal getTotalDebtByUploadId(
            @Param("uploadId") Long uploadId
    );


    /**
     * Number of customers in an ageing upload.
     */
    long countByUploadId(Long uploadId);


    /**
     * Classification totals.
     *
     * Each record represents one customer.
     */
//    @Query("""
//        SELECT
//            COALESCE(SUM(r.currentAmount), 0),
//            COALESCE(SUM(r.days30), 0),
//            COALESCE(SUM(r.days60), 0),
//            COALESCE(SUM(r.days90), 0),
//            COALESCE(SUM(r.days120), 0),
//            COALESCE(SUM(r.over120), 0)
//        FROM DebtAgeingRecord r
//        WHERE r.upload.id = :uploadId
//    """)
//    Object[] getClassificationTotals(
//            @Param("uploadId") Long uploadId
//    );

    @Query("""
    SELECT new ke.co.signature.Dashboard.DebtClassificationTotalsDTO(
        SUM(r.currentAmount),
        SUM(r.days30),
        SUM(r.days60),
        SUM(r.days90),
        SUM(r.days120),
        SUM(r.over120)
    )
    FROM DebtAgeingRecord r
    WHERE r.upload.id = :uploadId
""")
    DebtClassificationTotalsDTO getClassificationTotals(
            @Param("uploadId") Long uploadId
    );

    @Query("""
    SELECT r.customer.id
    FROM DebtAgeingRecord r
    WHERE r.upload.id = :uploadId
""")
    List<Long> findCustomerIdsByUploadId(
            @Param("uploadId") Long uploadId
    );
}