package org.bouncycastle.cert;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.AttCertValidityPeriod;
import org.bouncycastle.asn1.x509.Attribute;
import org.bouncycastle.asn1.x509.AttributeCertificate;
import org.bouncycastle.asn1.x509.AttributeCertificateInfo;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.operator.ContentVerifier;
import org.bouncycastle.operator.ContentVerifierProvider;
import org.electroncash.util.ArrayList;
import org.electroncash.util.BigInteger;
import org.electroncash.util.List;
import org.electroncash.util.Set;
public class X509AttributeCertificateHolder
{
    private static Attribute[] EMPTY_ARRAY = new Attribute[0];
    
    private AttributeCertificate attrCert;
    private Extensions extensions;

    private static AttributeCertificate parseBytes(byte[] certEncoding)
        throws IOException
    {
        try
        {
            return AttributeCertificate.getInstance(ASN1Primitive.fromByteArray(certEncoding));
        }
        catch (ClassCastException e)
        {
            throw new CertIOException("malformed data: " + e.getMessage(), e);
        }
        catch (IllegalArgumentException e)
        {
            throw new CertIOException("malformed data: " + e.getMessage(), e);
        }
    }

    public X509AttributeCertificateHolder(byte[] certEncoding)
        throws IOException
    {
        this(parseBytes(certEncoding));
    }

    public X509AttributeCertificateHolder(AttributeCertificate attrCert)
    {
        this.attrCert = attrCert;
        this.extensions = attrCert.getAcinfo().getExtensions();
    }

    public byte[] getEncoded()
        throws IOException
    {
        return attrCert.getEncoded();
    }

    public int getVersion()
    {
        return attrCert.getAcinfo().getVersion().getValue().intValue() + 1;
    }

    public BigInteger getSerialNumber()
    {
        return attrCert.getAcinfo().getSerialNumber().getValue();
    }

    public AttributeCertificateHolder getHolder()
    {
        return new AttributeCertificateHolder((ASN1Sequence)attrCert.getAcinfo().getHolder().toASN1Primitive());
    }

    public AttributeCertificateIssuer getIssuer()
    {
        return new AttributeCertificateIssuer(attrCert.getAcinfo().getIssuer());
    }

    public Date getNotBefore()
    {
        return CertUtils.recoverDate(attrCert.getAcinfo().getAttrCertValidityPeriod().getNotBeforeTime());
    }

    public Date getNotAfter()
    {
        return CertUtils.recoverDate(attrCert.getAcinfo().getAttrCertValidityPeriod().getNotAfterTime());
    }

    public Attribute[] getAttributes()
    {
        ASN1Sequence seq = attrCert.getAcinfo().getAttributes();
        Attribute[] attrs = new Attribute[seq.size()];

        for (int i = 0; i != seq.size(); i++)
        {
            attrs[i] = Attribute.getInstance(seq.getObjectAt(i));
        }

        return attrs;
    }

    public Attribute[] getAttributes(ASN1ObjectIdentifier type)
    {
        ASN1Sequence    seq = attrCert.getAcinfo().getAttributes();
        List            list = new ArrayList();

        for (int i = 0; i != seq.size(); i++)
        {
            Attribute attr = Attribute.getInstance(seq.getObjectAt(i));
            if (attr.getAttrType().equals(type))
            {
                list.add(attr);
            }
        }

        if (list.size() == 0)
        {
            return EMPTY_ARRAY;
        }

        return (Attribute[])list.toArray(new Attribute[list.size()]);
    }

    public boolean hasExtensions()
    {
        return extensions != null;
    }

    public Extension getExtension(ASN1ObjectIdentifier oid)
    {
        if (extensions != null)
        {
            return extensions.getExtension(oid);
        }

        return null;
    }

    public Extensions getExtensions()
    {
        return extensions;
    }

    public List getExtensionOIDs()
    {
        return CertUtils.getExtensionOIDs(extensions);
    }

    public Set getCriticalExtensionOIDs()
    {
        return CertUtils.getCriticalExtensionOIDs(extensions);
    }

    public Set getNonCriticalExtensionOIDs()
    {
        return CertUtils.getNonCriticalExtensionOIDs(extensions);
    }

    public boolean[] getIssuerUniqueID()
    {
        return CertUtils.bitStringToBoolean(attrCert.getAcinfo().getIssuerUniqueID());
    }

    public AlgorithmIdentifier getSignatureAlgorithm()
    {
        return attrCert.getSignatureAlgorithm();
    }

    public byte[] getSignature()
    {
        return attrCert.getSignatureValue().getBytes();
    }

    public AttributeCertificate toASN1Structure()
    {
        return attrCert;
    }

    public boolean isValidOn(Date date)
    {
        AttCertValidityPeriod certValidityPeriod = attrCert.getAcinfo().getAttrCertValidityPeriod();

        return !CertUtils.dateBefore(date, CertUtils.recoverDate(certValidityPeriod.getNotBeforeTime())) && !CertUtils.dateAfter(date, CertUtils.recoverDate(certValidityPeriod.getNotAfterTime()));
    }

    public boolean isSignatureValid(ContentVerifierProvider verifierProvider)
        throws CertException
    {
        AttributeCertificateInfo acinfo = attrCert.getAcinfo();

        if (!CertUtils.isAlgIdEqual(acinfo.getSignature(), attrCert.getSignatureAlgorithm()))
        {
            throw new CertException("signature invalid - algorithm identifier mismatch");
        }

        ContentVerifier verifier;

        try
        {
            verifier = verifierProvider.get((acinfo.getSignature()));

            OutputStream sOut = verifier.getOutputStream();
            DEROutputStream dOut = new DEROutputStream(sOut);

            dOut.writeObject(acinfo);

            sOut.close();
        }
        catch (Exception e)
        {
            throw new CertException("unable to process signature: " + e.getMessage(), e);
        }

        return verifier.verify(attrCert.getSignatureValue().getBytes());
    }

    public boolean equals(
        Object o)
    {
        if (o == this)
        {
            return true;
        }

        if (!(o instanceof X509AttributeCertificateHolder))
        {
            return false;
        }

        X509AttributeCertificateHolder other = (X509AttributeCertificateHolder)o;

        return this.attrCert.equals(other.attrCert);
    }

    public int hashCode()
    {
        return this.attrCert.hashCode();
    }
}
