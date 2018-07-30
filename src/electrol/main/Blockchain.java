package electrol.main;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;

import org.json.me.JSONException;
import org.json.me.JSONObject;


import electrol.java.util.Arrays;
import electrol.java.util.HashMap;
import electrol.java.util.Map;
import electrol.util.BigInteger;
import electrol.util.BitcoinMainnet;
import electrol.util.Files;

public class Blockchain {
	private Map blockchains = new HashMap();
	private int checkpoint;
	private Integer parent_id;
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
		update_size();
	}


	public boolean check_header(JSONObject header) throws JSONException {
		String header_hash = BlockchainsUtil.hash_header(header);
		int height = header.getInt("block_height");
		return header_hash.equals(get_hash(height));
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
			if(y.parent_id.intValue() == checkpoint) {
				max = Math.max( max, y.checkpoint);
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

	public void verify_header(JSONObject header,JSONObject prev_header,BigInteger bits) throws Exception {
		String prev_hash = BlockchainsUtil.hash_header(prev_header);
		String _hash = BlockchainsUtil.hash_header(header);
		if(!prev_hash.equals(header.optString("prev_block_hash"))) {
			throw new Exception("prev hash mismatch: "+prev_hash+" vs "+header.get("prev_block_hash'"));
		}
		if(header.optInt("block_height") == BitcoinMainnet.BITCOIN_CASH_FORK_BLOCK_HEIGHT &&
				!_hash.equals(BitcoinMainnet.BITCOIN_CASH_FORK_BLOCK_HASH)){
			throw new Exception("block at height is not cash chain fork block");
		}
		if(!bits.equals(new BigInteger(header.getString("bits")))) {
			System.out.println("bits "+bits);
			System.out.println("bits cmpare"+new BigInteger(header.getString("bits")));
			throw new Exception("bits mistmatch");
		}
		BigInteger target = BlockchainsUtil.bits_to_target(bits);
		if(new BigInteger(_hash,16).compareTo(target) > 0) {
			throw new Exception("insufficient proof of work");
		}
	}

	public void verify_chunk(int index, byte[] data) throws Exception {
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
		JSONObject tmp;
		if (blocks.getInt("timestamp") > blocks2.getInt("timestamp") ) {
			tmp = blocks;
			blocks = blocks2;
			blocks2 = tmp;    
		}
		if (blocks.getInt("timestamp") > blocks1.getInt("timestamp")) {
			tmp = blocks;
			blocks = blocks1;
			blocks1 = tmp;
		}
		if (blocks1.getInt("timestamp") > blocks2.getInt("timestamp")) {
			tmp = blocks1;
			blocks1 = blocks2;
			blocks2 = tmp;
		}
		return blocks1.getInt("block_height");
	}

	public BigInteger get_bits(JSONObject header) throws JSONException {
		// Difficulty adjustment interval?
		int height = header.optInt("block_height");
		//Genesis
		if(height == 0)
			return BlockchainsUtil.MAX_BITS;

		JSONObject  prior = read_header(height - 1);
		int bits = prior.optInt("bits");
		
		//NOV 13 HF DAA

		int prevheight = height - 1;
		int daa_mtp=get_median_time_past(prevheight);
		if(daa_mtp >= 1510600000) {
			System.out.println("prevous height "+prevheight);
			int  daa_starting_height=get_suitable_block_height(prevheight - 144);
			int  daa_ending_height=get_suitable_block_height(prevheight);
			System.out.println("daa_ending_height "+daa_ending_height);
			BigInteger  daa_cumulative_work= BigInteger.ZERO;
			for(int daa_i = daa_starting_height+1 ;daa_i < daa_ending_height+1;daa_i++) {
				JSONObject daa_prior = read_header(daa_i);
				BigInteger daa_bits_for_a_block=new BigInteger(daa_prior.getString("bits"));
				BigInteger daa_work_for_a_block=BlockchainsUtil.bits_to_work(daa_bits_for_a_block);
				daa_cumulative_work = daa_cumulative_work.add(daa_work_for_a_block);
			}

			System.out.println("daa cumalcative "+daa_cumulative_work);
			
			//# calculate and sanitize elapsed time
			int daa_starting_timestamp = read_header(daa_starting_height).getInt("timestamp");
			int daa_ending_timestamp = read_header(daa_ending_height).getInt("timestamp");
			int daa_elapsed_time=daa_ending_timestamp-daa_starting_timestamp;
			if (daa_elapsed_time>172800)
				daa_elapsed_time=172800;
			if (daa_elapsed_time<43200)
				daa_elapsed_time=43200;

			
			BigInteger daa_Wn= daa_cumulative_work.multiply(new BigInteger("600")).divide(new BigInteger(String.valueOf(daa_elapsed_time)));
			BigInteger daa_target= new BigInteger("1").shiftLeft(256).divide(daa_Wn.subtract(BigInteger.ONE));
			BigInteger daa_retval = BlockchainsUtil.target_to_bits(daa_target);
			System.out.println("d aa "+daa_retval);
			return daa_retval;
		}
		//END OF NOV-2017 DAA
		
		if(height%2016 == 0) {
			return get_new_bits(height);
		}
		
		
		if(bits == BlockchainsUtil.MAX_BITS.intValue()) {
			return BlockchainsUtil.MAX_BITS;
		}
		int mtp_6blocks = get_median_time_past(height - 1) - get_median_time_past(height - 7);
		if(mtp_6blocks < 12 * 3600) {
			return new BigInteger(String.valueOf(bits));
			
		}
		// If it took over 12hrs to produce the last 6 blocks, increase the
		// target by 25% (reducing difficulty by 20%).
		BigInteger target = BlockchainsUtil.bits_to_target(new BigInteger(String.valueOf(bits)));
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
			if(height % 2016 != 0) {
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
		BigInteger prior_target = BlockchainsUtil.bits_to_target(new BigInteger(prior.getString("bits")));
		int target_span = 14 * 24 * 60 * 60;
		int span = prior.optInt("timestamp") - first.optInt("timestamp");     
		span = Math.min(Math.max(span, target_span / 4), target_span * 4);
		BigInteger new_target = prior_target.multiply(new BigInteger(Integer.toString(span))).divide(new BigInteger(Integer.toString(target_span)));
		return BlockchainsUtil.target_to_bits(new_target);
	}
	public boolean connect_chunk(int idx,String hexdata) {
		try {
			byte[] data = BlockchainsUtil.bfh(hexdata);
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
		int d = (index * 2016 - checkpoint) * 80;
		if(d < 0) {
			chunk = Arrays.reverse(chunk);
			d = 0;
		}
		write(chunk, d);
		swap_with_parent();
	}
	public void write(byte[] data,int offset) {
		try {
			String filename = getPath();
			FileConnection connection = (FileConnection)Connector.open(Files.getDefaultPath()+filename, Connector.WRITE);
			OutputStream stream = connection.openOutputStream(offset);
			stream.write(data);
			stream.flush();
			stream.close();
			update_size();
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void write(InputStream data,int offset) {
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
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void swap_with_parent() throws IOException {
		if(parent_id == null)
			return;
		int parent_branch_size = parent().height() - checkpoint + 1;
		if(parent_branch_size >= size())
			return;
		System.out.println("swap" +checkpoint +" "+parent_id);
		Integer parent_id = this.parent_id;
		int checkpoint = this.checkpoint;
		Blockchain parent = parent();
		
		String parent_filename = parent.getPath();
		
		FileConnection connection = (FileConnection) Connector.open(Files.getDefaultPath()+getPath(), Connector.READ);
		
		
		FileConnection parentConnection = (FileConnection) Connector.open(Files.getDefaultPath()+parent_filename, Connector.READ_WRITE);
		DataInputStream dis = parentConnection.openDataInputStream();
		dis.skip((checkpoint - parent.checkpoint)*80);
		byte[] parentData = new byte[(int)(parent_branch_size*80)];
		dis.read(parentData);
		write(parentData, 0);
		
		parent.write(connection.openInputStream(), (checkpoint - parent.checkpoint)*80);
		
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
		write(data, delta*80);
		swap_with_parent();
	}

	public boolean can_connect(JSONObject header,boolean check_height) throws JSONException {
		int height = header.optInt("block_height");
		if(check_height && height() != height - 1)
			return false;
		if(height == 0)
			return BlockchainsUtil.hash_header(header).equals(BitcoinMainnet.GENESIS);
		JSONObject previous_header = read_header(height -1);
		if(previous_header == null)
			return false;
		String prev_hash = BlockchainsUtil.hash_header(previous_header);
		if(!prev_hash.equals(header.getString("prev_block_hash"))){
			return false;
		}
		BigInteger bits = get_bits(header);
		try {
			verify_header(header, previous_header, bits);
		}catch(Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public Blockchain fork(JSONObject header) throws JSONException, IOException {
		int checkpoint = header.getInt("block_height");
		Blockchain b=new Blockchain(checkpoint, new Integer(parent().checkpoint));
		save_header(header);
		return b;
	}
}
