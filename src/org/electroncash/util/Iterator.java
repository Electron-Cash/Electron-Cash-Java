package org.electroncash.util;

import java.util.NoSuchElementException;

public abstract interface Iterator
{
  public abstract boolean hasNext();
  
  public abstract Object next()
    throws NoSuchElementException;
  
  public abstract void remove()
    throws RuntimeException, IllegalStateException;
}
