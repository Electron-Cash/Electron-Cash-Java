package main.electron;

import electrol.java.util.Arrays;
import java.lang.String;

import java.math.BigInteger;

public class BCDataStream{

  public byte[] input;
  public int cursor;

  public BCDataStream(String hex) throws Exception {
    cursor = 0;
    try {
      input = new BigInteger(hex,16).toByteArray();
      if(input[0] == (byte)0 ){// Heisenberg leading zero removing
        input = Arrays.slice(input ,1, input.length);
      }
    } catch(Exception e) {
        throw e;
    }
  }

  public int readInt16() {
    int x;
    x = (input[cursor + 1] << 8) | (input[cursor] & 0xff);
    if ((x>>>15&1)==1){
        x = ((x^0x7fff)&0x7fff)+1; //2's complement 16 bit
        x *= -1;
    }
    cursor += 2;
    return x;
  }

  public int readUint16() {
    int x;
    x = ((input[cursor + 1] & 0xff) << 8) | (input[cursor] & 0xff);
    cursor += 2;
    return x;
  }

  public int readInt32() {
    int x = (input[cursor + 3] & 0xff)<<24 |
            (input[cursor + 2] & 0xff)<<16 |
            (input[cursor + 1] & 0xff)<<8 |
            (input[cursor] & 0xff);
    cursor += 4;
    return x;
  }

  public long readUint32() {
    long x;
    x = ((long) input[cursor + 3] & 0xff) << 24 |
        ((long) input[cursor + 2] & 0xff) << 16 |
        ((long) input[cursor + 1] & 0xff) << 8  |
        ((long) input[cursor] & 0xff);
    cursor += 4;
    return x;
  }

  public BigInteger readUint64(){
    BigInteger result = new BigInteger(1, Arrays.reverse(Arrays.slice(input, cursor, cursor + 8)));
    cursor += 8;
    return result;
  }

  public long readCompactSize(){
    long size;
    int selector = input[cursor] & 0xFF;
    cursor += 1;
    switch(selector){
      case 253: size = (long)readUint16();
                break;
      case 254: size = readUint32();
                break;
      default: size = (long)selector;
               break;
    }
    return size;
  }

  public byte[] readBytes(int len){
    byte[] result = Arrays.slice(input, cursor, cursor + len);
    cursor += len;
    return result;
  }

}
