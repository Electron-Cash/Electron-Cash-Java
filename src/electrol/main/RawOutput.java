package electrol.main;

import electrol.main.Deserialize.VariableInt;
import electrol.util.BigInteger;

public class RawOutput {

	private long amount;
	private long scriptSize = 0;
	private String script = "";

	public long getAmount() {
		return amount;
	}

	public void setAmount(long amount) {
		this.amount = amount;
	}

	public long getScriptSize() {
		return scriptSize;
	}

	public void setScriptSize(long scriptSize) {
		this.scriptSize = scriptSize;
	}

	public String getScript() {
		return script;
	}

	public void setScript(String script) {
		this.script = script;
	}

	public String toString() {
		return "RawOutput [amount=" + amount + ", scriptSize=" + scriptSize + ", script=" + script + "]";
	}

	
	public static RawOutput parse(String txData) {
		RawOutput output = new RawOutput();
		byte[] rawTx = ByteUtilities.toByteArray(txData);
		int buffPointer = 0;

		byte[] satoshiBytes = ByteUtilities.readBytes(rawTx, buffPointer, 8);
		buffPointer += 8;
		satoshiBytes = ByteUtilities.flipEndian(satoshiBytes);
		output.setAmount(new BigInteger(1, satoshiBytes).longValue());

		VariableInt varScriptSize = Deserialize.readVariableInt(rawTx, buffPointer);
		buffPointer += varScriptSize.getSize();
		output.setScriptSize(varScriptSize.getValue());

		byte[] scriptBytes = ByteUtilities.readBytes(rawTx, buffPointer, (int) output.getScriptSize());
		output.setScript(ByteUtilities.toHexString(scriptBytes));

		return output;
	}

	public long getDataSize() {
		int sizeSize = Deserialize.writeVariableInt(getScriptSize()).length;
		return 8 + sizeSize + getScriptSize();
	}
}