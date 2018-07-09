package electrol.main;

import java.util.Vector;

import org.bouncycastle.util.Arrays;

import electrol.util.BigInteger;

public class Deserialize {
	private int version;
	private long inputCount = 0;
	private Vector inputs = new Vector();
	private long outputCount = 0;
	private Vector outputs = new Vector();
	private long lockTime;

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public long getInputCount() {
		return inputCount;
	}

	public void setInputCount(long inputCount) {
		this.inputCount = inputCount;
	}

	public Vector getInputs() {
		return inputs;
	}

	public void setInputs(Vector inputs) {
		this.inputs = inputs;
	}

	public long getOutputCount() {
		return outputCount;
	}

	public void setOutputCount(long outputCount) {
		this.outputCount = outputCount;
	}

	public Vector getOutputs() {
		return outputs;
	}

	public void setOutputs(Vector outputs) {
		this.outputs = outputs;
	}

	public long getLockTime() {
		return lockTime;
	}

	public void setLockTime(long lockTime) {
		this.lockTime = lockTime;
	}

	public static void main(String[] args) {
		Deserialize d =parse("010000000188c2c88cbb4bfb067693079a5216f9733876f30e25d2987ee9ced44454358a41010000006a47304402202905b3fe4bea550da5a5f93406a2b586931e05492f9fc98ac799c64f4645abef02200ae66129517407977267085907fc15563d5b0f2ec9fc25314a851d5453679ca7412102caff869df6f5b6abb9b5a86691f5701d3a337df1c1dc67ba0b36a2d70d6a20c2feffffff02e40c0000000000001976a914f7ac43e762da97d5d0cd5fe56207b7863eee51c788ac586f0100000000001976a914b9f54603bf5225400d65e4ec6364277cb300f0c188ac03050800");
		System.out.println(d.getInputCount());
	}

	public static Deserialize parse(String txData) {
		Deserialize tx = new Deserialize();
		byte[] rawTx = ByteUtilities.toByteArray(txData);
		int buffPointer = 0;

		// Version
		byte[] version = ByteUtilities.readBytes(rawTx, buffPointer, 4);
		buffPointer += 4;
		version = ByteUtilities.flipEndian(version);
		tx.setVersion(new BigInteger(1, version).intValue());

		// Number of inputs
		VariableInt varInputCount = readVariableInt(rawTx, buffPointer);
		buffPointer += varInputCount.getSize();
		tx.setInputCount(varInputCount.getValue());

		// Parse inputs
		for (long i = 0; i < tx.getInputCount(); i++) {
			byte[] inputData = Arrays.copyOfRange(rawTx, buffPointer, rawTx.length);
			RawInput input = RawInput.parse(ByteUtilities.toHexString(inputData));
			buffPointer += input.getDataSize();
			tx.getInputs().addElement(input);
		}

		// Get the number of outputs
		VariableInt varOutputCount = readVariableInt(rawTx, buffPointer);
		buffPointer += varOutputCount.getSize();
		tx.setOutputCount(varOutputCount.getValue());

		// Parse outputs
		for (long i = 0; i < tx.getOutputCount(); i++) {
			byte[] outputData = Arrays.copyOfRange(rawTx, buffPointer, rawTx.length);
			RawOutput output = RawOutput.parse(ByteUtilities.toHexString(outputData));
			buffPointer += output.getDataSize();
			tx.getOutputs().addElement(output);
		}

		// Parse lock time
		byte[] lockBytes = ByteUtilities.readBytes(rawTx, buffPointer, 4);
		buffPointer += 4;
		lockBytes = ByteUtilities.flipEndian(lockBytes);
		tx.setLockTime(new BigInteger(1, lockBytes).longValue());

		return tx;
	}

	public static VariableInt readVariableInt(byte[] data, int start) {
		int checkSize = 0xFF & data[start];
		VariableInt varInt = new VariableInt();
		varInt.setSize(0);

		if (checkSize < 0xFD) {
			varInt.setSize(1);
			varInt.setValue(checkSize);
			return varInt;
		}

		if (checkSize == 0xFD) {
			varInt.setSize(3);
		} else if (checkSize == 0xFE) {
			varInt.setSize(5);
		} else if (checkSize == 0xFF) {
			varInt.setSize(9);
		}

		if (varInt.getSize() == 0) {
			return null;
		}

		byte[] newData = ByteUtilities.readBytes(data, start + 1, varInt.getSize() - 1);
		newData = ByteUtilities.flipEndian(newData);
		varInt.setValue(new BigInteger(1, newData).longValue());
		return varInt;
	}

	public static byte[] writeVariableInt(long data) {
		byte[] newData;

		if (data < 0x00FD) {
			newData = new byte[1];
			newData[0] = (byte) (data & 0xFF);
		} else if (data <= 0xFFFF) {
			newData = new byte[3];
			newData[0] = (byte) 0xFD;
		} else if (data <= 4294967295L /* 0xFFFFFFFF */) {
			newData = new byte[5];
			newData[0] = (byte) 0xFE;
		} else {
			newData = new byte[9];
			newData[0] = (byte) 0xFF;
		}

		byte[] intData = BigInteger.valueOf(data).toByteArray();
		intData = ByteUtilities.stripLeadingNullBytes(intData);
		intData = ByteUtilities.leftPad(intData, newData.length - 1, (byte) 0x00);
		intData = ByteUtilities.flipEndian(intData);

		for (int i = 0; i < (newData.length - 1); i++) {
			newData[i + 1] = intData[i];
		}

		return newData;
	}

	public static class VariableInt {
		int size;
		long value;

		public int getSize() {
			return size;
		}

		public void setSize(int size) {
			this.size = size;
		}

		public long getValue() {
			return value;
		}

		public void setValue(long value) {
			this.value = value;
		}
	}

}
