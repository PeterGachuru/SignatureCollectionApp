package ke.co.signature.Customer;

import ke.co.signature.Auth.Role.RoleValue;
import ke.co.signature.Auth.User.User;
import ke.co.signature.Auth.User.UserRepository;
import ke.co.signature.Auth.User.UserService;
import ke.co.signature.Configs.ConfigurationService;
import ke.co.signature.Configs.CustomerUnit.CustomerUnit;
import ke.co.signature.Configs.Region.Region;
import ke.co.signature.Configs.Region.RegionRepository;
import ke.co.signature.Configs.Town.Town;
import ke.co.signature.Configs.Town.TownRepository;
import ke.co.signature.CreditSale.CreditSaleRepository;
import lombok.AllArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ke.co.signature.CreditSale.CreditSale;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ke.co.signature.Auth.Role.RoleValue.ROLE_REGIONAL_REP;

@Service
@AllArgsConstructor
public class CustomerService {


    private final CustomerRepository customerRepository;
    private final CreditSaleRepository creditSaleRepository;
    private final ConfigurationService configurationService;

    private final UserService userService;

    private final RegionRepository regionRepository;
    private final TownRepository townRepository;
    private final UserRepository userRepository;

    public void uploadCustomersFromExcel(MultipartFile file) throws Exception {
        Workbook workbook = WorkbookFactory.create(file.getInputStream());

        Sheet sheet = workbook.getSheetAt(0);

        // =========================
        // READ HEADER ROW
        // =========================
        Row headerRow = sheet.getRow(0);

        Map<String, Integer> columns = new HashMap<>();

        for (Cell cell : headerRow) {

            columns.put(
                    cell.getStringCellValue().trim().toUpperCase(),
                    cell.getColumnIndex()
            );
        }

        // =========================
        // PROCESS DATA ROWS
        // =========================
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {

            Row row = sheet.getRow(i);

            if (row == null) {
                continue;
            }

            String account =
                    getCellValue(row.getCell(columns.get("ACCOUNT")));

            String customerName =
                    getCellValue(row.getCell(columns.get("CUSTOMER NAME")));

            String townName =
                    getCellValue(row.getCell(columns.get("TOWN")));

            String regionName =
                    getCellValue(row.getCell(columns.get("REGION")));

            // Skip blank rows
            if (account.isBlank()) {
                continue;
            }

            // =========================
            // REGION
            // =========================
            Region region = configurationService.createAndReturnRegion(regionName);
            // =========================
            // TOWN
            // =========================
            Town town = configurationService.createAndReturnTown(townName, region);

            // =========================
            // CREATE CUSTOMER
            // =========================
            Customer customer = new Customer();

            customer.setCustomerCode(account);
            customer.setUsername(account);

            customer.setBusinessName(customerName);

            customer.setTown(town);

            customer.setActive(true);

            boolean exists =
                    customerRepository.existsByCustomerCode(account);

            if (exists) {
                System.out.println("Customer already exists: "+customer.getBusinessName());
                updateCustomer(customer);
                continue;
            }

            createCustomer(customer);
        }

        workbook.close();
    }
    private String getCellValue(Cell cell) {

        if (cell == null) {
            return "";
        }

        cell.setCellType(CellType.STRING);

        return cell.getStringCellValue().trim();
    }

    public Customer createCustomer(Customer customer) {
        System.out.println("To create new customer: "+customer.getBusinessName());
        if (customerRepository.existsByCustomerCode(customer.getCustomerCode())) {
            throw new IllegalArgumentException("Customer Code already exists");
        }
        if (customerRepository.existsByUsername(customer.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        if(userService.userExists(customer.getUsername())) {
            return null;
        }

        userService.createUser(customer.getUsername(),
                userService.generateEasyPassword(customer.getUsername()),
                RoleValue.ROLE_CUSTOMER_ADMIN
        );
        System.out.println(customer);
        return customerRepository.save(customer);
    }
    public Customer updateCustomer(Customer customer) {

        Customer existingCustomer = customerRepository.findByCustomerCode(customer.getCustomerCode()).get();

        existingCustomer.update(customer);


        return customerRepository.save(existingCustomer);
    }

    public Customer findById(Long customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Customer not found with ID: " + customerId)
                );
    }

    public BigDecimal getTotalOutstandingCredit(Long customerId) {

        Customer customer = findById(customerId);

        return creditSaleRepository.findByCustomer(customer)
                .stream()
                .map(CreditSale::getBalance)   // IMPORTANT: use balance, not amount
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Page<CustomerCreditDTO> listCustomersWithTotalCredit(
            String username,
            String search,
            int page,
            int size
    ){

        Pageable pageable = PageRequest.of(page, size);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean regionalRep = user.getRoles()
                .stream()
                .anyMatch(r -> {
                    System.out.println("Role seen: "+r.getName());
                    return r.getName().equals(ROLE_REGIONAL_REP);
                });

        Page<Customer> customersPage;

        if (regionalRep) {

            System.out.println("Is regional rep");

            List<Long> regionIds = user.getRegions()
                    .stream()
                    .map(Region::getId)
                    .toList();

            System.out.println("Regions: "+ Arrays.deepToString(regionIds.toArray()));

            customersPage = customerRepository.searchCustomersByRegions(
                    search,
                    regionIds,
                    pageable
            );

        } else {

            customersPage = customerRepository.searchCustomers(
                    search,
                    pageable
            );
        }

        return customersPage.map(c -> {

            BigDecimal total = creditSaleRepository.findByCustomer(c)
                    .stream()
                    .map(CreditSale::getBalance)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            String repName = "Rep name";

            String unitName = c.getUnit() != null
                    ? c.getUnit().getName()
                    : "-";

            String townName = c.getTown() != null
                    ? c.getTown().getName()
                    : "-";

            String regionName =
                    (c.getTown() != null && c.getTown().getRegion() != null)
                            ? c.getTown().getRegion().getName()
                            : "-";

            return new CustomerCreditDTO(
                    c.getId(),
                    c.getBusinessName(),
                    c.getCustomerCode(),
                    c.getUsername(),
                    c.getPhone(),
                    c.getLocation(),
                    unitName,
                    townName,
                    regionName,
                    repName,
                    total
            );
        });
    }

    public Customer createCustomerFromImport(String customerCode, String customerName) {
        return createCustomerFromImport(customerCode, customerName, (Region) null,  null, null);
    }

    public Customer createCustomerFromImport(String customerCode, String customerName,
                                             Region region, Town town, CustomerUnit customerUnit) {

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

        userService.createUser(customer.getUsername(),
                userService.generateEasyPassword(customer.getUsername()),
                RoleValue.ROLE_CUSTOMER_ADMIN
        );

        return customerRepository.save(customer);
    }

    public Customer createCustomerFromImport(String customerCode, String customerName,
                                         String regionName, String townName, String unitName) {
        Region region = configurationService.createAndReturnRegion(regionName);
        Town town = configurationService.createAndReturnTown(townName, region);
        CustomerUnit customerUnit = configurationService.createAndReturnCustomerUnit(unitName);

        return createCustomerFromImport(customerCode, customerName, region, town, customerUnit);
    }
}
