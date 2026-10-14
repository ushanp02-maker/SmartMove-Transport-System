
package com.smartmove.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "DRIVERS")
public class Driver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "NAME", nullable = false, length = 120)
    private String name;

    @Column(name = "EMAIL", length = 180, unique = true)
    private String email;

    @Column(name = "PHONE", length = 25)
    private String phone;

    @Column(name = "LICENSE_NUMBER", nullable = false,
            unique = true, length = 50)
    private String licenseNumber;

    @Column(name = "LICENSE_EXPIRY")
    private LocalDate licenseExpiry;

    @Column(name = "EXPERIENCE_YEARS")
    private Integer experienceYears = 0;

    @Column(name = "STATUS", nullable = false, length = 30)
    private String status = "AVAILABLE";

    @Column(name = "JOINED_DATE")
    private LocalDate joinedDate;

    public Driver() {
    }

    @PrePersist
    public void onCreate() {
        if (joinedDate == null) {
            joinedDate = LocalDate.now();
        }

        if (status == null) {
            status = "AVAILABLE";
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public LocalDate getLicenseExpiry() {
        return licenseExpiry;
    }

    public void setLicenseExpiry(LocalDate licenseExpiry) {
        this.licenseExpiry = licenseExpiry;
    }

    public Integer getExperienceYears() {
        return experienceYears;
    }

    public void setExperienceYears(Integer experienceYears) {
        this.experienceYears = experienceYears;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getJoinedDate() {
        return joinedDate;
    }

    public void setJoinedDate(LocalDate joinedDate) {
        this.joinedDate = joinedDate;
    }
}
