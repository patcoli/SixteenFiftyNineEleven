package buspirate.i2c;

public class BusPirateI2CPixy2 extends BusPirateI2C {

    public BusPirateI2CPixy2() throws NoBusPirateFoundException, I2CModeNotSupported, BusPirateCommPortClosedException, I2CModeProtocolException {
        super();
        setPeripheral(true, I2C_POWER_BIT);
    }

    /**
     * Reads one or more bytes using the bus pirate I2C mode and TCS34725 read protocol
     * as defined by the <a href="https://cdn-shop.adafruit.com/datasheets/TCS34725.pdf">TCS34725 Datasheet</a> and
     * the <a href="http://dangerousprototypes.com/docs/I2C_(binary)">Bus Pirate I2C binary mode</a>.
     * Reads at position last defined by the contents of the command register.
     * 
     * @param slaveAddress  The slave address of the device to communicate with
     * @param bytesRead     The data bytes read; buffer must be pre-allocated
     * @param length        The number of bytes to read
     * @return              True if successful; false if not successful
     */
    public boolean read(byte slaveAddress, byte[] bytesRead, int length) {
        if (length < 1) {
            throw new IllegalArgumentException("Length must be 1 or greater");
        }
        if (bytesRead.length < (length)) {
            throw new IllegalArgumentException("bytesRead buffer must be at least greater than length.");
        }
        // Build up the message as per page 12 of the spec
        try {
            sendStartBit();
            writeBulk(new byte[] {getSlaveAddressRead(slaveAddress)});
            for(int i = 0; i < length; i++) {
                bytesRead[i] = readByte();
                if (i == (length - 1)) {
                    sendNACK();
                } else {
                    sendACK();
                }
            }
            sendStopBit();
            return true;
        } catch (IllegalArgumentException e) {
            throw e;                                                                    // Rethrow as this is a usage error 
        } catch (Exception e) {
            System.err.println(e);
            return false;
        }
    }

    /**
     * Writes one or more bytes using the bus pirate I2C mode and TCS34725 write protocol
     * as defined by the <a href="https://cdn-shop.adafruit.com/datasheets/TCS34725.pdf">TCS34725 Datasheet</a> and
     * the <a href="http://dangerousprototypes.com/docs/I2C_(binary)">Bus Pirate I2C binary mode</a>.
     * Writes at position last defined by the contents of the command register.
     * 
     * @param slaveAddress  The slave address of the device to communicate with
     * @param commandCode   The command code of the write (usually a register)
     * @param bytesToWrite  The buffer with data to write
     * @return              True if successful; false if not successful
     */
    public boolean write(byte slaveAddress, byte commandCode, byte[] bytesToWrite) {
        if (bytesToWrite.length < 1) {
            throw new IllegalArgumentException("Length of buffer must be 1 or greater");
        }
        // Build up the message as per page 12 of the spec
        try {
            sendStartBit();
            writeBulk(new byte[] {getSlaveAddressWrite(slaveAddress), commandCode});
            for(int i = 0; i < bytesToWrite.length; i++) {
                writeBulk(new byte[] {bytesToWrite[i]});                                // Bulk BP command could be used, but we'd have to break up buffer in 16 byte chunks...too lazy
            }
            sendStopBit();
            return true;
        } catch (IllegalArgumentException e) {
            throw e;                                                                    // Rethrow as this is a usage error 
        } catch (Exception e) {
            System.err.println(e);
            return false;
        }
    }

    /**
     * Writes a command and the reads one or more bytes using the bus pirate I2C mode and TCS34725 combined read protocol
     * as defined by the <a href="https://cdn-shop.adafruit.com/datasheets/TCS34725.pdf">TCS34725 Datasheet</a> and
     * the <a href="http://dangerousprototypes.com/docs/I2C_(binary)">Bus Pirate I2C binary mode</a>.
     * 
     * @param slaveAddress  The slave address of the device to communicate with
     * @param commandCode   The command code of the write (usually a register)
     * @param byteBuffer    The return buffer of data read
     * @param length        The number of bytes to read
     * @return              True if successful; false if not successful
     */
    public boolean readCombined(byte slaveAddress, byte commandCode, byte[] bytesRead, int length) {
        if (length < 1) {
            throw new IllegalArgumentException("Length must be 1 or greater");
        }
        if (bytesRead.length < (length)) {
            throw new IllegalArgumentException("bytesRead buffer must be at least greater than length.");
        }
        // Build up the message as per page 12 of the spec
        try {
            sendStartBit();
            writeBulk(new byte[] {getSlaveAddressWrite(slaveAddress), commandCode});    // Assumption is command bit is already set
            sendStartBit();
            writeBulk(new byte[] {getSlaveAddressRead(slaveAddress)});
            for(int i = 0; i < length; i++) {
                bytesRead[i] = readByte();
                if (i == (length - 1)) {
                    sendNACK();
                } else {
                    sendACK();
                }
            }
            sendStopBit();
            return true;
        } catch (IllegalArgumentException e) {
            throw e;                                                                    // Rethrow as this is a usage error 
        } catch (Exception e) {
            System.err.println(e);
            return false;
        }
    }
}