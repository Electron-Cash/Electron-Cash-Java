package org.bouncycastle.crypto.tls;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.bouncycastle.crypto.prng.ThreadedSeedGenerator;
import org.bouncycastle.util.Arrays;
import org.electroncash.security.SecureRandom;

public class TlsClientProtocol extends TlsProtocol {
	protected TlsClient tlsClient = null;
	protected TlsClientContextImpl tlsClientContext = null;

	protected byte[] selectedSessionID = null;

	protected TlsKeyExchange keyExchange = null;
	protected TlsAuthentication authentication = null;

	protected CertificateStatus certificateStatus = null;
	protected CertificateRequest certificateRequest = null;

	private static SecureRandom createSecureRandom() {

		ThreadedSeedGenerator tsg = new ThreadedSeedGenerator();
		SecureRandom random = new SecureRandom();

		random.setSeed(tsg.generateSeed(20, true));

		return random;
	}

	public TlsClientProtocol(InputStream input, OutputStream output) {
		this(input, output, createSecureRandom());
	}

	public TlsClientProtocol(InputStream input, OutputStream output, SecureRandom secureRandom) {
		super(input, output, secureRandom);
	}

	public void connect(TlsClient tlsClient) throws IOException {
		if (tlsClient == null) {
			throw new IllegalArgumentException("'tlsClient' cannot be null");
		}
		if (this.tlsClient != null) {
			throw new IllegalStateException("'connect' can only be called once");
		}

		this.tlsClient = tlsClient;

		this.securityParameters = new SecurityParameters();
		this.securityParameters.entity = ConnectionEnd.client;

		this.tlsClientContext = new TlsClientContextImpl(secureRandom, securityParameters);

		this.securityParameters.clientRandom = createRandomBlock(tlsClient.shouldUseGMTUnixTime(),
				tlsClientContext.getNonceRandomGenerator());

		this.tlsClient.init(tlsClientContext);
		this.recordStream.init(tlsClientContext);

		TlsSession sessionToResume = tlsClient.getSessionToResume();
		if (sessionToResume != null) {
			SessionParameters sessionParameters = sessionToResume.exportSessionParameters();
			if (sessionParameters != null) {
				this.tlsSession = sessionToResume;
				this.sessionParameters = sessionParameters;
			}
		}
		sendClientHelloMessage();
		this.connection_state = CS_CLIENT_HELLO;

		completeHandshake();
	}

	protected void cleanupHandshake() {
		super.cleanupHandshake();

		this.selectedSessionID = null;
		this.keyExchange = null;
		this.authentication = null;
		this.certificateStatus = null;
		this.certificateRequest = null;
	}

	protected AbstractTlsContext getContext() {
		return tlsClientContext;
	}

	protected TlsPeer getPeer() {
		return tlsClient;
	}

	protected void handleHandshakeMessage(short type, byte[] data) throws IOException {
		ByteArrayInputStream buf = new ByteArrayInputStream(data);

		if (this.resumedSession) {
			if (type != HandshakeType.finished || this.connection_state != CS_SERVER_HELLO) {
				throw new TlsFatalAlert(AlertDescription.unexpected_message);
			}

			processFinishedMessage(buf);
			this.connection_state = CS_SERVER_FINISHED;

			sendFinishedMessage();
			this.connection_state = CS_CLIENT_FINISHED;
			this.connection_state = CS_END;

			return;
		}

		switch (type) {
		case HandshakeType.certificate: {
			switch (this.connection_state) {
			case CS_SERVER_HELLO: {
				handleSupplementalData(null);
			}
			case CS_SERVER_SUPPLEMENTAL_DATA: {

				this.peerCertificate = Certificate.parse(buf);
				assertEmpty(buf);

				if (this.peerCertificate == null || this.peerCertificate.isEmpty()) {
					this.allowCertificateStatus = false;
				}

				this.keyExchange.processServerCertificate(this.peerCertificate);

				this.authentication = tlsClient.getAuthentication();
				this.authentication.notifyServerCertificate(this.peerCertificate);

				break;
			}
			default:
				throw new TlsFatalAlert(AlertDescription.unexpected_message);
			}

			this.connection_state = CS_SERVER_CERTIFICATE;
			break;
		}
		case HandshakeType.certificate_status: {
			switch (this.connection_state) {
			case CS_SERVER_CERTIFICATE: {
				if (!this.allowCertificateStatus) {

					throw new TlsFatalAlert(AlertDescription.unexpected_message);
				}

				this.certificateStatus = CertificateStatus.parse(buf);

				assertEmpty(buf);

				this.connection_state = CS_CERTIFICATE_STATUS;
				break;
			}
			default:
				throw new TlsFatalAlert(AlertDescription.unexpected_message);
			}
			break;
		}
		case HandshakeType.finished: {
			switch (this.connection_state) {
			case CS_CLIENT_FINISHED: {
				if (this.expectSessionTicket) {

					throw new TlsFatalAlert(AlertDescription.unexpected_message);
				}

			}
			case CS_SERVER_SESSION_TICKET: {
				processFinishedMessage(buf);
				this.connection_state = CS_SERVER_FINISHED;
				this.connection_state = CS_END;
				break;
			}
			default:
				throw new TlsFatalAlert(AlertDescription.unexpected_message);
			}
			break;
		}
		case HandshakeType.server_hello: {
			switch (this.connection_state) {
			case CS_CLIENT_HELLO: {
				receiveServerHelloMessage(buf);
				this.connection_state = CS_SERVER_HELLO;

				if (this.securityParameters.maxFragmentLength >= 0) {
					int plainTextLimit = 1 << (8 + this.securityParameters.maxFragmentLength);
					recordStream.setPlaintextLimit(plainTextLimit);
				}

				this.securityParameters.prfAlgorithm = getPRFAlgorithm(getContext(),
						this.securityParameters.getCipherSuite());

				this.securityParameters.verifyDataLength = 12;

				this.recordStream.notifyHelloComplete();

				if (this.resumedSession) {
					this.securityParameters.masterSecret = Arrays.clone(this.sessionParameters.getMasterSecret());
					this.recordStream.setPendingConnectionState(getPeer().getCompression(), getPeer().getCipher());

					sendChangeCipherSpecMessage();
				} else {
					invalidateSession();

					if (this.selectedSessionID.length > 0) {
						this.tlsSession = new TlsSessionImpl(this.selectedSessionID, null);
					}
				}

				break;
			}
			default:
				throw new TlsFatalAlert(AlertDescription.unexpected_message);
			}
			break;
		}
		case HandshakeType.supplemental_data: {
			switch (this.connection_state) {
			case CS_SERVER_HELLO: {
				handleSupplementalData(readSupplementalDataMessage(buf));
				break;
			}
			default:
				throw new TlsFatalAlert(AlertDescription.unexpected_message);
			}
			break;
		}
		case HandshakeType.server_hello_done: {
			switch (this.connection_state) {
			case CS_SERVER_HELLO: {
				handleSupplementalData(null);
			}
			case CS_SERVER_SUPPLEMENTAL_DATA: {
				this.keyExchange.skipServerCredentials();
				this.authentication = null;

			}
			case CS_SERVER_CERTIFICATE:
			case CS_CERTIFICATE_STATUS: {
				this.keyExchange.skipServerKeyExchange();

			}
			case CS_SERVER_KEY_EXCHANGE:
			case CS_CERTIFICATE_REQUEST: {
				assertEmpty(buf);

				this.connection_state = CS_SERVER_HELLO_DONE;

				this.recordStream.getHandshakeHash().sealHashAlgorithms();

				Vector clientSupplementalData = tlsClient.getClientSupplementalData();
				if (clientSupplementalData != null) {
					sendSupplementalDataMessage(clientSupplementalData);
				}
				this.connection_state = CS_CLIENT_SUPPLEMENTAL_DATA;

				TlsCredentials clientCreds = null;
				if (certificateRequest == null) {
					this.keyExchange.skipClientCredentials();
				} else {
					clientCreds = this.authentication.getClientCredentials(certificateRequest);

					if (clientCreds == null) {
						this.keyExchange.skipClientCredentials();

						sendCertificateMessage(Certificate.EMPTY_CHAIN);
					} else {
						this.keyExchange.processClientCredentials(clientCreds);

						sendCertificateMessage(clientCreds.getCertificate());
					}
				}

				this.connection_state = CS_CLIENT_CERTIFICATE;

				sendClientKeyExchangeMessage();
				this.connection_state = CS_CLIENT_KEY_EXCHANGE;

				establishMasterSecret(getContext(), keyExchange);
				recordStream.setPendingConnectionState(getPeer().getCompression(), getPeer().getCipher());

				TlsHandshakeHash prepareFinishHash = recordStream.prepareToFinish();

				if (clientCreds != null && clientCreds instanceof TlsSignerCredentials) {
					TlsSignerCredentials signerCredentials = (TlsSignerCredentials) clientCreds;

					SignatureAndHashAlgorithm signatureAndHashAlgorithm;
					byte[] hash;

					if (TlsUtils.isTLSv12(getContext())) {
						signatureAndHashAlgorithm = signerCredentials.getSignatureAndHashAlgorithm();
						if (signatureAndHashAlgorithm == null) {
							throw new TlsFatalAlert(AlertDescription.internal_error);
						}

						hash = prepareFinishHash.getFinalHash(signatureAndHashAlgorithm.getHash());
					} else {
						signatureAndHashAlgorithm = null;
						hash = getCurrentPRFHash(getContext(), prepareFinishHash, null);
					}

					byte[] signature = signerCredentials.generateCertificateSignature(hash);
					DigitallySigned certificateVerify = new DigitallySigned(signatureAndHashAlgorithm, signature);
					sendCertificateVerifyMessage(certificateVerify);

					this.connection_state = CS_CERTIFICATE_VERIFY;
				}

				sendChangeCipherSpecMessage();
				sendFinishedMessage();
				this.connection_state = CS_CLIENT_FINISHED;
				break;
			}
			default:
				throw new TlsFatalAlert(AlertDescription.handshake_failure);
			}
			break;
		}
		case HandshakeType.server_key_exchange: {
			switch (this.connection_state) {
			case CS_SERVER_HELLO: {
				handleSupplementalData(null);
			}
			case CS_SERVER_SUPPLEMENTAL_DATA: {
				this.keyExchange.skipServerCredentials();
				this.authentication = null;

			}
			case CS_SERVER_CERTIFICATE:
			case CS_CERTIFICATE_STATUS: {
				this.keyExchange.processServerKeyExchange(buf);

				assertEmpty(buf);
				break;
			}
			default:
				throw new TlsFatalAlert(AlertDescription.unexpected_message);
			}

			this.connection_state = CS_SERVER_KEY_EXCHANGE;
			break;
		}
		case HandshakeType.certificate_request: {
			switch (this.connection_state) {
			case CS_SERVER_CERTIFICATE:
			case CS_CERTIFICATE_STATUS: {
				this.keyExchange.skipServerKeyExchange();

			}
			case CS_SERVER_KEY_EXCHANGE: {
				if (this.authentication == null) {

					throw new TlsFatalAlert(AlertDescription.handshake_failure);
				}

				this.certificateRequest = CertificateRequest.parse(getContext(), buf);

				assertEmpty(buf);

				this.keyExchange.validateCertificateRequest(this.certificateRequest);

				TlsUtils.trackHashAlgorithms(this.recordStream.getHandshakeHash(),
						this.certificateRequest.getSupportedSignatureAlgorithms());

				break;
			}
			default:
				throw new TlsFatalAlert(AlertDescription.unexpected_message);
			}

			this.connection_state = CS_CERTIFICATE_REQUEST;
			break;
		}
		case HandshakeType.session_ticket: {
			switch (this.connection_state) {
			case CS_CLIENT_FINISHED: {
				if (!this.expectSessionTicket) {

					throw new TlsFatalAlert(AlertDescription.unexpected_message);
				}

				invalidateSession();

				receiveNewSessionTicketMessage(buf);
				this.connection_state = CS_SERVER_SESSION_TICKET;
				break;
			}
			default:
				throw new TlsFatalAlert(AlertDescription.unexpected_message);
			}
		}
		case HandshakeType.hello_request: {
			assertEmpty(buf);

			if (this.connection_state == CS_END) {

				if (TlsUtils.isSSL(getContext())) {
					throw new TlsFatalAlert(AlertDescription.handshake_failure);
				}

				String message = "Renegotiation not supported";
				raiseWarning(AlertDescription.no_renegotiation, message);
			}
			break;
		}
		case HandshakeType.client_hello:
		case HandshakeType.client_key_exchange:
		case HandshakeType.certificate_verify:
		case HandshakeType.hello_verify_request:
		default:
			throw new TlsFatalAlert(AlertDescription.unexpected_message);
		}
	}

	protected void handleSupplementalData(Vector serverSupplementalData) throws IOException {
		this.tlsClient.processServerSupplementalData(serverSupplementalData);
		this.connection_state = CS_SERVER_SUPPLEMENTAL_DATA;

		this.keyExchange = tlsClient.getKeyExchange();
		this.keyExchange.init(getContext());
	}

	protected void receiveNewSessionTicketMessage(ByteArrayInputStream buf) throws IOException {
		NewSessionTicket newSessionTicket = NewSessionTicket.parse(buf);

		TlsProtocol.assertEmpty(buf);

		tlsClient.notifyNewSessionTicket(newSessionTicket);
	}

	protected void receiveServerHelloMessage(ByteArrayInputStream buf) throws IOException {
		ProtocolVersion server_version = TlsUtils.readVersion(buf);
		if (server_version.isDTLS()) {
			throw new TlsFatalAlert(AlertDescription.illegal_parameter);
		}

		if (!server_version.equals(this.recordStream.getReadVersion())) {
			throw new TlsFatalAlert(AlertDescription.illegal_parameter);
		}

		ProtocolVersion client_version = getContext().getClientVersion();
		if (!server_version.isEqualOrEarlierVersionOf(client_version)) {
			throw new TlsFatalAlert(AlertDescription.illegal_parameter);
		}

		this.recordStream.setWriteVersion(server_version);
		getContext().setServerVersion(server_version);
		this.tlsClient.notifyServerVersion(server_version);

		this.securityParameters.serverRandom = TlsUtils.readFully(32, buf);

		this.selectedSessionID = TlsUtils.readOpaque8(buf);
		if (this.selectedSessionID.length > 32) {
			throw new TlsFatalAlert(AlertDescription.illegal_parameter);
		}

		this.tlsClient.notifySessionID(this.selectedSessionID);

		this.resumedSession = this.selectedSessionID.length > 0 && this.tlsSession != null
				&& Arrays.areEqual(this.selectedSessionID, this.tlsSession.getSessionID());

		int selectedCipherSuite = TlsUtils.readUint16(buf);
		if (!Arrays.contains(this.offeredCipherSuites, selectedCipherSuite)
				|| selectedCipherSuite == CipherSuite.TLS_NULL_WITH_NULL_NULL
				|| selectedCipherSuite == CipherSuite.TLS_EMPTY_RENEGOTIATION_INFO_SCSV
				|| !TlsUtils.isValidCipherSuiteForVersion(selectedCipherSuite, server_version)) {
			throw new TlsFatalAlert(AlertDescription.illegal_parameter);
		}

		this.tlsClient.notifySelectedCipherSuite(selectedCipherSuite);

		short selectedCompressionMethod = TlsUtils.readUint8(buf);
		if (!Arrays.contains(this.offeredCompressionMethods, selectedCompressionMethod)) {
			throw new TlsFatalAlert(AlertDescription.illegal_parameter);
		}

		this.tlsClient.notifySelectedCompressionMethod(selectedCompressionMethod);

		this.serverExtensions = readExtensions(buf);

		if (this.serverExtensions != null) {
			Enumeration e = this.serverExtensions.keys();
			while (e.hasMoreElements()) {
				Integer extType = (Integer) e.nextElement();

				if (extType.equals(EXT_RenegotiationInfo)) {
					continue;
				}

				if (this.resumedSession) {

				}

				if (null == TlsUtils.getExtensionData(this.clientExtensions, extType)) {
					throw new TlsFatalAlert(AlertDescription.unsupported_extension);
				}
			}
		}

		{

			byte[] renegExtData = TlsUtils.getExtensionData(this.serverExtensions, EXT_RenegotiationInfo);
			if (renegExtData != null) {

				this.secure_renegotiation = true;

				if (!Arrays.constantTimeAreEqual(renegExtData, createRenegotiationInfo(TlsUtils.EMPTY_BYTES))) {
					throw new TlsFatalAlert(AlertDescription.handshake_failure);
				}
			}
		}

		this.tlsClient.notifySecureRenegotiation(this.secure_renegotiation);

		Hashtable sessionClientExtensions = clientExtensions, sessionServerExtensions = serverExtensions;
		if (this.resumedSession) {
			if (selectedCipherSuite != this.sessionParameters.getCipherSuite()
					|| selectedCompressionMethod != this.sessionParameters.getCompressionAlgorithm()) {
				throw new TlsFatalAlert(AlertDescription.illegal_parameter);
			}

			sessionClientExtensions = null;
			sessionServerExtensions = this.sessionParameters.readServerExtensions();
		}

		this.securityParameters.cipherSuite = selectedCipherSuite;
		this.securityParameters.compressionAlgorithm = selectedCompressionMethod;

		if (sessionServerExtensions != null) {

			boolean serverSentEncryptThenMAC = TlsExtensionsUtils.hasEncryptThenMACExtension(sessionServerExtensions);
			if (serverSentEncryptThenMAC && !TlsUtils.isBlockCipherSuite(selectedCipherSuite)) {
				throw new TlsFatalAlert(AlertDescription.illegal_parameter);
			}

			this.securityParameters.encryptThenMAC = serverSentEncryptThenMAC;

			this.securityParameters.maxFragmentLength = processMaxFragmentLengthExtension(sessionClientExtensions,
					sessionServerExtensions, AlertDescription.illegal_parameter);

			this.securityParameters.truncatedHMac = TlsExtensionsUtils
					.hasTruncatedHMacExtension(sessionServerExtensions);

			this.allowCertificateStatus = !this.resumedSession && TlsUtils.hasExpectedEmptyExtensionData(
					sessionServerExtensions, TlsExtensionsUtils.EXT_status_request, AlertDescription.illegal_parameter);

			this.expectSessionTicket = !this.resumedSession && TlsUtils.hasExpectedEmptyExtensionData(
					sessionServerExtensions, TlsProtocol.EXT_SessionTicket, AlertDescription.illegal_parameter);
		}

		if (sessionClientExtensions != null) {
			this.tlsClient.processServerExtensions(sessionServerExtensions);
		}
	}

	protected void sendCertificateVerifyMessage(DigitallySigned certificateVerify) throws IOException {
		HandshakeMessage message = new HandshakeMessage(HandshakeType.certificate_verify);

		certificateVerify.encode(message);

		message.writeToRecordStream();
	}

	protected void sendClientHelloMessage() throws IOException {
		this.recordStream.setWriteVersion(this.tlsClient.getClientHelloRecordLayerVersion());

		ProtocolVersion client_version = this.tlsClient.getClientVersion();
		if (client_version.isDTLS()) {
			throw new TlsFatalAlert(AlertDescription.internal_error);
		}

		getContext().setClientVersion(client_version);

		byte[] session_id = TlsUtils.EMPTY_BYTES;
		if (this.tlsSession != null) {
			session_id = this.tlsSession.getSessionID();
			if (session_id == null || session_id.length > 32) {
				session_id = TlsUtils.EMPTY_BYTES;
			}
		}

		this.offeredCipherSuites = this.tlsClient.getCipherSuites();

		this.offeredCompressionMethods = this.tlsClient.getCompressionMethods();

		if (session_id.length > 0 && this.sessionParameters != null) {
			if (!Arrays.contains(this.offeredCipherSuites, sessionParameters.getCipherSuite())
					|| !Arrays.contains(this.offeredCompressionMethods, sessionParameters.getCompressionAlgorithm())) {
				session_id = TlsUtils.EMPTY_BYTES;
			}
		}

		this.clientExtensions = this.tlsClient.getClientExtensions();

		HandshakeMessage message = new HandshakeMessage(HandshakeType.client_hello);

		TlsUtils.writeVersion(client_version, message);

		message.write(this.securityParameters.getClientRandom());

		TlsUtils.writeOpaque8(session_id, message);

		{

			byte[] renegExtData = TlsUtils.getExtensionData(clientExtensions, EXT_RenegotiationInfo);
			boolean noRenegExt = (null == renegExtData);

			boolean noSCSV = !Arrays.contains(offeredCipherSuites, CipherSuite.TLS_EMPTY_RENEGOTIATION_INFO_SCSV);

			if (noRenegExt && noSCSV) {

				this.offeredCipherSuites = Arrays.append(offeredCipherSuites,
						CipherSuite.TLS_EMPTY_RENEGOTIATION_INFO_SCSV);
			}

			TlsUtils.writeUint16ArrayWithUint16Length(offeredCipherSuites, message);
		}

		TlsUtils.writeUint8ArrayWithUint8Length(offeredCompressionMethods, message);

		if (clientExtensions != null) {
			writeExtensions(message, clientExtensions);
		}

		message.writeToRecordStream();
	}

	protected void sendClientKeyExchangeMessage() throws IOException {
		HandshakeMessage message = new HandshakeMessage(HandshakeType.client_key_exchange);

		this.keyExchange.generateClientKeyExchange(message);

		message.writeToRecordStream();
	}
}
