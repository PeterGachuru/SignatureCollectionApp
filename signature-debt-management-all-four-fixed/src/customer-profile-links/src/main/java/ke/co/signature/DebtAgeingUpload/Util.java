package ke.co.signature.DebtAgeingUpload;

public class Util {
    private DebtAgeingUploadDTO toDto(DebtAgeingUpload upload) {

        DebtAgeingUploadDTO dto = new DebtAgeingUploadDTO();

        dto.setId(upload.getId());
        dto.setFileName(upload.getFileName());
        dto.setReportDate(upload.getReportDate());
        dto.setUploadedAt(upload.getUploadedAt());
        dto.setUploadedBy(upload.getUploadedBy());
        dto.setTotalRecords(upload.getTotalRecords());
        dto.setTotalDebt(upload.getTotalDebt());

        return dto;
    }
    private DebtAgeingRecordDTO toDto(DebtAgeingRecord record) {

        DebtAgeingRecordDTO dto = new DebtAgeingRecordDTO();

        dto.setId(record.getId());

        dto.setCustomerId(record.getCustomer().getId());
        dto.setCustomerCode(record.getCustomer().getCustomerCode());
        dto.setCustomerName(record.getCustomer().getBusinessName());

        if (record.getCustomer().getTown() != null) {
            dto.setTown(record.getCustomer().getTown().getName());
        }

        if (record.getCustomer().getUnit() != null) {
            dto.setUnit(record.getCustomer().getUnit().getName());
        }

        dto.setCurrentAmount(record.getCurrentAmount());
        dto.setDays30(record.getDays30());
        dto.setDays60(record.getDays60());
        dto.setDays90(record.getDays90());
        dto.setDays120(record.getDays120());
        dto.setOver120(record.getOver120());
        dto.setTotalDebt(record.getTotalDebt());
        dto.setRemarks(record.getRemarks());

        return dto;
    }
}
