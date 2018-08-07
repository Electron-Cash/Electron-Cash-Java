package electrol.java.neo;

public class ByteOrder {

	public static final ByteOrder BIG_ENDIAN = new ByteOrder();
	public static final ByteOrder LITTLE_ENDIAN = new ByteOrder();
	public static ByteOrder nativeOrder() {
         throw new UnsupportedOperationException();
	}

}
