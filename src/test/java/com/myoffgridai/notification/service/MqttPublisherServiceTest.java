package com.myoffgridai.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.myoffgridai.config.AppConstants;
import com.myoffgridai.notification.dto.NotificationPayload;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link MqttPublisherService}.
 */
@ExtendWith(MockitoExtension.class)
class MqttPublisherServiceTest {

    @Mock
    private MqttClient mqttClient;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private MqttPublisherService service;

    @BeforeEach
    void setUp() {
        service = new MqttPublisherService(mqttClient, objectMapper);
    }

    @Test
    void publishToTopic_connectedClient_publishes() throws Exception {
        when(mqttClient.isConnected()).thenReturn(true);
        NotificationPayload payload = createPayload();

        boolean result = service.publishToTopic("test/topic", payload);

        assertTrue(result);
        ArgumentCaptor<MqttMessage> captor = ArgumentCaptor.forClass(MqttMessage.class);
        verify(mqttClient).publish(eq("test/topic"), captor.capture());
        assertEquals(AppConstants.MQTT_QOS, captor.getValue().getQos());
        assertFalse(captor.getValue().isRetained());
    }

    @Test
    void publishToTopic_disconnectedClient_returnsFalse() {
        when(mqttClient.isConnected()).thenReturn(false);
        NotificationPayload payload = createPayload();

        boolean result = service.publishToTopic("test/topic", payload);

        assertFalse(result);
    }

    @Test
    void publishToTopic_nullClient_returnsFalse() {
        MqttPublisherService nullService = new MqttPublisherService(null, objectMapper);
        NotificationPayload payload = createPayload();

        boolean result = nullService.publishToTopic("test/topic", payload);

        assertFalse(result);
    }

    @Test
    void publishToTopic_publishFails_returnsFalse() throws Exception {
        when(mqttClient.isConnected()).thenReturn(true);
        doThrow(new MqttException(MqttException.REASON_CODE_CLIENT_EXCEPTION))
                .when(mqttClient).publish(anyString(), any(MqttMessage.class));

        boolean result = service.publishToTopic("test/topic", createPayload());

        assertFalse(result);
    }

    @Test
    void publishToUser_usesCorrectTopic() throws Exception {
        when(mqttClient.isConnected()).thenReturn(true);
        String userId = "abc-123";

        service.publishToUser(userId, createPayload());

        String expectedTopic = String.format(AppConstants.MQTT_TOPIC_USER_NOTIFICATIONS, userId);
        verify(mqttClient).publish(eq(expectedTopic), any(MqttMessage.class));
    }

    @Test
    void publishBroadcast_usesBroadcastTopic() throws Exception {
        when(mqttClient.isConnected()).thenReturn(true);

        service.publishBroadcast(createPayload());

        verify(mqttClient).publish(eq(AppConstants.MQTT_TOPIC_BROADCAST), any(MqttMessage.class));
    }

    @Test
    void publishToTopic_payloadContainsJsonFields() throws Exception {
        when(mqttClient.isConnected()).thenReturn(true);
        NotificationPayload payload = new NotificationPayload(
                "notif-1", "SYSTEM_HEALTH", "Title", "Body", "WARNING",
                Instant.now(), Map.of("key", "value"));

        service.publishToTopic("test/topic", payload);

        ArgumentCaptor<MqttMessage> captor = ArgumentCaptor.forClass(MqttMessage.class);
        verify(mqttClient).publish(anyString(), captor.capture());
        String json = new String(captor.getValue().getPayload());
        assertTrue(json.contains("\"notificationId\":\"notif-1\""));
        assertTrue(json.contains("\"type\":\"SYSTEM_HEALTH\""));
        assertTrue(json.contains("\"severity\":\"WARNING\""));
    }

    private NotificationPayload createPayload() {
        return new NotificationPayload(
                "id-1", "GENERAL", "Test", "Test body", "INFO",
                Instant.now(), null);
    }
}
