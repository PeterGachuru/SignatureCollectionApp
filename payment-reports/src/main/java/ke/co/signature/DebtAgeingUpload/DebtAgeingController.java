package ke.co.signature.DebtAgeingUpload;

import ke.co.signature.Auth.User.User;
import ke.co.signature.Auth.User.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import ke.co.signature.Configs.Region.Region;

import java.util.List;

import static ke.co.signature.Auth.Role.RoleValue.ROLE_REGIONAL_REP;

@Controller
@RequestMapping("/debt-ageing")
@RequiredArgsConstructor
public class DebtAgeingController {

    private final DebtAgeingService debtAgeingService;
    private final DebtAgeingUploadRepository uploadRepository;
    private final DebtAgeingRecordRepository recordRepository;
    private final UserRepository userRepository;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/upload")
    public String uploadPage() {
        return "debt-ageing/upload";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/upload")
    public String uploadDebtAgeing(
            @RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes) {

        try {

            int records = debtAgeingService.importDebtAgeing(file);

            redirectAttributes.addFlashAttribute(
                    "success",
                    records + " debt ageing records uploaded successfully.");

        } catch (Exception ex) {
            ex.printStackTrace();

        }

        return "redirect:/debt-ageing";
    }

    @GetMapping
    public String uploadHistory(Model model) {

        List<DebtAgeingUpload> uploads =
                uploadRepository.findAll(
                        Sort.by(Sort.Direction.DESC, "uploadedAt"));

        model.addAttribute("uploads", uploads);

        return "debt-ageing/upload-history";
    }

    @GetMapping("/{uploadId}/records")
    public String viewAnalysis(
            @PathVariable Long uploadId,
            Model model) {

        DebtAgeingUpload upload = uploadRepository.findById(uploadId)
                .orElseThrow(() ->
                        new RuntimeException("Upload not found."));

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean regionalRep = user.getRoles()
                .stream()
                .anyMatch(r -> r.getName().equals(ROLE_REGIONAL_REP));

        List<DebtAgeingRecord> records;

        if (regionalRep) {

            List<Long> regionIds = user.getRegions()
                    .stream()
                    .map(Region::getId)
                    .toList();

            records = recordRepository
                    .findByUploadIdAndCustomerTownRegionIdInOrderByTotalDebtDesc(
                            uploadId,
                            regionIds
                    );

        } else {

            records = recordRepository
                    .findByUploadIdOrderByTotalDebtDesc(uploadId);

        }

        model.addAttribute("upload", upload);
        model.addAttribute("records", records);

        return "debt-ageing/debt-ageing-records";
    }
}