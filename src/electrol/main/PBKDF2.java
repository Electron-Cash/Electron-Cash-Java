// This file is licensed under GNU Affero General Public License v3.0
// SOURCE: https://github.com/pinae/ctSESAM-android/blob/master/app/src/main/java/de/pinyto/ctSESAM/PBKDF2.java

package electrol.main;
import java.nio.ByteBuffer;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.Mac;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.digests.SHA384Digest;
import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;

/**
 * This class creates PBKDF2 with sha256.
 */
public class PBKDF2 {
	public static  byte[] shaHMAC(String hashFunction, byte[] key, byte[] password) {

		if (key.length == 0) {
			key = new byte[] { 0x00 };
		}
		Digest d = null;
		if("SHA256".equals(hashFunction)) {
			d = new SHA256Digest();
		}
		else if("SHA384".equals(hashFunction)) {
			d = new SHA384Digest();
		}
		else if("SHA512".equals(hashFunction)) {
			d = new SHA512Digest();
		}
		Mac HMAC = new HMac(d);
		CipherParameters cp = new KeyParameter(key);
		HMAC.init(cp);
		HMAC.update(password, 0, password.length);
		byte[] out = new byte[64];
		HMAC.doFinal(out,0);
		return out;

	}

	private static byte[] F (String hashFunction, byte[] password, byte[] salt, int iterations, int i)
	{
		byte[] Si = new byte[salt.length+4];
		System.arraycopy(salt, 0, Si, 0, salt.length);
		ByteBuffer buf = ByteBuffer.allocateDirect(4).putInt(i);
		buf.position(0);
		byte[] iByteArray = new byte[buf.limit()];
		for(int it=0;it<buf.limit();it++) {
			iByteArray[buf.limit() -it-1] = buf.get(it);
		}
		System.arraycopy(iByteArray, 0, Si, salt.length, iByteArray.length);
		byte[] U = shaHMAC(hashFunction, password, Si);
		byte[] T = new byte[U.length];
		System.arraycopy(U, 0, T, 0, T.length);
		for (int c = 1; c < iterations; c++) {
			U = shaHMAC(hashFunction, password, U);
			for (int k = 0; k < U.length; k++) {
				T[k] = (byte) (((int) T[k]) ^ ((int) U[k]));
			}
		}
		return T;
	}

	/**
	 * Pass "SHA256" or "SHA384" or "SHA512" as the parameter hashFunction.
	 *
	 * @param hashFunction
	 * @param hashString
	 * @param salt
	 * @param iterations
	 * @return
	 */
	public static byte[] hmac (String hashFunction, byte[] hashString, byte[] salt, int iterations)
	{
		int dkLen = 64;
		int hLen = 64;
		if("SHA256".equals(hashFunction)) {
			dkLen = 32;
			hLen = 32;
		}
		else if("SHA384".equals(hashFunction)) {
			dkLen = 48;
			hLen = 48;
		}
		else if("SHA512".equals(hashFunction)) {
			dkLen = 64;
			hLen = 64;
		}
		int l = (int) Math.ceil(dkLen / hLen);
		int r = dkLen - (l - 1) * hLen;
		byte[] dk = new byte[dkLen];
		for (int i = 1; i <= l; i++) {
			byte[] T = F(hashFunction, hashString, salt, iterations, i);
			for (int k = 0; k < T.length; k++) {
				if (i-1+k < dk.length) {
					dk[i-1+k] = T[k];
				}
			}
		}
		return dk;
	}
}