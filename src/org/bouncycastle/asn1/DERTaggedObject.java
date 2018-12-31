package org.bouncycastle.asn1;

import java.io.IOException;

public class DERTaggedObject
extends ASN1TaggedObject
{
	private static final byte[] ZERO_BYTES = new byte[0];

	public DERTaggedObject(
			boolean       explicit,
			int           tagNo,
			ASN1Encodable obj)
	{
		super(explicit, tagNo, obj);
	}

	public DERTaggedObject(int tagNo, ASN1Encodable encodable)
	{
		super(true, tagNo, encodable);
	}

	boolean isConstructed()
	{
		if (!empty)
		{
			if (explicit)
			{
				return true;
			}
			ASN1Primitive primitive = obj.toASN1Primitive().toDERObject();

			return primitive.isConstructed();

		}

		return true;

	}

	int encodedLength()
			throws IOException
	{
		if (!empty)
		{
			ASN1Primitive primitive = obj.toASN1Primitive().toDERObject();
			int length = primitive.encodedLength();

			if (explicit)
			{
				return StreamUtil.calculateTagLength(tagNo) + StreamUtil.calculateBodyLength(length) + length;
			}
			length = length - 1;

			return StreamUtil.calculateTagLength(tagNo) + length;
		}
		return StreamUtil.calculateTagLength(tagNo) + 1;
	}

	void encode(
			ASN1OutputStream out)
					throws IOException
	{
		if (!empty)
		{
			ASN1Primitive primitive = obj.toASN1Primitive().toDERObject();

			if (explicit)
			{
				out.writeTag(BERTags.CONSTRUCTED | BERTags.TAGGED, tagNo);
				out.writeLength(primitive.encodedLength());
				out.writeObject(primitive);
			}
			else
			{
				int flags;
				if (primitive.isConstructed())
				{
					flags = BERTags.CONSTRUCTED | BERTags.TAGGED;
				}
				else
				{
					flags = BERTags.TAGGED;
				}

				out.writeTag(flags, tagNo);
				out.writeImplicitObject(primitive);
			}
		}
		else
		{
			out.writeEncoded(BERTags.CONSTRUCTED | BERTags.TAGGED, tagNo, ZERO_BYTES);
		}
	}
}
