package main;



import java.math.BigInteger;
import java.util.Random;

import electrol.java.util.HashMap;
import electrol.java.util.HashSet;
import electrol.java.util.Iterator;
import electrol.java.util.Map;
import electrol.java.util.Set;
import electrol.util.StringUtils;
import electrol.util.Utils;

public class Coinchooser {

	public static void main(String[] args) throws Exception {
		WalletAddress walletAddress1 = new WalletAddress("1AAxA3cp8NSrXSCMHcwVMdXD1juqVSqjG3", 529636, true, "ff0488b21e00000000000000000083f9baccf67870c86cd7903c45326b6a4bb122fb99e9236d5a8f5739def0125402b1975b7edae8a63ef6e8ed37e300d60637dd1b991487fec71e385b80820b1a5900000300'], 'value': 1100}, {'coinbase': False, 'prevout_hash': '7ddf1b8c2fbba194e9259e037e4f360fab1eb10333a14795911fa8a0c806fe39', 'prevout_n': 1, 'type': 'p2pkh', 'num_sig': 1, 'height': 525572, 'address': <Address 1HxFqPcJAW2HLy4pHxuNfaoHgrXB1tmfb4>, 'signatures': [None], 'x_pubkeys': ['ff0488b21e00000000000000000083f9baccf67870c86cd7903c45326b6a4bb122fb99e9236d5a8f5739def0125402b1975b7edae8a63ef6e8ed37e300d60637dd1b991487fec71e385b80820b1a5901000100");
		TxIn txin = new TxIn(walletAddress1, "d96eecd4edaac5836dcd829ac752164f267f952316a7d2e02d6f4c747cb4248d", 1,new BigInteger("840") );
		txin.setSignature(null);
		
		WalletAddress walletAddress2 = new WalletAddress("1PaaGaj6GqsY83NUQMm89iA1YZmyigHxf7", 525572, true, "ff0488b21e00000000000000000083f9baccf67870c86cd7903c45326b6a4bb122fb99e9236d5a8f5739def0125402b1975b7edae8a63ef6e8ed37e300d60637dd1b991487fec71e385b80820b1a5900000300'], 'value': 1100}, {'coinbase': False, 'prevout_hash': '7ddf1b8c2fbba194e9259e037e4f360fab1eb10333a14795911fa8a0c806fe39', 'prevout_n': 1, 'type': 'p2pkh', 'num_sig': 1, 'height': 525572, 'address': <Address 1HxFqPcJAW2HLy4pHxuNfaoHgrXB1tmfb4>, 'signatures': [None], 'x_pubkeys': ['ff0488b21e00000000000000000083f9baccf67870c86cd7903c45326b6a4bb122fb99e9236d5a8f5739def0125402b1975b7edae8a63ef6e8ed37e300d60637dd1b991487fec71e385b80820b1a5901000100");
		TxIn txin1 = new TxIn(walletAddress2, "7ddf1b8c2fbba194e9259e037e4f360fab1eb10333a14795911fa8a0c806fe39", 0,new BigInteger("3300") );
		txin1.setSignature(null);
		
		WalletAddress walletAddress3 = new WalletAddress("1ECiyNygHMUCxGNn38jcP3AU4KrCPtaRNt", 529636, true, "ff0488b21e00000000000000000083f9baccf67870c86cd7903c45326b6a4bb122fb99e9236d5a8f5739def0125402b1975b7edae8a63ef6e8ed37e300d60637dd1b991487fec71e385b80820b1a5901000200'], 'value': 1100}, {'coinbase': False, 'prevout_hash': '7ddf1b8c2fbba194e9259e037e4f360fab1eb10333a14795911fa8a0c806fe39', 'prevout_n': 1, 'type': 'p2pkh', 'num_sig': 1, 'height': 525572, 'address': <Address 1HxFqPcJAW2HLy4pHxuNfaoHgrXB1tmfb4>, 'signatures': [None], 'x_pubkeys': ['ff0488b21e00000000000000000083f9baccf67870c86cd7903c45326b6a4bb122fb99e9236d5a8f5739def0125402b1975b7edae8a63ef6e8ed37e300d60637dd1b991487fec71e385b80820b1a5901000100");
		TxIn txin2 = new TxIn(walletAddress3, "d96eecd4edaac5836dcd829ac752164f267f952316a7d2e02d6f4c747cb4248d", 0,new BigInteger("94040") );
		txin2.setSignature(null);
		
		Set set = new HashSet();
		set.add(txin);
		set.add(txin1);
		set.add(txin2);
		
		
		TxOut out = new TxOut("adddd", new BigInteger("100"));
		Set sout = new HashSet();
		sout.add(out);
		
		Set changeSet = new HashSet();
		changeSet.add("addresses");
		
		new Coinchooser().make_tx(set, sout, changeSet);
		
	}
	
	public Set keys(Set coins) {
		Iterator it = coins.iterator();
		Set key = new HashSet();
		while(it.hasNext()) {
			TxIn txin = (TxIn)it.next();
			key.add(txin.getAddr());
		}
		return key;
	}
	
	public void bucketize_coins(Set coins) {
		Map bucket = new HashMap();
		Iterator it = coins.iterator();
		while(it.hasNext()) {
			TxIn txin = (TxIn)it.next();
			bucket.put(txin.getAddr(), txin);
		}
	}
	
	public Transaction make_tx(Set coins, Set outputs, Set change_addr) throws Exception {
		Object[] arr = coins.toArray();
		Set utxos = new HashSet();
		Transaction transaction = Transaction.from_io(new HashSet(), outputs);
		BigInteger spent_amount = BigInteger.ZERO;
		Object[] out = outputs.toArray();
		
		for(int i=0;i < out.length;i++) {
			TxOut txout = (TxOut)out[i];
			spent_amount = spent_amount.add(txout.getAmount());
		}
		
		coins = bucket(coins, spent_amount.intValue());		
		transaction.addInputs(coins);
		Set change = change_outputs(transaction, change_addr);
		
		transaction.addOutputs(change);
		return transaction;
	}
	
	
	
	private Set change_outputs(Transaction transaction, Set change_addr) {
		Set txout = new HashSet();
		Object[] amounts = change_amounts(transaction , change_addr.size()).toArray();
		Object[] change_addrs = change_addr.toArray(); 
		for(int i=0;i<change_addrs.length;i++) {
			TxOut out = new TxOut((String)change_addrs[i], (BigInteger)amounts[i]);
			txout.add(out);
		}
		return txout;
	}

	private Set change_amounts(Transaction transaction, int size) {
		Object[] in = transaction.getInputs().toArray();
		
		Object[] out = transaction.getOutputs().toArray();
		
		int max = Integer.MIN_VALUE;
		
		for(int j=0; j < size;j++) {
			if(max < ((TxOut)out[j]).getAmount().intValue()){
				max = ((TxOut)out[j]).getAmount().intValue();
			}
		}
		double max_change = Math.max(max * 1.25, 0.02 * 100000000);
		
		int[] zeros = new int[size];
		BigInteger output_value = BigInteger.ZERO;
		for(int j=0; j < size;j++) {
			BigInteger o = ((TxOut)out[j]).getAmount();
			zeros[j] = trailing_zeroes(o.toString());
			output_value = output_value.add(((TxOut)out[j]).getAmount());
		
		}
		
		int max_zero = Integer.MIN_VALUE;
		int min_zero = Integer.MAX_VALUE;
		for(int i=0;i<zeros.length;i++) {
			if(zeros[i] > max_zero) {
				max_zero = zeros[i];
			}
			if(zeros[i] < min_zero) {
				min_zero = zeros[i];
			}
		}
		
		int length = Math.max(0, min_zero - 1) - ((max_zero + 1) + 1);
		
		BigInteger input_value = BigInteger.ZERO;
		for(int j=0; j < size;j++) {
			input_value = input_value.add(((TxIn)in[j]).getAmount());
		}
		
		int change_amount = 0;
		System.out.println(input_value);
		System.out.println(output_value);
		BigInteger fee = input_value.subtract(output_value);
		int n = 1;
		for(n = 1;n<size +1 ;n++) {
			 change_amount = Math.max(0, fee.intValue() - (180*n+78));
			 if(change_amount / n <= max_change) {
			      break;
			 }
		}
		int remaining = change_amount;
		Set amounts = new HashSet();
		
		System.out.println(n);
		while(n > 1) {
			int average = remaining / n;
			int amount = randint((int)(average * 0.7), (int)(average * 1.3));
			remaining -= amount;
			System.out.println(amount);
			amounts.add(new BigInteger(String.valueOf(amount)));
			n -= 1;
		}
		
		int N = 10;
		for(int i =0 ;i<Math.min(2, Math.max(0, min_zero - 1));i++) {
			N = N * N;
		}
		int amount = (remaining / N) * N;
		System.out.println(amount);
		amounts.add(new BigInteger(String.valueOf(amount)));
	
		return amounts;
	}
	
	int trailing_zeroes(String val) {
        int s = val.length();
        int count =0;
        for(int i=0;i<val.length();i++) {
        	if(val.charAt(s-i-1) == '0') {
        		count ++;
        		continue;
        	}
        	break;
        }
        return count;
	}

	private int randint(int i, int j) {
		return i+new Random().nextInt(j-i);
	}

	public Set bucket(Set coins, int amount) throws Exception{
		Set spendBucket = new HashSet();
		Object[] arr = coins.toArray();
		
		int tmp = 0;
		boolean check = true;
		TxIn in = null;
		for(int i=0;i<arr.length;i++) {
			in = (TxIn)arr[i];
			if(in.getAmount().intValue() > (amount + (180+78)) &&  (check || tmp > in.getAmount().intValue())) {
				tmp = in.getAmount().intValue();
				check = false;
			}
		}
		if(tmp != 0 ) {
			spendBucket.add(in);
			return spendBucket;
		}
		for(int i=0;i<arr.length;i++) {
			int fee = 78 + 180 *(i+1);
			in = (TxIn)arr[i];
			tmp = tmp + in.getAmount().intValue();
			if(tmp < (amount + fee)) {
				spendBucket.add(in);
			}
			else {
				check = false;
			}
		}
		if(check) {
			throw new Exception("not able to fullfill");
		}
		return spendBucket;
	}
	
}
