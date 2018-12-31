package org.bouncycastle.crypto.tls;

import java.io.IOException;
import java.io.OutputStream;

import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.encodings.PKCS1Encoding;
import org.bouncycastle.crypto.engines.RSABlindedEngine;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.util.Arrays;

public class TlsRSAUtils {
	public static byte[] generateEncryptedPreMasterSecret(TlsContext context, RSAKeyParameters rsaServerPublicKey,
			OutputStream output) throws IOException {

		byte[] premasterSecret = new byte[48];
		context.getSecureRandom().nextBytes(premasterSecret);
		TlsUtils.writeVersion(context.getClientVersion(), premasterSecret, 0);

		PKCS1Encoding encoding = new PKCS1Encoding(new RSABlindedEngine());
		encoding.init(true, new ParametersWithRandom(rsaServerPublicKey, context.getSecureRandom()));

		try {
			byte[] encryptedPreMasterSecret = encoding.processBlock(premasterSecret, 0, premasterSecret.length);

			if (TlsUtils.isSSL(context)) {
				output.write(encryptedPreMasterSecret);
			} else {
				TlsUtils.writeOpaque16(encryptedPreMasterSecret, output);
			}
		} catch (InvalidCipherTextException e) {

			throw new TlsFatalAlert(AlertDescription.internal_error);
		}

		return premasterSecret;
	}

	public static byte[] safeDecryptPreMasterSecret(TlsContext context, RSAKeyParameters rsaServerPrivateKey,
			byte[] encryptedPreMasterSecret) {

		ProtocolVersion clientVersion = context.getClientVersion();

		boolean versionNumberCheckDisabled = false;

		byte[] fallback = new byte[48];
		context.getSecureRandom().nextBytes(fallback);

		byte[] M = Arrays.clone(fallback);
		try {
			PKCS1Encoding encoding = new PKCS1Encoding(new RSABlindedEngine(), fallback);
			encoding.init(false, new ParametersWithRandom(rsaServerPrivateKey, context.getSecureRandom()));

			M = encoding.processBlock(encryptedPreMasterSecret, 0, encryptedPreMasterSecret.length);
		} catch (Exception e) {

		}

		if (versionNumberCheckDisabled && clientVersion.isEqualOrEarlierVersionOf(ProtocolVersion.TLSv10)) {

		} else {

			int correct = (clientVersion.getMajorVersion() ^ (M[0] & 0xff))
					| (clientVersion.getMinorVersion() ^ (M[1] & 0xff));
			correct |= correct >> 1;
			correct |= correct >> 2;
			correct |= correct >> 4;
			int mask = ~((correct & 1) - 1);

			for (int i = 0; i < 48; i++) {
				M[i] = (byte) ((M[i] & (~mask)) | (fallback[i] & mask));
			}
		}
		return M;
	}
}
