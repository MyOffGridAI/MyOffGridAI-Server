package com.myoffgridai.privacy.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.myoffgridai.ai.model.Conversation;
import com.myoffgridai.ai.model.Message;
import com.myoffgridai.ai.repository.ConversationRepository;
import com.myoffgridai.ai.repository.MessageRepository;
import com.myoffgridai.memory.model.Memory;
import com.myoffgridai.memory.repository.MemoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Exports all user data as an AES-256-GCM encrypted ZIP archive.
 *
 * <p>The export includes conversations, messages, and memories serialized
 * as JSON. The archive is encrypted using a passphrase-derived key with
 * PBKDF2 key derivation.</p>
 */
@Service
public class DataExportService {

    private static final Logger log = LoggerFactory.getLogger(DataExportService.class);

    private static final String AES_ALGORITHM = "AES/GCM/NoPadding";
    private static final String KEY_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;
    private static final int SALT_LENGTH = 16;
    private static final int KEY_LENGTH = 256;
    private static final int PBKDF2_ITERATIONS = 65536;

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final MemoryRepository memoryRepository;
    private final ObjectMapper objectMapper;

    /**
     * Constructs the data export service.
     *
     * @param conversationRepository the conversation repository
     * @param messageRepository      the message repository
     * @param memoryRepository       the memory repository
     */
    public DataExportService(ConversationRepository conversationRepository,
                             MessageRepository messageRepository,
                             MemoryRepository memoryRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.memoryRepository = memoryRepository;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * Exports all user data as an AES-256-GCM encrypted ZIP archive.
     *
     * <p>The returned byte array layout is:
     * {@code [salt (16 bytes)] [IV (12 bytes)] [encrypted ZIP data]}</p>
     *
     * @param userId     the user whose data to export
     * @param passphrase the passphrase used to derive the encryption key
     * @return the encrypted ZIP archive as a byte array
     */
    public byte[] exportUserData(UUID userId, String passphrase) {
        log.info("Starting data export for user {}", userId);

        try {
            byte[] zipBytes = buildZipArchive(userId);
            byte[] encrypted = encrypt(zipBytes, passphrase);
            log.info("Data export completed for user {}: {} bytes", userId, encrypted.length);
            return encrypted;
        } catch (Exception e) {
            log.error("Data export failed for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Data export failed: " + e.getMessage(), e);
        }
    }

    /**
     * Builds the unencrypted ZIP archive containing all user data as JSON files.
     *
     * @param userId the user ID
     * @return the ZIP archive as a byte array
     */
    private byte[] buildZipArchive(UUID userId) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {

            // Conversations
            List<Conversation> conversations = conversationRepository.findByUserId(userId);
            addJsonEntry(zos, "conversations.json", conversations);

            // Messages (grouped by conversation)
            for (Conversation conv : conversations) {
                List<Message> messages = messageRepository
                        .findByConversationIdOrderByCreatedAtAsc(conv.getId());
                addJsonEntry(zos, "messages/" + conv.getId() + ".json", messages);
            }

            // Memories
            List<Memory> memories = memoryRepository.findByUserId(userId);
            addJsonEntry(zos, "memories.json", memories);

            // Metadata
            Map<String, Object> metadata = Map.of(
                    "exportedAt", java.time.Instant.now().toString(),
                    "userId", userId.toString(),
                    "conversationCount", conversations.size(),
                    "memoryCount", memories.size()
            );
            addJsonEntry(zos, "export-metadata.json", metadata);
        }

        return baos.toByteArray();
    }

    /**
     * Adds a JSON-serialized entry to the ZIP archive.
     *
     * @param zos      the ZIP output stream
     * @param filename the entry filename
     * @param data     the data to serialize
     */
    private void addJsonEntry(ZipOutputStream zos, String filename, Object data) throws Exception {
        zos.putNextEntry(new ZipEntry(filename));
        zos.write(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(data));
        zos.closeEntry();
    }

    /**
     * Encrypts data using AES-256-GCM with a PBKDF2-derived key.
     *
     * @param data       the plaintext data
     * @param passphrase the passphrase
     * @return salt + IV + ciphertext
     */
    private byte[] encrypt(byte[] data, String passphrase) throws Exception {
        SecureRandom random = new SecureRandom();

        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);

        byte[] iv = new byte[GCM_IV_LENGTH];
        random.nextBytes(iv);

        SecretKey key = deriveKey(passphrase, salt);

        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec);

        byte[] ciphertext = cipher.doFinal(data);

        // Combine: salt + IV + ciphertext
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        result.write(salt);
        result.write(iv);
        result.write(ciphertext);

        return result.toByteArray();
    }

    /**
     * Derives an AES-256 key from a passphrase using PBKDF2.
     *
     * @param passphrase the passphrase
     * @param salt       the salt
     * @return the derived secret key
     */
    private SecretKey deriveKey(String passphrase, byte[] salt) throws Exception {
        KeySpec spec = new PBEKeySpec(passphrase.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGORITHM);
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }
}
