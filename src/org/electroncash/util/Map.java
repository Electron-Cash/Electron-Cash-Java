package org.electroncash.util;

public abstract interface Map
{
  public abstract int size();
  
  public abstract boolean isEmpty();
  
  public abstract boolean containsKey(Object paramObject)
    throws ClassCastException, NullPointerException;
  
  public abstract boolean containsValue(Object paramObject);
  
  public abstract Object get(Object paramObject)
    throws ClassCastException, NullPointerException;
  
  public abstract Object put(Object paramObject1, Object paramObject2)
    throws RuntimeException, ClassCastException, IllegalArgumentException, NullPointerException;
  
  public abstract Object remove(Object paramObject)
    throws RuntimeException;
  
  public abstract void putAll(Map paramMap)
    throws RuntimeException, ClassCastException, IllegalArgumentException, NullPointerException;
  
  public abstract void clear()
    throws RuntimeException;
  
  public abstract Set keySet();
  
  public abstract Collection values();
  
  public abstract Set entrySet();
  
  public abstract boolean equals(Object paramObject);
  
  public abstract int hashCode();
  
  public static abstract interface Entry
  {
    public abstract Object getKey();
    
    public abstract Object getValue();
    
    public abstract Object setValue(Object paramObject)
      throws RuntimeException, ClassCastException, IllegalArgumentException, NullPointerException;
    
    public abstract boolean equals(Object paramObject);
    
    public abstract int hashCode();
  }
}
