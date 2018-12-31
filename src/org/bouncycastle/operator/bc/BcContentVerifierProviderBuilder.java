package org.bouncycastle.operator.bc;

import java.io.IOException;
import java.io.OutputStream;

import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.crypto.Signer;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.operator.ContentVerifier;
import org.bouncycastle.operator.ContentVerifierProvider;
import org.bouncycastle.operator.OperatorCreationException;

public abstract class BcContentVerifierProviderBuilder {
	protected BcDigestProvider digestProvider;

	public BcContentVerifierProviderBuilder() {
		this.digestProvider = BcDefaultDigestProvider.INSTANCE;
	}

	public ContentVerifierProvider build(final X509CertificateHolder certHolder) {
		return new ContentVerifierProvider() {
			public boolean hasAssociatedCertificate() {
				return true;
			}

			public X509CertificateHolder getAssociatedCertificate() {
				return certHolder;
			}

			public ContentVerifier get(AlgorithmIdentifier algorithm) throws OperatorCreationException {
				try {
					AsymmetricKeyParameter publicKey = extractKeyParameters(certHolder.getSubjectPublicKeyInfo());
					BcSignerOutputStream stream = createSignatureStream(algorithm, publicKey);

					return new SigVerifier(algorithm, stream);
				} catch (IOException e) {
					throw new OperatorCreationException("exception on setup: " + e, e);
				}
			}
		};
	}

	public ContentVerifierProvider build(final AsymmetricKeyParameter publicKey) {
		return new ContentVerifierProvider() {
			public boolean hasAssociatedCertificate() {
				return false;
			}

			public X509CertificateHolder getAssociatedCertificate() {
				return null;
			}

			public ContentVerifier get(AlgorithmIdentifier algorithm) throws OperatorCreationException {
				BcSignerOutputStream stream = createSignatureStream(algorithm, publicKey);

				return new SigVerifier(algorithm, stream);
			}
		};
	}

	private BcSignerOutputStream createSignatureStream(AlgorithmIdentifier algorithm, AsymmetricKeyParameter publicKey)
			throws OperatorCreationException {
		Signer sig = createSigner(algorithm);

		sig.init(false, publicKey);

		return new BcSignerOutputStream(sig);
	}

	protected abstract AsymmetricKeyParameter extractKeyParameters(SubjectPublicKeyInfo publicKeyInfo)
			throws IOException;

	protected abstract Signer createSigner(AlgorithmIdentifier sigAlgId) throws OperatorCreationException;

	private class SigVerifier implements ContentVerifier {
		private BcSignerOutputStream stream;
		private AlgorithmIdentifier algorithm;

		SigVerifier(AlgorithmIdentifier algorithm, BcSignerOutputStream stream) {
			this.algorithm = algorithm;
			this.stream = stream;
		}

		public AlgorithmIdentifier getAlgorithmIdentifier() {
			return algorithm;
		}

		public OutputStream getOutputStream() {
			if (stream == null) {
				throw new IllegalStateException("verifier not initialised");
			}

			return stream;
		}

		public boolean verify(byte[] expected) {
			return stream.verify(expected);
		}
	}
}