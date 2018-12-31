package org.electroncash.util;

public final class RawInput { private String txHash;
  private int txIndex;
  
  public RawInput() {}
  
  private long scriptSize = 0L;
  private byte[] script;
  private long sequence = -1L;
  
  public String getTxHash() {
    return txHash;
  }
  
  public void setTxHash(String txHash) {
    this.txHash = txHash;
  }
  
  public int getTxIndex() {
    return txIndex;
  }
  
  public void setTxIndex(int txIndex) {
    this.txIndex = txIndex;
  }
  
  public long getScriptSize() {
    return scriptSize;
  }
  
  public void setScriptSize(long scriptSize) {
    this.scriptSize = scriptSize;
  }
  
  public byte[] getScript() {
    return script;
  }
  
  public void setScript(byte[] script) {
    this.script = script;
  }
  
  public long getSequence() {
    return sequence;
  }
  
  public void setSequence(long sequence) {
    this.sequence = sequence;
  }
  
  public String toString() {
    return 
      "RawInput [txHash=" + txHash + ", txIndex=" + txIndex + ", scriptSize=" + scriptSize + ", script=" + script + ", sequence=" + sequence + "]";
  }
  
  public static RawInput parse(String txData) {
    RawInput input = new RawInput();
    byte[] rawTx = ByteUtilities.toByteArray(txData);
    int buffPointer = 0;
    
    byte[] hashBytes = ByteUtilities.readBytes(rawTx, buffPointer, 32);
    buffPointer += 32;
    hashBytes = ByteUtilities.flipEndian(hashBytes);
    input.setTxHash(ByteUtilities.toHexString(hashBytes));
    
    byte[] indexBytes = ByteUtilities.readBytes(rawTx, buffPointer, 4);
    buffPointer += 4;
    indexBytes = ByteUtilities.flipEndian(indexBytes);
    input.setTxIndex(new BigInteger(1, indexBytes).intValue());
    
    Deserialize.VariableInt varScriptSize = Deserialize.readVariableInt(rawTx, buffPointer);
    buffPointer += varScriptSize.getSize();
    input.setScriptSize(varScriptSize.getValue());
    
    byte[] scriptBytes = ByteUtilities.readBytes(rawTx, buffPointer, (int)input.getScriptSize());
    buffPointer = (int)(buffPointer + input.getScriptSize());
    input.setScript(scriptBytes);
    
    byte[] sequenceBytes = ByteUtilities.readBytes(rawTx, buffPointer, 4);
    buffPointer += 4;
    sequenceBytes = ByteUtilities.flipEndian(sequenceBytes);
    input.setSequence(new BigInteger(1, sequenceBytes).longValue());
    
    return input;
  }
  
  public long getDataSize() {
    int sizeSize = Deserialize.writeVariableInt(getScriptSize()).length;
    return 36 + sizeSize + getScriptSize() + 4L;
  }
}
