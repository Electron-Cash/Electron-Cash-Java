package org.electroncash.nio;


public abstract class Buffer
{
  private final int _capacity;
  private int _limit;
  protected int _position;
  private int _mark;
  
  Buffer(int capacity, int limit, int position, int mark)
  {
    _capacity = capacity;
    _limit = limit;
    _position = position;
    _mark = mark;
  }
  
  public final Buffer clear() {
    _limit = _capacity;
    _position = 0;
    _mark = -1;
    return this;
  }
  
  public boolean isReadOnly() {
    return false;
  }
  
  public final int limit() {
    return _limit;
  }
  
  public final Buffer position(int newPosition) {
    if ((newPosition < 0) || (newPosition > _limit)) {
      throw new IllegalArgumentException();
    }
    
    if (newPosition <= _mark) {
      _mark = -1;
    }
    _position = newPosition;
    return this;
  }
  
  public final int remaining() {
    return _limit - _position;
  }
}
