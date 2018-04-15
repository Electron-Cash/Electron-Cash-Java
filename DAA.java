package main;

import java.math.BigInteger;

public class DAA {

	static boolean TEST_MODE = true;

	public DAA()

	{
	}

	public static BigInteger daa_Get_TimeStamp_From_Block_Height(int blockHeight) {

		// THIS WILL NEED TO BE DE-ABSTRACTED TO GET ACTUAL TIMESTAMP.
		// IN OTHER WORDS, YOU NEED TO WRITE THIS FUNCTION.

		return BigInteger.ZERO; // PLACEHOLDER

	}

	public static BigInteger daa_Get_Difficulty_Bits_From_Block_Height(int blockHeight) {

		if (TEST_MODE) {

			// https://hastebin.com/kolozazeda.vbs
			blockHeight = blockHeight - 518918;
			BigInteger[] arr = new BigInteger[145];
			arr[0] = BigInteger.ZERO;
			arr[1] = new BigInteger("402850571");
			arr[2] = new BigInteger("402849667");
			arr[3] = new BigInteger("402849845");
			arr[4] = new BigInteger("402850175");
			arr[5] = new BigInteger("402847541");
			arr[6] = new BigInteger("402850748");
			arr[7] = new BigInteger("402850236");
			arr[8] = new BigInteger("402846650");
			arr[9] = new BigInteger("402845124");
			arr[10] = new BigInteger("402844830");
			arr[11] = new BigInteger("402847078");
			arr[12] = new BigInteger("402847922");
			arr[13] = new BigInteger("402846214");
			arr[14] = new BigInteger("402845157");
			arr[15] = new BigInteger("402845003");
			arr[16] = new BigInteger("402841179");
			arr[17] = new BigInteger("402842082");
			arr[18] = new BigInteger("402843595");
			arr[19] = new BigInteger("402842215");
			arr[20] = new BigInteger("402845221");
			arr[21] = new BigInteger("402845239");
			arr[22] = new BigInteger("402845242");
			arr[23] = new BigInteger("402848858");
			arr[24] = new BigInteger("402847884");
			arr[25] = new BigInteger("402847060");
			arr[26] = new BigInteger("402846922");
			arr[27] = new BigInteger("402848673");
			arr[28] = new BigInteger("402848273");
			arr[29] = new BigInteger("402850136");
			arr[30] = new BigInteger("402853550");
			arr[31] = new BigInteger("402852549");
			arr[32] = new BigInteger("402853928");
			arr[33] = new BigInteger("402857261");
			arr[34] = new BigInteger("402857664");
			arr[35] = new BigInteger("402857466");
			arr[36] = new BigInteger("402855697");
			arr[37] = new BigInteger("402856230");
			arr[38] = new BigInteger("402855659");
			arr[39] = new BigInteger("402855787");
			arr[40] = new BigInteger("402855766");
			arr[41] = new BigInteger("402856541");
			arr[42] = new BigInteger("402856765");
			arr[43] = new BigInteger("402858155");
			arr[44] = new BigInteger("402858314");
			arr[45] = new BigInteger("402858335");
			arr[46] = new BigInteger("402858763");
			arr[47] = new BigInteger("402859216");
			arr[48] = new BigInteger("402862013");
			arr[49] = new BigInteger("402862010");
			arr[50] = new BigInteger("402862319");
			arr[51] = new BigInteger("402860424");
			arr[52] = new BigInteger("402861088");
			arr[53] = new BigInteger("402856727");
			arr[54] = new BigInteger("402857982");
			arr[55] = new BigInteger("402857620");
			arr[56] = new BigInteger("402860157");
			arr[57] = new BigInteger("402859867");
			arr[58] = new BigInteger("402861467");
			arr[59] = new BigInteger("402861649");
			arr[60] = new BigInteger("402860259");
			arr[61] = new BigInteger("402856941");
			arr[62] = new BigInteger("402852423");
			arr[63] = new BigInteger("402851693");
			arr[64] = new BigInteger("402848428");
			arr[65] = new BigInteger("402847960");
			arr[66] = new BigInteger("402848427");
			arr[67] = new BigInteger("402847766");
			arr[68] = new BigInteger("402847744");
			arr[69] = new BigInteger("402851272");
			arr[70] = new BigInteger("402852111");
			arr[71] = new BigInteger("402852058");
			arr[72] = new BigInteger("402851413");
			arr[73] = new BigInteger("402850747");
			arr[74] = new BigInteger("402849667");
			arr[75] = new BigInteger("402852283");
			arr[76] = new BigInteger("402858248");
			arr[77] = new BigInteger("402854791");
			arr[78] = new BigInteger("402855613");
			arr[79] = new BigInteger("402854975");
			arr[80] = new BigInteger("402856162");
			arr[81] = new BigInteger("402857149");
			arr[82] = new BigInteger("402852396");
			arr[83] = new BigInteger("402850208");
			arr[84] = new BigInteger("402850571");
			arr[85] = new BigInteger("402849638");
			arr[86] = new BigInteger("402848460");
			arr[87] = new BigInteger("402849367");
			arr[88] = new BigInteger("402850743");
			arr[89] = new BigInteger("402856312");
			arr[90] = new BigInteger("402859694");
			arr[91] = new BigInteger("402859122");
			arr[92] = new BigInteger("402859028");
			arr[93] = new BigInteger("402856837");
			arr[94] = new BigInteger("402854375");
			arr[95] = new BigInteger("402854126");
			arr[96] = new BigInteger("402857439");
			arr[97] = new BigInteger("402858400");
			arr[98] = new BigInteger("402857081");
			arr[99] = new BigInteger("402857843");
			arr[100] = new BigInteger("402854970");
			arr[101] = new BigInteger("402854958");
			arr[102] = new BigInteger("402854551");
			arr[103] = new BigInteger("402857584");
			arr[104] = new BigInteger("402857683");
			arr[105] = new BigInteger("402861209");
			arr[106] = new BigInteger("402860687");
			arr[107] = new BigInteger("402861031");
			arr[108] = new BigInteger("402860969");
			arr[109] = new BigInteger("402857544");
			arr[110] = new BigInteger("402855738");
			arr[111] = new BigInteger("402852201");
			arr[112] = new BigInteger("402852064");
			arr[113] = new BigInteger("402849891");
			arr[114] = new BigInteger("402847779");
			arr[115] = new BigInteger("402847692");
			arr[116] = new BigInteger("402846846");
			arr[117] = new BigInteger("402851636");
			arr[118] = new BigInteger("402855076");
			arr[119] = new BigInteger("402852416");
			arr[120] = new BigInteger("402852294");
			arr[121] = new BigInteger("402852901");
			arr[122] = new BigInteger("402852562");
			arr[123] = new BigInteger("402852253");
			arr[124] = new BigInteger("402856057");
			arr[125] = new BigInteger("402856559");
			arr[126] = new BigInteger("402855837");
			arr[127] = new BigInteger("402856030");
			arr[128] = new BigInteger("402860639");
			arr[129] = new BigInteger("402854140");
			arr[130] = new BigInteger("402854607");
			arr[131] = new BigInteger("402854613");
			arr[132] = new BigInteger("402853823");
			arr[133] = new BigInteger("402854390");
			arr[134] = new BigInteger("402854838");
			arr[135] = new BigInteger("402854500");
			arr[136] = new BigInteger("402852587");
			arr[137] = new BigInteger("402850866");
			arr[138] = new BigInteger("402848092");
			arr[139] = new BigInteger("402848060");
			arr[140] = new BigInteger("402847792");
			arr[141] = new BigInteger("402850611");
			arr[142] = new BigInteger("402854104");
			arr[143] = new BigInteger("402854142");
			arr[144] = new BigInteger("402853955");
			BigInteger retVal = arr[blockHeight];

			return retVal;
		}

		// THIS WILL NEED TO BE DE-ABSTRACTED TO GET ACTUAL BLOCKHEIGHT.
		// IN OTHER WORDS, YOU NEED TO WRITE THIS FUNCTION.

		return BigInteger.ZERO; // PLACEHOLDER
	}

	// ==================================================================================

	boolean daa_Is_Current_Block_Valid(int blockheight) {

		// Block 504035 comes after Nov 13th DAA HF.
		// If its before that, assume block is ok.
		if (blockheight < 504035) {
			return true;
		}

		BigInteger bits_Of_Difficulty_Given = daa_Get_Difficulty_Bits_From_Block_Height(blockheight);
		BigInteger bits_Required = daa_Get_Bits_Required(blockheight);
		if (bits_Required.compareTo(bits_Of_Difficulty_Given) == 1) {
			// Since required bits is greater than what's given,
			// block is not valid.
			return false;
		} else {
			return true;
		}
	}

	public static int daa_Get_Suitable_Height(int suitableHeight) {

		// In order to avoid a block in a very skewed timestamp to have too much
		// influence, we select the median of the 3 top most block as a start point
		// Reference: github.com/Bitcoin-ABC/bitcoin-abc/master/src/pow.cpp#L201

		int blocks2Height = suitableHeight - 2;
		int blocks1Height = suitableHeight - 1;
		int blocksHeight = suitableHeight;

		BigInteger blocks2TimeStamp = daa_Get_TimeStamp_From_Block_Height(blocksHeight);
		BigInteger blocks1TimeStamp = daa_Get_TimeStamp_From_Block_Height(blocks1Height);
		BigInteger blocksTimeStamp = daa_Get_TimeStamp_From_Block_Height(blocks2Height);

		// temporary variables use for place swapping.
		BigInteger tempStamp = BigInteger.ZERO;
		int tempHeight = 0;

		if (blocksTimeStamp.compareTo(blocks2TimeStamp) == 1) {
			tempStamp = blocksTimeStamp;
			blocksTimeStamp = blocks2TimeStamp;
			blocks2TimeStamp = tempStamp;
			tempHeight = blocksHeight;
			blocksHeight = blocks2Height;
			blocks2Height = tempHeight;
		}

		if (blocksTimeStamp.compareTo(blocks1TimeStamp) == 1) {
			tempStamp = blocksTimeStamp;
			blocksTimeStamp = blocks1TimeStamp;
			blocks1TimeStamp = tempStamp;
			tempHeight = blocksHeight;
			blocksHeight = blocks1Height;
			blocks1Height = tempHeight;
		}

		if (blocks1TimeStamp.compareTo(blocks2TimeStamp) == 1) {
			tempStamp = blocks1TimeStamp;
			blocks1TimeStamp = blocks2TimeStamp;
			blocks2TimeStamp = tempStamp;
			tempHeight = blocks1Height;
			blocks1Height = blocks2Height;
			blocks2Height = tempHeight;
		}

		return blocks1Height;

	}

	public static BigInteger daa_Bits_To_Work(BigInteger bits) {

		BigInteger someBits = daa_Bits_To_Target(bits);
		someBits = someBits.add(BigInteger.ONE);
		BigInteger bigWork = (BigInteger.ONE).shiftLeft(256);
		return bigWork.divide(someBits);
	}

	public static BigInteger daa_Bits_To_Target(BigInteger bits) {

		BigInteger mask1 = new BigInteger("00ffffff", 16);
		BigInteger size = bits.shiftRight(24);
		BigInteger word = bits.and(mask1);
		int val1 = 0;
		int i_size = size.intValue();

		// size variable will be small because we are shifting 24 bits.
		if (i_size <= 3) {
			val1 = (8 * (3 - i_size));
			return word.shiftRight(val1);
		} else {
			val1 = (8 * (i_size - 3));
			return word.shiftLeft(val1);
		}
	}

	public static BigInteger daa_Target_To_Bits(BigInteger target) {

		int size;
		BigInteger mask64 = new BigInteger("ffffffffffffffff", 16);
		BigInteger mask1 = new BigInteger("00800000", 16);
		BigInteger bi_compact = BigInteger.ZERO;
		int val1, val2;

		if (target.compareTo(BigInteger.ZERO) == 0) {
			return BigInteger.ZERO;
		}

		BigInteger MAX_BITS = new BigInteger("1d00ffff", 16);
		BigInteger MAX_TARGET = daa_Bits_To_Target(MAX_BITS);
		target = target.min(MAX_TARGET);
		size = (target.bitLength() + 7) / 8;
		val1 = (8 * (3 - size));
		val2 = (8 * (size - 3));

		if (size <= 3) {
			bi_compact = (target.and(mask64)).shiftLeft(val1);
		} else {
			bi_compact = (target.shiftRight(val2)).and(mask64);
		}

		if (bi_compact.and(mask1).compareTo(BigInteger.ZERO) == 1) {
			bi_compact = bi_compact.shiftRight(8);
			size++;
		}

		BigInteger bi_size = BigInteger.valueOf(size);
		bi_size = bi_size.shiftLeft(24);
		return bi_compact.or(bi_size);
	}

	// ========================================================================

	public static BigInteger daa_Get_Bits_Required(int blockheight) {

		int daa_Starting_Height;
		int daa_Ending_Height;
		BigInteger daa_Starting_Timestamp;
		BigInteger daa_Ending_Timestamp;
		BigInteger daa_Elapsed_Time;
		BigInteger daa_Wn;
		BigInteger daa_Target;
		BigInteger daa_Retval;
		BigInteger bi_172800 = new BigInteger("172800");
		BigInteger bi_43200 = new BigInteger("43200");
		BigInteger bi_600 = new BigInteger("600");
		BigInteger bigWork;
		int prevheight = blockheight - 1;

		daa_Starting_Height = daa_Get_Suitable_Height(prevheight - 144);
		daa_Ending_Height = daa_Get_Suitable_Height(prevheight);

		BigInteger daa_Cumulative_Work = BigInteger.ZERO;
		BigInteger daa_Work_For_A_Block = BigInteger.ZERO;
		BigInteger daa_Bits_For_A_Block = BigInteger.ZERO;

		// calculate cumulative work (EXcluding work from block daa_starting_height,
		// INcluding work from block daa_ending_height
		for (int daa_i = daa_Starting_Height + 1; daa_i <= daa_Ending_Height; daa_i++) {
			daa_Bits_For_A_Block = daa_Get_Difficulty_Bits_From_Block_Height(daa_i);
			daa_Work_For_A_Block = daa_Bits_To_Work(daa_Bits_For_A_Block);
			daa_Cumulative_Work = daa_Cumulative_Work.add(daa_Work_For_A_Block);
		}

		// calculate and sanitize elapsed time
		daa_Starting_Timestamp = daa_Get_TimeStamp_From_Block_Height(daa_Starting_Height);
		daa_Ending_Timestamp = daa_Get_TimeStamp_From_Block_Height(daa_Ending_Height);
		daa_Elapsed_Time = daa_Ending_Timestamp.subtract(daa_Starting_Timestamp);
		if (daa_Elapsed_Time.compareTo(bi_172800) == 1) {
			daa_Elapsed_Time = bi_172800;
		}
		if (daa_Elapsed_Time.compareTo(bi_43200) == -11) {
			daa_Elapsed_Time = bi_43200;
		}

		// calculate and return new target
		daa_Wn = daa_Cumulative_Work.multiply(bi_600); // daa_elapsed_time
		bigWork = (BigInteger.ONE).shiftLeft(256);
		daa_Target = bigWork.divide(daa_Wn);
		daa_Target = daa_Target.subtract(BigInteger.ONE);
		daa_Retval = daa_Target_To_Bits(daa_Target);
		return daa_Retval;

	}

	public static BigInteger daa_Get_Bits_Required_TEST(int blockheight) {

		// THIS FUNCTION ONLY FOR UNIT TESTING!
		// TEST EXPECTS ARGUMENT blockheight = 518918

		int daa_Starting_Height;
		int daa_Ending_Height;

		BigInteger daa_Starting_Timestamp;
		BigInteger daa_Ending_Timestamp;
		BigInteger daa_Elapsed_Time;
		BigInteger daa_Wn;
		BigInteger daa_Target;
		BigInteger daa_Retval;
		BigInteger bi_172800 = new BigInteger("172800");
		BigInteger bi_43200 = new BigInteger("43200");
		BigInteger bi_600 = new BigInteger("600");
		BigInteger bigWork;
		int prevheight = blockheight - 1;

		// TEST VARIABLES -- FROM ELECTRON CASH MAINNET OUTPUT
		daa_Starting_Height = 518918;
		daa_Ending_Height = 519062;

		BigInteger daa_Cumulative_Work = BigInteger.ZERO;
		BigInteger daa_Work_For_A_Block = BigInteger.ZERO;
		BigInteger daa_Bits_For_A_Block = BigInteger.ZERO;

		// calculate cumulative work (EXcluding work from block daa_starting_height,
		// INcluding work from block daa_ending_height

		for (int daa_i = daa_Starting_Height + 1; daa_i <= daa_Ending_Height; daa_i++) {

			daa_Bits_For_A_Block = daa_Get_Difficulty_Bits_From_Block_Height(daa_i);
			daa_Work_For_A_Block = daa_Bits_To_Work(daa_Bits_For_A_Block);
			daa_Cumulative_Work = daa_Cumulative_Work.add(daa_Work_For_A_Block);
		}

		// calculate and sanitize elapsed time

		// TEST VARIABLES -- FROM ELECTRON CASH MAINNET OUTPUT
		daa_Starting_Timestamp = new BigInteger("1519555872");
		daa_Ending_Timestamp = new BigInteger("1519642492");

		daa_Elapsed_Time = daa_Ending_Timestamp.subtract(daa_Starting_Timestamp);
		if (daa_Elapsed_Time.compareTo(bi_172800) == 1) {
			daa_Elapsed_Time = bi_172800;
		}
		if (daa_Elapsed_Time.compareTo(bi_43200) == -11) {
			daa_Elapsed_Time = bi_43200;
		}

		// calculate and return new target
		daa_Wn = (daa_Cumulative_Work.multiply(bi_600)).divide(daa_Elapsed_Time); // daa_elapsed_time
		bigWork = (BigInteger.ONE).shiftLeft(256);
		daa_Target = bigWork.divide(daa_Wn);
		daa_Target = daa_Target.subtract(BigInteger.ONE);
		daa_Retval = daa_Target_To_Bits(daa_Target);
		return daa_Retval;

	}

	public static void daa_test() {

		BigInteger myTest = daa_Get_Bits_Required_TEST(518918);
		System.out.println("We got required bits of " + myTest);

		// EXPECTED OUTPUT
		// We got required bits of 402853643

	}

	// end func
} // end class
