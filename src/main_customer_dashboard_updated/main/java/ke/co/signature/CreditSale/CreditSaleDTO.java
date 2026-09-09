package ke.co.signature.CreditSale;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreditSaleDTO {
    private Long id;
    private String saleCode;
    private String customerName;
    private String customerCode;
    private BigDecimal grossAmount;
    private BigDecimal discount;
    private BigDecimal vatIncluded;
    private BigDecimal invoiceAmount;
    private BigDecimal balance;
    private LocalDate saleDate;
    private String status;

    public CreditSaleDTO(Long id, String customerName, String customerCode, BigDecimal invoiceAmount,
                         BigDecimal balance, LocalDate saleDate, String status) {
        this.id = id;
        this.customerName = customerName;
        this.customerCode = customerCode;
        this.invoiceAmount = invoiceAmount;
        this.balance = balance;
        this.saleDate = saleDate;
        this.status = status;
    }

    public CreditSaleDTO(CreditSale cs) {
        this.id = cs.getId();
        this.saleCode = cs.getSaleCode();
        this.customerName = cs.getCustomer().getBusinessName();
        this.customerCode = cs.getCustomer().getCustomerCode();
        this.grossAmount = cs.getGrossAmount();;
        this.discount = cs.getDiscount();;
        this.invoiceAmount = cs.getInvoiceAmount();;
        this.balance = cs.getBalance();
        this.saleDate = cs.getSaleDate();
        this.status = status;
    }
}
