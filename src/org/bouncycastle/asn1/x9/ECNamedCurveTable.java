package org.bouncycastle.asn1.x9;

import java.util.Enumeration;
import java.util.Vector;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.sec.SECNamedCurves;

/**
 * A general class that reads all X9.62 style EC curve tables.
 */
public class ECNamedCurveTable
{
    /**
     * return a X9ECParameters object representing the passed in named
     * curve. The routine returns null if the curve is not present.
     *
     * @param name the name of the curve requested
     * @return an X9ECParameters object or null if the curve is not available.
     */
    public static X9ECParameters getByName(
        String name)
    {
        X9ECParameters ecP = SECNamedCurves.getByName(name);
        return ecP;
    }

    /**
     * return the object identifier signified by the passed in name. Null
     * if there is no object identifier associated with name.
     *
     * @return the object identifier associated with name, if present.
     */
    public static ASN1ObjectIdentifier getOID(
        String name)
    {
        ASN1ObjectIdentifier oid = SECNamedCurves.getOID(name);
        return oid;
    }

    /**
     * return a X9ECParameters object representing the passed in named
     * curve.
     *
     * @param oid the object id of the curve requested
     * @return an X9ECParameters object or null if the curve is not available.
     */
    public static X9ECParameters getByOID(
        ASN1ObjectIdentifier oid)
    {
        X9ECParameters ecP = SECNamedCurves.getByOID(oid);
       
        return ecP;
    }

    /**
     * return an enumeration of the names of the available curves.
     *
     * @return an enumeration of the names of the available curves.
     */
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
