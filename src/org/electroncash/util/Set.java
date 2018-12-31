package org.electroncash.util;

public abstract interface Set
  extends Collection
{
  public abstract int size();
  
  public abstract boolean isEmpty();
  
  public abstract boolean contains(Object paramObject);
  
  public abstract Iterator iterator();
  
  public abstract Object[] toArray();
  
  public abstract Object[] toArray(Object[] paramArrayOfObject);
  
  public abstract boolean add(Object paramObject);
  
  public abstract boolean remove(Object paramObject);
  
  public abstract boolean containsAll(Collection paramCollection);
  
  public abstract boolean addAll(Collection paramCollection);
  
  public abstract boolean retainAll(Collection paramCollection);
  
  public abstract boolean removeAll(Collection paramCollection);
  
  public abstract void clear();
  
  public abstract boolean equals(Object paramObject);
  
  public abstract int hashCode();
}
