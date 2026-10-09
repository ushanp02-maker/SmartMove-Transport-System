
package com.smartmove.backend.service;

import com.smartmove.backend.entity.SystemConfiguration;
import com.smartmove.backend.repository.SystemConfigurationRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class DatabaseInitializationService
        implements ApplicationRunner {

    private static final Logger logger =
            LoggerFactory.getLogger(
                    DatabaseInitializationService.class
            );

    private final SystemConfigurationRepository
            configurationRepository;

    private final SystemConfigurationService
            configurationService;

    private final boolean initializationEnabled;

    private final boolean failOnInitializationError;

    public DatabaseInitializationService(
            SystemConfigurationRepository configurationRepository,
            SystemConfigurationService configurationService,

            @Value(
                    "${smartmove.initialization.enabled:false}"
            )
            boolean initializationEnabled,

            @Value(
                    "${smartmove.initialization.fail-on-error:true}"
            )
            boolean failOnInitializationError
    ) {
        this.configurationRepository =
                configurationRepository;

        this.configurationService =
                configurationService;

        this.initializationEnabled =
                initializationEnabled;

        this.failOnInitializationError =
                failOnInitializationError;
    }

    // ==========================================
    // STARTUP INITIALIZATION
    // ==========================================

    @Override
    public void run(ApplicationArguments arguments) {

        if (!initializationEnabled) {
            logger.info(
                    "SmartMove automatic reference-data "
                            + "initialization is disabled"
            );
            return;
        }

        logger.info(
                "Starting SmartMove reference-data initialization"
        );

        try {
            InitializationReport report =
                    initializeReferenceData();

            logger.info(
                    "SmartMove initialization completed: "
                            + "created={}, existing={}, total={}",
                    report.createdCount(),
                    report.existingCount(),
                    report.totalDefinitions()
            );

        } catch (RuntimeException exception) {

            logger.error(
                    "SmartMove reference-data initialization failed",
                    exception
            );

            if (failOnInitializationError) {
                throw exception;
            }
        }
    }

    // ==========================================
    // INITIALIZATION RESPONSE
    // ==========================================

    public record InitializationReport(
            String status,
            int createdCount,
            int existingCount,
            int totalDefinitions,
            LocalDateTime completedAt,
            List<String> createdKeys,
            List<String> existingKeys
    ) {}

    // ==========================================
    // INITIALIZE REFERENCE DATA
    // ==========================================

    @Transactional
    public InitializationReport initializeReferenceData() {

        List<SystemConfigurationService.DefaultSetting>
                defaults = configurationService.defaultSettings();

        List<String> createdKeys = new ArrayList<>();
        List<String> existingKeys = new ArrayList<>();

        for (
                SystemConfigurationService.DefaultSetting
                        setting : defaults
        ) {

            String key = normalizeKey(setting.key());

            if (configurationRepository
                    .existsByConfigKeyIgnoreCase(key)) {

                existingKeys.add(key);
                continue;
            }

            SystemConfiguration configuration =
                    createDefaultConfiguration(setting);

            configurationRepository.save(configuration);

            createdKeys.add(key);
        }

        configurationRepository.flush();

        return new InitializationReport(
                "COMPLETED",
                createdKeys.size(),
                existingKeys.size(),
                defaults.size(),
                LocalDateTime.now(),
                List.copyOf(createdKeys),
                List.copyOf(existingKeys)
        );
    }

    // ==========================================
    // CREATE DEFAULT CONFIGURATION ENTITY
    // ==========================================

    private SystemConfiguration createDefaultConfiguration(
            SystemConfigurationService.DefaultSetting setting
    ) {

        SystemConfiguration configuration =
                new SystemConfiguration();

        configuration.setConfigKey(
                normalizeKey(setting.key())
        );

        configuration.setConfigValue(
                setting.value()
        );

        configuration.setValueType(
                normalizeValueType(setting.type())
        );

        configuration.setCategory(
                normalizeCategory(setting.category())
        );

        configuration.setDisplayName(
                setting.displayName()
        );

        configuration.setDescription(
                setting.description()
        );

        configuration.setUnit(
                setting.unit()
        );

        configuration.setActive(true);

        configuration.setSuperAdminOnly(
                setting.superAdminOnly()
        );

        configuration.setSystemDefined(true);

        configuration.setMinValue(
                setting.minimum()
        );

        configuration.setMaxValue(
                setting.maximum()
        );

        // Startup initialization does not run
        // under an authenticated user account.
        // Audit ownership is therefore left null.
        configuration.setCreatedBy(null);
        configuration.setUpdatedBy(null);

        return configuration;
    }

    // ==========================================
    // CHECK INITIALIZATION STATUS
    // ==========================================

    @Transactional(readOnly = true)
    public InitializationStatus getInitializationStatus() {

        List<SystemConfigurationService.DefaultSetting>
                defaults = configurationService.defaultSettings();

        int existing = 0;
        List<String> missing = new ArrayList<>();

        for (
                SystemConfigurationService.DefaultSetting
                        setting : defaults
        ) {

            String key = normalizeKey(setting.key());

            if (configurationRepository
                    .existsByConfigKeyIgnoreCase(key)) {
                existing++;
            } else {
                missing.add(key);
            }
        }

        return new InitializationStatus(
                missing.isEmpty(),
                defaults.size(),
                existing,
                missing.size(),
                List.copyOf(missing)
        );
    }

    public record InitializationStatus(
            boolean initialized,
            int totalDefinitions,
            int existingDefinitions,
            int missingDefinitions,
            List<String> missingKeys
    ) {}

    // ==========================================
    // NORMALIZATION HELPERS
    // ==========================================

    private String normalizeKey(String value) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Default configuration key cannot be blank"
            );
        }

        String normalized =
                value.trim().toUpperCase(Locale.ROOT);

        if (!normalized.matches("[A-Z][A-Z0-9_]*")) {
            throw new IllegalArgumentException(
                    "Invalid default configuration key: "
                            + value
            );
        }

        return normalized;
    }

    private String normalizeValueType(String value) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Default configuration type is required"
            );
        }

        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeCategory(String value) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Default configuration category is required"
            );
        }

        return value.trim().toUpperCase(Locale.ROOT);
    }
}
