package ke.co.signature;

import jakarta.persistence.*;
import ke.co.signature.Auth.User.User;
import ke.co.signature.Utils.SecurityUtils;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@MappedSuperclass
@Getter
@Setter
public abstract class BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    protected User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_id")
    protected User updatedBy;

    protected Date createdAt;
    protected Date updatedAt;


    // Automatically called before saving a new entity
    @PrePersist
    protected void onCreate() {
        System.out.println("BaseEntity:onCreate");
        Date now = new Date();
        this.createdAt = now;
        this.updatedAt = now;
        this.createdBy = SecurityUtils.getCurrentUser();
    }

    // Automatically called before updating an existing entity
    @PreUpdate
    protected void onUpdate() {
        System.out.println("BaseEntity:onUpdate");
        this.updatedAt = new Date();
        this.updatedBy = SecurityUtils.getCurrentUser();
    }
}