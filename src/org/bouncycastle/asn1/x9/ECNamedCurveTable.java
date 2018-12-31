package org.bouncycastle.asn1.x9;

import java.util.Enumeration;
import java.util.Vector;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.sec.SECNamedCurves;

public class ECNamedCurveTable
{
    public static X9ECParameters getByName(
        String name)
    {
        X9ECParameters ecP = SECNamedCurves.getByName(name);
        return ecP;
    }

    public static ASN1ObjectIdentifier getOID(
        String name)
    {
        ASN1ObjectIdentifier oid = SECNamedCurves.getOID(name);
        return oid;
    }

    public static X9ECParameters getByOID(
        ASN1ObjectIdentifier oid)
    {
        X9ECParameters ecP = SECNamedCurves.getByOID(oid);
       
        return ecP;
    }

    public static Enumeration getNames()
    {
        Vector v = new Vector();

        addEnumeration(v, SECNamedCurves.getNames());

        return v.elements();
    }

    private static void addEnumeration(
        Vector v,
        Enumeration e)
    {
        while (e.hasMoreElements())
        {
            v.addElement(e.nextElement());
        }
    }
}
