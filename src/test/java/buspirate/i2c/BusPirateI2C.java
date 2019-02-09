package buspirate.i2c;

import java.io.*;

import purejavacomm.UnsupportedCommOperationException;

public class BusPirateI2C extends BusPirate {
    public final static byte I2C_MODE = 0b00000010;
	public final static byte I2C_START_BIT_CMD = 0b00000010;
	public final static byte I2C_STOP_BIT_CMD = 0b00000011;
	public final static byte I2C_READ_BYTE_CMD = 0b00000100;
	public final static byte I2C_ACK_BIT_CMD = 0b00000110;
    public final static byte I2C_NACK_BIT_CMD = 0b00000111;
    public final static byte I2C_BULK_I2C_WRITE_CMD = 0b00010000;
    public final static byte I2C_CONFIGURE_PERIPHERALS_CMD = 0b01000000;
    public final static int I2C_POWER_BIT = 3;
    public final static int I2C_PULLUP_BIT = 2;
    public final static int I2C_AUX_BIT = 1;
    public final static int I2C_CS_BIT = 0;
    public final static byte I2C_WRITE_THEN_READ_CMD = 0b00001000;

    private byte peripheralRegisterBuffer = 0x00;

    public BusPirateI2C() throws NoBusPirateFoundException, I2CModeNotSupported {
        super();
        if (!setMode(I2C_MODE, "I2C1")) {
            throw new I2CModeNotSupported();
        }
    }

    /**
     * Use for writing a bus pirate I2C command that just responds with 0x01 success.
     * @param command   The command to call
     * @throws BusPirateCommPortClosedException
     * @throws I2CModeProtocolException
     */
    private void commonCommand(byte command) throws BusPirateCommPortClosedException, I2CModeProtocolException {
        // Send the message to the device and wait synchrounsly for the response
        try {
            if (port == null) {                                             // Port must be open
                throw new BusPirateCommPortClosedException();
            } else {
                byte[] sendBuffer = {command};
                byte[] rcvdBuffer = new byte[100];
                port.enableReceiveTimeout(1000);
                out.write(sendBuffer, 0, sendBuffer.length);
                int count = in.read(rcvdBuffer);
                if (count == 1) {
                    if (rcvdBuffer[0] != 0x01) {
                        throw new I2CModeProtocolException("Unexpected response from command " + String.format("0x%02X ", command));
                    }
                } else {
                    throw new I2CModeProtocolException("Length of response was unexpected.");
                }
            }
        } catch (IOException | UnsupportedCommOperationException e) {
            System.err.println(e);
            throw new I2CModeProtocolException("An exception occurred communicating with bus pirate.");
        }
    }

    /**
     * Sets the peripheral bit.  Attempts to preserve any existing bits set by buffering register value.
     * Note that the bus pirate protocol does not provide the current state of this register.
     * 
     * @param state                                Set to true to turn on peripheral
     * @param peripheralBit                        The peripheral bit to set
     * @throws BusPirateCommPortClosedException    Thrown if the communication port is closed
     * @throws I2CModeProtocolException            Thrown if an unexpected condition is detected communicating with the bus pirate
     */
    public void setPeripheral(boolean state, int peripheralBit) throws BusPirateCommPortClosedException, I2CModeProtocolException {
        // Send the message to the device and wait synchrounsly for the response
        if (state) {
            peripheralRegisterBuffer |= (1 << peripheralBit);     // Twiddle bit on
        } else {
            peripheralRegisterBuffer &= ~(1 << peripheralBit);    // Twiddle bit off while preserving state of other bits
        }
        commonCommand((byte)(I2C_CONFIGURE_PERIPHERALS_CMD | peripheralRegisterBuffer));
    }

    /**
     * Sends start bit to the device.
     * 
     * @throws BusPirateCommPortClosedException    Thrown if the communication port is closed
     * @throws I2CModeProtocolException            Thrown if an unexpected condition is detected communicating with the bus pirate
     */
    protected void sendStartBit() throws BusPirateCommPortClosedException, I2CModeProtocolException {
        commonCommand(I2C_START_BIT_CMD);
    }

    /**
     * Sends stop bit to the device.
     * 
     * @throws BusPirateCommPortClosedException    Thrown if the communication port is closed
     * @throws I2CModeProtocolException            Thrown if an unexpected condition is detected communicating with the bus pirate
     */
    protected void sendStopBit() throws BusPirateCommPortClosedException, I2CModeProtocolException {
        commonCommand(I2C_STOP_BIT_CMD);
    }

    /**
     * Sends ACK bit to the device.
     * 
     * @throws BusPirateCommPortClosedException    Thrown if the communication port is closed
     * @throws I2CModeProtocolException            Thrown if an unexpected condition is detected communicating with the bus pirate
     */
    protected void sendACK() throws BusPirateCommPortClosedException, I2CModeProtocolException {
        commonCommand(I2C_ACK_BIT_CMD);
    }
    
    /**
     * Sends NACK bit to the device.
     * 
     * @throws BusPirateCommPortClosedException    Thrown if the communication port is closed
     * @throws I2CModeProtocolException            Thrown if an unexpected condition is detected communicating with the bus pirate
     */
    protected void sendNACK() throws BusPirateCommPortClosedException, I2CModeProtocolException {
        commonCommand(I2C_NACK_BIT_CMD);
    }

    /**
     * Read byte from the device.
     * 
     * @return                                     The byte read
     * @throws BusPirateCommPortClosedException    Thrown if the communication port is closed
     * @throws I2CModeProtocolException            Thrown if an unexpected condition is detected communicating with the bus pirate
     */
    protected byte readByte() throws BusPirateCommPortClosedException, I2CModeProtocolException {
        try {
            if (port == null) {                                             // Port must be open
                throw new BusPirateCommPortClosedException();
            } else {
                byte[] sendBuffer = {I2C_READ_BYTE_CMD};
                byte[] rcvdBuffer = new byte[100];
                port.enableReceiveTimeout(1000);
                out.write(sendBuffer, 0, sendBuffer.length);
                int count = in.read(rcvdBuffer);
                if (count == 1) {
                    return rcvdBuffer[0];
                } else {
                    throw new I2CModeProtocolException("Unexpected number of bytes received.");
                }
            }
        } catch (IOException | UnsupportedCommOperationException e) {
            System.err.println(e);
            throw new I2CModeProtocolException("An exception occurred communicating with bus pirate.");
        }
    }

    /**
     * Write up to 16 bytes at once to the device.
     * 
     * @param bytesToWrite                          Bytes to write
     * @throws BusPirateCommPortClosedException     Thrown if the communication port is closed
     * @throws I2CModeProtocolException             Thrown if an unexpected condition is detected communicating with the bus pirate
     * @throws IllegalArgumentException             Thrown if number of bytes exceeded
     */
    protected void writeBulk(byte[] bytesToWrite) throws BusPirateCommPortClosedException, I2CModeProtocolException, IllegalArgumentException {
        // Send the message to the device and wait synchrounsly for the response
        try {
            if (port == null) {                                             // Port must be open
                throw new BusPirateCommPortClosedException();
            } else {
                byte[] sendBuffer = {getBulkI2CWriteCommand(bytesToWrite.length)};  // Send the bulk write command to BP
                byte[] rcvdBuffer = new byte[100];
                port.enableReceiveTimeout(1000);
                out.write(sendBuffer, 0, sendBuffer.length);
                int count = in.read(rcvdBuffer);
                if (count == 1) {
                    if (rcvdBuffer[0] == 0x01) {
                        // BP bulk write command succedded...proceed with writing data
                        out.write(bytesToWrite, 0, bytesToWrite.length);
                        count = in.read(rcvdBuffer);
                        if (count != bytesToWrite.length) {
                            throw new I2CModeProtocolException("Length of response writing bytes was unexpected.");
                        }
                    } else {
                        throw new I2CModeProtocolException("Unexpected response writing bytes.");
                    }
                } else {
                    throw new I2CModeProtocolException("Length of response from command was unexpected.");
                }
            }
        } catch (IOException | UnsupportedCommOperationException e) {
            System.err.println(e);
            throw new I2CModeProtocolException("An exception occurred communicating with bus pirate.");
        }
    }

    /**
     * Get the I2C slave address to put on the wire with the read bit set.
     * 
     * @param slaveAddress  The slave address of the device
     * @return              Returned address ready for the wire
     */
    protected static byte getSlaveAddressRead(byte slaveAddress) {
        return (byte)((slaveAddress << 1) + 1);
    }

    /**
     * Get the I2C slave address to put on the wire with the write bit set.
     * 
     * @param slaveAddress  The slave address of the device
     * @return              Returned address ready for the wire
     */
    protected static byte getSlaveAddressWrite(byte slaveAddress) {
        return (byte)((slaveAddress << 1));
    }

    /**
     * Get the I2C bulk write command to put on the wire with number of bytes to write.
     * 
     * @param count         Count of number of bytes to write
     * @return              Returned command ready for the wire
     */
    protected static byte getBulkI2CWriteCommand(int count) {
        if (count > 16) {
            throw new IllegalArgumentException("Bulk write count cannot exceed 16 bytes.");
        }
        if (count < 1) {
            throw new IllegalArgumentException("Bulk write count must write at least one byte.");
        }
        // The count appended to low nibble of the bulk write command is zero based
        byte countNibble = (byte)(count - 1);
        return (byte)(I2C_BULK_I2C_WRITE_CMD | countNibble);
    }

    public static class I2CModeNotSupported extends Exception {
		public I2CModeNotSupported(String message) {
			super(message);
		}
		public I2CModeNotSupported() {
			super();
		}
	}

    public static class I2CModeProtocolException extends Exception {
		public I2CModeProtocolException(String message) {
			super(message);
		}
		public I2CModeProtocolException() {
			super();
		}
	}
}