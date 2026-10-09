
package com.smartmove.backend.service;

import com.smartmove.backend.entity.SystemConfiguration;
import com.smartmove.backend.entity.UserAccount;
import com.smartmove.backend.entity.UserRole;
import com.smartmove.backend.repository.SystemConfigurationRepository;

import tools.jackson.databind.ObjectMapper;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class SystemConfigurationService {

    private static final Set<String> VALUE_TYPES = Set.of(
            "STRING",
            "INTEGER",
            "DECIMAL",
            "BOOLEAN",
            "JSON"
    );

    private static final Set<String> CATEGORIES = Set.of(
            "BOOKING",
            "PAYMENT",
            "TRIP",
            "TRACKING",
            "MAINTENANCE",
            "NOTIFICATION",
            "SECURITY",
            "GENERAL"
    );

    private static final Set<String> RESERVED_PREFIXES = Set.of(
            "SPRING_",
            "DATABASE_",
            "DB_",
            "SECRET_",
            "PASSWORD_",
            "TOKEN_",
            "PRIVATE_KEY_",
            "API_KEY_"
    );

    private final SystemConfigurationRepository repository;
    private final CurrentUserService currentUserService;
    private final ObjectMapper objectMapper;

    public SystemConfigurationService(
            SystemConfigurationRepository repository,
            CurrentUserService currentUserService,
            ObjectMapper objectMapper
    ) {
        this.repository = repository;
        this.currentUserService = currentUserService;
        this.objectMapper = objectMapper;
    }

    // ==========================================
    // REQUEST AND RESPONSE DTOs
    // ==========================================

    public record CreateConfigurationRequest(
            String configKey,
            String configValue,
            String valueType,
            String category,
            String displayName,
            String description,
            String unit,
            Boolean active,
            Boolean superAdminOnly,
            String minValue,
            String maxValue,
            String allowedValues
    ) {}

    public record UpdateConfigurationRequest(
            String configValue,
            String valueType,
            String category,
            String displayName,
            String description,
            String unit,
            Boolean active,
            Boolean superAdminOnly,
            String minValue,
            String maxValue,
            String allowedValues
    ) {}

    public record ConfigurationProfile(
            Long id,
            String configKey,
            String configValue,
            String valueType,
            String category,
            String displayName,
            String description,
            String unit,
            Boolean active,
            Boolean superAdminOnly,
            Boolean systemDefined,
            String minValue,
            String maxValue,
            String allowedValues,
            Long createdByAccountId,
            Long updatedByAccountId,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Long rowVersion
    ) {}

    public record ConfigurationGroupCount(
            String name,
            long count
    ) {}

    public record ConfigurationStatistics(
            long totalConfigurations,
            long activeConfigurations,
            long inactiveConfigurations,
            long superAdminOnlyConfigurations,
            long systemDefinedConfigurations,
            long updatedLast24Hours,
            List<ConfigurationGroupCount> categories,
            List<ConfigurationGroupCount> valueTypes
    ) {}

    public record DefaultSetting(
            String key,
            String value,
            String type,
            String category,
            String displayName,
            String description,
            String unit,
            boolean superAdminOnly,
            String minimum,
            String maximum
    ) {}

    // ==========================================
    // CREATE CONFIGURATION
    // ==========================================

    @Transactional
    public ConfigurationProfile createConfiguration(
            CreateConfigurationRequest request
    ) {
        requireSuperAdmin();

        if (request == null) {
            throw badRequest("Configuration details are required");
        }

        String key = validateKey(request.configKey());

        if (repository.existsByConfigKeyIgnoreCase(key)) {
            throw conflict("Configuration key already exists");
        }

        SystemConfiguration configuration =
                new SystemConfiguration();

        configuration.setConfigKey(key);
        configuration.setConfigValue(request.configValue());
        configuration.setValueType(
                normalizeOrDefault(request.valueType(), "STRING")
        );
        configuration.setCategory(
                normalizeOrDefault(request.category(), "GENERAL")
        );
        configuration.setDisplayName(request.displayName());
        configuration.setDescription(request.description());
        configuration.setUnit(request.unit());
        configuration.setActive(
                request.active() == null || request.active()
        );
        configuration.setSuperAdminOnly(
                Boolean.TRUE.equals(request.superAdminOnly())
        );
        configuration.setSystemDefined(false);
        configuration.setMinValue(request.minValue());
        configuration.setMaxValue(request.maxValue());
        configuration.setAllowedValues(request.allowedValues());

        UserAccount actor = currentUserService.getCurrentAccount();

        configuration.setCreatedBy(actor);
        configuration.setUpdatedBy(actor);

        validateConfiguration(configuration);

        try {
            return toProfile(
                    repository.saveAndFlush(configuration)
            );
        } catch (DataIntegrityViolationException exception) {
            throw conflict("Configuration key already exists");
        }
    }

    // ==========================================
    // UPDATE CONFIGURATION
    // ==========================================

    @Transactional
    public ConfigurationProfile updateConfiguration(
            Long configurationId,
            UpdateConfigurationRequest request
    ) {
        requireAdmin();

        if (request == null) {
            throw badRequest("Configuration details are required");
        }

        SystemConfiguration configuration =
                findForUpdate(configurationId);

        requireCanModify(configuration);

        if (request.configValue() != null) {
            configuration.setConfigValue(request.configValue());
        }

        if (request.valueType() != null) {
            configuration.setValueType(request.valueType());
        }

        if (request.category() != null) {
            configuration.setCategory(request.category());
        }

        if (request.displayName() != null) {
            configuration.setDisplayName(request.displayName());
        }

        if (request.description() != null) {
            configuration.setDescription(request.description());
        }

        if (request.unit() != null) {
            configuration.setUnit(request.unit());
        }

        if (request.active() != null) {
            configuration.setActive(request.active());
        }

        if (request.superAdminOnly() != null) {
            requireSuperAdmin();
            configuration.setSuperAdminOnly(
                    request.superAdminOnly()
            );
        }

        if (request.minValue() != null) {
            configuration.setMinValue(request.minValue());
        }

        if (request.maxValue() != null) {
            configuration.setMaxValue(request.maxValue());
        }

        if (request.allowedValues() != null) {
            configuration.setAllowedValues(
                    request.allowedValues()
            );
        }

        configuration.setUpdatedBy(
                currentUserService.getCurrentAccount()
        );

        validateConfiguration(configuration);

        return toProfile(
                repository.saveAndFlush(configuration)
        );
    }

    // ==========================================
    // UPDATE CONFIGURATION VALUE
    // ==========================================

    @Transactional
    public ConfigurationProfile updateValue(
            Long configurationId,
            String value
    ) {
        requireAdmin();

        SystemConfiguration configuration =
                findForUpdate(configurationId);

        requireCanModify(configuration);

        configuration.setConfigValue(value);
        configuration.setUpdatedBy(
                currentUserService.getCurrentAccount()
        );

        validateConfiguration(configuration);

        return toProfile(
                repository.saveAndFlush(configuration)
        );
    }

    // ==========================================
    // ACTIVATE CONFIGURATION
    // ==========================================

    @Transactional
    public ConfigurationProfile activateConfiguration(
            Long configurationId
    ) {
        requireAdmin();

        SystemConfiguration configuration =
                findForUpdate(configurationId);

        requireCanModify(configuration);

        configuration.setActive(true);
        configuration.setUpdatedBy(
                currentUserService.getCurrentAccount()
        );

        validateConfiguration(configuration);

        return toProfile(
                repository.saveAndFlush(configuration)
        );
    }

    // ==========================================
    // DEACTIVATE CONFIGURATION
    // ==========================================

    @Transactional
    public ConfigurationProfile deactivateConfiguration(
            Long configurationId
    ) {
        requireAdmin();

        SystemConfiguration configuration =
                findForUpdate(configurationId);

        requireCanModify(configuration);

        configuration.setActive(false);
        configuration.setUpdatedBy(
                currentUserService.getCurrentAccount()
        );

        return toProfile(
                repository.saveAndFlush(configuration)
        );
    }

    // ==========================================
    // DELETE CUSTOM CONFIGURATION
    // ==========================================

    @Transactional
    public void deleteConfiguration(
            Long configurationId
    ) {
        requireSuperAdmin();

        SystemConfiguration configuration =
                findForUpdate(configurationId);

        if (Boolean.TRUE.equals(
                configuration.getSystemDefined()
        )) {
            throw conflict(
                    "System-defined configurations cannot be deleted"
            );
        }

        repository.delete(configuration);
        repository.flush();
    }

    // ==========================================
    // GET CONFIGURATION BY ID
    // ==========================================

    @Transactional(readOnly = true)
    public ConfigurationProfile getConfigurationById(
            Long configurationId
    ) {
        requireAdmin();

        SystemConfiguration configuration =
                findById(configurationId);

        requireCanView(configuration);

        return toProfile(configuration);
    }

    // ==========================================
    // GET CONFIGURATION BY KEY
    // ==========================================

    @Transactional(readOnly = true)
    public ConfigurationProfile getConfigurationByKey(
            String key
    ) {
        requireAdmin();

        SystemConfiguration configuration =
                repository.findByConfigKeyIgnoreCase(
                        validateKey(key)
                ).orElseThrow(() ->
                        notFound()
                );

        requireCanView(configuration);

        return toProfile(configuration);
    }

    // ==========================================
    // GET ALL CONFIGURATIONS
    // ==========================================

    @Transactional(readOnly = true)
    public List<ConfigurationProfile> getAllConfigurations() {
        requireAdmin();

        List<SystemConfiguration> configurations =
                isSuperAdmin()
                        ? repository
                          .findAllByOrderByCategoryAscConfigKeyAsc()
                        : repository
                          .findAdminVisibleConfigurations();

        return configurations.stream()
                .map(this::toProfile)
                .toList();
    }

    // ==========================================
    // PAGINATED CONFIGURATIONS
    // ==========================================

    @Transactional(readOnly = true)
    public Page<ConfigurationProfile> getConfigurationsPage(
            Pageable pageable
    ) {
        requireAdmin();
        requirePageable(pageable);

        Page<SystemConfiguration> configurations =
                isSuperAdmin()
                        ? repository
                          .findAllByOrderByCategoryAscConfigKeyAsc(
                                  pageable
                          )
                        : repository
                          .findAdminVisibleConfigurations(
                                  pageable
                          );

        return configurations.map(this::toProfile);
    }

    // ==========================================
    // CONFIGURATIONS BY CATEGORY
    // ==========================================

    @Transactional(readOnly = true)
    public List<ConfigurationProfile> getByCategory(
            String category
    ) {
        requireAdmin();

        String normalizedCategory =
                validateCategory(category);

        List<SystemConfiguration> configurations =
                isSuperAdmin()
                        ? repository
                          .findByCategoryIgnoreCaseOrderByConfigKeyAsc(
                                  normalizedCategory
                          )
                        : repository
                          .findAdminVisibleByCategory(
                                  normalizedCategory
                          );

        return configurations.stream()
                .map(this::toProfile)
                .toList();
    }

    // ==========================================
    // SEARCH CONFIGURATIONS
    // ==========================================

    @Transactional(readOnly = true)
    public Page<ConfigurationProfile> searchConfigurations(
            String keyword,
            Pageable pageable
    ) {
        requireAdmin();
        requirePageable(pageable);

        if (keyword == null || keyword.isBlank()) {
            return getConfigurationsPage(pageable);
        }

        String search = keyword.trim();

        if (isSuperAdmin()) {
            return repository
                    .findByConfigKeyContainingIgnoreCaseOrDisplayNameContainingIgnoreCase(
                            search,
                            search,
                            pageable
                    )
                    .map(this::toProfile);
        }

        // Restrict results before pagination so
        // Super Admin-only records are not exposed.
        String normalizedSearch =
                search.toLowerCase(Locale.ROOT);

        List<ConfigurationProfile> filtered =
                repository.findAdminVisibleConfigurations()
                        .stream()
                        .filter(configuration ->
                                containsIgnoreCase(
                                        configuration.getConfigKey(),
                                        normalizedSearch
                                )
                                        || containsIgnoreCase(
                                        configuration.getDisplayName(),
                                        normalizedSearch
                                )
                        )
                        .map(this::toProfile)
                        .toList();

        return toPage(filtered, pageable);
    }

    // ==========================================
    // RECENTLY UPDATED CONFIGURATIONS
    // ==========================================

    @Transactional(readOnly = true)
    public List<ConfigurationProfile> getRecentlyUpdated() {
        requireAdmin();

        return repository.findTop20ByOrderByUpdatedAtDesc()
                .stream()
                .filter(configuration ->
                        isSuperAdmin()
                                || !Boolean.TRUE.equals(
                                configuration.getSuperAdminOnly()
                        )
                )
                .map(this::toProfile)
                .toList();
    }

    // ==========================================
    // CONFIGURATION STATISTICS
    // ==========================================

    @Transactional(readOnly = true)
    public ConfigurationStatistics getStatistics() {
        requireSuperAdmin();

        LocalDateTime now = LocalDateTime.now();

        return new ConfigurationStatistics(
                repository.count(),
                repository.countByActiveTrue(),
                repository.countByActiveFalse(),
                repository.countBySuperAdminOnlyTrue(),
                repository.countBySystemDefinedTrue(),
                repository.countByUpdatedAtBetween(
                        now.minusHours(24),
                        now
                ),
                mapCounts(
                        repository.countConfigurationsByCategory()
                ),
                mapCounts(
                        repository.countConfigurationsByValueType()
                )
        );
    }

    // ==========================================
    // READ ACTIVE STRING VALUE
    // ==========================================

    @Transactional(readOnly = true)
    public String getString(
            String key,
            String fallback
    ) {
        return repository.findActiveValueByKey(
                validateKey(key)
        ).orElse(fallback);
    }

    // ==========================================
    // READ ACTIVE INTEGER VALUE
    // ==========================================

    @Transactional(readOnly = true)
    public int getInteger(
            String key,
            int fallback
    ) {
        String value = getString(key, null);

        if (value == null) {
            return fallback;
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    // ==========================================
    // READ ACTIVE LONG VALUE
    // ==========================================

    @Transactional(readOnly = true)
    public long getLong(
            String key,
            long fallback
    ) {
        String value = getString(key, null);

        if (value == null) {
            return fallback;
        }

        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    // ==========================================
    // READ ACTIVE DECIMAL VALUE
    // ==========================================

    @Transactional(readOnly = true)
    public BigDecimal getDecimal(
            String key,
            BigDecimal fallback
    ) {
        String value = getString(key, null);

        if (value == null) {
            return fallback;
        }

        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    // ==========================================
    // READ ACTIVE BOOLEAN VALUE
    // ==========================================

    @Transactional(readOnly = true)
    public boolean getBoolean(
            String key,
            boolean fallback
    ) {
        String value = getString(key, null);

        if (value == null) {
            return fallback;
        }

        if ("true".equalsIgnoreCase(value.trim())) {
            return true;
        }

        if ("false".equalsIgnoreCase(value.trim())) {
            return false;
        }

        return fallback;
    }

    // ==========================================
    // INITIALIZE DEFAULT CONFIGURATIONS
    // ==========================================

    @Transactional
    public int initializeDefaults() {
        requireSuperAdmin();

        int created = 0;

        UserAccount actor =
                currentUserService.getCurrentAccount();

        for (DefaultSetting setting : defaultSettings()) {

            if (repository.existsByConfigKeyIgnoreCase(
                    setting.key()
            )) {
                continue;
            }

            SystemConfiguration configuration =
                    new SystemConfiguration();

            configuration.setConfigKey(setting.key());
            configuration.setConfigValue(setting.value());
            configuration.setValueType(setting.type());
            configuration.setCategory(setting.category());
            configuration.setDisplayName(
                    setting.displayName()
            );
            configuration.setDescription(
                    setting.description()
            );
            configuration.setUnit(setting.unit());
            configuration.setActive(true);
            configuration.setSuperAdminOnly(
                    setting.superAdminOnly()
            );
            configuration.setSystemDefined(true);
            configuration.setMinValue(setting.minimum());
            configuration.setMaxValue(setting.maximum());

            configuration.setCreatedBy(actor);
            configuration.setUpdatedBy(actor);

            validateConfiguration(configuration);

            repository.save(configuration);
            created++;
        }

        repository.flush();

        return created;
    }

    // ==========================================
    // DEFAULT CONFIGURATION DEFINITIONS
    // ==========================================

    public List<DefaultSetting> defaultSettings() {
        return List.of(
                new DefaultSetting(
                        "BOOKING_MAX_SEATS_PER_REQUEST",
                        "6",
                        "INTEGER",
                        "BOOKING",
                        "Maximum seats per booking",
                        "Maximum seats allowed in one booking",
                        "SEATS",
                        false,
                        "1",
                        "100"
                ),
                new DefaultSetting(
                        "CANCELLATION_CUTOFF_HOURS",
                        "2",
                        "INTEGER",
                        "BOOKING",
                        "Cancellation cutoff",
                        "Minimum hours before departure for cancellation",
                        "HOURS",
                        false,
                        "0",
                        "168"
                ),
                new DefaultSetting(
                        "BOOKING_REFUND_PERCENT",
                        "100",
                        "INTEGER",
                        "PAYMENT",
                        "Booking refund percentage",
                        "Refund percentage for eligible cancellations",
                        "PERCENT",
                        true,
                        "0",
                        "100"
                ),
                new DefaultSetting(
                        "GPS_UPDATE_DISTANCE_KM",
                        "3",
                        "DECIMAL",
                        "TRACKING",
                        "GPS update distance",
                        "Suggested distance between GPS updates",
                        "KM",
                        false,
                        "0.1",
                        "100"
                ),
                new DefaultSetting(
                        "GPS_LOCATION_MAX_AGE_SECONDS",
                        "300",
                        "INTEGER",
                        "TRACKING",
                        "Maximum GPS location age",
                        "Maximum age before a GPS location is stale",
                        "SECONDS",
                        false,
                        "10",
                        "86400"
                ),
                new DefaultSetting(
                        "TRIP_ASSIGNMENT_BUFFER_MINUTES",
                        "30",
                        "INTEGER",
                        "TRIP",
                        "Trip assignment buffer",
                        "Scheduling buffer between assignments",
                        "MINUTES",
                        false,
                        "0",
                        "1440"
                ),
                new DefaultSetting(
                        "MAINTENANCE_REMINDER_DAYS",
                        "14",
                        "INTEGER",
                        "MAINTENANCE",
                        "Maintenance reminder period",
                        "Days before maintenance to display a reminder",
                        "DAYS",
                        false,
                        "1",
                        "365"
                ),
                new DefaultSetting(
                        "PAYMENT_DEFAULT_CURRENCY",
                        "LKR",
                        "STRING",
                        "PAYMENT",
                        "Default payment currency",
                        "Currency for payment records",
                        null,
                        true,
                        null,
                        null
                ),
                new DefaultSetting(
                        "ON_DEMAND_MAX_PASSENGERS",
                        "30",
                        "INTEGER",
                        "TRIP",
                        "Maximum on-demand passengers",
                        "Maximum passengers per on-demand request",
                        "SEATS",
                        false,
                        "1",
                        "1000"
                ),
                new DefaultSetting(
                        "STAFF_TRANSPORT_MAX_PASSENGERS",
                        "100",
                        "INTEGER",
                        "TRIP",
                        "Maximum staff transport passengers",
                        "Maximum passengers per staff transport request",
                        "SEATS",
                        false,
                        "1",
                        "1000"
                )
        );
    }

    // ==========================================
    // VALIDATE CONFIGURATION
    // ==========================================

    private void validateConfiguration(
            SystemConfiguration configuration
    ) {
        configuration.setConfigKey(
                validateKey(configuration.getConfigKey())
        );

        String type = normalizeOrDefault(
                configuration.getValueType(),
                "STRING"
        );

        String category = validateCategory(
                normalizeOrDefault(
                        configuration.getCategory(),
                        "GENERAL"
                )
        );

        if (!VALUE_TYPES.contains(type)) {
            throw badRequest(
                    "Unsupported configuration value type"
            );
        }

        configuration.setValueType(type);
        configuration.setCategory(category);

        String value = configuration.getConfigValue();

        if (value == null || value.isBlank()) {
            throw badRequest("Configuration value is required");
        }

        if (value.length() > 2000) {
            throw badRequest(
                    "Configuration value exceeds 2000 characters"
            );
        }

        if (configuration.getDisplayName() == null
                || configuration.getDisplayName().isBlank()) {
            throw badRequest("Display name is required");
        }

        if (configuration.getDisplayName().length() > 150) {
            throw badRequest(
                    "Display name exceeds 150 characters"
            );
        }

        checkLength(
                configuration.getDescription(),
                1000,
                "Description"
        );

        checkLength(
                configuration.getUnit(),
                30,
                "Unit"
        );

        checkLength(
                configuration.getAllowedValues(),
                1000,
                "Allowed values"
        );

        checkLength(
                configuration.getMinValue(),
                100,
                "Minimum value"
        );

        checkLength(
                configuration.getMaxValue(),
                100,
                "Maximum value"
        );

        validateValueType(type, value);

        validateNumericBounds(configuration);

        validateAllowedValues(configuration);
    }

    // ==========================================
    // VALIDATE VALUE TYPE
    // ==========================================

    private void validateValueType(
            String type,
            String value
    ) {
        switch (type) {

            case "INTEGER" -> {
                try {
                    Long.parseLong(value.trim());
                } catch (NumberFormatException exception) {
                    throw badRequest(
                            "Configuration value must be an integer"
                    );
                }
            }

            case "DECIMAL" -> {
                try {
                    new BigDecimal(value.trim());
                } catch (NumberFormatException exception) {
                    throw badRequest(
                            "Configuration value must be a decimal"
                    );
                }
            }

            case "BOOLEAN" -> {
                if (!"true".equalsIgnoreCase(value.trim())
                        && !"false".equalsIgnoreCase(
                        value.trim()
                )) {
                    throw badRequest(
                            "Boolean value must be true or false"
                    );
                }
            }

            case "JSON" -> {
                try {
                    objectMapper.readTree(value);
                } catch (Exception exception) {
                    throw badRequest(
                            "Configuration value must contain valid JSON"
                    );
                }
            }

            case "STRING" -> {
                // Already checked for blank and length.
            }

            default -> throw badRequest(
                    "Unsupported configuration value type"
            );
        }
    }

    // ==========================================
    // VALIDATE NUMERIC BOUNDS
    // ==========================================

    private void validateNumericBounds(
            SystemConfiguration configuration
    ) {
        String type = configuration.getValueType();

        String min = configuration.getMinValue();
        String max = configuration.getMaxValue();

        if (!"INTEGER".equals(type)
                && !"DECIMAL".equals(type)) {

            if (notBlank(min) || notBlank(max)) {
                throw badRequest(
                        "Numeric bounds require INTEGER or DECIMAL type"
                );
            }

            return;
        }

        BigDecimal value = parseDecimal(
                configuration.getConfigValue(),
                "Configuration value"
        );

        BigDecimal minimum = notBlank(min)
                ? parseDecimal(min, "Minimum value")
                : null;

        BigDecimal maximum = notBlank(max)
                ? parseDecimal(max, "Maximum value")
                : null;

        if (minimum != null
                && maximum != null
                && minimum.compareTo(maximum) > 0) {
            throw badRequest(
                    "Minimum value cannot exceed maximum value"
            );
        }

        if ("INTEGER".equals(type)) {
            if (minimum != null
                    && minimum.stripTrailingZeros().scale() > 0) {
                throw badRequest(
                        "Integer minimum must be a whole number"
                );
            }

            if (maximum != null
                    && maximum.stripTrailingZeros().scale() > 0) {
                throw badRequest(
                        "Integer maximum must be a whole number"
                );
            }
        }

        if (minimum != null
                && value.compareTo(minimum) < 0) {
            throw badRequest(
                    "Configuration value is below the minimum"
            );
        }

        if (maximum != null
                && value.compareTo(maximum) > 0) {
            throw badRequest(
                    "Configuration value exceeds the maximum"
            );
        }
    }

    // ==========================================
    // VALIDATE ALLOWED VALUES
    // ==========================================

    private void validateAllowedValues(
            SystemConfiguration configuration
    ) {
        String allowed = configuration.getAllowedValues();

        if (!notBlank(allowed)) {
            return;
        }

        String value =
                configuration.getConfigValue().trim();

        for (String option : allowed.split(",")) {
            if (option.trim().equalsIgnoreCase(value)) {
                return;
            }
        }

        throw badRequest(
                "Configuration value is not in the allowed values"
        );
    }

    // ==========================================
    // VALIDATE CATEGORY
    // FIXES THE REPORTED COMPILATION ERROR
    // ==========================================

    private String validateCategory(
            String category
    ) {
        if (category == null || category.isBlank()) {
            throw badRequest(
                    "Configuration category is required"
            );
        }

        String normalized = normalize(category);

        if (!CATEGORIES.contains(normalized)) {
            throw badRequest(
                    "Unsupported configuration category"
            );
        }

        return normalized;
    }

    // ==========================================
    // VALIDATE CONFIGURATION KEY
    // ==========================================

    private String validateKey(
            String key
    ) {
        if (key == null || key.isBlank()) {
            throw badRequest(
                    "Configuration key is required"
            );
        }

        String normalized = normalize(key);

        if (normalized.length() > 120) {
            throw badRequest(
                    "Configuration key exceeds 120 characters"
            );
        }

        if (!normalized.matches("[A-Z][A-Z0-9_]*")) {
            throw badRequest(
                    "Configuration key must contain only "
                            + "letters, digits and underscores"
            );
        }

        for (String prefix : RESERVED_PREFIXES) {
            if (normalized.startsWith(prefix)) {
                throw badRequest(
                        "This configuration key prefix is reserved"
                );
            }
        }

        return normalized;
    }

    // ==========================================
    // AUTHORIZATION
    // ==========================================

    private void requireAdmin() {
        if (!currentUserService.hasAnyRole(
                UserRole.ADMIN,
                UserRole.SUPER_ADMIN
        )) {
            throw forbidden();
        }
    }

    private void requireSuperAdmin() {
        currentUserService.requireSuperAdmin();
    }

    private boolean isSuperAdmin() {
        return currentUserService.hasRole(
                UserRole.SUPER_ADMIN
        );
    }

    private void requireCanView(
            SystemConfiguration configuration
    ) {
        if (Boolean.TRUE.equals(
                configuration.getSuperAdminOnly()
        ) && !isSuperAdmin()) {
            throw forbidden();
        }
    }

    private void requireCanModify(
            SystemConfiguration configuration
    ) {
        requireCanView(configuration);
    }

    // ==========================================
    // ENTITY LOOKUPS
    // ==========================================

    private SystemConfiguration findById(
            Long id
    ) {
        validateId(id);

        return repository.findById(id)
                .orElseThrow(this::notFound);
    }

    private SystemConfiguration findForUpdate(
            Long id
    ) {
        validateId(id);

        return repository.findByIdForUpdate(id)
                .orElseThrow(this::notFound);
    }

    // ==========================================
    // ENTITY TO RESPONSE DTO
    // ==========================================

    private ConfigurationProfile toProfile(
            SystemConfiguration configuration
    ) {
        return new ConfigurationProfile(
                configuration.getId(),
                configuration.getConfigKey(),
                configuration.getConfigValue(),
                configuration.getValueType(),
                configuration.getCategory(),
                configuration.getDisplayName(),
                configuration.getDescription(),
                configuration.getUnit(),
                configuration.getActive(),
                configuration.getSuperAdminOnly(),
                configuration.getSystemDefined(),
                configuration.getMinValue(),
                configuration.getMaxValue(),
                configuration.getAllowedValues(),
                configuration.getCreatedBy() == null
                        ? null
                        : configuration.getCreatedBy().getId(),
                configuration.getUpdatedBy() == null
                        ? null
                        : configuration.getUpdatedBy().getId(),
                configuration.getCreatedAt(),
                configuration.getUpdatedAt(),
                configuration.getRowVersion()
        );
    }

    // ==========================================
    // STATISTICS MAPPING
    // ==========================================

    private List<ConfigurationGroupCount> mapCounts(
            List<Object[]> rows
    ) {
        return rows.stream()
                .map(row -> new ConfigurationGroupCount(
                        row[0] == null
                                ? "UNKNOWN"
                                : row[0].toString(),
                        row[1] instanceof Number number
                                ? number.longValue()
                                : 0L
                ))
                .toList();
    }

    // ==========================================
    // IN-MEMORY PAGINATION
    // ==========================================

    private Page<ConfigurationProfile> toPage(
            List<ConfigurationProfile> records,
            Pageable pageable
    ) {
        long offset = pageable.getOffset();

        int start = (int) Math.min(
                offset,
                records.size()
        );

        int end = Math.min(
                start + pageable.getPageSize(),
                records.size()
        );

        return new PageImpl<>(
                records.subList(start, end),
                pageable,
                records.size()
        );
    }

    private void requirePageable(
            Pageable pageable
    ) {
        if (pageable == null || pageable.isUnpaged()) {
            throw badRequest(
                    "Pagination details are required"
            );
        }

        if (pageable.getPageSize() < 1
                || pageable.getPageSize() > 200) {
            throw badRequest(
                    "Page size must be between 1 and 200"
            );
        }
    }

    // ==========================================
    // VALIDATION HELPERS
    // ==========================================

    private void validateId(
            Long id
    ) {
        if (id == null || id <= 0) {
            throw badRequest(
                    "Valid configuration ID is required"
            );
        }
    }

    private BigDecimal parseDecimal(
            String value,
            String fieldName
    ) {
        try {
            return new BigDecimal(value.trim());
        } catch (Exception exception) {
            throw badRequest(
                    fieldName + " must be numeric"
            );
        }
    }

    private void checkLength(
            String value,
            int maximum,
            String fieldName
    ) {
        if (value != null && value.length() > maximum) {
            throw badRequest(
                    fieldName + " exceeds "
                            + maximum + " characters"
            );
        }
    }

    private boolean notBlank(
            String value
    ) {
        return value != null && !value.isBlank();
    }

    private boolean containsIgnoreCase(
            String value,
            String keyword
    ) {
        return value != null
                && value.toLowerCase(Locale.ROOT)
                .contains(keyword);
    }

    private String normalize(
            String value
    ) {
        return value == null
                ? ""
                : value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeOrDefault(
            String value,
            String fallback
    ) {
        return value == null || value.isBlank()
                ? fallback
                : normalize(value);
    }

    // ==========================================
    // HTTP ERROR HELPERS
    // ==========================================

    private ResponseStatusException badRequest(
            String message
    ) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message
        );
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Configuration not found"
        );
    }

    private ResponseStatusException forbidden() {
        return new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "This operation is not permitted"
        );
    }

    private ResponseStatusException conflict(
            String message
    ) {
        return new ResponseStatusException(
                HttpStatus.CONFLICT,
                message
        );
    }
}
