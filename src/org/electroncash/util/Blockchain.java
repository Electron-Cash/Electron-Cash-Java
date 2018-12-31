package org.electroncash.util;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import org.json.me.JSONException;
import org.json.me.JSONObject;

public class Blockchain
{
	private Map blockchains = new HashMap();
	private int checkpoint;
	private Integer parent_id;
	private ConnectionManager catch_up;
	private long _size;
	private final BigInteger BIG_INT_600 = BigInteger.valueOf(600L);
	private final BigInteger TARGET_SPAN = BigInteger.valueOf(1209600L);
	private final int MAX_BITS = 486604799;
	private final byte[] bytes = new byte[80];
	public static final byte CHUNK_ACCEPTED = 0;

	public ConnectionManager get_catch_up() { return catch_up; }

	public static final byte CHUNK_LACKED_PROOF = 1;
	public void set_catch_up(ConnectionManager catch_up) {
		this.catch_up = catch_up;
	}

	public Blockchain(int checkpoint, Integer parent_id) throws IOException {
		this.checkpoint = checkpoint;
		this.parent_id = parent_id;
		catch_up = null;
		update_size();
	}

	public boolean check_header(JSONObject header) throws JSONException, IOException {
		String header_hash = BlockchainsUtil.hash_header(header);
		int height = header.getInt("block_height");
		return header_hash.equals(get_hash(height));
	}

	public String get_hash(int height) throws JSONException, IOException {
		return BlockchainsUtil.hash_header(read_header(height, null));
	}

	public JSONObject read_header(int height, HeaderChunk chunk) throws JSONException, IOException {
		if ((chunk != null) && (chunk.contains_height(height))) {
			byte[] header = chunk.get_header_at_height(height);
			return BlockchainsUtil.deserializeHeader(header, height);
		}
		if (height < 0) {
			return null;
		}
		if (height < checkpoint) {
			return parent().read_header(height, null);
		}
		if (height > height()) {
			return null;
		}
		int delta = height - checkpoint;
		String path = getPath();
		if (Files.isExist(path)) {
			byte[] h = Files.readBytes(path, delta * 80, 80);
			if (org.bouncycastle.util.Arrays.areEqual(h, bytes)) {
				return null;
			}

			return BlockchainsUtil.deserializeHeader(h, height);
		}
		return null;
	}

	public Blockchain parent() {
		return (Blockchain)blockchains.get(parent_id);
	}

	public Integer get_max_child() {
		Object[] arr = blockchains.values().toArray();
		int max = Integer.MIN_VALUE;
		for (int i = 0; i < arr.length; i++) {
			Blockchain y = (Blockchain)arr[i];
			if (parent_id.intValue() == checkpoint) {
				max = Math.max(max, checkpoint);
			}
		}
		if (max > Integer.MIN_VALUE) {
			return new Integer(max);
		}

		return null;
	}

	public Integer get_checkpoint()
	{
		Integer mc = get_max_child();
		if (mc != null) {
			return mc;
		}
		return new Integer(checkpoint);
	}

	public long get_branch_size() {
		return height() - get_checkpoint().intValue() + 1;
	}

	public int height() {
		return checkpoint + (int)size() - 1;
	}

	public long size() {
		return _size;
	}

	public void update_size() throws IOException {
		String p = getPath();
		_size = (Files.filesize(p) / 80L);
	}

	public void verify_header(JSONObject header, JSONObject prev_header, int bits) throws Exception {
		String prev_hash = BlockchainsUtil.hash_header(prev_header);
		String _hash = BlockchainsUtil.hash_header(header);
		if (!prev_hash.equals(header.optString("prev_block_hash"))) {
			throw new Exception("prev hash mismatch: " + prev_hash + " vs " + header.get("prev_block_hash'"));
		}
		if ((header.optInt("block_height") == 478559) && 
				(!_hash.equals("000000000000000000651ef99cb9fcbe0dadde1d424bd9f15ff20136191a5eec"))) {
			throw new Exception("block at height is not cash chain fork block");
		}
		if (bits != header.getInt("bits")) {
			System.out.println("bits compare " + header.getInt("bits") + "- bits " + bits);
			throw new Exception("bits mistmatch");
		}
		BigInteger target = BlockchainsUtil.bits_to_target(bits);

		if (new BigInteger(_hash, 16).compareTo(target) > 0) {
			throw new Exception("insufficient proof of work");
		}
	}

	public void verify_chunk(int chunk_base_height, byte[] chunk_data) throws Exception {
		HeaderChunk chunk = new HeaderChunk(chunk_base_height, chunk_data);

		JSONObject prev_header = null;
		if (chunk_base_height != 0) {
			prev_header = read_header(chunk_base_height - 1, null);
		}
		int header_count = chunk_data.length / 80;
		for (int i = 0; i < header_count; i++) {
			byte[] raw_header = chunk.get_header_at_index(i);
			JSONObject header = BlockchainsUtil.deserializeHeader(raw_header, chunk_base_height + i);

			int bits = get_bits(header, chunk);
			verify_header(header, prev_header, bits);
			prev_header = header;
		}
	}

	public long get_median_time_past(int height, HeaderChunk chunk) throws JSONException, IOException
	{
		if (height < 0)
			return 0L;
		int max = Math.max(0, height - 10);
		int[] times = new int[height + 1 - max];

		for (int i = max; i <= height; i++) {
			times[(i - max)] = read_header(i, chunk).optInt("timestamp");
		}
		Arrays.sort(times);
		return times[(times.length / 2)];
	}

	public int get_suitable_block_height(int suitableheight, HeaderChunk chunk) throws JSONException, IOException {
		JSONObject blocks2 = read_header(suitableheight, chunk);
		JSONObject blocks1 = read_header(suitableheight - 1, chunk);
		JSONObject blocks = read_header(suitableheight - 2, chunk);

		if (blocks.getInt("timestamp") > blocks2.getInt("timestamp")) {
			JSONObject tmp = blocks;
			blocks = blocks2;
			blocks2 = tmp;
		}
		if (blocks.getInt("timestamp") > blocks1.getInt("timestamp")) {
			JSONObject tmp = blocks;
			blocks = blocks1;
			blocks1 = tmp;
		}
		if (blocks1.getInt("timestamp") > blocks2.getInt("timestamp")) {
			JSONObject tmp = blocks1;
			blocks1 = blocks2;
			blocks2 = tmp;
		}
		return blocks1.getInt("block_height");
	}

	public int get_bits(JSONObject header, HeaderChunk chunk) throws JSONException, IOException {
		int height = header.optInt("block_height");
		if (height == 0) {
			return 486604799;
		}
		JSONObject prior = read_header(height - 1, chunk);
		int bits = prior.optInt("bits");
		int prevheight = height - 1;
		long daa_mtp = get_median_time_past(prevheight, chunk);
		if (daa_mtp >= 1510600000L) {
			int daa_starting_height = get_suitable_block_height(prevheight - 144, chunk);
			int daa_ending_height = get_suitable_block_height(prevheight, chunk);
			BigInteger daa_cumulative_work = BigInteger.ZERO;
			for (int daa_i = daa_starting_height + 1; daa_i < daa_ending_height + 1; daa_i++) {
				JSONObject daa_prior = read_header(daa_i, chunk);
				int daa_bits_for_a_block = daa_prior.getInt("bits");
				BigInteger daa_work_for_a_block = BlockchainsUtil.bits_to_work(daa_bits_for_a_block);
				daa_cumulative_work = daa_cumulative_work.add(daa_work_for_a_block);
			}
			int daa_starting_timestamp = read_header(daa_starting_height, chunk).getInt("timestamp");
			int daa_ending_timestamp = read_header(daa_ending_height, chunk).getInt("timestamp");
			int daa_elapsed_time = daa_ending_timestamp - daa_starting_timestamp;
			if (daa_elapsed_time > 172800)
				daa_elapsed_time = 172800;
			if (daa_elapsed_time < 43200) {
				daa_elapsed_time = 43200;
			}
			BigInteger daa_Wn = daa_cumulative_work.multiply(BIG_INT_600).divide(BigInteger.valueOf(daa_elapsed_time));

			BigInteger daa_target = BlockchainsUtil.ONE_LEFT_SHIFT_256.divide(daa_Wn).subtract(BigInteger.ONE);
			int daa_ret = BlockchainsUtil.target_to_bits(daa_target);
			return daa_ret;
		}

		if (height % 252 == 0) {
			return get_new_bits(height, chunk);
		}

		if (bits == 486604799) {
			return 486604799;
		}

		long mtp_6blocks = get_median_time_past(height - 1, chunk) - get_median_time_past(height - 7, chunk);
		if (mtp_6blocks < 43200L) {
			return bits;
		}

		BigInteger target = BlockchainsUtil.bits_to_target(bits);
		target = target.shiftRight(2).add(target);
		return BlockchainsUtil.target_to_bits(target);
	}

	public String getPath() {
		String fileName = "blockchain";
		if (parent_id == null) {
			return fileName;
		}
		return "forks/fork_" + parent_id.intValue() + "_" + checkpoint;
	}

	public int get_new_bits(int height, HeaderChunk chunk) throws JSONException, IOException {
		try {
			if (height % 252 != 0) {
				throw new Exception("Not Valid");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (height == 0)
			return 486604799;
		JSONObject first = read_header(height - 252, chunk);
		JSONObject prior = read_header(height - 1, chunk);
		BigInteger prior_target = BlockchainsUtil.bits_to_target(prior.getInt("bits"));

		int span = prior.getInt("timestamp") - first.getInt("timestamp");
		span = Math.min(Math.max(span, 302400), 4838400);
		BigInteger new_target = prior_target.multiply(BigInteger.valueOf(span)).divide(TARGET_SPAN);
		return BlockchainsUtil.target_to_bits(new_target);

	}

	public static final byte CHUNK_FORKS = 2;
	public static final byte CHUNK_BAD = 3;
	private String old_path;
	public byte connect_chunk(int base_height, byte[] hexdata, boolean proof_was_provided)
			throws JSONException, IOException
	{
		HeaderChunk chunk = new HeaderChunk(base_height, hexdata);
		int header_count = hexdata.length / 80;
		int top_height = base_height + header_count - 1;
		if (top_height <= 540250) {
			if (!proof_was_provided)
				return 1;
		} else if ((base_height < 540250) && (proof_was_provided)) {
			if (top_height <= height())
				return 0;
		} else if (base_height != height() + 1) {
			int intersection_height = Math.min(top_height, height());
			JSONObject chunk_header = BlockchainsUtil.deserializeHeader(chunk.get_header_at_height(intersection_height), 
					intersection_height);
			JSONObject our_header = read_header(intersection_height, null);
			if (!BlockchainsUtil.hash_header(chunk_header).equals(BlockchainsUtil.hash_header(our_header)))
				return 2;
			if (intersection_height <= height())
				return 0;
		} else {
			JSONObject our_header = read_header(height(), null);
			JSONObject chunk_header = BlockchainsUtil.deserializeHeader(chunk.get_header_at_height(base_height), 
					base_height);
			if (!BlockchainsUtil.hash_header(our_header).equals(chunk_header.getString("prev_block_hash")))
				return 2;
		}
		try {
			if (!proof_was_provided)
				verify_chunk(base_height, hexdata);
			save_chunk(base_height, hexdata);
			return 0;
		} catch (Exception e) {
			e.printStackTrace(); }
		return 3;
	}

	public void save_chunk(int index, byte[] chunk) throws IOException
	{
		int d = (index - checkpoint) * 80;
		if (d < 0) {
			chunk = Arrays.reverse(chunk);
			d = 0;
		}
		int top_height = index + chunk.length / 80 - 1;
		boolean truncate = top_height > 540250;
		write(chunk, d, truncate);
		swap_with_parent();
	}

	public void write(byte[] data, int offset, boolean truncate) {
		try {
			String filename = getPath();
			FileConnection connection = (FileConnection)Connector.open(Files.getDefaultPath()+filename, Connector.WRITE);
			OutputStream stream = connection.openOutputStream(offset);
			stream.write(data);
			stream.flush();
			stream.close();
			update_size();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void write(InputStream data, int offset) {
		try {
			String filename = getPath();
			FileConnection connection = (FileConnection)Connector.open(Files.getDefaultPath()+filename, Connector.WRITE);
			OutputStream stream = connection.openOutputStream(offset);
			int length;
			byte[] buffer = new byte[1024];
			while ((length = data.read(buffer)) != -1) {
				stream.write(buffer, 0, length);
			}
			data.close();
			stream.flush();
			stream.close();
			update_size();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void swap_with_parent() throws IOException {
		if (this.parent_id == null)
			return;
		int parent_branch_size = parent().height() - this.checkpoint + 1;
		if (parent_branch_size >= size())
			return;
		System.out.println("swap" + this.checkpoint + " " + this.parent_id);
		Integer parent_id = this.parent_id;
		int checkpoint = this.checkpoint;
		Blockchain parent = parent();

		String parent_filename = parent.getPath();

		FileConnection connection = (FileConnection)Connector.open(Files.getDefaultPath() + getPath(), 1);

		FileConnection parentConnection = (FileConnection)Connector.open(Files.getDefaultPath() + parent_filename, 
				3);
		DataInputStream dis = parentConnection.openDataInputStream();
		dis.skip((checkpoint - checkpoint) * 80);
		byte[] parentData = new byte[parent_branch_size * 80];
		dis.read(parentData);
		write(parentData, 0, true);
		parentConnection.close();
		parent.write(connection.openInputStream(), (checkpoint - checkpoint) * 80);

		Object[] blockchainArray = blockchains.values().toArray();
		for (int i = 0; i < blockchainArray.length; i++) {
			Blockchain block = (Blockchain)blockchainArray[i];
			block.setOldPath(block.getPath());
		}
		this.parent_id = parent.parent_id; 
        parent.parent_id = parent_id;
        this.checkpoint = parent.checkpoint; 
        parent.checkpoint = checkpoint;
        this._size = parent._size; 
        parent._size = parent_branch_size;
		for (int i = 0; i < blockchainArray.length; i++) {
			Blockchain block = (Blockchain)blockchainArray[i];
			if ((!block.equals(this)) && (!block.equals(parent)))
			{

				if (block.getOldPath() != block.getPath()) {
					System.out.println("renaming " + old_path + block.getPath());
					Files.rename(old_path, block.getPath());
				} }
		}
		blockchains.put(new Integer(checkpoint), this);
		blockchains.put(new Integer(checkpoint), parent);
	}


	private void setOldPath(String path)
	{
		old_path = path;
	}

	private String getOldPath()
	{
		return old_path;
	}

	public void save_header(JSONObject header) throws JSONException, IOException
	{
		int delta = header.optInt("block_height") - checkpoint;
		byte[] data = BlockchainsUtil.bfh(BlockchainsUtil.serializeHeader(header));
		write(data, delta * 80, true);
		swap_with_parent();
	}

	public boolean can_connect(JSONObject header, boolean check_height) throws JSONException, IOException {
		int height = header.optInt("block_height");
		if ((check_height) && (height() != height - 1))
			return false;
		if (height == 0)
			return BlockchainsUtil.hash_header(header).equals("000000000019d6689c085ae165831e934ff763ae46a2a6c172b3f1b60a8ce26f");
		JSONObject previous_header = read_header(height - 1, null);
		if (previous_header == null)
			return false;
		String prev_hash = BlockchainsUtil.hash_header(previous_header);
		if (!prev_hash.equals(header.getString("prev_block_hash"))) {
			return false;
		}
		int bits = get_bits(header, null);
		try {
			verify_header(header, previous_header, bits);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public Blockchain fork(JSONObject header) throws JSONException, IOException {
		int checkpoint = header.getInt("block_height");
		Blockchain b = new Blockchain(checkpoint, new Integer(parent().checkpoint));
		save_header(header);
		return b;
	}
}
