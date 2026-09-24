package ke.co.signature.Customer;

import jakarta.persistence.*;
import ke.co.signature.BaseEntity;
import ke.co.signature.Configs.CustomerUnit.CustomerUnit;
import ke.co.signature.Configs.Town.Town;
import lombok.Data;

@Entity
@Data
public class Customer extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Unique key used for debt mapping (VERY IMPORTANT)
    @Column(nullable = false, unique = true, length = 30)
    private String customerCode;

    // Unique key used for debt mapping (VERY IMPORTANT)
    @Column(nullable = false, unique = true, length = 150)
    private String username;

    @Column(nullable = false, length = 150)
    private String businessName;

    @Column(length = 200)
    private String contactPerson;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String email;

    @Column(length = 150)
    private String location;

    @ManyToOne
    @JoinColumn(name = "unit_id")
    private CustomerUnit unit;

    @ManyToOne
    @JoinColumn(name = "town_id")
    private Town town;

    @Column(nullable = false)
    private Boolean active = true;

    public void update(Customer customer) {
        town = customer.getTown();
        phone = customer.getPhone();
        email = customer.getEmail();
        businessName = customer.getBusinessName();
        location = customer.getLocation();
        unit = customer.getUnit();
        contactPerson = customer.getContactPerson();
    }
}
