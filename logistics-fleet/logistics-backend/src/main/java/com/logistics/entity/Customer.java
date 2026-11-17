package com.logistics.entity;

import com.logistics.entity.enums.ActiveStatus;
import com.logistics.entity.enums.CustomerType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "customers")
public class Customer extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Column(unique = true, length = 20)
    private String customerCode;

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(nullable = false, length = 15)
    private String phone;

    @Column(length = 15)
    private String alternatePhone;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CustomerType customerType = CustomerType.INDIVIDUAL;

    @Column(length = 150)
    private String companyName;

    @Column(length = 15)
    private String gstNumber;

    @Column(name = "address_line1", nullable = false, length = 200)
    private String addressLine1;

    @Column(name = "address_line2", length = 200)
    private String addressLine2;

    @Column(nullable = false, length = 60)
    private String city;

    @Column(nullable = false, length = 60)
    private String state;

    @Column(nullable = false, length = 60)
    private String country = "India";

    @Column(nullable = false, length = 10)
    private String pincode;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ActiveStatus status = ActiveStatus.ACTIVE;
}
