package electrol;

public class Version {
	public static final String PACKAGE_VERSION = "3.1.5";     //version of the client package
	public static final String PROTOCOL_VERSION = "1.1";     //protocol version requested

    //The hash of the mnemonic seed must begin with this
	public static final String 	SEED_PREFIX  = "01";      //Standard wallet


	public static String seed_prefix(String seed_type) {
		//assert seed_type.equals("standard");
		return SEED_PREFIX;
	}
}
