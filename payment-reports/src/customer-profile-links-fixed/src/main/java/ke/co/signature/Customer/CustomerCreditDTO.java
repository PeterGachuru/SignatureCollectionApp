package ke.co.signature.Customer;


import lombok.Data;

import java.math.BigDecimal;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CustomerCreditDTO {

    private Long id;
    private String businessName;
    private String customerCode;
    private String username;
    private String phone;
    private String location;

    // ✅ NEW FIELDS
    private String unitName;
    private String townName;
    private String regionName;

    private String repName;
    private BigDecimal totalCredit;

    // ✅ Updated constructor
    public CustomerCreditDTO(
            Long id,
            String businessName,
            String customerCode,
            String username,
            String phone,
            String location,
            String unitName,
            String townName,
            String regionName,
            String repName,
            BigDecimal totalCredit
    ) {
        this.id = id;
        this.businessName = businessName;
        this.customerCode = customerCode;
        this.username = username;
        this.phone = phone;
        this.location = location;
        this.unitName = unitName;
        this.townName = townName;
        this.regionName = regionName;
        this.repName = repName;
        this.totalCredit = totalCredit;
    }
}
