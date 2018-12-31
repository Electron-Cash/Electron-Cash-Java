package org.bouncycastle.cert;

import java.util.Date;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.TBSCertList;
import org.electroncash.util.BigInteger;
import org.electroncash.util.List;
import org.electroncash.util.Set;

public class X509CRLEntryHolder
{
    private TBSCertList.CRLEntry entry;
    private GeneralNames ca;

    X509CRLEntryHolder(TBSCertList.CRLEntry entry, boolean isIndirect, GeneralNames previousCA)
    {
        this.entry = entry;
        this.ca = previousCA;

        if (isIndirect && entry.hasExtensions())
        {
            Extension currentCaName = entry.getExtensions().getExtension(Extension.certificateIssuer);

            if (currentCaName != null)
            {
                ca = GeneralNames.getInstance(currentCaName.getParsedValue());
            }
        }
    }

    public BigInteger getSerialNumber()
    {
        return entry.getUserCertificate().getValue();
    }

    public Date getRevocationDate()
    {
        return entry.getRevocationDate().getDate();
    }
    public boolean hasExtensions()
    {
        return entry.hasExtensions();
    }

    public GeneralNames getCertificateIssuer()
    {
        return this.ca;
    }

    public Extension getExtension(ASN1ObjectIdentifier oid)
    {
        Extensions extensions = entry.getExtensions();

        if (extensions != null)
        {
            return extensions.getExtension(oid);
        }

        return null;
    }

    public Extensions getExtensions()
    {
        return entry.getExtensions();
    }
    public List getExtensionOIDs()
    {
        return CertUtils.getExtensionOIDs(entry.getExtensions());
    }

    public Set getCriticalExtensionOIDs()
    {
        return CertUtils.getCriticalExtensionOIDs(entry.getExtensions());
    }

    public Set getNonCriticalExtensionOIDs()
    {
        return CertUtils.getNonCriticalExtensionOIDs(entry.getExtensions());
    }
}
