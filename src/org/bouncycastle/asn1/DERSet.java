package org.bouncycastle.asn1;

import java.io.IOException;
import java.util.Enumeration;

public class DERSet
    extends ASN1Set
{
    private int bodyLength = -1;

    public DERSet()
    {
    }

    public DERSet(
        ASN1Encodable obj)
    {
        super(obj);
    }

    public DERSet(
        ASN1EncodableVector v)
    {
        super(v, true);
    }
    
    public DERSet(
        ASN1Encodable[]   a)
    {
        super(a, true);
    }

    DERSet(
        ASN1EncodableVector v,
        boolean                  doSort)
    {
        super(v, doSort);
    }

    private int getBodyLength()
        throws IOException
    {
        if (bodyLength < 0)
        {
            int length = 0;

            for (Enumeration e = this.getObjects(); e.hasMoreElements();)
            {
                Object    obj = e.nextElement();

                length += ((ASN1Encodable)obj).toASN1Primitive().toDERObject().encodedLength();
            }

            bodyLength = length;
        }

        return bodyLength;
    }

    int encodedLength()
        throws IOException
    {
        int length = getBodyLength();

        return 1 + StreamUtil.calculateBodyLength(length) + length;
    }

    void encode(
        ASN1OutputStream out)
        throws IOException
    {
        ASN1OutputStream        dOut = out.getDERSubStream();
        int                     length = getBodyLength();

        out.write(BERTags.SET | BERTags.CONSTRUCTED);
        out.writeLength(length);

        for (Enumeration e = this.getObjects(); e.hasMoreElements();)
        {
            Object    obj = e.nextElement();

            dOut.writeObject((ASN1Encodable)obj);
        }
    }
}
