package ke.co.signature.Configs.Bank;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BankRepository extends JpaRepository<Bank, Long> {

    List<Bank> findByActiveTrueOrderByNameAsc();

    boolean existsByCodeIgnoreCase(String code);
}