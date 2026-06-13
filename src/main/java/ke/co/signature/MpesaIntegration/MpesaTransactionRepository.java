package ke.co.signature.MpesaIntegration;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MpesaTransactionRepository extends JpaRepository<MpesaTransaction, Long> {
    MpesaTransaction findByCheckoutRequestId(String checkoutRequestId);
}
