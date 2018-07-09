package org.bouncycastle.util;

import electrol.java.util.Collection;

public interface Store
{
    Collection getMatches(Selector selector)
        throws StoreException;
}
