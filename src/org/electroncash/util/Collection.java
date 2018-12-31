package org.electroncash.util;

public abstract interface Collection
{
  public abstract boolean add(Object paramObject)
    throws RuntimeException, ClassCastException, IllegalArgumentException;
  
  public abstract boolean addAll(Collection paramCollection)
    throws RuntimeException, ClassCastException, IllegalArgumentException;
  
  public abstract void clear()
    throws RuntimeException;
  
  public abstract boolean contains(Object paramObject);
  
  public abstract boolean containsAll(Collection paramCollection);
  
  public abstract boolean equals(Object paramObject);
  
  public abstract int hashCode();
  
  public abstract boolean isEmpty();
  
  public abstract Iterator iterator();
  
  public abstract boolean remove(Object paramObject)
    throws RuntimeException;
  
  public abstract boolean removeAll(Collection paramCollection)
    throws RuntimeException;
  
  public abstract boolean retainAll(Collection paramCollection)
    throws RuntimeException;
  
  public abstract int size();
  
  public abstract Object[] toArray();
  
  public abstract Object[] toArray(Object[] paramArrayOfObject)
    throws ArrayStoreException;
}
