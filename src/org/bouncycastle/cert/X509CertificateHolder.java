package org.bouncycastle.cert;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.Certificate;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x509.TBSCertificate;
import org.bouncycastle.operator.ContentVerifier;
import org.bouncycastle.operator.ContentVerifierProvider;
import org.electroncash.util.BigInteger;
import org.electroncash.util.List;
import org.electroncash.util.Set;

/**
 * Holding class for an X.509 Certificate structure.
 */
public class X509CertificateHolder
{
    private Certificate x509Certificate;
    private Extensions  extensions;

    private static Certificate parseBytes(byte[] certEncoding)
        throws IOException
    {
        try
        {
            return Certificate.getInstance(ASN1Primitive.fromByteArray(certEncoding));
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

    public X509CertificateHolder(byte[] certEncoding)
        throws IOException
    {
        this(parseBytes(certEncoding));
    }

    public X509CertificateHolder(Certificate x509Certificate)
    {
        this.x509Certificate = x509Certificate;
        this.extensions = x509Certificate.getTBSCertificate().getExtensions();
    }

    public int getVersionNumber()
    {
        return x509Certificate.getVersionNumber();
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

    public BigInteger getSerialNumber()
    {
        return x509Certificate.getSerialNumber().getValue();
    }

    public X500Name getIssuer()
    {
        return X500Name.getInstance(x509Certificate.getIssuer());
    }

    public X500Name getSubject()
    {
        return X500Name.getInstance(x509Certificate.getSubject());
    }

    public Date getNotBefore()
    {
        return x509Certificate.getStartDate().getDate();
    }

    public Date getNotAfter()
    {
        return x509Certificate.getEndDate().getDate();
    }

    public SubjectPublicKeyInfo getSubjectPublicKeyInfo()
    {
        return x509Certificate.getSubjectPublicKeyInfo();
    }

    public Certificate toASN1Structure()
    {
        return x509Certificate;
    }

    public AlgorithmIdentifier getSignatureAlgorithm()
    {
        return x509Certificate.getSignatureAlgorithm();
    }

    public byte[] getSignature()
    {
        return x509Certificate.getSignature().getBytes();
    }

    public boolean isValidOn(Date date)
    {
        return !CertUtils.dateBefore(date, x509Certificate.getStartDate().getDate()) && !CertUtils.dateAfter(date, x509Certificate.getEndDate().getDate());
    }

    public boolean isSignatureValid(ContentVerifierProvider verifierProvider)
        throws CertException
    {
        TBSCertificate tbsCert = x509Certificate.getTBSCertificate();

        if (!CertUtils.isAlgIdEqual(tbsCert.getSignature(), x509Certificate.getSignatureAlgorithm()))
        {
            throw new CertException("signature invalid - algorithm identifier mismatch");
        }

        ContentVerifier verifier;

        try
        {
            verifier = verifierProvider.get((tbsCert.getSignature()));

            OutputStream sOut = verifier.getOutputStream();
            DEROutputStream dOut = new DEROutputStream(sOut);

            dOut.writeObject(tbsCert);

            sOut.close();
        }
        catch (Exception e)
        {
            throw new CertException("unable to process signature: " + e.getMessage(), e);
        }

        return verifier.verify(x509Certificate.getSignature().getBytes());
    }

    public boolean equals(
        Object o)
    {
        if (o == this)
        {
            return true;
        }

        if (!(o instanceof X509CertificateHolder))
        {
            return false;
        }

        X509CertificateHolder other = (X509CertificateHolder)o;

        return this.x509Certificate.equals(other.x509Certificate);
    }

    public int hashCode()
    {
        return this.x509Certificate.hashCode();
    }

    public byte[] getEncoded()
        throws IOException
    {
        return x509Certificate.getEncoded();
    }
}
