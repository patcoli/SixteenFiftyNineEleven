package pseudoresonance.links;

import buspirate.i2c.*;
import buspirate.i2c.BusPirate.*;
import buspirate.i2c.BusPirateI2C.I2CModeNotSupported;
import buspirate.i2c.BusPirateI2C.I2CModeProtocolException;
//import pseudoresonance.pixy2api.*;
import pseudoresonance.pixy2api.Pixy2.Checksum;
import edu.wpi.first.wpilibj.*;
import java.util.Arrays;

public class I2CBSLink extends I2CLink {
    public final static int PIXY_I2C_DEFAULT_ADDR = 0x54;
	private final static int PIXY_I2C_MAX_SEND = 16; // don't send any more than 16 bytes at a time

    private BusPirateI2CPixy2 bspI2C = null;
	I2C i2c = null;

	/**
	 * Opens I2C port
	 *
	 * @param arg I2C port
	 * 
	 * @return Returns 0
	 */
    public int open(int arg) {
    try {
            try {
                setBspI2C(new BusPirateI2CPixy2());
            } catch (I2CModeNotSupported | BusPirateCommPortClosedException | I2CModeProtocolException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
			}
    } catch (NoBusPirateFoundException e) {
    System.out.println("No bus pirate found.");

}
        return 0;
    }

    /**
	 * @return the bspI2C
	 */
	public BusPirateI2CPixy2 getBspI2C() {
		return bspI2C;
	}

	/**
	 * @param bspI2C the bspI2C to set
	 */
	public void setBspI2C(BusPirateI2CPixy2 bspI2C) {
		this.bspI2C = bspI2C;
	}

	public void close() {
    //	i2c.close();
    if (getBspI2C() != null) {
            try {
                getBspI2C().setPeripheral(false, BusPirateI2C.I2C_POWER_BIT);
            } catch (BusPirateCommPortClosedException | I2CModeProtocolException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
		}
        getBspI2C().reset();
        getBspI2C().closePort();
        setBspI2C(null);

    }

    }
    
    /**
	 * Receives and reads specified length of bytes from I2C
	 *
	 * @param buffer Byte buffer to return value
	 * @param length Length of value to read
	 * @param cs     Checksum
	 * 
	 * @return Length of value read
	 */
	public int receive(byte[] buffer, int length, Checksum cs) {
		int i, n;
		if (cs != null)
			cs.reset();
		for (i = 0; i < length; i += n) {
			// n is the number read -- it most likely won't be equal to length
			n = 0;
			byte[] read = new byte[length - i];
            //i2c.transaction(new byte[0], (byte) 0, read, (length - i));
           // bspI2C.readCombined(PIXY_I2C_DEFAULT_ADDR,  (byte)((int) read , bytesRead, length)
           getBspI2C().read((byte) PIXY_I2C_DEFAULT_ADDR, read, (length - i));

			for (int k = 0; k < read.length; k++) {
				n++;
				byte b = read[k];
				if (cs != null) {
					int csb = b & 0xff;
					cs.updateChecksum(csb);
				}
				buffer[k + i] = b;
			}
		}
		return length;
	}

	/**
	 * Receives and reads specified length of bytes from I2C
	 *
	 * @param buffer Byte buffer to return value
	 * @param length Length of value to read
	 * 
	 * @return Length of value read
	 */
	public int receive(byte[] buffer, int length) {
		return receive(buffer, length, null);
	}

	/**
	 * Writes and sends buffer over I2C
	 *
	 * @param buffer Byte buffer to send
	 * @param length Length of value to send
	 * 
	 * @return Length of value sent
	 */
	public int send(byte[] buffer, int length) {
        int i, packet;
       // Byte[] read = new Byte[PIXY_I2C_MAX_SEND];
		for (i = 0; i < length; i += PIXY_I2C_MAX_SEND) {
			if (length - i < PIXY_I2C_MAX_SEND)
				packet = (length - i);
			else
				packet = PIXY_I2C_MAX_SEND;
            byte[] send = Arrays.copyOfRange(buffer, i, packet);

            getBspI2C().write((byte) PIXY_I2C_DEFAULT_ADDR, BusPirateI2C.I2C_WRITE_THEN_READ_CMD, send);
		
        }
            //i2c.transaction(send, packet, new byte[0], 0);
          
		return length;
	}
}







