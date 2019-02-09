
package buspirate.i2c;

import purejavacomm.UnsupportedCommOperationException;

import java.nio.ByteBuffer;

import com.google.common.util.concurrent.Futures;


import com.google.common.util.concurrent.Futures;


import com.google.common.util.concurrent.Futures;

import buspirate.i2c.BusPirate.BusPirateCommPortClosedException;
import buspirate.i2c.BusPirate.NoBusPirateFoundException;
import buspirate.i2c.BusPirateI2C.I2CModeNotSupported;
import buspirate.i2c.BusPirateI2C.I2CModeProtocolException;
import pseudoresonance.pixy2api.*;
import pseudoresonance.links.I2CBSLink;
import pseudoresonance.links.I2CLink;



public class BusPirateMain {
    


public BusPirateMain(){

} 
public  static void main(String[] args) {
try {
  //  BusPirateMain.init();

  I2CBSLink i2c = new I2CBSLink();

  Pixy2 pixy = Pixy2.createInstance(i2c);
  pixy.init();
  pixy.getVersion();
 
  System.out.printf("0x%08x\n",i2c.getBspI2C().getSlaveAddressRead((byte) I2CBSLink.PIXY_I2C_DEFAULT_ADDR));
  System.out.printf("0x%08x\n",i2c.getBspI2C().getSlaveAddressWrite((byte) I2CBSLink.PIXY_I2C_DEFAULT_ADDR));

  System.out.printf("0x%08x\n",(byte)I2CLink.PIXY_I2C_DEFAULT_ADDR);
  pixy.getVersionInfo().print();

  for(int i=0; i < 50; i++){
            int lcnt = pixy.getLine().getAllFeatures();

            
        
            if (lcnt > 0){
           for ( Pixy2Line.Vector vec : pixy.getLine().getVectors()){
            System.out.print( " Found "+ i + " of 50  :");
                vec.print();
           }
        }
        Thread.sleep(100);
    }

    }

catch (Exception e) {
    //TODO: handle exception
    e.printStackTrace();
}
   
    
}
public static void init() throws I2CModeNotSupported, BusPirateCommPortClosedException, I2CModeProtocolException {
    try {
        BusPirateI2CPixy2 busPirateI2C = new BusPirateI2CPixy2();
        Thread.sleep(1000);
        System.out.printf("0x%08x\n",busPirateI2C.getSlaveAddressRead((byte)I2CLink.PIXY_I2C_DEFAULT_ADDR));
        System.out.printf("0x%08x\n",busPirateI2C.getSlaveAddressWrite((byte)I2CLink.PIXY_I2C_DEFAULT_ADDR));

        System.out.printf("0x%08x\n",(byte)I2CLink.PIXY_I2C_DEFAULT_ADDR);

        Thread.sleep(100);
            Byte[] sendBuffer = new Byte[32];
            byte[] readBuffer = new byte[32];
      // java.nio.ByteBuffer rcvdBuffer = ByteBuffer.allocate(32);
            boolean b = false;
            //busPirateI2C.readCombined((byte) I2CLink.PIXY_I2C_DEFAULT_ADDR, Pixy2.PIXY_TYPE_REQUEST_VERSION,rcvdBuffer, rcvdBuffer.length);

        //         b =   busPirateI2C.write((byte) I2CLink.PIXY_I2C_DEFAULT_ADDR, Pixy2.PIXY_TYPE_REQUEST_VERSION,sendBuffer);
                      
                 Thread.sleep(100);

            if (b)
       {
             busPirateI2C.read(BusPirateI2C.getSlaveAddressRead((byte)I2CLink.PIXY_I2C_DEFAULT_ADDR), readBuffer, readBuffer.length);
        for (int i=1; i < sendBuffer.length; i++ ) {
            System.out.printf("%hhu: 0x%08x\n", i, sendBuffer[i]); 
        }
        
       } else {
            System.out.println("Don't know");

       }

       busPirateI2C.setPeripheral(false, BusPirateI2C.I2C_POWER_BIT);
       busPirateI2C .reset();
       busPirateI2C .closePort();
       busPirateI2C  = null;
        
    } catch(NoBusPirateFoundException e) {
        System.out.println("No bus pirate found.");
    } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
}


}

