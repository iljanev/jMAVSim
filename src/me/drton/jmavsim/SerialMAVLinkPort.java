package me.drton.jmavsim;

import jssc.SerialPort;
import jssc.SerialPortException;
import me.drton.jmavlib.mavlink.MAVLinkMessage;
import me.drton.jmavlib.mavlink.MAVLinkSchema;
import me.drton.jmavlib.mavlink.MAVLinkStream;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;

/**
 * User: ton Date: 28.11.13 Time: 23:30
 */
public class SerialMAVLinkPort extends MAVLinkPort {
    private MAVLinkSchema schema;
    private SerialPort serialPort;
    private ByteChannel channel = null;
    private MAVLinkStream stream;

    public SerialMAVLinkPort(MAVLinkSchema schema) {
        super(schema);
        this.schema = schema;
    }

    public void open(String portName, int baudRate, int dataBits, int stopBits, int parity) throws IOException {
        serialPort = new SerialPort(portName);
        try {
            serialPort.openPort();
            serialPort.setParams(baudRate, dataBits, stopBits, parity);
        } catch (SerialPortException e) {
            throw new IOException(e);
        }
        channel = new ByteChannel() {
            @Override
            public int read(ByteBuffer buffer) throws IOException {
                try {
                    int available = serialPort.getInputBufferBytesCount();
                    if (available <= 0) {
                        return 0;
                    }
                    byte[] b = serialPort.readBytes(Math.min(available,buffer.remaining()));
                    if (b != null) {
                        buffer.put(b);
                        return b.length;
                    } else {
                        return 0;
                    }
                } catch (SerialPortException e) {
                    throw new IOException(e);
                }
            }

            @Override
            public int write(ByteBuffer buffer) throws IOException {
                try {
                    byte[] b = new byte[buffer.remaining()];
                    buffer.get(b);
                    return serialPort.writeBytes(b) ? b.length : 0;
                } catch (SerialPortException e) {
                    throw new IOException(e);
                }
            }

            @Override
            public boolean isOpen() {
                return serialPort.isOpened();
            }

            @Override
            public void close() throws IOException {
                try {
                    serialPort.closePort();
                } catch (SerialPortException e) {
                    throw new IOException(e);
                }
            }
        };
        stream = new MAVLinkStream(schema);
    }

    @Override
    public void close() throws IOException {
        try {
            serialPort.closePort();
        } catch (SerialPortException e) {
            throw new IOException(e);
        }
        serialPort = null;
    }

    @Override
    public boolean isOpened() {
        return serialPort != null && serialPort.isOpened();
    }

    @Override
    public void handleMessage(MAVLinkMessage msg) {
        if (isOpened()) {
            try {
                stream.write(msg, channel);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void update(long t) {
        MAVLinkMessage msg;
        while (isOpened()) {
            try {
                msg = stream.read(channel);
                if (msg == null) {
                    break;
                }
                sendMessage(msg);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
    }

    public void sendRaw(byte[] data) throws IOException {
        try {
            serialPort.writeBytes(data);
        } catch (SerialPortException e) {
            throw new IOException(e);
        }
    }
}
