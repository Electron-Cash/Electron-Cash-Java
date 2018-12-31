package org.bouncycastle.asn1;

import java.io.IOException;
import java.util.Date;

import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.Strings;

public class ASN1UTCTime
extends ASN1Primitive
{
	private byte[]      time;

	public static ASN1UTCTime getInstance(
			Object  obj)
	{
		if (obj == null || obj instanceof ASN1UTCTime)
		{
			return (ASN1UTCTime)obj;
		}

		if (obj instanceof ASN1UTCTime)
		{
			return new ASN1UTCTime(((ASN1UTCTime)obj).time);
		}

		throw new IllegalArgumentException("illegal object in getInstance: " + obj.getClass().getName());
	}

	public static ASN1UTCTime getInstance(
			ASN1TaggedObject obj,
			boolean          explicit)
	{
		ASN1Object o = obj.getObject();

		if (explicit || o instanceof ASN1UTCTime)
		{
			return getInstance(o);
		}
		return new ASN1UTCTime(((ASN1OctetString)o).getOctets());
	}

	public ASN1UTCTime(
			String  time)
	{
		if (time.charAt(time.length() - 1) != 'Z')
		{
			if (time.indexOf('-') < 0 && time.indexOf('+') < 0)
			{
				throw new IllegalArgumentException("time needs to be in format YYMMDDHHMMSSZ");
			}
		}

		this.time = Strings.toByteArray(time);
	}

	public ASN1UTCTime(
			Date time)
	{
		this.time = Strings.toByteArray(DateFormatter.toUTCDateString(time));
	}

	ASN1UTCTime(
			byte[]  time)
	{
		this.time = time;
	}

	public Date getDate()
	{
		return DateFormatter.adjustedFromUTCDateString(time);
	}

	public Date getAdjustedDate()
	{
		return DateFormatter.adjustedFromUTCDateString(time);
	}

	public String getTime()
	{
		String stime = Strings.fromByteArray(time);

		if (stime.indexOf('-') < 0 && stime.indexOf('+') < 0)
		{
			if (stime.length() == 11)
			{
				return stime.substring(0, 10) + "00GMT+00:00";
			}
			return stime.substring(0, 12) + "GMT+00:00";
			
		}
		int index = stime.indexOf('-');
		if (index < 0)
		{
			index = stime.indexOf('+');
		}
		String d = stime;

		if (index == stime.length() - 3)
		{
			d += "00";
		}

		if (index == 10)
		{
			return d.substring(0, 10) + "00GMT" + d.substring(10, 13) + ":" + d.substring(13, 15);
		}
		return d.substring(0, 12) + "GMT" + d.substring(12, 15) + ":" +  d.substring(15, 17);


	}

	public String getAdjustedTime()
	{
		String   d = this.getTime();

		if (d.charAt(0) < '5')
		{
			return "20" + d;
		}
		return "19" + d;
	}

	public String getTimeString()
	{
		return Strings.fromByteArray(time);
	}

	boolean isConstructed()
	{
		return false;
	}

	int encodedLength()
	{
		int length = time.length;

		return 1 + StreamUtil.calculateBodyLength(length) + length;
	}

	void encode(
			ASN1OutputStream  out)
					throws IOException
	{
		out.write(BERTags.UTC_TIME);

		int length = time.length;

		out.writeLength(length);

		for (int i = 0; i != length; i++)
		{
			out.write(time[i]);
		}
	}

	boolean asn1Equals(
			ASN1Primitive o)
	{
		if (!(o instanceof ASN1UTCTime))
		{
			return false;
		}

		return Arrays.areEqual(time, ((ASN1UTCTime)o).time);
	}

	public int hashCode()
	{
		return Arrays.hashCode(time);
	}

	public String toString() 
	{
		return Strings.fromByteArray(time);
	}
}
