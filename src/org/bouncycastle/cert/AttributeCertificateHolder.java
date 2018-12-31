package org.bouncycastle.cert;

import java.io.OutputStream;

import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.Holder;
import org.bouncycastle.asn1.x509.IssuerSerial;
import org.bouncycastle.asn1.x509.ObjectDigestInfo;
import org.bouncycastle.operator.DigestCalculator;
import org.bouncycastle.operator.DigestCalculatorProvider;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.Selector;
import org.electroncash.util.ArrayList;
import org.electroncash.util.BigInteger;
import org.electroncash.util.List;

public class AttributeCertificateHolder
    implements Selector
{
    private static DigestCalculatorProvider digestCalculatorProvider;

    final Holder holder;

    AttributeCertificateHolder(ASN1Sequence seq)
    {
        holder = Holder.getInstance(seq);
    }

    public AttributeCertificateHolder(X500Name issuerName,
        BigInteger serialNumber)
    {
        holder = new Holder(new IssuerSerial(
            new GeneralNames(new GeneralName(issuerName)),
            new ASN1Integer(serialNumber)));
    }

    public AttributeCertificateHolder(X509CertificateHolder cert)
    {
        holder = new Holder(new IssuerSerial(generateGeneralNames(cert.getIssuer()),
            new ASN1Integer(cert.getSerialNumber())));
    }

    public AttributeCertificateHolder(X500Name principal)
    {
        holder = new Holder(generateGeneralNames(principal));
    }

    public AttributeCertificateHolder(int digestedObjectType,
        ASN1ObjectIdentifier digestAlgorithm, ASN1ObjectIdentifier otherObjectTypeID, byte[] objectDigest)
    {
        holder = new Holder(new ObjectDigestInfo(digestedObjectType,
            otherObjectTypeID, new AlgorithmIdentifier(digestAlgorithm), Arrays
                .clone(objectDigest)));
    }

    public int getDigestedObjectType()
    {
        if (holder.getObjectDigestInfo() != null)
        {
            return holder.getObjectDigestInfo().getDigestedObjectType()
                .getValue().intValue();
        }
        return -1;
    }

    public AlgorithmIdentifier getDigestAlgorithm()
    {
        if (holder.getObjectDigestInfo() != null)
        {
            return holder.getObjectDigestInfo().getDigestAlgorithm();
        }
        return null;
    }

    public byte[] getObjectDigest()
    {
        if (holder.getObjectDigestInfo() != null)
        {
            return holder.getObjectDigestInfo().getObjectDigest().getBytes();
        }
        return null;
    }

    public ASN1ObjectIdentifier getOtherObjectTypeID()
    {
        if (holder.getObjectDigestInfo() != null)
        {
            new ASN1ObjectIdentifier(holder.getObjectDigestInfo().getOtherObjectTypeID().getId());
        }
        return null;
    }

    private GeneralNames generateGeneralNames(X500Name principal)
    {
        return new GeneralNames(new GeneralName(principal));
    }

    private boolean matchesDN(X500Name subject, GeneralNames targets)
    {
        GeneralName[] names = targets.getNames();

        for (int i = 0; i != names.length; i++)
        {
            GeneralName gn = names[i];

            if (gn.getTagNo() == GeneralName.directoryName)
            {
                if (X500Name.getInstance(gn.getName()).equals(subject))
                {
                    return true;
                }
            }
        }

        return false;
    }

    private X500Name[] getPrincipals(GeneralName[] names)
    {
        List l = new ArrayList(names.length);

        for (int i = 0; i != names.length; i++)
        {
            if (names[i].getTagNo() == GeneralName.directoryName)
            {
                l.add(X500Name.getInstance(names[i].getName()));
            }
        }

        return (X500Name[])l.toArray(new X500Name[l.size()]);
    }

    public X500Name[] getEntityNames()
    {
        if (holder.getEntityName() != null)
        {
            return getPrincipals(holder.getEntityName().getNames());
        }

        return null;
    }

    public X500Name[] getIssuer()
    {
        if (holder.getBaseCertificateID() != null)
        {
            return getPrincipals(holder.getBaseCertificateID().getIssuer().getNames());
        }

        return null;
    }

    public BigInteger getSerialNumber()
    {
        if (holder.getBaseCertificateID() != null)
        {
            return holder.getBaseCertificateID().getSerial().getValue();
        }

        return null;
    }

    public Object clone()
    {
        return new AttributeCertificateHolder((ASN1Sequence)holder.toASN1Primitive());
    }

    public boolean match(Object obj)
    {
        if (!(obj instanceof X509CertificateHolder))
        {
            return false;
        }

        X509CertificateHolder x509Cert = (X509CertificateHolder)obj;

        if (holder.getBaseCertificateID() != null)
        {
            return holder.getBaseCertificateID().getSerial().getValue().equals(x509Cert.getSerialNumber())
                && matchesDN(x509Cert.getIssuer(), holder.getBaseCertificateID().getIssuer());
        }

        if (holder.getEntityName() != null)
        {
            if (matchesDN(x509Cert.getSubject(),
                holder.getEntityName()))
            {
                return true;
            }
        }

        if (holder.getObjectDigestInfo() != null)
        {
            try
            {
                DigestCalculator digCalc = digestCalculatorProvider.get(holder.getObjectDigestInfo().getDigestAlgorithm());
                OutputStream     digOut = digCalc.getOutputStream();

                switch (getDigestedObjectType())
                {
                case ObjectDigestInfo.publicKey:
                    // TODO: DSA Dss-parms
                    digOut.write(x509Cert.getSubjectPublicKeyInfo().getEncoded());
                    break;
                case ObjectDigestInfo.publicKeyCert:
                    digOut.write(x509Cert.getEncoded());
                    break;
                }

                digOut.close();

                if (!Arrays.areEqual(digCalc.getDigest(), getObjectDigest()))
                {
                    return false;
                }
            }
            catch (Exception e)
            {
                return false;
            }
        }

        return false;
    }

    public boolean equals(Object obj)
    {
        if (obj == this)
        {
            return true;
        }

        if (!(obj instanceof AttributeCertificateHolder))
        {
            return false;
        }

        AttributeCertificateHolder other = (AttributeCertificateHolder)obj;

        return this.holder.equals(other.holder);
    }

    public int hashCode()
    {
        return this.holder.hashCode();
    }

    public static void setDigestCalculatorProvider(DigestCalculatorProvider digCalcProvider)
    {
        digestCalculatorProvider = digCalcProvider;
    }
}
