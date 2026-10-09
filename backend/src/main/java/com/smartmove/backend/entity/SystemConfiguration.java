
package com.smartmove.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.time.LocalDateTime;
import java.util.Locale;

@Entity
@Table(
        name = "SYSTEM_CONFIGURATIONS",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "UK_SYSTEM_CONFIG_KEY",
                        columnNames = {"CONFIG_KEY"}
                )
        },
        indexes = {
                @Index(
                        name = "IDX_SYSTEM_CONFIG_CATEGORY",
                        columnList = "CATEGORY"
                ),
                @Index(
                        name = "IDX_SYSTEM_CONFIG_ACTIVE",
                        columnList = "IS_ACTIVE"
                ),
                @Index(
                        name = "IDX_SYSTEM_CONFIG_UPDATED",
                        columnList = "UPDATED_AT"
                )
        }
)
public class SystemConfiguration {

    // ==========================================
    // PRIMARY KEY
    // ==========================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    // ==========================================
    // CONFIGURATION IDENTIFICATION
    // ==========================================

    // Example:
    // BOOKING_MAX_SEATS_PER_REQUEST
    // GPS_UPDATE_DISTANCE_KM
    // CANCELLATION_CUTOFF_HOURS
    @Column(
            name = "CONFIG_KEY",
            nullable = false,
            length = 120
    )
    private String configKey;

    // Stored as text and interpreted according
    // to the configured value type.
    @Column(
            name = "CONFIG_VALUE",
            nullable = false,
            length = 2000
    )
    private String configValue;

    // STRING, INTEGER, DECIMAL, BOOLEAN, JSON
    @Column(
            name = "VALUE_TYPE",
            nullable = false,
            length = 20
    )
    private String valueType = "STRING";

    // BOOKING, PAYMENT, TRIP, TRACKING,
    // MAINTENANCE, NOTIFICATION, SECURITY,
    // GENERAL
    @Column(
            name = "CATEGORY",
            nullable = false,
            length = 40
    )
    private String category = "GENERAL";

    // ==========================================
    // DESCRIPTION AND DISPLAY
    // ==========================================

    @Column(
            name = "DISPLAY_NAME",
            nullable = false,
            length = 150
    )
    private String displayName;

    @Column(
            name = "DESCRIPTION",
            length = 1000
    )
    private String description;

    // Optional measurement unit:
    // KM, MINUTES, HOURS, DAYS, LKR,
    // PERCENT, SEATS, SECONDS
    @Column(
            name = "UNIT",
            length = 30
    )
    private String unit;

    // ==========================================
    // CONFIGURATION CONTROL
    // ==========================================

    @Column(
            name = "IS_ACTIVE",
            nullable = false
    )
    private Boolean active = true;

    // When true, only a Super Admin should
    // be permitted to modify this setting.
    @Column(
            name = "IS_SUPER_ADMIN_ONLY",
            nullable = false
    )
    private Boolean superAdminOnly = false;

    // When true, the configuration cannot be
    // deleted through ordinary admin APIs.
    @Column(
            name = "IS_SYSTEM_DEFINED",
            nullable = false
    )
    private Boolean systemDefined = false;

    // ==========================================
    // VALIDATION CONSTRAINTS
    // ==========================================

    // Optional numeric bounds.
    // Stored as strings to support both
    // integer and decimal configuration types.
    @Column(
            name = "MIN_VALUE",
            length = 100
    )
    private String minValue;

    @Column(
            name = "MAX_VALUE",
            length = 100
    )
    private String maxValue;

    // Optional comma-separated allowed values.
    // Example: PENDING,CONFIRMED,CANCELLED
    @Column(
            name = "ALLOWED_VALUES",
            length = 1000
    )
    private String allowedValues;

    // ==========================================
    // AUDIT INFORMATION
    // ==========================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CREATED_BY_ACCOUNT_ID")
    private UserAccount createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UPDATED_BY_ACCOUNT_ID")
    private UserAccount updatedBy;

    @Column(
            name = "CREATED_AT",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(
            name = "UPDATED_AT",
            nullable = false
    )
    private LocalDateTime updatedAt;

    // Prevents lost updates when two admins
    // edit the same configuration concurrently.
    @Version
    @Column(
            name = "ROW_VERSION",
            nullable = false
    )
    private Long rowVersion;

    // ==========================================
    // CONSTRUCTORS
    // ==========================================

    public SystemConfiguration() {
    }

    // ==========================================
    // JPA LIFECYCLE
    // ==========================================

    @PrePersist
    public void onCreate() {

        LocalDateTime now = LocalDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        updatedAt = now;

        applyDefaults();
    }

    @PreUpdate
    public void onUpdate() {

        updatedAt = LocalDateTime.now();

        applyDefaults();
    }

    private void applyDefaults() {

        if (configKey != null) {
            configKey = configKey
                    .trim()
                    .toUpperCase(Locale.ROOT);
        }

        if (valueType == null || valueType.isBlank()) {
            valueType = "STRING";
        } else {
            valueType = valueType
                    .trim()
                    .toUpperCase(Locale.ROOT);
        }

        if (category == null || category.isBlank()) {
            category = "GENERAL";
        } else {
            category = category
                    .trim()
                    .toUpperCase(Locale.ROOT);
        }

        if (active == null) {
            active = true;
        }

        if (superAdminOnly == null) {
            superAdminOnly = false;
        }

        if (systemDefined == null) {
            systemDefined = false;
        }

        if (configValue != null) {
            configValue = configValue.trim();
        }

        if (displayName != null) {
            displayName = displayName.trim();
        }

        if (unit != null) {
            unit = unit.trim().toUpperCase(Locale.ROOT);
        }
    }

    // ==========================================
    // GETTERS AND SETTERS
    // ==========================================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigValue() {
        return configValue;
    }

    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }

    public String getValueType() {
        return valueType;
    }

    public void setValueType(String valueType) {
        this.valueType = valueType;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Boolean getSuperAdminOnly() {
        return superAdminOnly;
    }

    public void setSuperAdminOnly(Boolean superAdminOnly) {
        this.superAdminOnly = superAdminOnly;
    }

    public Boolean getSystemDefined() {
        return systemDefined;
    }

    public void setSystemDefined(Boolean systemDefined) {
        this.systemDefined = systemDefined;
    }

    public String getMinValue() {
        return minValue;
    }

    public void setMinValue(String minValue) {
        this.minValue = minValue;
    }

    public String getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(String maxValue) {
        this.maxValue = maxValue;
    }

    public String getAllowedValues() {
        return allowedValues;
    }

    public void setAllowedValues(String allowedValues) {
        this.allowedValues = allowedValues;
    }

    public UserAccount getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UserAccount createdBy) {
        this.createdBy = createdBy;
    }

    public UserAccount getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(UserAccount updatedBy) {
        this.updatedBy = updatedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getRowVersion() {
        return rowVersion;
    }

    public void setRowVersion(Long rowVersion) {
        this.rowVersion = rowVersion;
    }
}
