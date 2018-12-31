package org.bouncycastle.asn1;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;

public abstract class ASN1Sequence
extends ASN1Primitive
{
	protected Vector seq = new Vector();

	public static ASN1Sequence getInstance(
			Object  obj)
	{
		if (obj == null || obj instanceof ASN1Sequence)
		{
			return (ASN1Sequence)obj;
		}
		else if (obj instanceof ASN1SequenceParser)
		{
			return ASN1Sequence.getInstance(((ASN1SequenceParser)obj).toASN1Primitive());
		}
		else if (obj instanceof byte[])
		{
			try
			{
				return ASN1Sequence.getInstance(fromByteArray((byte[])obj));
			}
			catch (IOException e)
			{
				throw new IllegalArgumentException("failed to construct sequence from byte[]: " + e.getMessage());
			}
		}
		else if (obj instanceof ASN1Encodable)
		{
			ASN1Primitive primitive = ((ASN1Encodable)obj).toASN1Primitive();

			if (primitive instanceof ASN1Sequence)
			{
				return (ASN1Sequence)primitive;
			}
		}

		throw new IllegalArgumentException("unknown object in getInstance: " + obj.getClass().getName());
	}

	public static ASN1Sequence getInstance(
			ASN1TaggedObject    obj,
			boolean             explicit)
	{
		if (explicit)
		{
			if (!obj.isExplicit())
			{
				throw new IllegalArgumentException("object implicit - explicit expected.");
			}

			return ASN1Sequence.getInstance(obj.getObject().toASN1Primitive());
		}

		if (obj.isExplicit())
		{
			if (obj instanceof BERTaggedObject)
			{
				return new BERSequence(obj.getObject());
			}
			return new DLSequence(obj.getObject());
		}
		if (obj.getObject() instanceof ASN1Sequence)
		{
			return (ASN1Sequence)obj.getObject();
		}


		throw new IllegalArgumentException("unknown object in getInstance: " + obj.getClass().getName());
	}

	protected ASN1Sequence()
	{
	}

	protected ASN1Sequence(
			ASN1Encodable obj)
	{
		seq.addElement(obj);
	}

	protected ASN1Sequence(
			ASN1EncodableVector v)
	{
		for (int i = 0; i != v.size(); i++)
		{
			seq.addElement(v.get(i));
		}
	}

	protected ASN1Sequence(
			ASN1Encodable[]   array)
	{
		for (int i = 0; i != array.length; i++)
		{
			seq.addElement(array[i]);
		}
	}

	public ASN1Encodable[] toArray()
	{
		ASN1Encodable[] values = new ASN1Encodable[this.size()];

		for (int i = 0; i != this.size(); i++)
		{
			values[i] = this.getObjectAt(i);
		}

		return values;
	}

	public Enumeration getObjects()
	{
		return seq.elements();
	}

	public ASN1SequenceParser parser()
	{
		final ASN1Sequence outer = this;

		return new ASN1SequenceParser()
		{
			private final int max = size();

			private int index;

			public ASN1Encodable readObject() throws IOException
			{
				if (index == max)
				{
					return null;
				}

				ASN1Encodable obj = getObjectAt(index++);
				if (obj instanceof ASN1Sequence)
				{
					return ((ASN1Sequence)obj).parser();
				}
				if (obj instanceof ASN1Set)
				{
					return ((ASN1Set)obj).parser();
				}

				return obj;
			}

			public ASN1Primitive getLoadedObject()
			{
				return outer;
			}

			public ASN1Primitive toASN1Primitive()
			{
				return outer;
			}
		};
	}

	public ASN1Encodable getObjectAt(
			int index)
	{
		return (ASN1Encodable)seq.elementAt(index);
	}

	public int size()
	{
		return seq.size();
	}

	public int hashCode()
	{
		Enumeration             e = this.getObjects();
		int                     hashCode = size();

		while (e.hasMoreElements())
		{
			Object o = getNext(e);
			hashCode *= 17;

			hashCode ^= o.hashCode();
		}

		return hashCode;
	}

	boolean asn1Equals(
			ASN1Primitive o)
	{
		if (!(o instanceof ASN1Sequence))
		{
			return false;
		}

		ASN1Sequence   other = (ASN1Sequence)o;

		if (this.size() != other.size())
		{
			return false;
		}

		Enumeration s1 = this.getObjects();
		Enumeration s2 = other.getObjects();

		while (s1.hasMoreElements())
		{
			ASN1Encodable obj1 = getNext(s1);
			ASN1Encodable obj2 = getNext(s2);

			ASN1Primitive o1 = obj1.toASN1Primitive();
			ASN1Primitive o2 = obj2.toASN1Primitive();

			if (o1 == o2 || o1.equals(o2))
			{
				continue;
			}

			return false;
		}

		return true;
	}

	private ASN1Encodable getNext(Enumeration e)
	{
		ASN1Encodable encObj = (ASN1Encodable)e.nextElement();

		return encObj;
	}

	ASN1Primitive toDERObject()
	{
		ASN1Sequence derSeq = new DERSequence();

		derSeq.seq = this.seq;

		return derSeq;
	}

	ASN1Primitive toDLObject()
	{
		ASN1Sequence dlSeq = new DLSequence();

		dlSeq.seq = this.seq;

		return dlSeq;
	}

	boolean isConstructed()
	{
		return true;
	}

	abstract void encode(ASN1OutputStream out)
			throws IOException;

	public String toString() 
	{
		return seq.toString();
	}
}
