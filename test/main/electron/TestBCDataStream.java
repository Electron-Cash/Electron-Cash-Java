package main.electron;

import static org.junit.Assert.*;
import org.junit.Test;

import electrol.java.util.Arrays;
import java.math.BigInteger;

public class TestBCDataStream{

  @Test
  public void Test_001_test_init(){

    boolean exitByException = false;
    String testString = "AAee";
    byte[] testArray ={(byte) 0xAA,(byte) 0xEE};

    try {
      BCDataStream testStream = new BCDataStream(testString);
      assertTrue(Arrays.equals(testArray, testStream.input));
      assertEquals(testStream.cursor, 0);
    } catch (Exception e) {
      exitByException = true;
    }

  }

  @Test
  public void Test_002_test_fail_on_init(){

    boolean exitByException = false;
    String testString = "AAXY";
    try {
      BCDataStream testStream = new BCDataStream(testString);
      for(byte b: testStream.input){
        System.out.println((int) b & 0xFF);
      }
    } catch (Exception e) {
      exitByException = true;
    }
    assertTrue(exitByException);
  }

  @Test
  public void Test_003_test_int16_read(){
      String testString = "ff7f00803930c7cf";
      try {
        BCDataStream testStream = new BCDataStream(testString);
        int val1 = testStream.readInt16();
        int val2 = testStream.readInt16();
        int val3 = testStream.readInt16();
        int val4 = testStream.readInt16();
        assertEquals(val1, 32767);
        assertEquals(val2, -32768);
        assertEquals(val3, 12345);
        assertEquals(val4, -12345);
      } catch (Exception e) {
          assertTrue(false);
      }
  }

  @Test
  public void Test_004_test_uint16_read(){
    String testString = "ffff0000393031d4";
    try {
      BCDataStream testStream = new BCDataStream(testString);
      int val1 = testStream.readUint16();
      int val2 = testStream.readUint16();
      int val3 = testStream.readUint16();
      int val4 = testStream.readUint16();
      assertEquals(val1, 65535);
      assertEquals(val2, 0);
      assertEquals(val3, 12345);
      assertEquals(val4, 54321);
    } catch (Exception e) {
        assertTrue(false);
    }
  }

  @Test
  public void Test_005_test_int32_read(){
      String testString = "ffffff7f00000080d20296492efd69b6";
      try {
        BCDataStream testStream = new BCDataStream(testString);
        int val1 = testStream.readInt32();
        int val2 = testStream.readInt32();
        int val3 = testStream.readInt32();
        int val4 = testStream.readInt32();
        assertEquals(val1, 2147483647);
        assertEquals(val2, -2147483648);
        assertEquals(val3, 1234567890);
        assertEquals(val4, -1234567890);
      } catch (Exception e) {
          assertTrue(false);
      }
  }

  @Test
  public void Test_006_test_uint32_read(){
      String testString = "ffffffff00000000c7353a42d2029649";
      try {
        BCDataStream testStream = new BCDataStream(testString);
        long val1 = testStream.readUint32();
        long val2 = testStream.readUint32();
        long val3 = testStream.readUint32();
        long val4 = testStream.readUint32();
        assertEquals(val1, 4294967295L);
        assertEquals(val2, 0L);
        assertEquals(val3, 1111111111L);
        assertEquals(val4, 1234567890L);
      } catch (Exception e) {
          assertTrue(false);
      }
  }

  @Test
  public void Test_007_test_uint64_read(){
      String testString = "ffffffffffffffff0000000000000000b17067dc8ca954ab";
      try {
        BCDataStream testStream = new BCDataStream(testString);
        BigInteger val1 = testStream.readUint64();
        BigInteger val2 = testStream.readUint64();
        BigInteger val3 = testStream.readUint64();
        assertEquals(new BigInteger("18446744073709551615"), val1);
        assertEquals(BigInteger.ZERO, val2);
        assertEquals(new BigInteger("12345678900987654321"),val3);
      } catch (Exception e) {
          assertTrue(false);
      }
  }


  @Test
  public void Test_010_test_read_compact_size(){
    String testString = "fde803fe40420f0004";
    try {
      BCDataStream testStream = new BCDataStream(testString);
      long size1 = testStream.readCompactSize();
      assertEquals(1000, size1);
      long size2 = testStream.readCompactSize();
      assertEquals(1000000, size2);
      long size3 = testStream.readCompactSize();
      assertEquals(4, size3);
    } catch (Exception e) {
      assertTrue(false);
    }
  }

  @Test
  public void Test_020_test_read_bytes(){
    String testString = "112233aabbcc";
    byte[] shouldBe = {0x11, 0x22, 0x33};
    try {
      BCDataStream testStream = new BCDataStream(testString);
      byte[] fromStream = testStream.readBytes(3);
      assertTrue(Arrays.equals(fromStream, shouldBe));
    } catch(Exception e) {
      assertTrue(false);
    }
  }

}
