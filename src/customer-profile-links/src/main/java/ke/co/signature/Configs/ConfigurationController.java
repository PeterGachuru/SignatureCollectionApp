package ke.co.signature.Configs;

import ke.co.signature.Configs.CustomerUnit.CustomerUnit;
import ke.co.signature.Configs.Region.Region;
import ke.co.signature.Configs.Town.Town;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/config")
public class ConfigurationController {

    private final ConfigurationService configService;

    public ConfigurationController(ConfigurationService configService) {
        this.configService = configService;
    }

    // Main Configurations Page
    @GetMapping
    public String mainConfigPage() {
        return "config/config-main";
    }

    // ----------- Regions ----------------
    @GetMapping("/regions")
    public String listRegions(Model model) {
        model.addAttribute("regions", configService.listRegions());
        model.addAttribute("region", new Region());
        return "config/regions";
    }

    @PostMapping("/regions/save")
    public String saveRegion(@ModelAttribute Region region, RedirectAttributes redirectAttributes) {
        configService.saveRegion(region);
        redirectAttributes.addFlashAttribute("success", "Region saved successfully");
        return "redirect:/config/regions";
    }

    // ----------- Towns ----------------
    @GetMapping("/towns")
    public String listTowns(Model model) {
        model.addAttribute("towns", configService.listTowns());
        model.addAttribute("town", new Town());
        model.addAttribute("regions", configService.listRegions());
        return "config/towns";
    }

    @PostMapping("/towns/save")
    public String saveTown(@ModelAttribute Town town, RedirectAttributes redirectAttributes) {
        configService.saveTown(town);
        redirectAttributes.addFlashAttribute("success", "Town saved successfully");
        return "redirect:/config/towns";
    }

    // ----------- Customer Units ----------------
    @GetMapping("/units")
    public String listUnits(Model model) {
        model.addAttribute("units", configService.listUnits());
        model.addAttribute("unit", new CustomerUnit());
        return "config/units";
    }

    @PostMapping("/units/save")
    public String saveUnit(@ModelAttribute CustomerUnit unit, RedirectAttributes redirectAttributes) {
        configService.saveUnit(unit);
        redirectAttributes.addFlashAttribute("success", "Unit saved successfully");
        return "redirect:/config/units";
    }
}

