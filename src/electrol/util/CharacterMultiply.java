package electrol.util;

public class CharacterMultiply {
	public static String multiply(String ch, int times) {
		StringBuffer buffer = new StringBuffer(times);
		for(int i =0;i<times;i++) {
			buffer.append(ch);
		}
		return buffer.toString();
	}
}
