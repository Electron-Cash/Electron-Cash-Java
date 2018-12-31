package org.bouncycastle.asn1;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ASN1StreamParser
{
	private final InputStream _in;
	private final int         _limit;
	private final byte[][] tmpBuffers;

	public ASN1StreamParser(
			InputStream in)
	{
		this(in, StreamUtil.findLimit(in));
	}

	public ASN1StreamParser(
			InputStream in,
			int         limit)
	{
		this._in = in;
		this._limit = limit;

		this.tmpBuffers = new byte[11][];
	}

	public ASN1StreamParser(
			byte[] encoding)
	{
		this(new ByteArrayInputStream(encoding), encoding.length);
	}

	ASN1Encodable readIndef(int tagValue) throws IOException
	{
		switch (tagValue)
		{
		case BERTags.EXTERNAL:
			return new DERExternalParser(this);
		case BERTags.OCTET_STRING:
			return new BEROctetStringParser(this);
		case BERTags.SEQUENCE:
			return new BERSequenceParser(this);
		case BERTags.SET:
			return new BERSetParser(this);
		default:
			throw new ASN1Exception("unknown BER object encountered: 0x" + Integer.toHexString(tagValue));
		}
	}

	ASN1Encodable readImplicit(boolean constructed, int tag) throws IOException
	{
		if (_in instanceof IndefiniteLengthInputStream)
		{
			if (!constructed)
			{
				throw new IOException("indefinite length primitive encoding encountered");
			}

			return readIndef(tag);
		}

		if (constructed)
		{
			switch (tag)
			{
			case BERTags.SET:
				return new DERSetParser(this);
			case BERTags.SEQUENCE:
				return new DERSequenceParser(this);
			case BERTags.OCTET_STRING:
				return new BEROctetStringParser(this);
			}
		}
		else
		{
			switch (tag)
			{
			case BERTags.SET:
				throw new ASN1Exception("sequences must use constructed encoding (see X.690 8.9.1/8.10.1)");
			case BERTags.SEQUENCE:
				throw new ASN1Exception("sets must use constructed encoding (see X.690 8.11.1/8.12.1)");
			case BERTags.OCTET_STRING:
				return new DEROctetStringParser((DefiniteLengthInputStream)_in);
			}
		}

		throw new RuntimeException("implicit tagging not implemented");
	}

	ASN1Primitive readTaggedObject(boolean constructed, int tag) throws IOException
	{
		if (!constructed)
		{
			DefiniteLengthInputStream defIn = (DefiniteLengthInputStream)_in;
			return new DERTaggedObject(false, tag, new DEROctetString(defIn.toByteArray()));
		}

		ASN1EncodableVector v = readVector();

		if (_in instanceof IndefiniteLengthInputStream)
		{
			return v.size() == 1
					?   new BERTaggedObject(true, tag, v.get(0))
							:   new BERTaggedObject(false, tag, BERFactory.createSequence(v));
		}

		return v.size() == 1
				?   new DERTaggedObject(true, tag, v.get(0))
						:   new DERTaggedObject(false, tag, DERFactory.createSequence(v));
	}

	public ASN1Encodable readObject()
			throws IOException
	{
		int tag = _in.read();
		if (tag == -1)
		{
			return null;
		}

		set00Check(false);

		int tagNo = ASN1InputStream.readTagNumber(_in, tag);

		boolean isConstructed = (tag & BERTags.CONSTRUCTED) != 0;

		int length = ASN1InputStream.readLength(_in, _limit);

		if (length < 0)
		{
			if (!isConstructed)
			{
				throw new IOException("indefinite length primitive encoding encountered");
			}

			IndefiniteLengthInputStream indIn = new IndefiniteLengthInputStream(_in, _limit);
			ASN1StreamParser sp = new ASN1StreamParser(indIn, _limit);

			if ((tag & BERTags.APPLICATION) != 0)
			{
				return new BERApplicationSpecificParser(tagNo, sp);
			}

			if ((tag & BERTags.TAGGED) != 0)
			{
				return new BERTaggedObjectParser(true, tagNo, sp);
			}

			return sp.readIndef(tagNo);
		}
		DefiniteLengthInputStream defIn = new DefiniteLengthInputStream(_in, length);

		if ((tag & BERTags.APPLICATION) != 0)
		{
			return new DERApplicationSpecific(isConstructed, tagNo, defIn.toByteArray());
		}

		if ((tag & BERTags.TAGGED) != 0)
		{
			return new BERTaggedObjectParser(isConstructed, tagNo, new ASN1StreamParser(defIn));
		}

		if (isConstructed)
		{
			switch (tagNo)
			{
			case BERTags.OCTET_STRING:
				return new BEROctetStringParser(new ASN1StreamParser(defIn));
			case BERTags.SEQUENCE:
				return new DERSequenceParser(new ASN1StreamParser(defIn));
			case BERTags.SET:
				return new DERSetParser(new ASN1StreamParser(defIn));
			case BERTags.EXTERNAL:
				return new DERExternalParser(new ASN1StreamParser(defIn));
			default:
				throw new IOException("unknown tag " + tagNo + " encountered");
			}
		}

		switch (tagNo)
		{
		case BERTags.OCTET_STRING:
			return new DEROctetStringParser(defIn);
		}

		try
		{
			return ASN1InputStream.createPrimitiveDERObject(tagNo, defIn, tmpBuffers);
		}
		catch (IllegalArgumentException e)
		{
			throw new ASN1Exception("corrupted stream detected", e);
		}

	}

	private void set00Check(boolean enabled)
	{
		if (_in instanceof IndefiniteLengthInputStream)
		{
			((IndefiniteLengthInputStream)_in).setEofOn00(enabled);
		}
	}

	ASN1EncodableVector readVector() throws IOException
	{
		ASN1EncodableVector v = new ASN1EncodableVector();

		ASN1Encodable obj;
		while ((obj = readObject()) != null)
		{
			if (obj instanceof InMemoryRepresentable)
			{
				v.add(((InMemoryRepresentable)obj).getLoadedObject());
			}
			else
			{
				v.add(obj.toASN1Primitive());
			}
		}

		return v;
	}
}
