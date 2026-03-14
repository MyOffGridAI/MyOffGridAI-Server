package com.myoffgridai.privacy.service;

import com.myoffgridai.auth.model.User;
import com.myoffgridai.auth.repository.UserRepository;
import com.myoffgridai.common.exception.FortressOperationException;
import com.myoffgridai.privacy.dto.FortressStatus;
import com.myoffgridai.system.model.SystemConfig;
import com.myoffgridai.system.service.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Controls the Privacy Fortress — OS-level network lockdown via iptables.
 *
 * <p>When fortress mode is enabled, all outbound traffic is blocked except
 * LAN-local communication. In mock mode ({@code app.fortress.mock=true}),
 * no iptables commands are executed; the service only updates the database.</p>
 */
@Service
public class FortressService {

    private static final Logger log = LoggerFactory.getLogger(FortressService.class);

    private static final String IPTABLES_BLOCK_OUTBOUND =
            "iptables -A OUTPUT -o eth0 -j DROP";
    private static final String IPTABLES_ALLOW_LAN =
            "iptables -A OUTPUT -d 192.168.0.0/16 -j ACCEPT";
    private static final String IPTABLES_ALLOW_LOOPBACK =
            "iptables -A OUTPUT -o lo -j ACCEPT";
    private static final String IPTABLES_FLUSH =
            "iptables -F OUTPUT";
    private static final String IPTABLES_VERIFY =
            "iptables -L OUTPUT -n";

    private static final int COMMAND_TIMEOUT_SECONDS = 10;

    private final SystemConfigService systemConfigService;
    private final UserRepository userRepository;
    private final boolean mockMode;

    /**
     * Constructs the fortress service.
     *
     * @param systemConfigService the system config service
     * @param userRepository      the user repository
     * @param mockMode            whether to run in mock mode (no iptables calls)
     */
    public FortressService(SystemConfigService systemConfigService,
                           UserRepository userRepository,
                           @Value("${app.fortress.mock:true}") boolean mockMode) {
        this.systemConfigService = systemConfigService;
        this.userRepository = userRepository;
        this.mockMode = mockMode;
        log.info("FortressService initialized, mock mode: {}", mockMode);
    }

    /**
     * Enables fortress mode, blocking all outbound internet traffic.
     *
     * @param userId the ID of the user enabling fortress mode
     * @throws FortressOperationException if iptables commands fail
     */
    public void enable(UUID userId) {
        log.info("Enabling fortress mode for user {}", userId);

        if (!mockMode) {
            try {
                executeCommand(IPTABLES_ALLOW_LOOPBACK);
                executeCommand(IPTABLES_ALLOW_LAN);
                executeCommand(IPTABLES_BLOCK_OUTBOUND);
            } catch (Exception e) {
                throw new FortressOperationException("Failed to enable fortress mode: " + e.getMessage(), e);
            }
        } else {
            log.info("Mock mode: skipping iptables commands for enable");
        }

        systemConfigService.setFortressEnabled(true, userId);
        log.info("Fortress mode enabled successfully");
    }

    /**
     * Disables fortress mode, restoring normal network connectivity.
     *
     * @param userId the ID of the user disabling fortress mode
     * @throws FortressOperationException if iptables commands fail
     */
    public void disable(UUID userId) {
        log.info("Disabling fortress mode for user {}", userId);

        if (!mockMode) {
            try {
                executeCommand(IPTABLES_FLUSH);
            } catch (Exception e) {
                throw new FortressOperationException("Failed to disable fortress mode: " + e.getMessage(), e);
            }
        } else {
            log.info("Mock mode: skipping iptables commands for disable");
        }

        systemConfigService.setFortressEnabled(false, userId);
        log.info("Fortress mode disabled successfully");
    }

    /**
     * Returns the current fortress status including verification of iptables state.
     *
     * @return the current fortress status
     */
    public FortressStatus getFortressStatus() {
        SystemConfig config = systemConfigService.getConfig();

        String enabledByUsername = null;
        if (config.getFortressEnabledByUserId() != null) {
            enabledByUsername = userRepository.findById(config.getFortressEnabledByUserId())
                    .map(User::getUsername)
                    .orElse("unknown");
        }

        boolean verified = verifyFortressState(config.isFortressEnabled());

        return new FortressStatus(
                config.isFortressEnabled(),
                config.getFortressEnabledAt(),
                enabledByUsername,
                verified
        );
    }

    /**
     * Checks whether fortress mode is currently active in the database.
     *
     * @return true if fortress mode is enabled
     */
    public boolean isFortressActive() {
        return systemConfigService.getConfig().isFortressEnabled();
    }

    /**
     * Verifies whether the actual iptables state matches the expected state.
     * In mock mode, always returns true.
     *
     * @param expectedEnabled the expected fortress enabled state
     * @return true if the actual state matches the expected state
     */
    private boolean verifyFortressState(boolean expectedEnabled) {
        if (mockMode) {
            return true;
        }

        try {
            String output = executeCommand(IPTABLES_VERIFY);
            boolean hasDropRule = output.contains("DROP");
            return hasDropRule == expectedEnabled;
        } catch (Exception e) {
            log.warn("Failed to verify fortress state: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Executes a shell command and returns its output.
     *
     * @param command the command to execute
     * @return the command output
     * @throws FortressOperationException if the command fails or times out
     */
    private String executeCommand(String command) {
        try {
            ProcessBuilder pb = new ProcessBuilder("sh", "-c", command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new FortressOperationException("Command timed out: " + command);
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new FortressOperationException(
                        "Command failed with exit code " + exitCode + ": " + output);
            }

            return output.toString();
        } catch (FortressOperationException e) {
            throw e;
        } catch (Exception e) {
            throw new FortressOperationException("Command execution failed: " + e.getMessage(), e);
        }
    }
}
