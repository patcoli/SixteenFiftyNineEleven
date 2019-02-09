package buspirate.i2c;

import purejavacomm.*;
import java.util.*;
import java.io.*;

public class BusPirate {
    protected SerialPort port = null;
    protected InputStream in = null;
    protected OutputStream out = null;
    private static CommPortIdentifier busPiratePortIdentifier = null;
    public final static byte BP_BITBANG = 0b00000000;
    public final static byte BP_SET_MODE = 0b00000010;
    public final static byte BP_GET_MODE = 0b000000001;
    public final static byte BP_RESET = 0b00001111;

    public BusPirate() throws NoBusPirateFoundException {
        // Find a bus pirate plugged into a comm port
        Boolean bbReady = false;

        if (busPiratePortIdentifier == null) {
            busPiratePortIdentifier = findBusPiratePort();
            if (busPiratePortIdentifier != null) {
                bbReady = true;
            }
        } else {
            System.out.println("Scanning port: " + busPiratePortIdentifier.getName());
            try {
                openPort(busPiratePortIdentifier);
                if (bitBangMode()) {
                    bbReady = true;
                    System.out.println("BusPirate found, port: " + busPiratePortIdentifier.getName());
                } else {
                    closePort();
                    busPiratePortIdentifier = findBusPiratePort();
                    if (busPiratePortIdentifier != null) {
                        bbReady = true;
                    }
                }
            } catch(PortInUseException ex) {
                System.err.println("Port already in use: " + ex);
            } catch(IOException ex) {
                System.err.println("IO exception testing port: " + ex);
            } catch(InterruptedException ex) {
                System.err.println("Interrupted exception testing port: " + ex);
            } catch(UnsupportedCommOperationException ex) {
                System.err.println("Unsupported comm operation testing port: " + ex);
            }
        }
        if (!bbReady) {
            throw new NoBusPirateFoundException();
        }
    }

    private CommPortIdentifier findBusPiratePort() {
        Enumeration<CommPortIdentifier> e = CommPortIdentifier.getPortIdentifiers();
        while(e.hasMoreElements()) {
            CommPortIdentifier commPortIdentifier = e.nextElement();
            System.out.println("Scanning port: " + commPortIdentifier.getName());
            try {
                openPort(commPortIdentifier);
                if (bitBangMode()) {
                    System.out.println("BusPirate found, port: " + commPortIdentifier.getName());
                    return commPortIdentifier;
                } else {
                    closePort();
                }
            } catch(PortInUseException ex) {
                System.err.println("Port already in use: " + ex);
            } catch(IOException ex) {
                System.err.println("IO exception testing port: " + ex);
            } catch(InterruptedException ex) {
                System.err.println("Interrupted exception testing port: " + ex);
            } catch(UnsupportedCommOperationException ex) {
                System.err.println("Unsupported comm operation testing port: " + ex);
            }
        }
        return null;
    }

    /**
     * Open up a comm port so this instance can attempt communication with a bus pirate.
     * @param commPortIdentifier    The comm port identifier to open
     * @throws PortInUseException
     * @throws UnsupportedCommOperationException
     * @throws IOException
     * @throws InterruptedException
     */
    protected void openPort(CommPortIdentifier commPortIdentifier) throws PortInUseException, UnsupportedCommOperationException, IOException, InterruptedException {
        port = (SerialPort) commPortIdentifier.open(
            "BusPirate",    // Name of the application asking for the port 
            2000            // Wait max. 2 sec. to acquire port
        );
        port.setSerialPortParams(115200, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
        out = port.getOutputStream();
        in = port.getInputStream();
        drain();
    }

    /**
     * Closes a comm port if it was opened bt the instance.  This should be performed prior to letting
     * this instance get teed up for garbage collection, as the port may remain open until then.
     */
    public void closePort() {
		if (port != null) {
			try {
                out.flush();
				port.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			finally {
                port = null;
                in = null;
                out = null;
			}
		}
    }
    
    /**
     * Drains any cruft sitting in the bus pirate serial port receive buffer.
     * 
     * @throws InterruptedException
     * @throws IOException
     */
    protected void drain() throws InterruptedException, IOException {
		Thread.sleep(10);
		int n;
		while ((n = in.available()) > 0) {
			for (int i = 0; i < n; ++i)
				in.read();
			Thread.sleep(10);
		}
    }

    /**
     * Set the bus pirate into raw bitbang mode.
     * 
     * @return  True if bus pirate confirms bitbang mode; false otherwise
     */
    protected boolean bitBangMode() {
        int tries = 0;
        byte[] sent = new byte[100];
        byte[] rcvd = new byte[100];

        try {
            if (port == null) {
                return false;
            } else {
                port.enableReceiveTimeout(100);
                while(true) {
                    sent[0] = BP_BITBANG;
                    out.write(sent, 0, 1);
                    tries++;
                    Thread.sleep(10);
                    int count = in.read(rcvd, 0, 5);
                    if (count != 5 && tries > 20) {
                        return false;
                    } else if ((new String(rcvd, 0, 5, "US-ASCII")).contains("BBIO1")) {
                        return true;
                    }

                    if (tries > 25) {
                        return false;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println(e);
            return false;
        }
    }

    /**
     * Performs a complete hardware reset of the bus pirate and returns the 
     * device to the user terminal interface.  Serial port may have version
     * string and garbage after reset.  Drain port before re-entering bitbang
     * mode.
     * 
     * @return  True if bus pirate reports reset confirmation; false otherwise
     */
    public boolean reset() {
        byte[] sent = new byte[100];
        byte[] rcvd = new byte[100];

        try {
            if (port == null) {
                return false;
            } else {
                setMode(BP_BITBANG, "BBIO1");           // Must be in raw bitbang mode to send reset command
                port.enableReceiveTimeout(1000);
                sent[0] = BP_RESET;
                out.write(sent, 0, 1);
                int count = in.read(rcvd, 0, 1);
                if (count > 0) {                        // Responds with at least one byte with a one in it...may have other jibberish if it resets too quickly
                    if (rcvd[0] == 0x01) {
                        return true;
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            }
        } catch (Exception e) {
            System.err.println(e);
            return false;
        }
    }

    /**
     * Sets the binary bitbang protocol mode that the bus pirate should switch into
     * 
     * @param mode              The mode as documented <a href="http://dangerousprototypes.com/docs/Bitbang">here</a>
     * @param expectedReply     The string that should be returned from the bus pirate to confirm mode switch
     * @return                  True if the bus pirate mode was set successfully; false otherwise
     */
    protected boolean setMode(byte mode, String expectedReply) {
        byte[] sent = new byte[100];
        byte[] rcvd = new byte[100];

        try {
            if (port == null) {
                return false;
            } else {
                port.enableReceiveTimeout(1000);
                sent[0] = mode;
                out.write(sent, 0, 1);
                Thread.sleep(10);
                int count = in.read(rcvd, 0, expectedReply.length());
                if (count == expectedReply.length()) {
                    return isModeString(rcvd, expectedReply);
                } else {
                    return false;
                }
            }
        } catch (Exception e) {
            System.err.println(e);
            return false;
        }
    }

    /**
     * Gets the binary bitbang protocol mode that the bus pirate is responding to
     * 
     * @param expectedReply     The expected mode string reply as documented <a href="http://dangerousprototypes.com/docs/Bitbang">here</a>
     * @return                  True is the mode matches the sent string
     */
    protected boolean confirmMode(String expectedReply) {
        byte[] sent = new byte[100];
        byte[] rcvd = new byte[100];

        try {
            if (port == null) {
                return false;
            } else {
                port.enableReceiveTimeout(1000);
                sent[0] = BP_GET_MODE;
                out.write(sent, 0, 1);
                Thread.sleep(10);
                int count = in.read(rcvd, 0, expectedReply.length());
                if (count == expectedReply.length()) {
                    return isModeString(rcvd, expectedReply);
                } else {
                    return false;
                }
            }
        } catch (Exception e) {
            System.err.println(e);
            return false;
        }
    }

    private boolean isModeString(byte[] bufferToTest, String expectedReply) throws UnsupportedEncodingException {
        if (bufferToTest.length < expectedReply.length()) {
            return false;
        } else {
            if ((new String(bufferToTest, 0, expectedReply.length(), "US-ASCII")).contains(expectedReply)) {
                return true;
            } else {
                return false;
            }
        }
    }

    public static class NoBusPirateFoundException extends Exception {
		public NoBusPirateFoundException(String message) {
			super(message);
		}
		public NoBusPirateFoundException() {
			super();
		}
	}

    public static class BusPirateCommPortClosedException extends Exception {
		public BusPirateCommPortClosedException(String message) {
			super(message);
		}
		public BusPirateCommPortClosedException() {
			super();
		}
	}
}