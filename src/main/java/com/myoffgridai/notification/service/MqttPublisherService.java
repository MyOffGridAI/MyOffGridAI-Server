package com.myoffgridai.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myoffgridai.config.AppConstants;
import com.myoffgridai.notification.dto.NotificationPayload;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

/**
 * Publishes push notification payloads to the local Mosquitto MQTT broker.
 *
 * <p>Each user device subscribes to its own topic. This service constructs
 * the topic string and publishes a JSON payload. If the broker is unavailable
 * or MQTT is disabled, publication is silently skipped with a log warning —
 * MQTT is best-effort; the notification is still persisted to the database.</p>
 */
@Service
public class MqttPublisherService {

    private static final Logger log = LoggerFactory.getLogger(MqttPublisherService.class);

    private final MqttClient mqttClient;
    private final ObjectMapper objectMapper;

    /**
     * Constructs the MQTT publisher service.
     *
     * @param mqttClient   the MQTT client bean (nullable when mqtt.enabled=false)
     * @param objectMapper the Jackson object mapper for JSON serialization
     */
    public MqttPublisherService(@Nullable MqttClient mqttClient,
                                ObjectMapper objectMapper) {
        this.mqttClient = mqttClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Publishes a notification to a specific MQTT topic.
     *
     * @param topic   the MQTT topic string
     * @param payload the notification payload to serialize and publish
     * @return true if the message was published successfully, false otherwise
     */
    public boolean publishToTopic(String topic, NotificationPayload payload) {
        if (mqttClient == null || !mqttClient.isConnected()) {
            log.warn("MQTT client unavailable — skipping publish to topic: {}", topic);
            return false;
        }

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(payload);
            MqttMessage message = new MqttMessage(bytes);
            message.setQos(AppConstants.MQTT_QOS);
            message.setRetained(false);
            mqttClient.publish(topic, message);
            log.debug("Published notification to MQTT topic: {}", topic);
            return true;
        } catch (Exception e) {
            log.warn("Failed to publish to MQTT topic {}: {}", topic, e.getMessage());
            return false;
        }
    }

    /**
     * Publishes a notification to a specific user's device topic.
     *
     * @param userId  the user ID used to construct the topic
     * @param payload the notification payload
     * @return true if published successfully
     */
    public boolean publishToUser(String userId, NotificationPayload payload) {
        String topic = String.format(AppConstants.MQTT_TOPIC_USER_NOTIFICATIONS, userId);
        return publishToTopic(topic, payload);
    }

    /**
     * Publishes a notification to all connected devices (broadcast).
     *
     * @param payload the notification payload
     * @return true if published successfully
     */
    public boolean publishBroadcast(NotificationPayload payload) {
        return publishToTopic(AppConstants.MQTT_TOPIC_BROADCAST, payload);
    }
}
