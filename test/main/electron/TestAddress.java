package main.electron;

import static org.junit.Assert.*;
import org.junit.Test;

import java.math.BigInteger;
// import main.electron.Base58;
import electrol.java.util.ArrayList;

import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.RIPEMD160Digest;

public class TestAddress{

  @Test
  public void TestFromString(){
    byte LEGACY_FMT = (byte)1;
    byte CASHADDR_FMT = (byte)0;
    String legacyAddress = "15hETetDmcXm1mM4sEf7U2KXC9hDHFMSzz";
    String cashAddress ="bitcoincash:qqehccy89v7ftlfgr9v0zvhjzyy7eatdkqt05lt3nw";
    try {
      Address fromCashaddr = Address.from_string(cashAddress);
      Address fromLegacy = Address.from_string(legacyAddress);
      assertEquals(cashAddress, fromCashaddr.toString(CASHADDR_FMT));
      assertEquals(legacyAddress, fromCashaddr.toString(LEGACY_FMT));
      assertEquals(cashAddress, fromLegacy.toString(CASHADDR_FMT));
      assertEquals(legacyAddress, fromLegacy.toString(LEGACY_FMT));
    } catch(Exception e){
      assertFalse(true);
    }
  }

  @Test
  public void TestFromStrings(){
    String[] addrs = new String[]{"bitcoincash:qqehccy89v7ftlfgr9v0zvhjzyy7eatdkqt05lt3nw","15hETetDmcXm1mM4sEf7U2KXC9hDHFMSzz"};
    ArrayList addresses = new ArrayList();
    try{
      addresses = Address.from_strings(addrs);
    } catch(Exception e){
      assertTrue(false);
    }
    assertEquals(addrs.length, addresses.size());
  }

  @Test
  public void TestFromPubkey(){
      String pubkey = "03655932dba459cdddc62f471788fac3f6f4d19382a023122e6f8da743dbcc7aca";
    String address = "1HdGRAJjzsPZrVrJfkFRMjM3jCib7viZgD";
    try{
      Address restored = Address.from_pubkey(pubkey);
      System.out.println(restored.toString());
    } catch (Exception ex){
      ex.printStackTrace();
      assertFalse(true);
    }
  }

  @Test
  public void TestEquals(){
    String address = "1HdGRAJjzsPZrVrJfkFRMjM3jCib7viZgD";
    try {
      Address firstAddress = Address.from_string(address);
      Address secondAddress = Address.from_string(address);
      assertTrue("Addresses should be equal", firstAddress.equals(secondAddress));
    } catch (Exception ex) {
      assertTrue("EXCEPTION IN A MAIN THREAD", false);
    }

  }

}
