package org.electroncash.nio;

public final class ByteBuffer extends Buffer
{
  private ByteOrder _order = ByteOrder.BIG_ENDIAN;
  private final byte[] _bytes;
  
  private ByteBuffer(byte[] bytes)
  {
    super(bytes.length, bytes.length, 0, -1);
    _bytes = bytes;
  }
  
  public static ByteBuffer allocateDirect(int capacity) {
    return new ByteBuffer(new byte[capacity]);
  }
  
  public ByteBuffer get(byte[] dst, int offset, int length) {
    for (int i = offset; i < offset + length; i++) {
      dst[i] = get();
    }
    
    return this;
  }
  
  public ByteBuffer get(byte[] dst) {
    return get(dst, 0, dst.length);
  }
  
  public ByteBuffer put(byte[] src, int offset, int length) {
    for (int i = offset; i < offset + length; i++) {
      put(src[i]);
    }
    return this;
  }
  
  public final ByteBuffer put(byte[] src) {
    return put(src, 0, src.length);
  }
  
  public byte get() {
    return _bytes[(_position++)];
  }
  
  public ByteBuffer put(byte b) {
    _bytes[(_position++)] = b;
    return this;
  }
  
  public byte get(int index) {
    return _bytes[index];
  }
  
  public boolean isDirect() {
    return false;
  }
  
  public char getChar() {
    return getChar(_position++);
  }
  
  public char getChar(int index) {
    return (char)getShort(index);
  }
  
  public ByteBuffer putChar(int index, char value) {
    return putShort(index, (short)value);
  }
  
  public short getShort() {
    return getShort(_position++);
  }
  
  private short getShort(int index) {
    if (_order == ByteOrder.LITTLE_ENDIAN) {
      return (short)((_bytes[index] & 0xFF) + (_bytes[(++index)] << 8));
    }
    return (short)((_bytes[index] << 8) + (_bytes[(++index)] & 0xFF));
  }
  
  private ByteBuffer putShort(int index, short value)
  {
    if (_order == ByteOrder.LITTLE_ENDIAN) {
      _bytes[index] = ((byte)value);
      _bytes[(++index)] = ((byte)(value >> 8));
    } else {
      _bytes[index] = ((byte)(value >> 8));
      _bytes[(++index)] = ((byte)value);
    }
    return this;
  }
  
  public int getInt() {
    return getInt(_position++);
  }
  
  public ByteBuffer putInt(int value) {
    return putInt(_position++, value);
  }
  
  public int getInt(int index) {
    if (_order == ByteOrder.LITTLE_ENDIAN) {
      return (_bytes[index] & 0xFF) + ((_bytes[(++index)] & 0xFF) << 8) + (
        (_bytes[(++index)] & 0xFF) << 16) + (
        (_bytes[(++index)] & 0xFF) << 24);
    }
    return (_bytes[index] << 24) + ((_bytes[(++index)] & 0xFF) << 16) + (
      (_bytes[(++index)] & 0xFF) << 8) + (
      _bytes[(++index)] & 0xFF);
  }
  
  public ByteBuffer putInt(int index, int value)
  {
    if (_order == ByteOrder.LITTLE_ENDIAN) {
      _bytes[index] = ((byte)value);
      _bytes[(++index)] = ((byte)(value >> 8));
      _bytes[(++index)] = ((byte)(value >> 16));
      _bytes[(++index)] = ((byte)(value >> 24));
    } else {
      _bytes[index] = ((byte)(value >> 24));
      _bytes[(++index)] = ((byte)(value >> 16));
      _bytes[(++index)] = ((byte)(value >> 8));
      _bytes[(++index)] = ((byte)value);
    }
    return this;
  }
}
