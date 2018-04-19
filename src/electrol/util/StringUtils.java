package electrol.util;

public class StringUtils {
	
	private static final String HEXES    = "0123456789abcdef";
	private static final String ZERO = "0000000000000000000000000000000000000000000000000000000000000000";
	
	public static boolean contains(String s, String a) {
		int subStringLenth = a.length();
		for(int i=0;(i+subStringLenth) <= s.length();i++) {
			if(s.substring(i,i+subStringLenth).equals(a)) {
				return true;
			}
		}
		return false;
	}
	public static String replaceMultipleSpacesFromString(String s){
	    if(s.length() == 0 ) 
	    	return "";
	    int timesSpace = 0;
	    String res = "";

	    for (int i = 0; i < s.length(); i++) {
	        char c = s.charAt(i);

	        if(c == ' '){
	            timesSpace++;
	            if(timesSpace < 2)
	                res += c;
	        }else{
	            res += c;
	            timesSpace = 0;
	        }
	    }
	    return res.trim();
	}
	public static String format(byte b, int size) {
		StringBuffer buffer = new StringBuffer(2);
		buffer
		.append(HEXES.charAt((b & 0xF0) >> 4))
		.append(HEXES.charAt((b & 0x0F)));
		return buffer.toString();
	}
	public static String format(int b, int size) {
		StringBuffer buffer = new StringBuffer(2);
		buffer
		.append(HEXES.charAt((b & 0xF0) >> 4))
		.append(HEXES.charAt((b & 0x0F)));
		return buffer.toString();
	}
	public static String[] split(String sb, String splitter){
	    String[] strs = new String[sb.length()];
	    int splitterLength = splitter.length();
	    int initialIndex = 0;
	    int indexOfSplitter = indexOf(sb, splitter, initialIndex);
	    int count = 0;
	    if(-1==indexOfSplitter) return new String[]{sb};
	    while(-1!=indexOfSplitter){
	        char[] chars = new char[indexOfSplitter-initialIndex];
	        sb.getChars(initialIndex, indexOfSplitter, chars, 0);
	        initialIndex = indexOfSplitter+splitterLength;
	        indexOfSplitter = indexOf(sb, splitter, indexOfSplitter+1);
	        strs[count] = new String(chars);
	        count++;
	    }
	    // get the remaining chars.
	    if(initialIndex+splitterLength<=sb.length()){
	        char[] chars = new char[sb.length()-initialIndex];
	        sb.getChars(initialIndex, sb.length(), chars, 0);
	        strs[count] = new String(chars);
	        count++;
	    }
	    String[] result = new String[count];
	    for(int i = 0; i<count; i++){
	        result[i] = strs[i];
	    }
	    return result;
	}
	
	public static int indexOf(String sb, String str, int start){
	    int index = -1;
	    if((start>=sb.length() || start<-1) || str.length()<=0) return index;
	    char[] tofind = str.toCharArray();
	    outer: for(;start<sb.length(); start++){
	        char c = sb.charAt(start);
	        if(c==tofind[0]){
	            if(1==tofind.length) return start;
	            inner: for(int i = 1; i<tofind.length;i++){ // start on the 2nd character
	                char find = tofind[i];
	                int currentSourceIndex = start+i;
	                if(currentSourceIndex<sb.length()){
	                    char source = sb.charAt(start+i);
	                    if(find==source){
	                        if(i==tofind.length-1){
	                            return start;
	                        }
	                        continue inner;
	                    } else {
	                        start++;
	                        continue outer;
	                    }
	                } else {
	                    return -1;
	                }

	            }
	        }
	    }
	    return index;
	}
	
	public static String reverse(String str) {
    	int length = str.length();
    	StringBuffer newString = new StringBuffer(length);
    	for(int i=length-1;i >= 0 ;i--) {
    		newString.append(str.charAt(i));
    	}
    	return newString.toString();
    }
	public static String format(BigInteger coord, int size) {
		String hex = coord.toString(16);
		int length = hex.length();
		int appendLength = size - length;
		StringBuffer buffer = new StringBuffer(ZERO.substring(0,appendLength));
		buffer.append(hex);
		return buffer.toString();
	}
}
