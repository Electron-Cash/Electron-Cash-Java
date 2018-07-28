package electrol.main;

public class Script{

	byte[] toBytes(int byte1, int byte2) {
		byte[] b = new byte[0];
		b= org.bouncycastle.util.Arrays.concatenate(b, toBytes(byte1));
		b= org.bouncycastle.util.Arrays.concatenate(b, toBytes(byte2));
		return b;
	}

	byte[] toBytes(int i){

		byte b1 = (byte) (i >> 24);
		byte b2 = (byte) (i >> 16);
		byte b3 = (byte) (i >> 8);
		byte b4 = (byte) i;
		if(b4 != 0 && b2 == 0 && b2 ==0 && b1 ==0) {
			return new byte[] {b4};
		}
		if(b3 != 0 && b2 ==0 && b1 ==0) {
			return new byte[] {b4, b3};
		}
		if(b2 !=0 && b1 ==0) {
			return new byte[] {b4, b3, b2};
		}
		if(b1 !=0) {
			return new byte[] {b4, b3,b2,b1};
		}
		return new byte[] {};
	}

	byte[] pack(short s) {
		byte b1 = (byte)(s >> 8);
		byte b2 = (byte) s;
		return new byte[] {b2,b1};
	}
	byte[] pack(int i) {
		byte b1 = (byte) (i >> 24);
		byte b2 = (byte) (i >> 16);
		byte b3 = (byte) (i >> 8);
		byte b4 = (byte) i;
		return new byte[] {b4,b3,b2,b1};
	}

	public byte[] P2PKH_script(byte[] hash160) {
		byte[] b = toBytes(AddressUtil.OP_DUP, AddressUtil.OP_HASH160);

		b = org.bouncycastle.util.Arrays.concatenate(b, push(hash160));
		byte[] c = toBytes(AddressUtil.OP_EQUALVERIFY, AddressUtil.OP_CHECKSIG);
		b = org.bouncycastle.util.Arrays.concatenate(b, c);       
		return b;
	}


	public byte[] P2SH_script(byte[] hash160) {
		byte[] b = toBytes(AddressUtil.OP_HASH160);
		b = org.bouncycastle.util.Arrays.concatenate(b, push(hash160));
		b = org.bouncycastle.util.Arrays.concatenate(b, toBytes(AddressUtil.OP_EQUAL));
		return b;
	}

	public byte[] push(byte[] data) {
		int n = data.length;
		if(n < AddressUtil.OP_PUSHDATA1) {
			byte[] b = toBytes(n);
			return org.bouncycastle.util.Arrays.concatenate(b, data);
		}
		if(n < 256) {
			byte[] b = toBytes(AddressUtil.OP_PUSHDATA1,n);
			return org.bouncycastle.util.Arrays.concatenate(b, data);
		}
		if(n < 65536) {
			byte[] b = toBytes(AddressUtil.OP_PUSHDATA2);
			b = org.bouncycastle.util.Arrays.concatenate(b, pack((short)n));
			return org.bouncycastle.util.Arrays.concatenate(b, data);
		}
		byte[] b = toBytes(AddressUtil.OP_PUSHDATA4);
		b = org.bouncycastle.util.Arrays.concatenate(b, pack(n));
		return org.bouncycastle.util.Arrays.concatenate(b, data);

	}
}