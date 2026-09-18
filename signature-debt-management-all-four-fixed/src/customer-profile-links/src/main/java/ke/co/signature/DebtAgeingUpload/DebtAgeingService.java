package ke.co.signature.DebtAgeingUpload;

import ke.co.signature.Customer.Customer;
import ke.co.signature.Customer.CustomerRepository;
import ke.co.signature.Customer.CustomerService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class DebtAgeingService {
    private final DebtAgeingUploadRepository uploadRepository;
    private final DebtAgeingRecordRepository recordRepository;
    private final CustomerRepository customerRepository;
    private final CustomerService customerService;

    /**
     * Imports one ageing Excel file.
     *
     * @return number of successfully imported records.
     */
    public int importDebtAgeing(MultipartFile file) {

        System.out.println("--importDebtAgeing--");

        validateFile(file);

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String username = authentication.getName();

        DebtAgeingUpload upload = new DebtAgeingUpload();

        upload.setFileName(file.getOriginalFilename());
        upload.setUploadedAt(LocalDateTime.now());
        upload.setUploadedBy(username);

        /*
            Change this if your report contains
            a report date inside the spreadsheet.
         */
        upload.setReportDate(LocalDate.now());

        upload.setTotalDebt(BigDecimal.ZERO);
        upload.setTotalRecords(0);

        upload = uploadRepository.save(upload);

        int importedRows = 0;

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {

            importedRows = readAndSaveRecords(workbook, upload);

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return importedRows;
    }

    /**
     * Ensures uploaded file is valid before reading.
     */
    private void validateFile(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Please select an Excel file.");
        }

        String filename = file.getOriginalFilename();

        if (filename == null) {
            throw new RuntimeException("Invalid file.");
        }

        filename = filename.toLowerCase();

        if (!(filename.endsWith(".xls")
                || filename.endsWith(".xlsx"))) {

            throw new RuntimeException(
                    "Only Excel (.xls or .xlsx) files are supported.");
        }
    }

    /**
     * Reads the workbook and saves every record.
     *
     * Implement in Part 2.
     */

    private Map<String, Integer> getHeaderMap(Sheet sheet) {

        Row headerRow = sheet.getRow(0);

        if (headerRow == null) {
            throw new RuntimeException("Header row not found.");
        }

        Map<String, Integer> headers = new HashMap<>();

        for (Cell cell : headerRow) {

            if (cell == null) {
                continue;
            }

            String header = normalizeHeader(cell.toString());

            if (!header.isBlank()) {
                headers.put(header, cell.getColumnIndex());
            }
        }

        return headers;
    }

    private String normalizeHeader(String header) {

        if (header == null) {
            return "";
        }

        return header.trim()
                .replaceAll("\\s+", " ")
                .toUpperCase();
    }

    private Cell getCell(Row row,
                         Map<String, Integer> headers,
                         String columnName) {

        Integer index = headers.get(normalizeHeader(columnName));

        if (index == null) {
            throw new RuntimeException(
                    "Column '" + columnName + "' not found in Excel.");
        }

        return row.getCell(index);
    }

    private int readAndSaveRecords(
            Workbook workbook,
            DebtAgeingUpload upload) {

        Sheet sheet = workbook.getSheetAt(0);

        Map<String, Integer> headers = getHeaderMap(sheet);

        validateRequiredHeaders(headers);

        int records = 0;

        BigDecimal grandTotal = BigDecimal.ZERO;

        for (int rowNumber = 1;
             rowNumber <= sheet.getLastRowNum();
             rowNumber++) {

            Row row = sheet.getRow(rowNumber);

            if (row == null) {
                continue;
            }

            String customerCode = getString(row, headers, "CUS_CODE");
            String customerName = getString(row, headers, "NAME");
            String unit = getString(row, headers, "UNIT");
            String town = getString(row, headers, "TOWN");
            String region = getString(row, headers, "REGION");

            if (customerCode.isBlank()) {
                continue;
            }

            Optional<Customer> customer = customerRepository
                    .findByCustomerCode(customerCode);

            if (customer.isEmpty()) {
                customerService.createCustomerFromImport(customerCode, customerName, region, town, unit);
                customer = customerRepository.findByCustomerCode(customerCode);
            }

            DebtAgeingRecord record = new DebtAgeingRecord();

            record.setUpload(upload);

            record.setCustomer(customer.get());

            record.setCurrentAmount(
                    getBigDecimal(row, headers, "CURRENT")
            );

            record.setDays30(
                    getBigDecimal(row, headers, "30 DAYS")
            );

            record.setDays60(
                    getBigDecimal(row, headers, "60 DAYS")
            );

            record.setDays90(
                    getBigDecimal(row, headers, "90 DAYS")
            );

            record.setDays120(
                    getBigDecimal(row, headers, "120 DAYS")
            );

//            record.setOver120(
//                    getBigDecimal(row, headers, "OVER 120 DAYS")
//            );

            record.setTotalDebt(
                    getBigDecimal(row, headers, "AGEING TOTAL")
            );

            recordRepository.save(record);

            grandTotal = grandTotal.add(record.getTotalDebt());

            records++;
        }

        upload.setTotalDebt(grandTotal);
        upload.setTotalRecords(records);

        uploadRepository.save(upload);

        return records;
    }

    private String getString(Row row,
                             Map<String, Integer> headers,
                             String columnName) {

        Cell cell = getCell(row, headers, columnName);

        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {

            case STRING:
                return cell.getStringCellValue().trim();

            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue()
                            .toLocalDate()
                            .toString();
                }

                double value = cell.getNumericCellValue();

                if (value == (long) value) {
                    return String.valueOf((long) value);
                }

                return String.valueOf(value);

            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());

            case FORMULA:

                switch (cell.getCachedFormulaResultType()) {

                    case STRING:
                        return cell.getStringCellValue().trim();

                    case NUMERIC:

                        double formulaValue = cell.getNumericCellValue();

                        if (formulaValue == (long) formulaValue) {
                            return String.valueOf((long) formulaValue);
                        }

                        return String.valueOf(formulaValue);

                    case BOOLEAN:
                        return String.valueOf(cell.getBooleanCellValue());

                    default:
                        return "";
                }

            default:
                return "";
        }
    }

    private BigDecimal getBigDecimal(Row row,
                                     Map<String, Integer> headers,
                                     String columnName) {

        Cell cell = getCell(row, headers, columnName);

        if (cell == null) {
            return BigDecimal.ZERO;
        }

        switch (cell.getCellType()) {

            case NUMERIC:
                return BigDecimal.valueOf(cell.getNumericCellValue());

            case STRING:

                String value = cell.getStringCellValue().trim();

                if (value.isBlank()) {
                    return BigDecimal.ZERO;
                }

                value = value.replace(",", "");

                try {
                    return new BigDecimal(value);
                } catch (NumberFormatException ex) {
                    return BigDecimal.ZERO;
                }

            case FORMULA:

                if (cell.getCachedFormulaResultType() == CellType.NUMERIC) {
                    return BigDecimal.valueOf(cell.getNumericCellValue());
                }

                return BigDecimal.ZERO;

            default:
                return BigDecimal.ZERO;
        }
    }

    private void validateRequiredHeaders(Map<String,Integer> headers) {
        List<String> required = List.of(
                "CUS_CODE",
                "NAME",
                "UNIT",
                "TOWN",
                "REGION",
                "CURRENT",
                "30 DAYS",
                "60 DAYS",
                "90 DAYS",
                "120 DAYS",
//                "OVER 120 DAYS",
                "AGEING TOTAL"
        );

        for(String header : required){
            if(!headers.containsKey(header)){
                throw new RuntimeException(
                        "Required column '" + header + "' is missing.");
            }
        }
    }
}