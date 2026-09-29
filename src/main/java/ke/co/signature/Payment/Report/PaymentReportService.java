package ke.co.signature.Payment.Report;

import ke.co.signature.Payment.PaymentInProgress.PaymentInProgress;
import ke.co.signature.Payment.PaymentInProgress.PaymentInProgressRepository;
import ke.co.signature.Payment.PaymentRepository;
import ke.co.signature.Payment.PostedPayment;
import ke.co.signature.Payment.PaymentSplit.PaymentSplit;
import ke.co.signature.Payment.PaymentSplit.PaymentSplitRepository;
import ke.co.signature.Payment.DebtBucket;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PaymentReportService {

    private final PaymentRepository paymentRepository;
    private final PaymentInProgressRepository inProgressRepository;
    private final PaymentSplitRepository splitRepository;

    public PaymentReportService(PaymentRepository paymentRepository,
                                PaymentInProgressRepository inProgressRepository,
                                PaymentSplitRepository splitRepository) {
        this.paymentRepository = paymentRepository;
        this.inProgressRepository = inProgressRepository;
        this.splitRepository = splitRepository;
    }

    public byte[] pendingExcel() throws IOException {
        List<PaymentInProgress> rows = inProgressRepository.findAllByOrderByCreatedAtDesc();
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Pending Transactions");
            List<String> headers = pendingHeaders();
            createHeader(sheet, headers);
            int rowNum = 1;
            for (PaymentInProgress p : rows) {
                Row row = sheet.createRow(rowNum++);
                write(row, pendingValues(p));
            }
            finishSheet(sheet, headers.size());
            return workbookBytes(workbook);
        }
    }

    public byte[] postedExcel() throws IOException {
        List<PostedPayment> payments = paymentRepository.findAll();
        Map<Long, List<PaymentSplit>> splits = loadSplits(payments);
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Posted Transactions");
            List<String> headers = postedHeaders();
            createHeader(sheet, headers);
            int rowNum = 1;
            for (PostedPayment p : payments) {
                Row row = sheet.createRow(rowNum++);
                write(row, postedValues(p, splits.getOrDefault(p.getId(), List.of())));
            }
            finishSheet(sheet, headers.size());
            return workbookBytes(workbook);
        }
    }

    public byte[] pendingPdf() {
        List<PaymentInProgress> rows = inProgressRepository.findAllByOrderByCreatedAtDesc();
        List<String> headers = pendingHeaders();
        List<List<String>> values = rows.stream().map(this::pendingValues).toList();
        return pdf("PENDING PAYMENT TRANSACTIONS", headers, values);
    }

    public byte[] postedPdf() {
        List<PostedPayment> payments = paymentRepository.findAll();
        Map<Long, List<PaymentSplit>> splits = loadSplits(payments);
        List<String> headers = postedHeaders();
        List<List<String>> values = payments.stream()
                .map(p -> postedValues(p, splits.getOrDefault(p.getId(), List.of())))
                .toList();
        return pdf("POSTED PAYMENT TRANSACTIONS", headers, values);
    }

    private Map<Long, List<PaymentSplit>> loadSplits(List<PostedPayment> payments) {
        Map<Long, List<PaymentSplit>> result = new HashMap<>();
        for (PostedPayment payment : payments) {
            result.put(payment.getId(), splitRepository.findByPostedPaymentId(payment.getId()));
        }
        return result;
    }

    private List<String> pendingHeaders() {
        return List.of(
                "ID", "Customer ID", "Customer Code", "Customer Name", "Username", "Contact Person",
                "Customer Phone", "Customer Email", "Customer Location", "Amount", "Reference",
                "M-Pesa Phone", "Payment Mode", "Status", "Transaction Date", "Entry Source", "Bank Code",
                "Bank Name", "Cheque Number", "Cheque Date", "Failure Reason", "Created At", "Created By",
                "Updated At", "Updated By", "Unpost Audit ID", "Unposted At", "Unposted By"
        );
    }

    private List<String> postedHeaders() {
        return List.of(
                "ID", "Customer ID", "Customer Code", "Customer Name", "Username", "Contact Person",
                "Customer Phone", "Customer Email", "Customer Location", "Amount", "Reference",
                "M-Pesa Phone", "Payment Mode", "Status", "Transaction Date", "Entry Source", "Bank Code",
                "Bank Name", "Cheque Number", "Cheque Date", "Unallocated Amount", "Reversal Balance Impact",
                "Reversed At", "Reversed By", "Reversal Reason", "Created At", "Created By", "Updated At",
                "Updated By", "Applied Total", "Current", "30 Days", "60 Days", "90 Days", "120 Days", "Over 120 Days"
        );
    }

    private List<String> pendingValues(PaymentInProgress p) {
        return List.of(
                s(p.getId()), s(p.getCustomer() == null ? null : p.getCustomer().getId()),
                s(p.getCustomer() == null ? null : p.getCustomer().getCustomerCode()),
                s(p.getCustomer() == null ? null : p.getCustomer().getBusinessName()),
                s(p.getCustomer() == null ? null : p.getCustomer().getUsername()),
                s(p.getCustomer() == null ? null : p.getCustomer().getContactPerson()),
                s(p.getCustomer() == null ? null : p.getCustomer().getPhone()),
                s(p.getCustomer() == null ? null : p.getCustomer().getEmail()),
                s(p.getCustomer() == null ? null : p.getCustomer().getLocation()),
                s(p.getAmount()), s(p.getReference()), s(p.getPhoneNumber()), s(p.getPaymentMode()),
                s(p.getStatus()), s(p.getPaymentDate()), s(p.getEntrySource()),
                s(p.getBank() == null ? null : p.getBank().getCode()),
                s(p.getBank() == null ? null : p.getBank().getName()), s(p.getChequeNumber()), s(p.getChequeDate()),
                s(p.getFailureReason()), s(p.getCreatedAt()), user(p.getCreatedBy()), s(p.getUpdatedAt()),
                user(p.getUpdatedBy()), s(p.getUnpostAuditId()), s(p.getUnpostedAt()), user(p.getUnpostedBy())
        );
    }

    private List<String> postedValues(PostedPayment p, List<PaymentSplit> splits) {
        BigDecimal current = bucket(splits, DebtBucket.CURRENT);
        BigDecimal d30 = bucket(splits, DebtBucket.DAYS_30);
        BigDecimal d60 = bucket(splits, DebtBucket.DAYS_60);
        BigDecimal d90 = bucket(splits, DebtBucket.DAYS_90);
        BigDecimal d120 = bucket(splits, DebtBucket.DAYS_120);
        BigDecimal over120 = bucket(splits, DebtBucket.OVER_120);
        BigDecimal applied = current.add(d30).add(d60).add(d90).add(d120).add(over120);
        return List.of(
                s(p.getId()), s(p.getCustomer() == null ? null : p.getCustomer().getId()),
                s(p.getCustomer() == null ? null : p.getCustomer().getCustomerCode()),
                s(p.getCustomer() == null ? null : p.getCustomer().getBusinessName()),
                s(p.getCustomer() == null ? null : p.getCustomer().getUsername()),
                s(p.getCustomer() == null ? null : p.getCustomer().getContactPerson()),
                s(p.getCustomer() == null ? null : p.getCustomer().getPhone()),
                s(p.getCustomer() == null ? null : p.getCustomer().getEmail()),
                s(p.getCustomer() == null ? null : p.getCustomer().getLocation()),
                s(p.getAmount()), s(p.getReference()), s(p.getPhoneNumber()), s(p.getPaymentMode()), s(p.getStatus()),
                s(p.getPaymentDate()), s(p.getEntrySource()), s(p.getBank() == null ? null : p.getBank().getCode()),
                s(p.getBank() == null ? null : p.getBank().getName()), s(p.getChequeNumber()), s(p.getChequeDate()),
                s(p.getUnallocatedAmount()), s(p.getReversalBalanceImpact()), s(p.getReversedAt()),
                user(p.getReversedBy()), s(p.getReversalReason()), s(p.getCreatedAt()), user(p.getCreatedBy()),
                s(p.getUpdatedAt()), user(p.getUpdatedBy()), s(applied), s(current), s(d30), s(d60), s(d90), s(d120), s(over120)
        );
    }

    private BigDecimal bucket(List<PaymentSplit> splits, DebtBucket bucket) {
        return splits.stream()
                .filter(s -> s.getBucket() == bucket)
                .map(PaymentSplit::getAmountApplied)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String user(ke.co.signature.Auth.User.User user) {
        return user == null ? "" : user.getUsername();
    }

    private String s(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private void createHeader(Sheet sheet, List<String> headers) {
        Row row = sheet.createRow(0);
        CellStyle style = sheet.getWorkbook().createCellStyle();
        Font font = sheet.getWorkbook().createFont();
        font.setBold(true);
        style.setFont(font);
        for (int i = 0; i < headers.size(); i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(headers.get(i));
            cell.setCellStyle(style);
        }
        sheet.createFreezePane(0, 1);
        row.setHeightInPoints(28);
    }

    private void write(Row row, List<String> values) {
        for (int i = 0; i < values.size(); i++) row.createCell(i).setCellValue(values.get(i));
    }

    private void finishSheet(Sheet sheet, int columns) {
        sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, Math.max(0, sheet.getLastRowNum()), 0, columns - 1));
        for (int i = 0; i < columns; i++) sheet.setColumnWidth(i, Math.min(60 * 256, 22 * 256));
    }

    private byte[] workbookBytes(Workbook workbook) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        return out.toByteArray();
    }

    /**
     * Dependency-free PDF writer. Each transaction is printed as a compact multi-line
     * record so the report can retain all fields even though there are many columns.
     */
    private byte[] pdf(String title, List<String> headers, List<List<String>> rows) {
        final int width = 842; // A4 landscape
        final int height = 595;
        final int left = 28;
        final int right = 28;
        final int top = 35;
        final int bottom = 35;
        final int lineHeight = 10;
        final int charsPerLine = 150;
        List<String> pageStreams = new ArrayList<>();
        List<String> current = new ArrayList<>();
        int lines = 0;
        current.add("TITLE|" + title);
        lines += 3;
        for (List<String> row : rows) {
            List<String> record = new ArrayList<>();
            for (int i = 0; i < headers.size(); i++) {
                String value = i < row.size() ? row.get(i) : "";
                record.add(headers.get(i) + "=" + safePdf(value));
            }
            String joined = String.join(" | ", record);
            List<String> wrapped = wrap(joined, charsPerLine);
            wrapped.add(0, "TRANSACTION");
            if (lines + wrapped.size() + 2 > 50) {
                pageStreams.add(buildPageStream(current, width, height, left, top, lineHeight));
                current = new ArrayList<>();
                current.add("TITLE|" + title);
                lines = 3;
            }
            current.addAll(wrapped);
            current.add("SEPARATOR");
            lines += wrapped.size() + 1;
        }
        if (!current.isEmpty()) pageStreams.add(buildPageStream(current, width, height, left, top, lineHeight));
        if (pageStreams.isEmpty()) pageStreams.add(buildPageStream(List.of("TITLE|" + title, "No transactions found."), width, height, left, top, lineHeight));
        return assemblePdf(pageStreams, width, height);
    }

    private String buildPageStream(List<String> lines, int width, int height, int left, int top, int lineHeight) {
        StringBuilder sb = new StringBuilder();
        sb.append("BT /F1 11 Tf ").append(left).append(" ").append(height - top).append(" Td\n");
        int y = 0;
        for (String line : lines) {
            if (line.startsWith("TITLE|")) {
                sb.append("/F1 14 Tf (").append(escape(line.substring(6))).append(") Tj\n/F1 8 Tf 0 -16 Td\n");
                y += 16;
            } else if (line.equals("TRANSACTION")) {
                sb.append("/F1 9 Tf 0 -4 Td (TRANSACTION) Tj\n/F1 7 Tf 0 -10 Td\n");
                y += 14;
            } else if (line.equals("SEPARATOR")) {
                sb.append("0 -8 Td 0 0 Td\n");
                y += 8;
            } else {
                sb.append("(").append(escape(line)).append(") Tj 0 -").append(lineHeight).append(" Td\n");
                y += lineHeight;
            }
        }
        sb.append("ET");
        return sb.toString();
    }

    private List<String> wrap(String text, int max) {
        List<String> out = new ArrayList<>();
        String remaining = text;
        while (remaining.length() > max) {
            int cut = remaining.lastIndexOf(' ', max);
            if (cut < max / 2) cut = max;
            out.add(remaining.substring(0, cut));
            remaining = remaining.substring(cut).trim();
        }
        if (!remaining.isEmpty()) out.add(remaining);
        return out;
    }

    private String safePdf(String value) {
        return value == null ? "" : value.replace('\n', ' ').replace('\r', ' ').replaceAll("[^\\x20-\\x7E]", "?");
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }

    private byte[] assemblePdf(List<String> streams, int width, int height) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            List<Integer> offsets = new ArrayList<>();
            write(out, "%PDF-1.4\n%\u00E2\u00E3\u00CF\u00D3\n");
            int pageCount = streams.size();
            int catalog = 1, pages = 2, font = 3;
            int next = 4;
            List<Integer> contentIds = new ArrayList<>();
            List<Integer> pageIds = new ArrayList<>();
            for (int i = 0; i < pageCount; i++) { pageIds.add(next++); contentIds.add(next++); }
            offsets.add(out.size()); write(out, catalog + " 0 obj\n<< /Type /Catalog /Pages " + pages + " 0 R >>\nendobj\n");
            offsets.add(out.size());
            StringBuilder kids = new StringBuilder(); for (int id : pageIds) kids.append(id).append(" 0 R ");
            write(out, pages + " 0 obj\n<< /Type /Pages /Kids [" + kids + "] /Count " + pageCount + " >>\nendobj\n");
            offsets.add(out.size()); write(out, font + " 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n");
            for (int i = 0; i < pageCount; i++) {
                offsets.add(out.size());
                write(out, pageIds.get(i) + " 0 obj\n<< /Type /Page /Parent " + pages + " 0 R /MediaBox [0 0 " + width + " " + height + "] /Resources << /Font << /F1 " + font + " 0 R >> >> /Contents " + contentIds.get(i) + " 0 R >>\nendobj\n");
                byte[] content = streams.get(i).getBytes(StandardCharsets.ISO_8859_1);
                offsets.add(out.size());
                write(out, contentIds.get(i) + " 0 obj\n<< /Length " + content.length + " >>\nstream\n"); out.write(content); write(out, "\nendstream\nendobj\n");
            }
            int xref = out.size();
            int objectCount = next;
            write(out, "xref\n0 " + objectCount + "\n0000000000 65535 f \n");
            for (int off : offsets) write(out, String.format("%010d 00000 n \n", off));
            write(out, "trailer\n<< /Size " + objectCount + " /Root " + catalog + " 0 R >>\nstartxref\n" + xref + "\n%%EOF\n");
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Could not generate PDF report.", e);
        }
    }

    private void write(ByteArrayOutputStream out, String value) throws IOException { out.write(value.getBytes(StandardCharsets.ISO_8859_1)); }
}
