package ke.co.signature.CreditSale;

import ke.co.signature.Customer.Customer;
import ke.co.signature.Customer.CustomerRepository;
import ke.co.signature.DebtClassification.DebtClassification;
import ke.co.signature.Payment.Payment;
import ke.co.signature.Payment.PaymentSplit.PaymentSplit;
import ke.co.signature.Payment.PaymentSplit.PaymentSplitDTO;
import ke.co.signature.Payment.PaymentSplit.PaymentSplitRepository;
import lombok.AllArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
public class CreditSaleService {

    private final CreditSaleRepository creditSaleRepository;
    private final CustomerRepository customerRepository;
    private final PaymentSplitRepository paymentSplitRepository;
    private final CreditSaleClassificationService classificationService;

    public CreditSale createCreditSale(Long customerId,
                                       BigDecimal grossAmount,
                                       BigDecimal discount, BigDecimal invoiceAmount, LocalDate saleDate,
                                       String description) {

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        CreditSale sale = new CreditSale();
        sale.setCustomer(customer);
        sale.setGrossAmount(grossAmount);
        sale.setDiscount(discount);
        sale.setBalance(invoiceAmount);
        sale.setInvoiceAmount(invoiceAmount);
        sale.setSaleDate(saleDate != null ? saleDate : LocalDate.now());
        sale.setDescription(description);

        CreditSale creditSale = creditSaleRepository.save(sale);
        updateClassification(creditSale);

        return creditSale;
    }

    public Page<CreditSaleDTO> listCreditSalesPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("saleDate").descending());

        return creditSaleRepository.findAll(pageable)
                .map(cs -> new CreditSaleDTO(cs
                ));
    }

    @Transactional(readOnly = true)
    public CreditSaleDetailsDTO getCreditSaleDetails(Long id) {

        CreditSale cs = creditSaleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Credit Sale not found"));

        // Fetch payment splits
        List<PaymentSplit> splits = paymentSplitRepository.findByCreditSaleId(id);

        // Map splits → DTO
        List<PaymentSplitDTO> splitDTOs = splits.stream().map(ps -> {
            Payment p = ps.getPayment();

            PaymentSplitDTO dto = new PaymentSplitDTO();
            dto.setId(ps.getId());
            dto.setAmountApplied(ps.getAmountApplied());

            dto.setPaymentId(p.getId());
            dto.setPaymentAmount(p.getAmount());
            dto.setPaymentReference(p.getReference());
            dto.setPhoneNumber(p.getPhoneNumber());
            dto.setPaymentMode(p.getPaymentMode() != null ? p.getPaymentMode().name() : null);
            dto.setPaymentDate(p.getPaymentDate());

            return dto;
        }).toList();

        // Build main DTO
        CreditSaleDetailsDTO dto = new CreditSaleDetailsDTO();
        dto.setId(cs.getId());

        dto.setCustomerName(cs.getCustomer().getBusinessName());
        dto.setCustomerCode(cs.getCustomer().getCustomerCode());

    if (cs.getClassification()!= null)
        dto.setClassificationName(cs.getClassification().getName());

        dto.setAmount(cs.getGrossAmount());
        dto.setInvoiceAmount(cs.getInvoiceAmount());
        dto.setBalance(cs.getBalance());

        dto.setSaleDate(cs.getSaleDate());
        dto.setDescription(cs.getDescription());
        dto.setStatus(cs.getStatus().name());

        dto.setCreatedAt(cs.getCreatedAt());
        dto.setCreatedBy(cs.getCreatedBy() != null ? cs.getCreatedBy().getUsername() : null);

        dto.setPaymentSplits(splitDTOs);

        return dto;
    }

    @Transactional
    public void updateClassification(CreditSale cs) {

        DebtClassification classification = classificationService.classify(cs);

        cs.setClassification(classification);

        // Optional: also sync status automatically
        if (cs.getBalance().compareTo(BigDecimal.ZERO) <= 0) {
            cs.setStatus(DebtStatus.PAID);
        } else if (classification != null) {
            switch (classification.getName()) {
                case "CURRENT" -> cs.setStatus(DebtStatus.CURRENT);
                case "30_DAYS" -> cs.setStatus(DebtStatus.OVERDUE);
                case "60_DAYS" -> cs.setStatus(DebtStatus.OVERDUE);
                case "90_DAYS" -> cs.setStatus(DebtStatus.OVERDUE);
            }
        }
    }

//    @Scheduled(cron = "0 0 1 * * *") // every day at 1 AM
    @Transactional
    @Bean
    public void reclassifyAll() {

        List<CreditSale> sales = creditSaleRepository.findAllActive();

        for (CreditSale cs : sales) {
            updateClassification(cs);
        }
    }

    @Transactional
    public int importCreditSales(MultipartFile file) throws Exception {

        Workbook workbook = WorkbookFactory.create(file.getInputStream());

        Sheet sheet = workbook.getSheetAt(0);

        DataFormatter formatter = new DataFormatter();

        Row headerRow = sheet.getRow(0);

        Map<String, Integer> headers = new HashMap<>();

        for (Cell cell : headerRow) {

            headers.put(
                    formatter.formatCellValue(cell)
                            .trim()
                            .toLowerCase(),
                    cell.getColumnIndex());

            System.out.println("Header: "+headers.get(cell.getColumnIndex()));
            System.out.println("Header: "+formatter.formatCellValue(cell)
                    .trim()
                    .toLowerCase());
        }

        int imported = 0;

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {

            Row row = sheet.getRow(i);

            if (row == null) {
                System.out.println("Row is null");
                continue;
            }

            String customerCode =
                    getCellValue(row, headers, "cus_code", formatter);
            String saleCode =
                    getCellValue(row, headers, "doc no", formatter);

            String customerName =
                    getCellValue(row, headers, "customer", formatter);

            if (customerCode == null || customerCode.isBlank()) {
                System.out.println("Customer code is blank");
                continue;
            }

            if (saleCode == null || saleCode.isBlank()) {
                System.out.println("Sale code is blank");
                continue;
            }

            if (creditSaleRepository.existsBySaleCode(saleCode)) {
                System.out.println("Sale code already exists");
                continue;
            }

            Customer customer =
                    customerRepository.findByCustomerCode(customerCode)
                            .orElseGet(() -> createCustomerFromImport(customerCode, customerName));

            String amountString =
                    getCellValue(row, headers, "total sales", formatter);

            BigDecimal amount = amount = parseAmount(amountString);

            String discountString =
                    getCellValue(row, headers, "disc", formatter);

            BigDecimal discount = parseAmount(discountString);


            String vatString =
                    getCellValue(row, headers, "vat", formatter);

            BigDecimal vat = parseAmount(discountString);

            String grossString =
                    getCellValue(row, headers, "gross", formatter);

            BigDecimal gross = parseAmount(grossString);

            LocalDate saleDate = LocalDate.now();
            String dateString = getCellValue(row, headers, "date", formatter);

            if (dateString != null && !dateString.isBlank()) {
//                System.out.println("dateString: "+dateString);
                DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

                saleDate = LocalDate.parse(dateString, inputFormatter);
            }
            CreditSale creditSale = new CreditSale();

            creditSale.setSaleCode(saleCode);
            creditSale.setCustomer(customer);
            creditSale.setGrossAmount(gross);
            creditSale.setInvoiceAmount(amount);
            creditSale.setDiscount(discount);
            creditSale.setBalance(amount);
            creditSale.setVatIncluded(vat);
            creditSale.setSaleDate(saleDate);
            creditSale.setDescription("Uploaded");
            creditSale.setStatus(DebtStatus.CURRENT);

            System.out.println("Created "+creditSale);

            creditSaleRepository.save(creditSale);

            imported++;
        }

        workbook.close();

        return imported;
    }

    private Customer createCustomerFromImport(String customerCode, String customerName) {

        Customer customer = new Customer();

        customer.setCustomerCode(customerCode);

        // Since Excel doesn't guarantee these fields, generate safe defaults
        customer.setUsername(customerCode.toLowerCase());

        customer.setBusinessName(customerName);

        customer.setContactPerson(null);
        customer.setPhone(null);
        customer.setEmail(null);
        customer.setLocation(null);

        customer.setActive(true);

        return customerRepository.save(customer);
    }

    private String getCellValue(
            Row row,
            Map<String, Integer> headers,
            String headerName,
            DataFormatter formatter) {

        Integer index = headers.get(headerName.toLowerCase());

        if (index == null) {
            return null;
        }

        Cell cell = row.getCell(index);

        if (cell == null) {
            return null;
        }

        return formatter.formatCellValue(cell).trim();
    }

    private BigDecimal parseAmount(String value) {
        try {
            if (value == null || value.trim().isEmpty()) {
                return BigDecimal.ZERO;
            }

            String cleaned = value.trim();

            boolean isNegative = false;

            // Handle brackets for negative values: (5000)
            if (cleaned.startsWith("(") && cleaned.endsWith(")")) {
                isNegative = true;
                cleaned = cleaned.substring(1, cleaned.length() - 1);
            }

            // Remove commas: 12,000 → 12000
            cleaned = cleaned.replace(",", "");

            BigDecimal amount = new BigDecimal(cleaned);

            return isNegative ? amount.negate() : amount;
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
