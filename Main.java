package main;

import java.io.*;
import java.math.BigInteger;

import tupleList.TupleList;
import tupleList.Tuple; 
 
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.interfaces.ECPrivateKey;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPrivateKeySpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.math.ec.ECPoint;
 
import java.security.KeyFactory; 
import java.security.NoSuchAlgorithmException; 
import java.security.PrivateKey; 
import java.security.PublicKey; 
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;
import org.bouncycastle.jce.provider.BouncyCastleProvider; 
  
 

public class Main {
 
    private static final String EC_GEN_PARAM_SPEC = "secp256k1";
    private static final String KEY_PAIR_GEN_ALGORITHM = "ECDSA"; 
  
    public static String generateRawBitcoinAddress(ECPublicKey ecPublicKey) {
        ECPoint q = ecPublicKey.getQ();
        BigInteger affineXCoord = q.getAffineXCoord().toBigInteger();
        BigInteger affineYCoord = q.getAffineYCoord().toBigInteger();
        return String.format("%064x", affineXCoord) + String.format("%064x", affineYCoord);
    }
 
 public static void main(String[] args) {
	 
	 

	Security.addProvider(new BouncyCastleProvider());
	Security.getProviders(); 
	 
        System.out.println("STARTING PROGRAmM...");
        KeyFactory keyFactory=null;
   	 try {
   	   	  keyFactory = KeyFactory.getInstance("ECDSA");
   	 }
   		catch (NoSuchAlgorithmException e) {

   		 System.out.println("NO SUCH ALGORITHM EXCEPTION"); 
   			   System.exit(0);	

   		}
     
     
        String testSeed;
        Seed mySeed = new Seed();
        testSeed= mySeed.generateSeed();
        System.out.println(testSeed);
        
        testSeed="raw program because index dutch current minute leaf analyst conduct reject nephew";
         System.out.println("ALTERNATE SEED FOR DEBUGGING");
         System.out.println(testSeed);
        
        
        byte[] bip32root = MasterKeys.get_seed_from_mnemonic(testSeed);
        byte[] root512 = Bitcoin.get_root512_from_bip32root(bip32root);
       System.out.println("root 512 is "+Bitcoin.bytesToHex(root512));
        byte[] kBytes = Arrays.copyOfRange(root512, 0, 32);
        byte[] cBytes = Arrays.copyOfRange(root512, 32, 64);
        String jonaldPubKey = Bitcoin.get_pubkey_from_secret(kBytes);
        System.out.println("pubkey");
       System.out.println(jonaldPubKey);
       String serializedXprv = Bitcoin.serializeXprv(cBytes,  kBytes);
        byte[] cKbytes=Bitcoin.hexStringToByteArray(jonaldPubKey);
        String serializedXpub=Bitcoin.serializeXpub(cBytes, cKbytes );
        
          
        String final_xprv= Bitcoin.base_encode_58(serializedXprv);
       System.out.println ("final_xprv");
        System.out.println(final_xprv);
        
          
        String final_xpub= Bitcoin.base_encode_58(serializedXpub);
       System.out.println ("final_xpub");
        System.out.println(final_xpub);
        
        // Clean up comments and variable names before proceeding
        
        
        String deserializedXprvPieces[] = Bitcoin.deserialize_xkey(final_xprv, true);
        String xprv_c=deserializedXprvPieces[4];
        String xprv_k=deserializedXprvPieces[5];
        
        System.out.println("deserializedXprvPieces c "+ xprv_c );

        System.out.println("deserializedXprvPieces k "+ xprv_k );

        //String [] testmonk = _CKD_pub();
        
        
        
        //DAA.daa_test();
       
       // String deserialized_xpub=Bitcoin.base_decode_58(final_xpub);
        String deserialized_xpub[] =Bitcoin.deserialize_xkey(final_xpub,false);
        System.out.println("---------");
      //  System.out.println(deserialized_xpub[0]);

       // System.out.println(deserialized_xpub[1]);

       // System.out.println(deserialized_xpub[2]);

       // System.out.println(deserialized_xpub[3]);
        String my_c=deserialized_xpub[4];
        String my_cK=deserialized_xpub[5];
        
        System.out.println(deserialized_xpub[4]);

        System.out.println("AAA");
        System.out.println(deserialized_xpub[5]);
         
        String[] mytest9 = Bitcoin._CKD_priv(xprv_k,xprv_c,"00000000",false);
        
       
        System.out.println("----");
        System.out.println("mytest9 "+mytest9[0]);
         
        String [] test11=Bitcoin._CKD_pub(my_cK, my_c, "00000000");


         
         // THIS IS WHERE WE 
       
       //MULTITHREAD
       //Runner runner1= new Runner();

       //Runner runner2= new Runner();
       //runner1.start();
       //runner2.start();
       
    } // end main func
 
 
 
 
} // end class
		
		
		 
 
 