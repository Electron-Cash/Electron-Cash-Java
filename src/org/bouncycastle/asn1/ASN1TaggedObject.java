package org.bouncycastle.asn1;

import java.io.IOException;

public abstract class ASN1TaggedObject
    extends ASN1Primitive
    implements ASN1TaggedObjectParser
{
    int             tagNo;
    boolean         empty = false;
    boolean         explicit = true;
    ASN1Encodable obj = null;

    static public ASN1TaggedObject getInstance(
        ASN1TaggedObject    obj,
        boolean             explicit)
    {
        if (explicit)
        {
            return (ASN1TaggedObject)obj.getObject();
        }

        throw new IllegalArgumentException("implicitly tagged tagged object");
    }

    static public ASN1TaggedObject getInstance(
        Object obj) 
    {
        if (obj == null || obj instanceof ASN1TaggedObject) 
        {
                return (ASN1TaggedObject)obj;
        }
        else if (obj instanceof byte[])
        {
            try
            {
                return ASN1TaggedObject.getInstance(fromByteArray((byte[])obj));
            }
            catch (IOException e)
            {
                throw new IllegalArgumentException("failed to construct tagged object from byte[]: " + e.getMessage());
            }
        }

        throw new IllegalArgumentException("unknown object in getInstance: " + obj.getClass().getName());
    }

    public ASN1TaggedObject(
        boolean         explicit,
        int             tagNo,
        ASN1Encodable   obj)
    {
        if (obj instanceof ASN1Choice)
        {
            this.explicit = true;
        }
        else
        {
            this.explicit = explicit;
        }
        
        this.tagNo = tagNo;

        if (this.explicit)
        {
            this.obj = obj;
        }
        else
        {
            ASN1Primitive prim = obj.toASN1Primitive();

            if (prim instanceof ASN1Set)
            {
                 
            }

            this.obj = obj;
        }
    }
    
    boolean asn1Equals(
        ASN1Primitive o)
    {
        if (!(o instanceof ASN1TaggedObject))
        {
            return false;
        }
        
        ASN1TaggedObject other = (ASN1TaggedObject)o;
        
        if (tagNo != other.tagNo || empty != other.empty || explicit != other.explicit)
        {
            return false;
        }
        
        if(obj == null)
        {
            if (other.obj != null)
            {
                return false;
            }
        }
        else
        {
            if (!(obj.toASN1Primitive().equals(other.obj.toASN1Primitive())))
            {
                return false;
            }
        }
        
        return true;
    }
    
    public int hashCode()
    {
        int code = tagNo;

        if (obj != null)
        {
            code ^= obj.hashCode();
        }

        return code;
    }

    public int getTagNo()
    {
        return tagNo;
    }

    public boolean isExplicit()
    {
        return explicit;
    }

    public boolean isEmpty()
    {
        return empty;
    }

    public ASN1Primitive getObject()
    {
        if (obj != null)
        {
            return obj.toASN1Primitive();
        }

        return null;
    }

    public ASN1Encodable getObjectParser(
        int     tag,
        boolean isExplicit)
    {
        switch (tag)
        {
        case BERTags.SET:
            return ASN1Set.getInstance(this, isExplicit).parser();
        case BERTags.SEQUENCE:
            return ASN1Sequence.getInstance(this, isExplicit).parser();
        case BERTags.OCTET_STRING:
            return ASN1OctetString.getInstance(this, isExplicit).parser();
        }

        if (isExplicit)
        {
            return getObject();
        }

        throw new RuntimeException("implicit tagging not implemented for tag: " + tag);
    }

    public ASN1Primitive getLoadedObject()
    {
        return this.toASN1Primitive();
    }

    ASN1Primitive toDERObject()
    {
        return new DERTaggedObject(explicit, tagNo, obj);
    }

    ASN1Primitive toDLObject()
    {
        return new DLTaggedObject(explicit, tagNo, obj);
    }

    abstract void encode(ASN1OutputStream out)
        throws IOException;

    public String toString()
    {
        return "[" + tagNo + "]" + obj;
    }
}
