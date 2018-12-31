package org.bouncycastle.crypto.tls;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.prng.RandomGenerator;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.Integers;
import org.electroncash.security.SecureRandom;

public abstract class TlsProtocol {
	protected static final Integer EXT_RenegotiationInfo = Integers.valueOf(ExtensionType.renegotiation_info);
	protected static final Integer EXT_SessionTicket = Integers.valueOf(ExtensionType.session_ticket);

	private static final String TLS_ERROR_MESSAGE = "Internal TLS error, this could be an attack";

	protected static final short CS_START = 0;
	protected static final short CS_CLIENT_HELLO = 1;
	protected static final short CS_SERVER_HELLO = 2;
	protected static final short CS_SERVER_SUPPLEMENTAL_DATA = 3;
	protected static final short CS_SERVER_CERTIFICATE = 4;
	protected static final short CS_CERTIFICATE_STATUS = 5;
	protected static final short CS_SERVER_KEY_EXCHANGE = 6;
	protected static final short CS_CERTIFICATE_REQUEST = 7;
	protected static final short CS_SERVER_HELLO_DONE = 8;
	protected static final short CS_CLIENT_SUPPLEMENTAL_DATA = 9;
	protected static final short CS_CLIENT_CERTIFICATE = 10;
	protected static final short CS_CLIENT_KEY_EXCHANGE = 11;
	protected static final short CS_CERTIFICATE_VERIFY = 12;
	protected static final short CS_CLIENT_FINISHED = 13;
	protected static final short CS_SERVER_SESSION_TICKET = 14;
	protected static final short CS_SERVER_FINISHED = 15;
	protected static final short CS_END = 16;

	private ByteQueue applicationDataQueue = new ByteQueue();
	private ByteQueue alertQueue = new ByteQueue(2);
	private ByteQueue handshakeQueue = new ByteQueue();

	protected RecordStream recordStream;
	protected SecureRandom secureRandom;

	private TlsInputStream tlsInputStream = null;
	private TlsOutputStream tlsOutputStream = null;

	private volatile boolean closed = false;
	private volatile boolean failedWithError = false;
	private volatile boolean appDataReady = false;
	private volatile boolean splitApplicationDataRecords = true;
	private byte[] expected_verify_data = null;

	protected TlsSession tlsSession = null;
	protected SessionParameters sessionParameters = null;
	protected SecurityParameters securityParameters = null;
	protected Certificate peerCertificate = null;

	protected int[] offeredCipherSuites = null;
	protected short[] offeredCompressionMethods = null;
	protected Hashtable clientExtensions = null;
	protected Hashtable serverExtensions = null;

	protected short connection_state = CS_START;
	protected boolean resumedSession = false;
	protected boolean receivedChangeCipherSpec = false;
	protected boolean secure_renegotiation = false;
	protected boolean allowCertificateStatus = false;
	protected boolean expectSessionTicket = false;

	public TlsProtocol(InputStream input, OutputStream output, SecureRandom secureRandom) {
		this.recordStream = new RecordStream(this, input, output);
		this.secureRandom = secureRandom;
	}

	protected abstract AbstractTlsContext getContext();

	protected abstract TlsPeer getPeer();

	protected void handleChangeCipherSpecMessage() {
	}

	protected abstract void handleHandshakeMessage(short type, byte[] buf) throws IOException;

	protected void cleanupHandshake() {
		if (this.expected_verify_data != null) {
			Arrays.fill(this.expected_verify_data, (byte) 0);
			this.expected_verify_data = null;
		}

		this.securityParameters.clear();
		this.peerCertificate = null;

		this.offeredCipherSuites = null;
		this.offeredCompressionMethods = null;
		this.clientExtensions = null;
		this.serverExtensions = null;

		this.resumedSession = false;
		this.receivedChangeCipherSpec = false;
		this.secure_renegotiation = false;
		this.allowCertificateStatus = false;
		this.expectSessionTicket = false;
	}

	protected void completeHandshake() throws IOException {
		try {

			while (this.connection_state != CS_END) {
				if (this.closed) {
				}

				safeReadRecord();
			}
			this.recordStream.finaliseHandshake();
			this.splitApplicationDataRecords = !TlsUtils.isTLSv11(getContext());

			if (!appDataReady) {
				this.appDataReady = true;

				this.tlsInputStream = new TlsInputStream(this);
				this.tlsOutputStream = new TlsOutputStream(this);
			}
			
			
			if (this.tlsSession != null) {
				if (this.sessionParameters == null) {
					this.sessionParameters = new SessionParameters.Builder()
							.setCipherSuite(this.securityParameters.cipherSuite)
							.setCompressionAlgorithm(this.securityParameters.compressionAlgorithm)
							.setMasterSecret(this.securityParameters.masterSecret)
							.setPeerCertificate(this.peerCertificate).setServerExtensions(this.serverExtensions)
							.build();

					this.tlsSession = new TlsSessionImpl(this.tlsSession.getSessionID(), this.sessionParameters);
				}

				getContext().setResumableSession(this.tlsSession);
			}
			getPeer().notifyHandshakeComplete();
		} finally {
			cleanupHandshake();
		}
	}

	protected void processRecord(short protocol, byte[] buf, int offset, int len) throws IOException {

		switch (protocol) {
		case ContentType.alert: {
			alertQueue.addData(buf, offset, len);
			processAlert();
			break;
		}
		case ContentType.application_data: {
			if (!appDataReady) {
				throw new TlsFatalAlert(AlertDescription.unexpected_message);
			}
			applicationDataQueue.addData(buf, offset, len);
			processApplicationData();
			break;
		}
		case ContentType.change_cipher_spec: {
			processChangeCipherSpec(buf, offset, len);
			break;
		}
		case ContentType.handshake: {
			handshakeQueue.addData(buf, offset, len);
			processHandshake();
			break;
		}
		case ContentType.heartbeat: {
			if (!appDataReady) {
				throw new TlsFatalAlert(AlertDescription.unexpected_message);
			}

		}
		default:

		}
	}

	private void processHandshake() throws IOException {
		boolean read;
		do {
			read = false;

			if (handshakeQueue.size() >= 4) {
				byte[] beginning = new byte[4];
				handshakeQueue.read(beginning, 0, 4, 0);
				ByteArrayInputStream bis = new ByteArrayInputStream(beginning);
				short type = TlsUtils.readUint8(bis);
				int len = TlsUtils.readUint24(bis);

				if (handshakeQueue.size() >= (len + 4)) {

					byte[] buf = handshakeQueue.removeData(len, 4);

					switch (type) {
					case HandshakeType.hello_request:
						break;
					case HandshakeType.finished: {
						if (this.expected_verify_data == null) {
							this.expected_verify_data = createVerifyData(!getContext().isServer());
						}

					}
					default:
						recordStream.updateHandshakeData(beginning, 0, 4);
						recordStream.updateHandshakeData(buf, 0, len);
						break;
					}

					handleHandshakeMessage(type, buf);
					read = true;
				}
			}
		} while (read);
	}

	private void processApplicationData() {

	}

	private void processAlert() throws IOException {
		while (alertQueue.size() >= 2) {

			byte[] tmp = alertQueue.removeData(2, 0);
			short level = tmp[0];
			short description = tmp[1];

			getPeer().notifyAlertReceived(level, description);

			if (level == AlertLevel.fatal) {

				invalidateSession();

				this.failedWithError = true;
				this.closed = true;

				recordStream.safeClose();

				throw new IOException(TLS_ERROR_MESSAGE);
			} 

			if (description == AlertDescription.close_notify) {
				handleClose(false);
			}
		}
	}

	private void processChangeCipherSpec(byte[] buf, int off, int len) throws IOException {
		for (int i = 0; i < len; ++i) {
			short message = TlsUtils.readUint8(buf, off + i);

			if (message != ChangeCipherSpec.change_cipher_spec) {
				throw new TlsFatalAlert(AlertDescription.decode_error);
			}

			if (this.receivedChangeCipherSpec || alertQueue.size() > 0 || handshakeQueue.size() > 0) {
				throw new TlsFatalAlert(AlertDescription.unexpected_message);
			}

			recordStream.receivedReadCipherSpec();

			this.receivedChangeCipherSpec = true;

			handleChangeCipherSpecMessage();
		}
	}

	protected int applicationDataAvailable() {
		return applicationDataQueue.size();
	}

	protected int readApplicationData(byte[] buf, int offset, int len) throws IOException {
		if (len < 1) {
			return 0;
		}

		while (applicationDataQueue.size() == 0) {

			if (this.closed) {
				if (this.failedWithError) {

					throw new IOException(TLS_ERROR_MESSAGE);
				}

				return -1;
			}
			try {
				safeReadRecord();
			} catch (EOFException e) {
				return -1;
			}
		}

		len = Math.min(len, applicationDataQueue.size());
		applicationDataQueue.removeData(buf, offset, len, 0);
		return len;
	}

	protected void safeReadRecord() throws IOException {
		try {
			if (!recordStream.readRecord()) {
				throw new EOFException();
			}
		} catch (EOFException e) {
			throw e;
		} catch (TlsFatalAlert e) {
			if (!this.closed) {
				this.failWithError(AlertLevel.fatal, e.getAlertDescription(), "Failed to read record", e);
			}
			throw e;
		} catch (IOException e) {
			if (!this.closed) {
				this.failWithError(AlertLevel.fatal, AlertDescription.internal_error, "Failed to read record", e);
			}
			throw e;
		} catch (RuntimeException e) {
			if (!this.closed) {
				this.failWithError(AlertLevel.fatal, AlertDescription.internal_error, "Failed to read record", e);
			}
			throw e;
		}
	}

	protected void safeWriteRecord(short type, byte[] buf, int offset, int len) throws IOException {
		try {
			recordStream.writeRecord(type, buf, offset, len);
		} catch (TlsFatalAlert e) {
			if (!this.closed) {
				this.failWithError(AlertLevel.fatal, e.getAlertDescription(), "Failed to write record", e);
			}
			throw e;
		} catch (IOException e) {
			if (!closed) {
				this.failWithError(AlertLevel.fatal, AlertDescription.internal_error, "Failed to write record", e);
			}
			throw e;
		} catch (RuntimeException e) {
			if (!closed) {
				this.failWithError(AlertLevel.fatal, AlertDescription.internal_error, "Failed to write record", e);
			}
			throw e;
		}
	}

	protected void writeData(byte[] buf, int offset, int len) throws IOException {
		if (this.closed) {
			if (this.failedWithError) {
				throw new IOException(TLS_ERROR_MESSAGE);
			}

			throw new IOException("Sorry, connection has been closed, you cannot write more data");
		}

		while (len > 0) {

			if (this.splitApplicationDataRecords) {

				safeWriteRecord(ContentType.application_data, buf, offset, 1);
				++offset;
				--len;
			}

			if (len > 0) {
				int toWrite = Math.min(len, recordStream.getPlaintextLimit());
				safeWriteRecord(ContentType.application_data, buf, offset, toWrite);
				offset += toWrite;
				len -= toWrite;
			}
		}
	}

	protected void writeHandshakeMessage(byte[] buf, int off, int len) throws IOException {
		while (len > 0) {
			int toWrite = Math.min(len, recordStream.getPlaintextLimit());
			safeWriteRecord(ContentType.handshake, buf, off, toWrite);
			off += toWrite;
			len -= toWrite;
		}
	}

	public OutputStream getOutputStream() {
		return this.tlsOutputStream;
	}

	public InputStream getInputStream() {
		return this.tlsInputStream;
	}

	protected void failWithError(short alertLevel, short alertDescription, String message, Exception cause)
			throws IOException {

		if (!closed) {

			this.closed = true;

			if (alertLevel == AlertLevel.fatal) {

				invalidateSession();

				this.failedWithError = true;
			}
			raiseAlert(alertLevel, alertDescription, message, cause);
			recordStream.safeClose();
			if (alertLevel != AlertLevel.fatal) {
				return;
			}
		}

		throw new IOException(TLS_ERROR_MESSAGE);
	}

	protected void invalidateSession() {
		if (this.sessionParameters != null) {
			this.sessionParameters.clear();
			this.sessionParameters = null;
		}

		if (this.tlsSession != null) {
			this.tlsSession.invalidate();
			this.tlsSession = null;
		}
	}

	protected void processFinishedMessage(ByteArrayInputStream buf) throws IOException {
		byte[] verify_data = TlsUtils.readFully(expected_verify_data.length, buf);

		assertEmpty(buf);

		if (!Arrays.constantTimeAreEqual(expected_verify_data, verify_data)) {

			throw new TlsFatalAlert(AlertDescription.decrypt_error);
		}
	}

	protected void raiseAlert(short alertLevel, short alertDescription, String message, Exception cause)
			throws IOException {
		getPeer().notifyAlertRaised(alertLevel, alertDescription, message, cause);

		byte[] error = new byte[2];
		error[0] = (byte) alertLevel;
		error[1] = (byte) alertDescription;

		safeWriteRecord(ContentType.alert, error, 0, 2);
	}

	protected void raiseWarning(short alertDescription, String message) throws IOException {
		raiseAlert(AlertLevel.warning, alertDescription, message, null);
	}

	protected void sendCertificateMessage(Certificate certificate) throws IOException {
		if (certificate == null) {
			certificate = Certificate.EMPTY_CHAIN;
		}

		if (certificate.getLength() == 0) {
			TlsContext context = getContext();
			if (!context.isServer()) {
				ProtocolVersion serverVersion = getContext().getServerVersion();
				if (serverVersion.isSSL()) {
					String message = serverVersion.toString() + " client didn't provide credentials";
					raiseWarning(AlertDescription.no_certificate, message);
					return;
				}
			}
		}

		HandshakeMessage message = new HandshakeMessage(HandshakeType.certificate);

		certificate.encode(message);

		message.writeToRecordStream();
	}

	protected void sendChangeCipherSpecMessage() throws IOException {
		byte[] message = new byte[] { 1 };
		safeWriteRecord(ContentType.change_cipher_spec, message, 0, message.length);
		recordStream.sentWriteCipherSpec();
	}

	protected void sendFinishedMessage() throws IOException {
		byte[] verify_data = createVerifyData(getContext().isServer());

		HandshakeMessage message = new HandshakeMessage(HandshakeType.finished, verify_data.length);

		message.write(verify_data);

		message.writeToRecordStream();
	}

	protected void sendSupplementalDataMessage(Vector supplementalData) throws IOException {
		HandshakeMessage message = new HandshakeMessage(HandshakeType.supplemental_data);

		writeSupplementalData(message, supplementalData);

		message.writeToRecordStream();
	}

	protected byte[] createVerifyData(boolean isServer) {
		TlsContext context = getContext();

		if (isServer) {
			return TlsUtils.calculateVerifyData(context, ExporterLabel.server_finished,
					getCurrentPRFHash(getContext(), recordStream.getHandshakeHash(), TlsUtils.SSL_SERVER));
		}

		return TlsUtils.calculateVerifyData(context, ExporterLabel.client_finished,
				getCurrentPRFHash(getContext(), recordStream.getHandshakeHash(), TlsUtils.SSL_CLIENT));
	}

	public void close() throws IOException {
		handleClose(true);
	}

	protected void handleClose(boolean user_canceled) throws IOException {
		if (!closed) {
			if (user_canceled && !appDataReady) {
				raiseWarning(AlertDescription.user_canceled, "User canceled handshake");
			}
			this.failWithError(AlertLevel.warning, AlertDescription.close_notify, "Connection closed", null);
		}
	}

	protected void flush() throws IOException {
		recordStream.flush();
	}

	protected short processMaxFragmentLengthExtension(Hashtable clientExtensions, Hashtable serverExtensions,
			short alertDescription) throws IOException {
		short maxFragmentLength = TlsExtensionsUtils.getMaxFragmentLengthExtension(serverExtensions);
		if (maxFragmentLength >= 0 && !this.resumedSession) {
			if (maxFragmentLength != TlsExtensionsUtils.getMaxFragmentLengthExtension(clientExtensions)) {
				throw new TlsFatalAlert(alertDescription);
			}
		}
		return maxFragmentLength;
	}

	protected static void assertEmpty(ByteArrayInputStream buf) throws IOException {
		if (buf.available() > 0) {
			throw new TlsFatalAlert(AlertDescription.decode_error);
		}
	}

	protected static byte[] createRandomBlock(boolean useGMTUnixTime, RandomGenerator randomGenerator) {
		byte[] result = new byte[32];
		randomGenerator.nextBytes(result);

		if (useGMTUnixTime) {
			TlsUtils.writeGMTUnixTime(result, 0);
		}

		return result;
	}

	protected static byte[] createRenegotiationInfo(byte[] renegotiated_connection) throws IOException {
		return TlsUtils.encodeOpaque8(renegotiated_connection);
	}

	protected static void establishMasterSecret(TlsContext context, TlsKeyExchange keyExchange) throws IOException {
		byte[] pre_master_secret = keyExchange.generatePremasterSecret();

		try {
			context.getSecurityParameters().masterSecret = TlsUtils.calculateMasterSecret(context, pre_master_secret);
		} finally {

			if (pre_master_secret != null) {
				Arrays.fill(pre_master_secret, (byte) 0);
			}
		}
	}

	protected static byte[] getCurrentPRFHash(TlsContext context, TlsHandshakeHash handshakeHash, byte[] sslSender) {
		Digest d = handshakeHash.forkPRFHash();

		if (sslSender != null && TlsUtils.isSSL(context)) {
			d.update(sslSender, 0, sslSender.length);
		}

		byte[] bs = new byte[d.getDigestSize()];
		d.doFinal(bs, 0);
		return bs;
	}

	protected static Hashtable readExtensions(ByteArrayInputStream input) throws IOException {
		if (input.available() < 1) {
			return null;
		}

		byte[] extBytes = TlsUtils.readOpaque16(input);

		assertEmpty(input);

		ByteArrayInputStream buf = new ByteArrayInputStream(extBytes);

		Hashtable extensions = new Hashtable();

		while (buf.available() > 0) {
			Integer extension_type = Integers.valueOf(TlsUtils.readUint16(buf));
			byte[] extension_data = TlsUtils.readOpaque16(buf);

			if (null != extensions.put(extension_type, extension_data)) {
				throw new TlsFatalAlert(AlertDescription.illegal_parameter);
			}
		}

		return extensions;
	}

	protected static Vector readSupplementalDataMessage(ByteArrayInputStream input) throws IOException {
		byte[] supp_data = TlsUtils.readOpaque24(input);

		assertEmpty(input);

		ByteArrayInputStream buf = new ByteArrayInputStream(supp_data);

		Vector supplementalData = new Vector();

		while (buf.available() > 0) {
			int supp_data_type = TlsUtils.readUint16(buf);
			byte[] data = TlsUtils.readOpaque16(buf);

			supplementalData.addElement(new SupplementalDataEntry(supp_data_type, data));
		}

		return supplementalData;
	}

	protected static void writeExtensions(OutputStream output, Hashtable extensions) throws IOException {
		ByteArrayOutputStream buf = new ByteArrayOutputStream();

		Enumeration keys = extensions.keys();
		while (keys.hasMoreElements()) {
			Integer key = (Integer) keys.nextElement();
			int extension_type = key.intValue();
			byte[] extension_data = (byte[]) extensions.get(key);

			TlsUtils.checkUint16(extension_type);
			TlsUtils.writeUint16(extension_type, buf);
			TlsUtils.writeOpaque16(extension_data, buf);
		}

		byte[] extBytes = buf.toByteArray();

		TlsUtils.writeOpaque16(extBytes, output);
	}

	protected static void writeSupplementalData(OutputStream output, Vector supplementalData) throws IOException {
		ByteArrayOutputStream buf = new ByteArrayOutputStream();

		for (int i = 0; i < supplementalData.size(); ++i) {
			SupplementalDataEntry entry = (SupplementalDataEntry) supplementalData.elementAt(i);

			int supp_data_type = entry.getDataType();
			TlsUtils.checkUint16(supp_data_type);
			TlsUtils.writeUint16(supp_data_type, buf);
			TlsUtils.writeOpaque16(entry.getData(), buf);
		}

		byte[] supp_data = buf.toByteArray();

		TlsUtils.writeOpaque24(supp_data, output);
	}

	protected static int getPRFAlgorithm(TlsContext context, int ciphersuite) throws IOException {
		boolean isTLSv12 = TlsUtils.isTLSv12(context);

		switch (ciphersuite) {
		case CipherSuite.TLS_DH_anon_WITH_CAMELLIA_128_CBC_SHA256:
		case CipherSuite.TLS_DH_anon_WITH_CAMELLIA_128_GCM_SHA256:
		case CipherSuite.TLS_DH_anon_WITH_CAMELLIA_256_CBC_SHA256:
		case CipherSuite.TLS_DH_DSS_WITH_AES_128_CBC_SHA256:
		case CipherSuite.TLS_DH_DSS_WITH_AES_128_GCM_SHA256:
		case CipherSuite.TLS_DH_DSS_WITH_AES_256_CBC_SHA256:
		case CipherSuite.TLS_DH_DSS_WITH_CAMELLIA_128_CBC_SHA256:
		case CipherSuite.TLS_DH_DSS_WITH_CAMELLIA_128_GCM_SHA256:
		case CipherSuite.TLS_DH_DSS_WITH_CAMELLIA_256_CBC_SHA256:
		case CipherSuite.TLS_DH_RSA_WITH_AES_128_CBC_SHA256:
		case CipherSuite.TLS_DH_RSA_WITH_AES_128_GCM_SHA256:
		case CipherSuite.TLS_DH_RSA_WITH_AES_256_CBC_SHA256:
		case CipherSuite.TLS_DH_RSA_WITH_CAMELLIA_128_CBC_SHA256:
		case CipherSuite.TLS_DH_RSA_WITH_CAMELLIA_128_GCM_SHA256:
		case CipherSuite.TLS_DH_RSA_WITH_CAMELLIA_256_CBC_SHA256:
		case CipherSuite.TLS_DHE_DSS_WITH_AES_128_CBC_SHA256:
		case CipherSuite.TLS_DHE_DSS_WITH_AES_128_GCM_SHA256:
		case CipherSuite.TLS_DHE_DSS_WITH_AES_256_CBC_SHA256:
		case CipherSuite.TLS_DHE_DSS_WITH_CAMELLIA_128_CBC_SHA256:
		case CipherSuite.TLS_DHE_DSS_WITH_CAMELLIA_128_GCM_SHA256:
		case CipherSuite.TLS_DHE_DSS_WITH_CAMELLIA_256_CBC_SHA256:
		case CipherSuite.TLS_DHE_PSK_WITH_AES_128_CCM:
		case CipherSuite.TLS_DHE_PSK_WITH_AES_128_GCM_SHA256:
		case CipherSuite.TLS_DHE_PSK_WITH_AES_256_CCM:
		case CipherSuite.TLS_DHE_PSK_WITH_CAMELLIA_128_GCM_SHA256:
		case CipherSuite.TLS_DHE_RSA_WITH_AES_128_CBC_SHA256:
		case CipherSuite.TLS_DHE_RSA_WITH_AES_128_CCM:
		case CipherSuite.TLS_DHE_RSA_WITH_AES_128_CCM_8:
		case CipherSuite.TLS_DHE_RSA_WITH_AES_128_GCM_SHA256:
		case CipherSuite.TLS_DHE_RSA_WITH_AES_256_CBC_SHA256:
		case CipherSuite.TLS_DHE_RSA_WITH_AES_256_CCM:
		case CipherSuite.TLS_DHE_RSA_WITH_AES_256_CCM_8:
		case CipherSuite.TLS_DHE_RSA_WITH_CAMELLIA_128_CBC_SHA256:
		case CipherSuite.TLS_DHE_RSA_WITH_CAMELLIA_128_GCM_SHA256:
		case CipherSuite.TLS_DHE_RSA_WITH_CAMELLIA_256_CBC_SHA256:
		case CipherSuite.TLS_DHE_RSA_WITH_CHACHA20_POLY1305_SHA256:
		case CipherSuite.TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA256:
		case CipherSuite.TLS_ECDH_ECDSA_WITH_AES_128_GCM_SHA256:
		case CipherSuite.TLS_ECDH_ECDSA_WITH_CAMELLIA_128_CBC_SHA256:
		case CipherSuite.TLS_ECDH_ECDSA_WITH_CAMELLIA_128_GCM_SHA256:
		case CipherSuite.TLS_ECDH_RSA_WITH_AES_128_CBC_SHA256:
		case CipherSuite.TLS_ECDH_RSA_WITH_AES_128_GCM_SHA256:
		case CipherSuite.TLS_ECDH_RSA_WITH_CAMELLIA_128_CBC_SHA256:
		case CipherSuite.TLS_ECDH_RSA_WITH_CAMELLIA_128_GCM_SHA256:
		case CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256:
		case CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256:
		case CipherSuite.TLS_ECDHE_ECDSA_WITH_CAMELLIA_128_CBC_SHA256:
		case CipherSuite.TLS_ECDHE_ECDSA_WITH_CAMELLIA_128_GCM_SHA256:
		case CipherSuite.TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256:
		case CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256:
		case CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256:
		case CipherSuite.TLS_ECDHE_RSA_WITH_CAMELLIA_128_CBC_SHA256:
		case CipherSuite.TLS_ECDHE_RSA_WITH_CAMELLIA_128_GCM_SHA256:
		case CipherSuite.TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256:
		case CipherSuite.TLS_PSK_DHE_WITH_AES_128_CCM_8:
		case CipherSuite.TLS_PSK_DHE_WITH_AES_256_CCM_8:
		case CipherSuite.TLS_PSK_WITH_AES_128_CCM:
		case CipherSuite.TLS_PSK_WITH_AES_128_CCM_8:
		case CipherSuite.TLS_PSK_WITH_AES_128_GCM_SHA256:
		case CipherSuite.TLS_PSK_WITH_AES_256_CCM:
		case CipherSuite.TLS_PSK_WITH_AES_256_CCM_8:
		case CipherSuite.TLS_PSK_WITH_CAMELLIA_128_GCM_SHA256:
		case CipherSuite.TLS_RSA_PSK_WITH_AES_128_GCM_SHA256:
		case CipherSuite.TLS_RSA_PSK_WITH_CAMELLIA_128_GCM_SHA256:
		case CipherSuite.TLS_RSA_WITH_AES_128_CBC_SHA256:
		case CipherSuite.TLS_RSA_WITH_AES_128_CCM:
		case CipherSuite.TLS_RSA_WITH_AES_128_CCM_8:
		case CipherSuite.TLS_RSA_WITH_AES_128_GCM_SHA256:
		case CipherSuite.TLS_RSA_WITH_AES_256_CBC_SHA256:
		case CipherSuite.TLS_RSA_WITH_AES_256_CCM:
		case CipherSuite.TLS_RSA_WITH_AES_256_CCM_8:
		case CipherSuite.TLS_RSA_WITH_CAMELLIA_128_CBC_SHA256:
		case CipherSuite.TLS_RSA_WITH_CAMELLIA_128_GCM_SHA256:
		case CipherSuite.TLS_RSA_WITH_CAMELLIA_256_CBC_SHA256:
		case CipherSuite.TLS_RSA_WITH_NULL_SHA256: {
			if (isTLSv12) {
				return PRFAlgorithm.tls_prf_sha256;
			}
			throw new TlsFatalAlert(AlertDescription.illegal_parameter);
		}

		case CipherSuite.TLS_DH_anon_WITH_CAMELLIA_256_GCM_SHA384:
		case CipherSuite.TLS_DH_DSS_WITH_AES_256_GCM_SHA384:
		case CipherSuite.TLS_DH_DSS_WITH_CAMELLIA_256_GCM_SHA384:
		case CipherSuite.TLS_DH_RSA_WITH_AES_256_GCM_SHA384:
		case CipherSuite.TLS_DH_RSA_WITH_CAMELLIA_256_GCM_SHA384:
		case CipherSuite.TLS_DHE_DSS_WITH_AES_256_GCM_SHA384:
		case CipherSuite.TLS_DHE_DSS_WITH_CAMELLIA_256_GCM_SHA384:
		case CipherSuite.TLS_DHE_PSK_WITH_AES_256_GCM_SHA384:
		case CipherSuite.TLS_DHE_PSK_WITH_CAMELLIA_256_GCM_SHA384:
		case CipherSuite.TLS_DHE_RSA_WITH_AES_256_GCM_SHA384:
		case CipherSuite.TLS_DHE_RSA_WITH_CAMELLIA_256_GCM_SHA384:
		case CipherSuite.TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA384:
		case CipherSuite.TLS_ECDH_ECDSA_WITH_AES_256_GCM_SHA384:
		case CipherSuite.TLS_ECDH_ECDSA_WITH_CAMELLIA_256_CBC_SHA384:
		case CipherSuite.TLS_ECDH_ECDSA_WITH_CAMELLIA_256_GCM_SHA384:
		case CipherSuite.TLS_ECDH_RSA_WITH_AES_256_CBC_SHA384:
		case CipherSuite.TLS_ECDH_RSA_WITH_AES_256_GCM_SHA384:
		case CipherSuite.TLS_ECDH_RSA_WITH_CAMELLIA_256_CBC_SHA384:
		case CipherSuite.TLS_ECDH_RSA_WITH_CAMELLIA_256_GCM_SHA384:
		case CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384:
		case CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384:
		case CipherSuite.TLS_ECDHE_ECDSA_WITH_CAMELLIA_256_CBC_SHA384:
		case CipherSuite.TLS_ECDHE_ECDSA_WITH_CAMELLIA_256_GCM_SHA384:
		case CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384:
		case CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384:
		case CipherSuite.TLS_ECDHE_RSA_WITH_CAMELLIA_256_CBC_SHA384:
		case CipherSuite.TLS_ECDHE_RSA_WITH_CAMELLIA_256_GCM_SHA384:
		case CipherSuite.TLS_PSK_WITH_AES_256_GCM_SHA384:
		case CipherSuite.TLS_PSK_WITH_CAMELLIA_256_GCM_SHA384:
		case CipherSuite.TLS_RSA_PSK_WITH_AES_256_GCM_SHA384:
		case CipherSuite.TLS_RSA_PSK_WITH_CAMELLIA_256_GCM_SHA384:
		case CipherSuite.TLS_RSA_WITH_AES_256_GCM_SHA384:
		case CipherSuite.TLS_RSA_WITH_CAMELLIA_256_GCM_SHA384: {
			if (isTLSv12) {
				return PRFAlgorithm.tls_prf_sha384;
			}
			throw new TlsFatalAlert(AlertDescription.illegal_parameter);
		}

		case CipherSuite.TLS_DHE_PSK_WITH_AES_256_CBC_SHA384:
		case CipherSuite.TLS_DHE_PSK_WITH_CAMELLIA_256_CBC_SHA384:
		case CipherSuite.TLS_DHE_PSK_WITH_NULL_SHA384:
		case CipherSuite.TLS_ECDHE_PSK_WITH_AES_256_CBC_SHA384:
		case CipherSuite.TLS_ECDHE_PSK_WITH_CAMELLIA_256_CBC_SHA384:
		case CipherSuite.TLS_ECDHE_PSK_WITH_NULL_SHA384:
		case CipherSuite.TLS_PSK_WITH_AES_256_CBC_SHA384:
		case CipherSuite.TLS_PSK_WITH_CAMELLIA_256_CBC_SHA384:
		case CipherSuite.TLS_PSK_WITH_NULL_SHA384:
		case CipherSuite.TLS_RSA_PSK_WITH_AES_256_CBC_SHA384:
		case CipherSuite.TLS_RSA_PSK_WITH_CAMELLIA_256_CBC_SHA384:
		case CipherSuite.TLS_RSA_PSK_WITH_NULL_SHA384: {
			if (isTLSv12) {
				return PRFAlgorithm.tls_prf_sha384;
			}
			return PRFAlgorithm.tls_prf_legacy;
		}

		default: {
			if (isTLSv12) {
				return PRFAlgorithm.tls_prf_sha256;
			}
			return PRFAlgorithm.tls_prf_legacy;
		}
		}
	}

	class HandshakeMessage extends ByteArrayOutputStream {
		HandshakeMessage(short handshakeType) throws IOException {
			this(handshakeType, 60);
		}

		HandshakeMessage(short handshakeType, int length) throws IOException {
			super(length + 4);
			TlsUtils.writeUint8(handshakeType, this);
			count += 3;
		}

		void writeToRecordStream() throws IOException {
			int length = count - 4;
			TlsUtils.checkUint24(length);
			TlsUtils.writeUint24(length, buf, 1);
			writeHandshakeMessage(buf, 0, count);
			buf = null;
		}
	}
}
