package org.bouncycastle.cert.path;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.util.Memoable;

import electrol.java.util.HashSet;
import electrol.java.util.Set;

public class CertPathValidationContext
    implements Memoable
{
    private Set criticalExtensions;

    private Set handledExtensions = new HashSet();
    private boolean endEntity;
    private int index;

    public CertPathValidationContext(Set criticalExtensionsOIDs)
    {
        this.criticalExtensions = criticalExtensionsOIDs;
    }

    public void addHandledExtension(ASN1ObjectIdentifier extensionIdentifier)
    {
        this.handledExtensions.add(extensionIdentifier);
    }

    public void setIsEndEntity(boolean isEndEntity)
    {
        this.endEntity = isEndEntity;
    }

    public Set getUnhandledCriticalExtensionOIDs()
    {
        Set rv = new HashSet(criticalExtensions);

        rv.removeAll(handledExtensions);

        return rv;
    }

    /**
     * Returns true if the current certificate is the end-entity certificate.
     *
     * @return if current cert end-entity, false otherwise.
     */
    public boolean isEndEntity()
    {
        return endEntity;
    }

    public Memoable copy()
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void reset(Memoable other)
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
