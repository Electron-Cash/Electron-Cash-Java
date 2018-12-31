package org.bouncycastle.asn1;

import java.io.IOException;
import java.util.Date;
import java.util.TimeZone;

import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.Strings;

public class ASN1GeneralizedTime
extends ASN1Primitive
{
	private byte[]      time;

	public static ASN1GeneralizedTime getInstance(
			Object  obj)
	{
		if (obj == null || obj instanceof ASN1GeneralizedTime)
		{
			return (ASN1GeneralizedTime)obj;
		}

		if (obj instanceof ASN1GeneralizedTime)
		{
			return new ASN1GeneralizedTime(((ASN1GeneralizedTime)obj).time);
		}

		throw new IllegalArgumentException("illegal object in getInstance: " + obj.getClass().getName());
	}

	public static ASN1GeneralizedTime getInstance(
			ASN1TaggedObject obj,
			boolean          explicit)
	{
		ASN1Primitive o = obj.getObject();

		if (explicit || o instanceof ASN1GeneralizedTime)
		{
			return getInstance(o);
		}
		return new ASN1GeneralizedTime(((ASN1OctetString)o).getOctets());
		
	}

	public ASN1GeneralizedTime(
			String  time)
	{
		char last = time.charAt(time.length() - 1);
		if (last != 'Z' && !(last >= 0 && last <= '9'))
		{
			if (time.indexOf('-') < 0 && time.indexOf('+') < 0)
			{
				throw new IllegalArgumentException("time needs to be in format YYYYMMDDHHMMSS[.f]Z or YYYYMMDDHHMMSS[.f][+-]HHMM");
			}
		}

		this.time = Strings.toByteArray(time);
	}
	public ASN1GeneralizedTime(
			Date time)
	{
		this.time = Strings.toByteArray(DateFormatter.getGeneralizedTimeDateString(time, false));
	}

	protected ASN1GeneralizedTime(Date date, boolean includeMillis)
	{
		this.time = Strings.toByteArray(DateFormatter.getGeneralizedTimeDateString(date, includeMillis));
			
	}

	ASN1GeneralizedTime(
			byte[]  bytes)
	{
		this.time = bytes;
	}

	public String getTimeString()
	{
		return Strings.fromByteArray(time);
	}

	public String getTime()
	{
		String stime = Strings.fromByteArray(time);

		if (stime.charAt(stime.length() - 1) == 'Z')
		{
			return stime.substring(0, stime.length() - 1) + "GMT+00:00";
		}

		int signPos = stime.length() - 5;
		char sign = stime.charAt(signPos);
		if (sign == '-' || sign == '+')
		{
			return stime.substring(0, signPos)+ "GMT"+ stime.substring(signPos, signPos + 3)+ ":"
					+ stime.substring(signPos + 3);
		}

		signPos = stime.length() - 3;
		sign = stime.charAt(signPos);
		if (sign == '-' || sign == '+')
		{
			return stime.substring(0, signPos)+ "GMT"+ stime.substring(signPos)+ ":00";
		}

		return stime + calculateGMTOffset();
	}

	private String calculateGMTOffset()
	{
		String sign = "+";
		TimeZone timeZone = TimeZone.getDefault();
		int offset = timeZone.getRawOffset();
		if (offset < 0)
		{
			sign = "-";
			offset = -offset;
		}
		int hours = offset / (60 * 60 * 1000);
		int minutes = (offset - (hours * 60 * 60 * 1000)) / (60 * 1000);

		return "GMT" + sign + convert(hours) + ":" + convert(minutes);
	}

	private String convert(int time)
	{
		if (time < 10)
		{
			return "0" + time;
		}

		return Integer.toString(time);
	}

	public Date getDate()
	{
		return DateFormatter.fromGeneralizedTimeString(time);
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
		out.writeEncoded(BERTags.GENERALIZED_TIME, time);
	}

	boolean asn1Equals(
			ASN1Primitive  o)
	{
		if (!(o instanceof ASN1GeneralizedTime))
		{
			return false;
		}

		return Arrays.areEqual(time, ((ASN1GeneralizedTime)o).time);
	}

	public int hashCode()
	{
		return Arrays.hashCode(time);
	}
}
