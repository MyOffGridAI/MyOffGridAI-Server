package com.myoffgridai.sensors.service;

import com.fazecast.jSerialComm.SerialPort;
import com.myoffgridai.common.exception.SensorConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Manages physical serial port connections via jSerialComm.
 * Provides a clean interface for opening, reading, and closing serial ports.
 */
@Service
public class SerialPortService {

    private static final Logger log = LoggerFactory.getLogger(SerialPortService.class);

    /**
     * Lists all available serial ports on the device.
     *
     * @return list of port descriptor strings, empty if none found
     */
    public List<String> listAvailablePorts() {
        SerialPort[] ports = SerialPort.getCommPorts();
        List<String> result = new ArrayList<>();
        for (SerialPort port : ports) {
            result.add(port.getSystemPortPath());
        }
        log.debug("Found {} available serial ports", result.size());
        return result;
    }

    /**
     * Opens a serial port at the given path and baud rate.
     *
     * @param portPath the system port path (e.g. "/dev/ttyUSB0")
     * @param baudRate the baud rate
     * @return the opened serial port
     * @throws SensorConnectionException if the port cannot be found or opened
     */
    public SerialPort openPort(String portPath, int baudRate) {
        SerialPort port = SerialPort.getCommPort(portPath);
        port.setBaudRate(baudRate);
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 1000, 0);

        if (!port.openPort()) {
            throw new SensorConnectionException(
                    "Cannot open serial port: " + portPath + " at baud rate " + baudRate);
        }

        log.info("Opened serial port {} at {} baud", portPath, baudRate);
        return port;
    }

    /**
     * Closes a serial port. Logs a warning if already closed, never throws.
     *
     * @param port the serial port to close
     */
    public void closePort(SerialPort port) {
        if (port == null) return;
        if (!port.isOpen()) {
            log.warn("Attempted to close already-closed port: {}", port.getSystemPortPath());
            return;
        }
        port.closePort();
        log.info("Closed serial port: {}", port.getSystemPortPath());
    }

    /**
     * Reads bytes from the serial port until a newline character or timeout.
     *
     * @param port the open serial port
     * @return the trimmed line, or empty if timeout with no data
     */
    public Optional<String> readLine(SerialPort port) {
        try {
            InputStream in = port.getInputStream();
            StringBuilder sb = new StringBuilder();
            int b;
            while ((b = in.read()) != -1) {
                if (b == '\n') break;
                if (b != '\r') sb.append((char) b);
            }
            String line = sb.toString().trim();
            return line.isEmpty() ? Optional.empty() : Optional.of(line);
        } catch (Exception e) {
            log.debug("Read error on port {}: {}", port.getSystemPortPath(), e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Tests a serial port connection by attempting to read one line within the timeout.
     *
     * @param portPath  the port path to test
     * @param baudRate  the baud rate
     * @param timeoutMs the maximum time to wait for data
     * @return true if a non-empty line was received, false otherwise
     */
    public boolean testConnection(String portPath, int baudRate, int timeoutMs) {
        SerialPort port = null;
        try {
            port = openPort(portPath, baudRate);
            port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, timeoutMs, 0);
            Optional<String> line = readLine(port);
            return line.isPresent();
        } catch (Exception e) {
            log.debug("Test connection failed for {}: {}", portPath, e.getMessage());
            return false;
        } finally {
            closePort(port);
        }
    }

    /**
     * Tests a serial port connection and returns the raw sample if successful.
     *
     * @param portPath  the port path to test
     * @param baudRate  the baud rate
     * @param timeoutMs the maximum time to wait for data
     * @return the raw sample line, or null if no data received
     */
    public String testConnectionWithSample(String portPath, int baudRate, int timeoutMs) {
        SerialPort port = null;
        try {
            port = openPort(portPath, baudRate);
            port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, timeoutMs, 0);
            return readLine(port).orElse(null);
        } catch (Exception e) {
            log.debug("Test connection failed for {}: {}", portPath, e.getMessage());
            return null;
        } finally {
            closePort(port);
        }
    }
}
