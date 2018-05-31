package main.electron;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Base58{

  static String b58chars = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
  static BigInteger base = BigInteger.valueOf(58);

  public static byte[] doubleSHA256 (byte[] income){
    MessageDigest md = null;
    try {
			md = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			System.out.println("NO SUCH ALGORITHM EXCEPTION");
			System.exit(0);
		}
    return md.digest(md.digest(income));
  }

  public static byte[] decode (String txt){
    BigInteger value = BigInteger.ZERO;
    for(int i=0; i< txt.length();i++){
      value = value.multiply(base).add(BigInteger.valueOf(b58chars.indexOf(txt.charAt(i))));
    }

    int count = 0;
    for(char c:txt.toCharArray()){
      if (c!='1') {
        break;}
      else { count++;}
    }

    byte[] postBuffer = value.toByteArray();
    if (postBuffer[0] ==( byte) 0){
      count--;
    }
    byte[] result = new byte[count + postBuffer.length];
    for(int i = 0; i < result.length ; i++){
      if (i< count){
        result[i] = (byte)0;

      } else {
        result[i] = postBuffer[i-count];
      }
    }
    return result;
  }

  public static String encode (byte[] buffer){
    BigInteger value = new BigInteger(1, buffer);
    String result = "";
    BigInteger mod;
    while(!value.equals(BigInteger.ZERO)){
      mod = value.mod(base);
      value = value.divide(base);
      result = b58chars.charAt(mod.intValue()) + result;
    }

    for(byte b: buffer){
      if(b != (byte)0) {
        break;
      } else {
      result = "1" + result;
      }
    }

    return result;

  }

  public static byte[] decode_check (String txt) throws Exception {
    byte[] decoded = decode(txt);
    byte[] check = new byte[4];
    byte[] result = new byte[decoded.length - 4];
    for(int i = 0; i < decoded.length; i++){
      if(i < (decoded.length - 4)){
        result[i] = decoded[i];
      } else {
        check[i - (decoded.length - 4)] = decoded[i];
      }
    }
    byte[] doubleSHA = doubleSHA256(result);
    for(int i = 0; i < 4; i++){
      if (doubleSHA[i] != check[i]) {
        throw new Exception();
      }
    }
    return result;
  }

  public static String encode_check(byte[] buffer){
    byte[] doubleSHA = doubleSHA256(buffer);
    byte[] beBytes = new byte[buffer.length + 4];
    for(int i = 0; i < beBytes.length; i++){
      if (i < buffer.length){
        beBytes[i] = buffer[i];
      } else {
        beBytes[i] = doubleSHA[i - (beBytes.length - 4)];
      }
    }
    return encode(beBytes);
  }

}
