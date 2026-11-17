package com.logistics.entity;

import com.logistics.entity.enums.DocumentType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "vehicle_documents")
public class VehicleDocument extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private DocumentType documentType;

    @Column(length = 50)
    private String documentNumber;

    private String fileUrl;

    private LocalDate expiryDate;
}
