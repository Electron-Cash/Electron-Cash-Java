package org.bouncycastle.crypto.tls;

import org.bouncycastle.crypto.AsymmetricBlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.Signer;
import org.bouncycastle.crypto.digests.NullDigest;
import org.bouncycastle.crypto.encodings.PKCS1Encoding;
import org.bouncycastle.crypto.engines.RSABlindedEngine;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.signers.GenericSigner;
import org.bouncycastle.crypto.signers.RSADigestSigner;

public class TlsRSASigner extends AbstractTlsSigner {
	public byte[] generateRawSignature(SignatureAndHashAlgorithm algorithm, AsymmetricKeyParameter privateKey,
			byte[] hash) throws CryptoException {
		Signer signer = makeSigner(algorithm, true, true,
				new ParametersWithRandom(privateKey, this.context.getSecureRandom()));
		signer.update(hash, 0, hash.length);
		return signer.generateSignature();
	}

	public boolean verifyRawSignature(SignatureAndHashAlgorithm algorithm, byte[] sigBytes,
			AsymmetricKeyParameter publicKey, byte[] hash) throws CryptoException {
		Signer signer = makeSigner(algorithm, true, false, publicKey);
		signer.update(hash, 0, hash.length);
		return signer.verifySignature(sigBytes);
	}

	public Signer createSigner(SignatureAndHashAlgorithm algorithm, AsymmetricKeyParameter privateKey) {
		return makeSigner(algorithm, false, true, new ParametersWithRandom(privateKey, this.context.getSecureRandom()));
	}

	public Signer createVerifyer(SignatureAndHashAlgorithm algorithm, AsymmetricKeyParameter publicKey) {
		return makeSigner(algorithm, false, false, publicKey);
	}

	public boolean isValidPublicKey(AsymmetricKeyParameter publicKey) {
		return publicKey instanceof RSAKeyParameters && !publicKey.isPrivate();
	}

	protected Signer makeSigner(SignatureAndHashAlgorithm algorithm, boolean raw, boolean forSigning,
			CipherParameters cp) {
		if ((algorithm != null) != TlsUtils.isTLSv12(context)) {
			throw new IllegalStateException();
		}

		if (algorithm != null && algorithm.getSignature() != SignatureAlgorithm.rsa) {
			throw new IllegalStateException();
		}

		Digest d;
		if (raw) {
			d = new NullDigest();
		} else if (algorithm == null) {
			d = new CombinedHash();
		} else {
			d = TlsUtils.createHash(algorithm.getHash());
		}

		Signer s;
		if (algorithm != null) {

			s = new RSADigestSigner(d, TlsUtils.getOIDForHashAlgorithm(algorithm.getHash()));
		} else {

			s = new GenericSigner(createRSAImpl(), d);
		}
		s.init(forSigning, cp);
		return s;
	}

	protected AsymmetricBlockCipher createRSAImpl() {

		return new PKCS1Encoding(new RSABlindedEngine());
	}
}
