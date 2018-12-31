package org.electroncash.util;

public abstract interface List
  extends Collection
{
  public abstract void add(int paramInt, Object paramObject)
    throws RuntimeException, ClassCastException, IllegalArgumentException, IndexOutOfBoundsException;
  
  public abstract boolean addAll(int paramInt, Collection paramCollection)
    throws RuntimeException, ClassCastException, IllegalArgumentException, IndexOutOfBoundsException;
  
  public abstract Object get(int paramInt)
    throws IndexOutOfBoundsException;
  
  public abstract int indexOf(Object paramObject);
  
  public abstract int lastIndexOf(Object paramObject);
  
  public abstract ListIterator listIterator();
  
  public abstract ListIterator listIterator(int paramInt)
    throws IndexOutOfBoundsException;
  
  public abstract Object remove(int paramInt)
    throws RuntimeException, IndexOutOfBoundsException;
  
  public abstract Object set(int paramInt, Object paramObject)
    throws RuntimeException, ClassCastException, IllegalArgumentException, IndexOutOfBoundsException;
}
