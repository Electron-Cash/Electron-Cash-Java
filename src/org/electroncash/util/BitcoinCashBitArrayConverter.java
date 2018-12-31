package org.electroncash.util;












public class BitcoinCashBitArrayConverter
{
  public BitcoinCashBitArrayConverter() {}
  










  public static byte[] convertBits(byte[] bytes8Bits, int from, int to, boolean strictMode)
  {
    int length = (int)(strictMode ? Math.floor(bytes8Bits.length * from / to) : 
      Math.ceil(bytes8Bits.length * from / to));
    int mask = (1 << to) - 1 & 0xFF;
    byte[] result = new byte[length];
    int index = 0;
    int accumulator = 0;
    int bits = 0;
    for (int i = 0; i < bytes8Bits.length; i++) {
      byte value = bytes8Bits[i];
      accumulator = (accumulator & 0xFF) << from | value & 0xFF;
      bits += from;
      while (bits >= to) {
        bits -= to;
        result[index] = ((byte)(accumulator >> bits & mask));
        index++;
      }
    }
    if (!strictMode) {
      if (bits > 0) {
        result[index] = ((byte)(accumulator << to - bits & mask));
        index++;
      }
    }
    else if ((bits >= from) || ((accumulator << to - bits & mask) != 0)) {
      throw new RuntimeException("Strict mode was used but input couldn't be converted without padding");
    }
    

    return result;
  }
}
