package org.bouncycastle.asn1;

import java.io.IOException;

import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.Strings;

public class DERT61String
    extends ASN1Primitive
    implements ASN1String
{
    private byte[] string;

    public static DERT61String getInstance(
        Object  obj)
    {
        if (obj == null || obj instanceof DERT61String)
        {
            return (DERT61String)obj;
        }

        if (obj instanceof byte[])
        {
            try
            {
                return (DERT61String)fromByteArray((byte[])obj);
            }
            catch (Exception e)
            {
                throw new IllegalArgumentException("encoding error in getInstance: " + e.toString());
            }
        }

        throw new IllegalArgumentException("illegal object in getInstance: " + obj.getClass().getName());
    }

    public static DERT61String getInstance(
        ASN1TaggedObject obj,
        boolean          explicit)
    {
        ASN1Primitive o = obj.getObject();

        if (explicit || o instanceof DERT61String)
        {
            return getInstance(o);
        }
        return new DERT61String(ASN1OctetString.getInstance(o).getOctets());
    }

    public DERT61String(
        byte[]   string)
    {
        this.string = string;
    }

    public DERT61String(
        String   string)
    {
        this(Strings.toByteArray(string));
    }

    public String getString()
    {
        return Strings.fromByteArray(string);
    }

    public String toString()
    {
        return getString();
    }

    boolean isConstructed()
    {
        return false;
    }

    int encodedLength()
    {
        return 1 + StreamUtil.calculateBodyLength(string.length) + string.length;
    }

    void encode(
        ASN1OutputStream out)
        throws IOException
    {
        out.writeEncoded(BERTags.T61_STRING, string);
    }

    public byte[] getOctets()
    {
        return Arrays.clone(string);
    }

    boolean asn1Equals(
        ASN1Primitive o)
    {
        if (!(o instanceof DERT61String))
        {
            return false;
        }

        return Arrays.areEqual(string, ((DERT61String)o).string);
    }
    
    public int hashCode()
    {
        return Arrays.hashCode(string);
    }
}
