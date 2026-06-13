package ke.co.signature.Configs;

import ke.co.signature.Configs.CustomerUnit.CustomerUnit;
import ke.co.signature.Configs.CustomerUnit.CustomerUnitRepository;
import ke.co.signature.Configs.Region.Region;
import ke.co.signature.Configs.Region.RegionRepository;
import ke.co.signature.Configs.Town.Town;
import ke.co.signature.Configs.Town.TownRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConfigurationService {

    private final RegionRepository regionRepository;
    private final TownRepository townRepository;
    private final CustomerUnitRepository customerUnitRepository;

    public ConfigurationService(RegionRepository regionRepository,
                                TownRepository townRepository,
                                CustomerUnitRepository customerUnitRepository) {
        this.regionRepository = regionRepository;
        this.townRepository = townRepository;
        this.customerUnitRepository = customerUnitRepository;
    }

    // Region
    public List<Region> listRegions() { return regionRepository.findAll(); }
    public Region saveRegion(Region region) { return regionRepository.save(region); }

    // Town
    public List<Town> listTowns() { return townRepository.findAll(); }
    public Town saveTown(Town town) { return townRepository.save(town); }

    // Customer Unit
    public List<CustomerUnit> listUnits() { return customerUnitRepository.findAll(); }
    public CustomerUnit saveUnit(CustomerUnit unit) { return customerUnitRepository.save(unit); }
}
