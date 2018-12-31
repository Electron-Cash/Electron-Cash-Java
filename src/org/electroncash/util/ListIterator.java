package org.electroncash.util;

import java.util.NoSuchElementException;

public abstract interface ListIterator
  extends Iterator
{
  public abstract boolean hasPrevious();
  
  public abstract Object previous()
    throws NoSuchElementException;
  
  public abstract int nextIndex();
  
  public abstract int previousIndex();
  
  public abstract void set(Object paramObject)
    throws RuntimeException, ClassCastException, IllegalArgumentException, IllegalStateException;
  
  public abstract void add(Object paramObject)
    throws RuntimeException, ClassCastException, IllegalArgumentException;
}
