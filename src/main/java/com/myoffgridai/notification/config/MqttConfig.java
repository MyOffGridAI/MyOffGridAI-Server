package com.myoffgridai.notification.config;

import com.myoffgridai.config.AppConstants;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for the Eclipse Paho MQTT client.
 *
 * <p>The client connects to the local Mosquitto broker on startup. Connection
 * is conditional on {@code app.mqtt.enabled=true} so that the test profile can
 * disable it without requiring a running broker.</p>
 *
 * <p>If the broker is unreachable at startup, a WARN is logged and the
 * application continues without MQTT support. Notifications are still
 * persisted to the database — MQTT push is best-effort.</p>
 */
@Configuration
@ConditionalOnProperty(name = "app.mqtt.enabled", havingValue = "true", matchIfMissing = false)
public class MqttConfig {

    private static final Logger log = LoggerFactory.getLogger(MqttConfig.class);

    /**
     * Creates and connects an MQTT client to the configured broker.
     *
     * @param brokerUrl the MQTT broker URL (e.g., {@code tcp://localhost:1883})
     * @param clientId  the MQTT client ID for this server instance
     * @return a connected {@link MqttClient}, or a disconnected client if the broker is unreachable
     * @throws MqttException if client creation fails (not connection)
     */
    @Bean(destroyMethod = "disconnect")
    public MqttClient mqttClient(
            @Value("${app.mqtt.broker-url}") String brokerUrl,
            @Value("${app.mqtt.client-id:" + AppConstants.MQTT_SERVER_CLIENT_ID + "}") String clientId)
            throws MqttException {

        MqttClient client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());

        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        options.setConnectionTimeout(10);
        options.setKeepAliveInterval(60);
        options.setAutomaticReconnect(true);

        try {
            client.connect(options);
            log.info("Connected to MQTT broker at {}", brokerUrl);
        } catch (MqttException e) {
            log.warn("Failed to connect to MQTT broker at {}: {}. " +
                    "MQTT push notifications will be unavailable until reconnection.",
                    brokerUrl, e.getMessage());
        }

        return client;
    }
}
