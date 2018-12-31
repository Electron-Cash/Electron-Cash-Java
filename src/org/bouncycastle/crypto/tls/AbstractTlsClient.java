package org.bouncycastle.crypto.tls;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

public abstract class AbstractTlsClient
    extends AbstractTlsPeer
    implements TlsClient
{
    protected TlsCipherFactory cipherFactory;

    protected TlsClientContext context;

    protected Vector supportedSignatureAlgorithms;
    protected int[] namedCurves;
    protected short[] clientECPointFormats, serverECPointFormats;

    protected int selectedCipherSuite;
    protected short selectedCompressionMethod;

    public AbstractTlsClient()
    {
        this(new DefaultTlsCipherFactory());
    }

    public AbstractTlsClient(TlsCipherFactory cipherFactory)
    {
        this.cipherFactory = cipherFactory;
    }

    public void init(TlsClientContext context)
    {
        this.context = context;
    }

    public TlsSession getSessionToResume()
    {
        return null;
    }

    public ProtocolVersion getClientHelloRecordLayerVersion()
    {
        return getClientVersion();
    }

    public ProtocolVersion getClientVersion()
    {
        return ProtocolVersion.TLSv12;
    }

    public Hashtable getClientExtensions()
        throws IOException
    {
        Hashtable clientExtensions = null;

        ProtocolVersion clientVersion = context.getClientVersion();

        /*
         * RFC 5246 7.4.1.4.1. Note: this extension is not meaningful for TLS versions prior to 1.2.
         * Clients MUST NOT offer it if they are offering prior versions.
         */
        if (TlsUtils.isSignatureAlgorithmsExtensionAllowed(clientVersion))
        {
            // TODO Provide a way for the user to specify the acceptable hash/signature algorithms.

            short[] hashAlgorithms = new short[]{ HashAlgorithm.sha512, HashAlgorithm.sha384, HashAlgorithm.sha256,
                HashAlgorithm.sha224, HashAlgorithm.sha1 };

            // TODO Sort out ECDSA signatures and add them as the preferred option here
            short[] signatureAlgorithms = new short[]{ SignatureAlgorithm.rsa };

            this.supportedSignatureAlgorithms = new Vector();
            for (int i = 0; i < hashAlgorithms.length; ++i)
            {
                for (int j = 0; j < signatureAlgorithms.length; ++j)
                {
                    this.supportedSignatureAlgorithms.addElement(new SignatureAndHashAlgorithm(hashAlgorithms[i],
                        signatureAlgorithms[j]));
                }
            }

            /*
             * RFC 5264 7.4.3. Currently, DSA [DSS] may only be used with SHA-1.
             */
            this.supportedSignatureAlgorithms.addElement(new SignatureAndHashAlgorithm(HashAlgorithm.sha1,
                SignatureAlgorithm.dsa));

            clientExtensions = TlsExtensionsUtils.ensureExtensionsInitialised(clientExtensions);

            TlsUtils.addSignatureAlgorithmsExtension(clientExtensions, supportedSignatureAlgorithms);
        }

        if (TlsECCUtils.containsECCCipherSuites(getCipherSuites()))
        {
            this.namedCurves = new int[]{ NamedCurve.secp256r1, NamedCurve.secp384r1 };
            this.clientECPointFormats = new short[]{ ECPointFormat.uncompressed,
                ECPointFormat.ansiX962_compressed_prime, ECPointFormat.ansiX962_compressed_char2, };

            clientExtensions = TlsExtensionsUtils.ensureExtensionsInitialised(clientExtensions);

            TlsECCUtils.addSupportedEllipticCurvesExtension(clientExtensions, namedCurves);
            TlsECCUtils.addSupportedPointFormatsExtension(clientExtensions, clientECPointFormats);
        }

        return clientExtensions;
    }

    public ProtocolVersion getMinimumVersion()
    {
        return ProtocolVersion.TLSv10;
    }

    public void notifyServerVersion(ProtocolVersion serverVersion)
        throws IOException
    {
        if (!getMinimumVersion().isEqualOrEarlierVersionOf(serverVersion))
        {
            throw new TlsFatalAlert(AlertDescription.protocol_version);
        }
    }

    public short[] getCompressionMethods()
    {
        return new short[]{CompressionMethod._null};
    }

    public void notifySessionID(byte[] sessionID)
    {
        // Currently ignored
    }

    public void notifySelectedCipherSuite(int selectedCipherSuite)
    {
        this.selectedCipherSuite = selectedCipherSuite;
    }

    public void notifySelectedCompressionMethod(short selectedCompressionMethod)
    {
        this.selectedCompressionMethod = selectedCompressionMethod;
    }

    public void processServerExtensions(Hashtable serverExtensions)
        throws IOException
    {
        if (serverExtensions != null)
        {
            /*
             * RFC 5246 7.4.1.4.1. Servers MUST NOT send this extension.
             */
            if (serverExtensions.containsKey(TlsUtils.EXT_signature_algorithms))
            {
                throw new TlsFatalAlert(AlertDescription.illegal_parameter);
            }

            int[] namedCurves = TlsECCUtils.getSupportedEllipticCurvesExtension(serverExtensions);
            if (namedCurves != null)
            {
                throw new TlsFatalAlert(AlertDescription.illegal_parameter);
            }

            this.serverECPointFormats = TlsECCUtils.getSupportedPointFormatsExtension(serverExtensions);
            if (this.serverECPointFormats != null && !TlsECCUtils.isECCCipherSuite(this.selectedCipherSuite))
            {
                throw new TlsFatalAlert(AlertDescription.illegal_parameter);
            }
        }
    }

    public void processServerSupplementalData(Vector serverSupplementalData)
        throws IOException
    {
        if (serverSupplementalData != null)
        {
            throw new TlsFatalAlert(AlertDescription.unexpected_message);
        }
    }

    public Vector getClientSupplementalData()
        throws IOException
    {
        return null;
    }

    public TlsCompression getCompression()
        throws IOException
    {
        switch (selectedCompressionMethod)
        {
        case CompressionMethod._null:
            return new TlsNullCompression();

        default:
            throw new TlsFatalAlert(AlertDescription.internal_error);
        }
    }

    public void notifyNewSessionTicket(NewSessionTicket newSessionTicket)
        throws IOException
    {
    }
}
