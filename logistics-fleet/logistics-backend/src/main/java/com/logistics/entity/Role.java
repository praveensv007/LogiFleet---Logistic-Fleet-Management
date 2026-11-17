package com.logistics.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "roles")
public class Role extends BaseEntity {

    /** ROLE_ADMIN, ROLE_FLEET_MANAGER, ROLE_WAREHOUSE_STAFF, ROLE_DRIVER, ROLE_CUSTOMER */
    @Column(nullable = false, unique = true, length = 30)
    private String name;

    private String description;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id"))
    private Set<Permission> permissions = new HashSet<>();

    public Role(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
