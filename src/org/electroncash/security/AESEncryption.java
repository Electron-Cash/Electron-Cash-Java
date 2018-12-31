package org.electroncash.security;

import org.bouncycastle.asn1.x9.ECNamedCurveTable;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PKCS7Padding;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECFieldElement;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.Hex;
import org.electroncash.util.BigInteger;
import org.electroncash.util.StringUtils;


public class AESEncryption
{
  private byte[] out = new byte[32];
  
  private AESEncryption() {}
  
  public static AESEncryption getInstance()
  {
    return new AESEncryption();
  }
  
  private ECPublicKeyParameters getECPublicKeyParameters(byte[] secretExponentBytes)
  {
    X9ECParameters ecParameter = ECNamedCurveTable.getByName("secp256k1");
    ECCurve curve = ecParameter.getCurve();
    BigInteger secretExponent = new BigInteger(secretExponentBytes);
    ECPoint ecPoint = ecParameter.getG().multiply(secretExponent);
    ECDomainParameters parameters = new ECDomainParameters(curve, ecPoint, ecParameter.getN(), ecParameter.getH(), ecParameter.getSeed());
    ECPublicKeyParameters ecPublicKey = new ECPublicKeyParameters(ecPoint, parameters);
    return ecPublicKey;
  }
  
  public byte[] process(byte[] keyBytes, boolean encryption) throws DataLengthException, IllegalStateException, InvalidCipherTextException
  {
    BufferedBlockCipher encryptorCipher = new PaddedBufferedBlockCipher(
      new CBCBlockCipher(new AESEngine()), new PKCS7Padding());
    encryptorCipher.init(encryption, new KeyParameter(out));
    byte[] output = new byte[encryptorCipher.getOutputSize(keyBytes.length)];
    int ciphertextLength = encryptorCipher.processBytes(keyBytes, 0, keyBytes.length, output, 0);
    encryptorCipher.doFinal(output, ciphertextLength);
    return output;
  }
  
  public byte[] encrypt(String message) throws DataLengthException, IllegalStateException, InvalidCipherTextException {
    return encrypt(message.getBytes());
  }
  
  public byte[] encrypt(byte[] message) throws DataLengthException, IllegalStateException, InvalidCipherTextException {
    byte[] out = process(message, true);
    return Base64.encode(org.bouncycastle.util.Arrays.concatenate("BIE1".getBytes(), out));
  }
  
  public byte[] decrypt(String message) throws DataLengthException, IllegalStateException, InvalidCipherTextException
  {
    byte[] out = Base64.decode(message);
    return decryptBytes(out);
  }
  
  public byte[] decrypt(byte[] message) throws DataLengthException, IllegalStateException, InvalidCipherTextException {
    byte[] out = Base64.decode(message);
    return decryptBytes(out);
  }
  
  private byte[] decryptBytes(byte[] out) throws DataLengthException, IllegalStateException, InvalidCipherTextException {
    String magic = new String(org.electroncash.util.Arrays.slice(out, 0, 4));
    if ("BIE1".equals(magic)) {
      byte[] pre = org.electroncash.util.Arrays.slice(out, 4, out.length);
      return process(pre, false);
    }
    return out;
  }
  
  public void init(byte[] secret) {
    PKCS5S2ParametersGenerator gen = new PKCS5S2ParametersGenerator(new SHA512Digest());
    gen.init(secret, new byte[0], 1024);
    byte[] dk = ((KeyParameter)gen.generateDerivedParameters(512)).getKey();
    String publicKey = getHexPubKeyfromECkeys(dk, true);
    byte[] keybytes = Hex.decode(publicKey);
    Digest digest = new SHA256Digest();
    digest.update(keybytes, 0, keybytes.length);
    
    digest.doFinal(out, 0);
  }
  
  public void init(String secret) {
    init(secret.getBytes());
  }
  
  private String getHexPubKeyfromECkeys(byte[] dk, boolean compressed) {
    ECPublicKeyParameters ecPublicKey = getECPublicKeyParameters(dk);
    ECPoint ec = ecPublicKey.getQ();
    BigInteger affineXCoord = ec.getAffineXCoord().toBigInteger();
    BigInteger affineYCoord = ec.getAffineYCoord().toBigInteger();
    if (!compressed) {
      return StringUtils.format(affineXCoord, 64) + StringUtils.format(affineYCoord, 64);
    }
    if (affineYCoord.and(BigInteger.ONE).compareTo(BigInteger.ONE) == 0) {
      return "03" + StringUtils.format(affineXCoord, 64);
    }
    return "02" + StringUtils.format(affineXCoord, 64);
  }
}
