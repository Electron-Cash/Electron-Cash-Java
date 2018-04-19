package electrol1;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.Buffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Vector;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;

import org.json.me.JSONArray;
import org.json.me.JSONException;
import org.json.me.JSONObject;

import electrol.java.util.Arrays;
import electrol.java.util.Collections;
import electrol.java.util.HashMap;
import electrol.java.util.HashSet;
import electrol.java.util.Map;
import electrol.java.util.Set;
import electrol.util.BigInteger;
import electrol.util.BitcoinMainnet;
import electrol.util.Config;
import electrol.util.Files;
import electrol.util.NetworkConstants;
import electrol.util.StringUtils;

public class Blockchain {
	Map blockchains = new HashMap();
	private int checkpoint;
	private Integer parent_id;
	private JSONArray checkPoints;
	private byte[] current_chunk;
	private Object catch_up;
	private int current_chunk_index;
	private long _size;

	public Object get_catch_up() {
		return catch_up;
	}
	public void set_catch_up(Object catch_up) {
		this.catch_up = catch_up;
	}

	public Blockchain(int checkpoint,Integer parent_id) {
		this.checkpoint = checkpoint;
		this.parent_id = parent_id;
		this.current_chunk = null;
		this.catch_up = null;
		this.checkPoints = BitcoinMainnet.getCheckpoints();
		update_size();
	}


	public boolean check_header(JSONObject header) throws JSONException {
		String header_hash = BlockchainsUtil.hash_header(header);
		int height = header.getInt("block_height");
		return header_hash == get_hash(height);
	}
	public Blockchain can_connect(JSONObject header) {
		Object[] values = blockchains.values().toArray();
		for(int i=0;i < values.length; i++) {
			Blockchain b = (Blockchain)values[i];
			if(b.can_connect(header) != null) {
				return b;
			}
		}
		return null;
	}
	public String get_hash(int height) throws JSONException {
		return BlockchainsUtil.hash_header(read_header(height));
	}
	public JSONObject read_header(int height) throws JSONException {
		if(current_chunk !=null && height / 2016 == current_chunk_index) {
			int n = height % 2016;
			byte[] h = Arrays.slice(current_chunk , n*80 , (n+1)*80);
			return BlockchainsUtil.deserializeHeader(h,height);
		}
		if(height < 0) {
			return null;
		}
		if(height < checkpoint) {
			return parent().read_header(height);
		}
		if(height > height()) {
			return null;
		}
		int delta = height - checkpoint;
		String path = getPath();
		System.out.println("delta  "+delta+" "+height);
		if(Files.isExist(path)) {
			byte[] h= Files.readBytes(path, delta*80, 80);
			
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
		for(int i=0;i<arr.length;i++) {
			Blockchain y = (Blockchain)arr[i];
			if(y.parent_id.intValue() != checkpoint) {
				max = Math.max( max, checkpoint);
			}
		}
		if(max > Integer.MIN_VALUE) {
			return new Integer(max);
		}
		else {
			return null;
		}
	}

	public Integer get_checkpoint() {
		Integer mc = get_max_child();
		if(mc != null) {
			return mc;
		}
		else {
			return new Integer(checkpoint);
		}
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
	public void update_size() {
		String p = getPath();
		_size = Files.filesize(p) / 80;
	}

	public void verify_header(JSONObject header,JSONObject prev_header,BigInteger bits) throws JSONException {
		String prev_hash = BlockchainsUtil.hash_header(prev_header);
		String hash = BlockchainsUtil.hash_header(header);

		if(!prev_hash.equals(header.get("prev_block_hash").toString())) {
			System.out.println("prev hash mismatch: "+prev_hash+" vs "+header.get("prev_block_hash'"));
		}
		if(new BigInteger(header.get("block_height").toString()).equals(BitcoinMainnet.BITCOIN_CASH_FORK_BLOCK_HEIGHT) &&
				!hash.equals(BitcoinMainnet.BITCOIN_CASH_FORK_BLOCK_HASH)){
			//err_str = "block at height %i is not cash chain fork block. hash %s" % (header.get('block_height'), hash_header(header))
		}
		if(!bits.equals((BigInteger)header.get("bits"))) {
			//raise VerifyError("bits mismatch: %s vs %s" % (bits, header.get('bits')))
		}
		BigInteger target = BlockchainsUtil.bits_to_target(bits);
		if(new BigInteger(hash,16).compareTo(target) > 0) {
			//raise VerifyError("insufficient proof of work: %s vs target %s" % (int('0x' + _hash, 16), target))
		}
	}

	public void verify_chunk(int index, byte[] data) throws JSONException {
		current_chunk = data;
		current_chunk_index = index;
		int num = data.length / 80;
		JSONObject prev_header = null;
		if(index != 0)
			prev_header = read_header(index*2016 - 1);
		for(int i=0; i < num;i ++) {
			byte[] raw_header = Arrays.slice(data, i*80, (i+1)*80);
			JSONObject header = BlockchainsUtil.deserializeHeader(raw_header, index*2016 + i);
			BigInteger bits = get_bits(header);
			verify_header(header, prev_header, bits);
			prev_header = header;
		}
		current_chunk = null;
	}

	public int get_median_time_past(int height) throws JSONException {
		if(height < 0)
			return 0;
		int max = Math.max(0 , height-10);
		int[] times= new int[height+1-max];;
		for(int i=max;i<=height;i++) {
			times[i-max] = ((Integer)(read_header(i).get("timestamp"))).intValue();
		}
		Arrays.sort(times);
		return times[times.length/2];
	}

	int get_suitable_block_height(int suitableheight) throws JSONException {
		JSONObject blocks2 = read_header(suitableheight);
		JSONObject blocks1 = read_header(suitableheight-1);
		JSONObject blocks = read_header(suitableheight-2);
		int blocks_timestamp = ((Integer)blocks.get("timestamp")).intValue();
		int blocks1_timestamp = ((Integer)blocks1.get("timestamp")).intValue();
		int blocks2_timestamp = ((Integer)blocks2.get("timestamp")).intValue();

		if (blocks_timestamp > blocks2_timestamp ) {
			blocks = blocks2;
			blocks2 = blocks;    
		}
		if (blocks_timestamp > blocks1_timestamp) {
			blocks = blocks1;
			blocks1 = blocks;
		}
		if (blocks1_timestamp > blocks2_timestamp) {
			blocks1 = blocks2;
			blocks2 = blocks1;
		}
		return ((Integer)blocks1.get("block_height")).intValue();
	}

	public BigInteger get_bits(JSONObject header) throws JSONException {
		System.out.println("Return bits for the given height.");
		// Difficulty adjustment interval?
		int height = ((Integer) header.get("block_height")).intValue();
		//Genesis
		if(height == 0)
			return BlockchainsUtil.MAX_BITS;

		JSONObject  prior = read_header(height - 1);
		BigInteger bits = (BigInteger)prior.get("bits");

		//NOV 13 HF DAA

		int prevheight = height - 1;
		int daa_mtp=get_median_time_past(prevheight);

		//if (daa_mtp >= 1509559291):  //leave this here for testing
		if(daa_mtp >= 1510600000) {

			/*            if(NetworkConstants.TESTNET
                # testnet 20 minute rule
                if header['timestamp'] - prior['timestamp'] > 20*60:
                    return MAX_BITS
			 */
			//           # determine block range
			int  daa_starting_height=get_suitable_block_height(prevheight - 144);
			int  daa_ending_height=get_suitable_block_height(prevheight);

			//calculate cumulative work (EXcluding work from block daa_starting_height, INcluding work from block daa_ending_height)
			BigInteger  daa_cumulative_work= new BigInteger("0");
			for(int daa_i = daa_starting_height+1 ;daa_i < daa_ending_height+1;daa_i++) {
				JSONObject daa_prior = read_header(daa_i);
				BigInteger daa_bits_for_a_block=(BigInteger)daa_prior.get("bits");
				BigInteger daa_work_for_a_block=BlockchainsUtil.bits_to_work(daa_bits_for_a_block);
				daa_cumulative_work = daa_cumulative_work.add(daa_work_for_a_block);
			}

			//# calculate and sanitize elapsed time
			int daa_starting_timestamp = ((Integer)read_header(daa_starting_height).get("timestamp")).intValue();
			int daa_ending_timestamp = ((Integer)read_header(daa_ending_height).get("timestamp")).intValue();
			int daa_elapsed_time=daa_ending_timestamp-daa_starting_timestamp;
			if (daa_elapsed_time>172800)
				daa_elapsed_time=172800;
			if (daa_elapsed_time<43200)
				daa_elapsed_time=43200;

			// calculate and return new target
			BigInteger daa_Wn= daa_cumulative_work.multiply(new BigInteger("600"));//daa_elapsed_time
			BigInteger daa_target= new BigInteger("1").shiftLeft(256); // daa_Wn -1
			BigInteger daa_retval = BlockchainsUtil.target_to_bits(daa_target);
			return daa_retval;
		}
		//END OF NOV-2017 DAA

		if(height%2016 == 0) {
			return get_new_bits(height);
		}
		/*if NetworkConstants.TESTNET:
            # testnet 20 minute rule
            if header['timestamp'] - prior['timestamp'] > 20*60:
                return MAX_BITS
            return self.read_header(height // 2016 * 2016)['bits']
		 */
		// bitcoin cash EDA
		// Can't go below minimum, so early bail
		if(bits == BlockchainsUtil.MAX_BITS)
			return bits;
		int mtp_6blocks = get_median_time_past(height - 1) - get_median_time_past(height - 7);
		if(mtp_6blocks < 12 * 3600)
			return bits;
		// If it took over 12hrs to produce the last 6 blocks, increase the
		// target by 25% (reducing difficulty by 20%).
		BigInteger target = BlockchainsUtil.bits_to_target(bits);
		target = target.shiftRight(2).add(target);

		return BlockchainsUtil.target_to_bits(target);
	}
	public String getPath() {
		String fileName = "blockchain_headers";
		if(parent_id == null) {
			return fileName;
		}
		else {
			return "forks/fork_"+parent_id.intValue()+"_"+checkpoint;
		}
	}
	public BigInteger get_new_bits(int height) throws JSONException {
		try {
			if(height % 2016 == 0) {
				throw new Exception("Not Valid");
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
		//Genesis
		if(height == 0)
			return BlockchainsUtil.MAX_BITS;
		JSONObject first = read_header(height - 2016);
		JSONObject prior = read_header(height - 1);
		int prior_target = BlockchainsUtil.bits_to_target((BigInteger)prior.get("bits")).intValue();
		int target_span = 14 * 24 * 60 * 60;
		int span = ((Integer)prior.get("timestamp")).intValue() - ((Integer)first.get("timestamp")).intValue();     
		span = Math.min(Math.max(span, target_span / 4), target_span * 4);
		int new_target = (prior_target * span) / target_span;
		return BlockchainsUtil.target_to_bits(new BigInteger(String.valueOf(new_target)));
	}
	public boolean connect_chunk(int idx,String hexdata) {
		try {
			byte[] data = BlockchainsUtil.bfh(hexdata);;
			verify_chunk(idx, data);
			save_chunk(idx, data);
			return true;
		}
		catch(Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	public void save_chunk(int index,byte[] chunk) throws IOException {
		String filename = getPath();
		int d = (index * 2016 - checkpoint) * 80;
		if(d < 0) {
			chunk = Arrays.reverse(chunk);
			d = 0;
		}
		write(chunk, d);
		swap_with_parent();
	}
	public static void main(String[] args) throws UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeySpecException, InvalidKeyException, IllegalStateException, ShortBufferException, IllegalBlockSizeException, BadPaddingException {
		byte[] data = new BigInteger("0000002077b268120a1331d7b3e3a48946973f4883f322c08a986b000000000000000000afc6ec5f809ab3ea385c195ac9ab957671ca592840bf6554ff874bf9b5c1ea8c3113be5ab11103180a300bdc").toByteArray();

		//System.out.println(blockchain._size);
		//blockchain.write(data, 41892880);
	}
	public void write(byte[] data,long offset) {
		try {
			String filename = getPath();
			System.out.println(filename);
			FileConnection con = (FileConnection) Connector.open(Files.getDefaultPath()+filename, Connector.READ_WRITE);
			DataInputStream in = con.openDataInputStream();
			if(offset != _size*80) {
				in.skip(offset);   
			}
			in.skip(offset);
			con.openDataOutputStream().write(data);
			con.openDataOutputStream().flush();
			update_size();
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void write(InputStream inputStream,long offset) {
		try {
			String filename = getPath();
			System.out.println(filename);
			FileConnection con = (FileConnection) Connector.open(Files.getDefaultPath()+filename, Connector.READ_WRITE);
			DataInputStream in = con.openDataInputStream();
			if(offset != _size*80) {
				in.skip(offset);   
			}
			in.skip(offset);
			DataOutputStream dos = con.openDataOutputStream();
			int length;
			byte[] buffer = new byte[1024];
			while ((length = inputStream.read(buffer)) != -1) {
				dos.write(buffer, 0, length);
			}
			dos.flush();
			dos.close();
			inputStream.close();
			update_size();
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void swap_with_parent() throws IOException {
		if(parent_id == null)
			return;
		long parent_branch_size = parent().height() - checkpoint + 1;
		if(parent_branch_size >= size())
			return;
		System.out.println("swap" +checkpoint +" "+parent_id);
		Integer parent_id = this.parent_id;
		int checkpoint = this.checkpoint;
		Blockchain parent = parent();
		
		String parent_filename = parent.getPath();
		FileConnection con = (FileConnection) Connector.open(Files.getDefaultPath()+parent_filename, Connector.READ_WRITE);
		DataInputStream in = con.openDataInputStream();
		in.skip((checkpoint - parent.checkpoint)*80);
		byte[] b = new byte[(int)parent_branch_size*80];
		in.read(b);
		write(b, 0);
		
		FileConnection self = (FileConnection) Connector.open(Files.getDefaultPath()+getPath(), Connector.READ_WRITE);
		DataInputStream dataInputStream = self.openDataInputStream();
		parent.write(dataInputStream, (checkpoint - parent.checkpoint)*80);
		Object[] blockchainArray = blockchains.values().toArray();
		for(int i=0;i<blockchainArray.length;i++) {
			Blockchain block = (Blockchain)blockchainArray[i];
			block.setOldPath(block.getPath());
		}
        this.parent_id = parent.parent_id; 
        parent.parent_id = parent_id;
        this.checkpoint = parent.checkpoint; 
        parent.checkpoint = checkpoint;
        this._size = parent._size; 
        parent._size = parent_branch_size;
        //move files
        for(int i=0;i<blockchainArray.length;i++) {
			Blockchain block = (Blockchain)blockchainArray[i];
			if(block.equals(this) || block.equals(parent) ) {
				continue;
			}
			if(block.getOldPath() != block.getPath()) {
				System.out.println("renaming "+ block.old_path +""+ block.getPath());
				Files.rename(block.old_path,block.getPath());
			}
		}
        // update pointers
        blockchains.put(new Integer(checkpoint),this);
        blockchains.put(new Integer(parent.checkpoint),parent);
	}
	private String old_path;
	private void setOldPath(String path) {
		this.old_path = path;
		
	}
	private String getOldPath() {
		return old_path;
		
	}
	public void save_header(JSONObject header) throws JSONException, IOException {
		int delta = header.optInt("block_height") - checkpoint;
		byte [] data = BlockchainsUtil.bfh(BlockchainsUtil.serializeHeader(header));
		try {
			if(delta == size() || data.length == 80) {
				throw new Exception("Not Valid");
			}
		}catch(Exception e) {
			e.printStackTrace();
		}

		write(data, delta*80);
		swap_with_parent();
	}

	public boolean can_connect(JSONObject header,boolean check_height) throws JSONException {
		int height = ((Integer)header.get("block_height")).intValue();
		if(check_height && height() != height - 1)
			return false;
		if(height == 0)
			return BlockchainsUtil.hash_header(header) == BitcoinMainnet.GENESIS;
		JSONObject previous_header = read_header(height -1);
		if(previous_header == null)
			return false;
		String prev_hash = BlockchainsUtil.hash_header(previous_header);
		if(!prev_hash.equals((String)header.get("prev_block_hash"))){
			return false;
		}
		BigInteger bits = get_bits(header);
		verify_header(header, previous_header, bits);
		return true;
	}
	public Blockchain fork(Blockchain parent, JSONObject header) throws JSONException, IOException {
		int checkpoint = header.getInt("block_height");
		Blockchain b=new Blockchain(checkpoint, new Integer(parent.checkpoint));
		save_header(header);
		return b;
	}
}
