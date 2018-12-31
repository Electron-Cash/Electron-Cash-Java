package org.bouncycastle.crypto.tls;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class RecordStream {
	private static int DEFAULT_PLAINTEXT_LIMIT = (1 << 14);

	private TlsProtocol handler;
	private InputStream input;
	private OutputStream output;
	private TlsCompression pendingCompression = null, readCompression = null, writeCompression = null;
	private TlsCipher pendingCipher = null, readCipher = null, writeCipher = null;
	private long readSeqNo = 0, writeSeqNo = 0;
	private ByteArrayOutputStream buffer = new ByteArrayOutputStream();

	private TlsContext context = null;
	private TlsHandshakeHash handshakeHash = null;

	private ProtocolVersion readVersion = null, writeVersion = null;
	private boolean restrictReadVersion = true;

	private int plaintextLimit, compressedLimit, ciphertextLimit;

	RecordStream(TlsProtocol handler, InputStream input, OutputStream output) {
		this.handler = handler;
		this.input = input;
		this.output = output;
		this.readCompression = new TlsNullCompression();
		this.writeCompression = this.readCompression;
		this.readCipher = new TlsNullCipher(context);
		this.writeCipher = this.readCipher;

		setPlaintextLimit(DEFAULT_PLAINTEXT_LIMIT);
	}

	void init(TlsContext context) {
		this.context = context;
		this.handshakeHash = new DeferredHash();
		this.handshakeHash.init(context);
	}

	int getPlaintextLimit() {
		return plaintextLimit;
	}

	void setPlaintextLimit(int plaintextLimit) {
		this.plaintextLimit = plaintextLimit;
		this.compressedLimit = this.plaintextLimit + 1024;
		this.ciphertextLimit = this.compressedLimit + 1024;
	}

	ProtocolVersion getReadVersion() {
		return readVersion;
	}

	void setReadVersion(ProtocolVersion readVersion) {
		this.readVersion = readVersion;
	}

	void setWriteVersion(ProtocolVersion writeVersion) {
		this.writeVersion = writeVersion;
	}

	void setRestrictReadVersion(boolean enabled) {
		this.restrictReadVersion = enabled;
	}

	void setPendingConnectionState(TlsCompression tlsCompression, TlsCipher tlsCipher) {
		this.pendingCompression = tlsCompression;
		this.pendingCipher = tlsCipher;
	}

	void sentWriteCipherSpec() throws IOException {
		if (pendingCompression == null || pendingCipher == null) {
			throw new TlsFatalAlert(AlertDescription.handshake_failure);
		}
		this.writeCompression = this.pendingCompression;
		this.writeCipher = this.pendingCipher;
		this.writeSeqNo = 0;
	}

	void receivedReadCipherSpec() throws IOException {
		if (pendingCompression == null || pendingCipher == null) {
			throw new TlsFatalAlert(AlertDescription.handshake_failure);
		}
		this.readCompression = this.pendingCompression;
		this.readCipher = this.pendingCipher;
		this.readSeqNo = 0;
	}

	void finaliseHandshake() throws IOException {
		if (readCompression != pendingCompression || writeCompression != pendingCompression
				|| readCipher != pendingCipher || writeCipher != pendingCipher) {
			throw new TlsFatalAlert(AlertDescription.handshake_failure);
		}
		pendingCompression = null;
		pendingCipher = null;
	}

	public boolean readRecord() throws IOException {
		
		byte[] recordHeader = TlsUtils.readAllOrNothing(5, input);
		if (recordHeader == null) {
			return false;
		}

		short type = TlsUtils.readUint8(recordHeader, 0);

		checkType(type, AlertDescription.unexpected_message);

		if (!restrictReadVersion) {
			int version = TlsUtils.readVersionRaw(recordHeader, 1);
			if ((version & 0xffffff00) != 0x0300) {
				throw new TlsFatalAlert(AlertDescription.illegal_parameter);
			}
		} else {
			ProtocolVersion version = TlsUtils.readVersion(recordHeader, 1);
			if (readVersion == null) {
				readVersion = version;
			} else if (!version.equals(readVersion)) {
				throw new TlsFatalAlert(AlertDescription.illegal_parameter);
			}
		}

		int length = TlsUtils.readUint16(recordHeader, 3);
		byte[] plaintext = decodeAndVerify(type, input, length);
		handler.processRecord(type, plaintext, 0, plaintext.length);
		return true;
	}

	protected byte[] decodeAndVerify(short type, InputStream input, int len) throws IOException {
		checkLength(len, ciphertextLimit, AlertDescription.record_overflow);

		byte[] buf = TlsUtils.readFully(len, input);
		byte[] decoded = readCipher.decodeCiphertext(readSeqNo++, type, buf, 0, buf.length);

		checkLength(decoded.length, compressedLimit, AlertDescription.record_overflow);

		OutputStream cOut = readCompression.decompress(buffer);
		if (cOut != buffer) {
			cOut.write(decoded, 0, decoded.length);
			cOut.flush();
			decoded = getBufferContents();
		}

		checkLength(decoded.length, plaintextLimit, AlertDescription.decompression_failure);

		if (decoded.length < 1 && type != ContentType.application_data) {
			throw new TlsFatalAlert(AlertDescription.illegal_parameter);
		}

		return decoded;
	}

	protected void writeRecord(short type, byte[] plaintext, int plaintextOffset, int plaintextLength)
			throws IOException {
		if (writeVersion == null) {
			return;
		}

		checkType(type, AlertDescription.internal_error);

		checkLength(plaintextLength, plaintextLimit, AlertDescription.internal_error);

		if (plaintextLength < 1 && type != ContentType.application_data) {
			throw new TlsFatalAlert(AlertDescription.internal_error);
		}

		if (type == ContentType.handshake) {
			updateHandshakeData(plaintext, plaintextOffset, plaintextLength);
		}

		OutputStream cOut = writeCompression.compress(buffer);

		byte[] ciphertext;
		if (cOut == buffer) {
			ciphertext = writeCipher.encodePlaintext(writeSeqNo++, type, plaintext, plaintextOffset, plaintextLength);
		} else {
			cOut.write(plaintext, plaintextOffset, plaintextLength);
			cOut.flush();
			byte[] compressed = getBufferContents();

			checkLength(compressed.length, plaintextLength + 1024, AlertDescription.internal_error);

			ciphertext = writeCipher.encodePlaintext(writeSeqNo++, type, compressed, 0, compressed.length);
		}

		checkLength(ciphertext.length, ciphertextLimit, AlertDescription.internal_error);

		byte[] record = new byte[ciphertext.length + 5];
		TlsUtils.writeUint8(type, record, 0);
		TlsUtils.writeVersion(writeVersion, record, 1);
		TlsUtils.writeUint16(ciphertext.length, record, 3);
		System.arraycopy(ciphertext, 0, record, 5, ciphertext.length);
		output.write(record);
		output.flush();
	}

	void notifyHelloComplete() {
		this.handshakeHash = handshakeHash.notifyPRFDetermined();
	}

	TlsHandshakeHash getHandshakeHash() {
		return handshakeHash;
	}

	TlsHandshakeHash prepareToFinish() {
		TlsHandshakeHash result = handshakeHash;
		this.handshakeHash = handshakeHash.stopTracking();
		return result;
	}

	void updateHandshakeData(byte[] message, int offset, int len) {
		handshakeHash.update(message, offset, len);
	}

	protected void safeClose() {
		try {
			input.close();
		} catch (IOException e) {
		}

		try {
			output.close();
		} catch (IOException e) {
		}
	}

	protected void flush() throws IOException {
		output.flush();
	}

	private byte[] getBufferContents() {
		byte[] contents = buffer.toByteArray();
		buffer.reset();
		return contents;
	}

	private static void checkType(short type, short alertDescription) throws IOException {
		switch (type) {
		case ContentType.application_data:
		case ContentType.alert:
		case ContentType.change_cipher_spec:
		case ContentType.handshake:
		case ContentType.heartbeat:
			break;
		default:
			throw new TlsFatalAlert(alertDescription);
		}
	}

	private static void checkLength(int length, int limit, short alertDescription) throws IOException {
		if (length > limit) {
			throw new TlsFatalAlert(alertDescription);
		}
	}
}
