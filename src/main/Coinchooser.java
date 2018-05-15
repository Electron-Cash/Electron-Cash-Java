package main;



import java.math.BigInteger;

import electrol.java.util.HashMap;
import electrol.java.util.HashSet;
import electrol.java.util.Iterator;
import electrol.java.util.Map;
import electrol.java.util.Set;

public class Coinchooser {

	public static void main(String[] args) throws Exception {
		WalletAddress walletAddress1 = new WalletAddress("1AAxA3cp8NSrXSCMHcwVMdXD1juqVSqjG3", 529636, true, "ff0488b21e00000000000000000083f9baccf67870c86cd7903c45326b6a4bb122fb99e9236d5a8f5739def0125402b1975b7edae8a63ef6e8ed37e300d60637dd1b991487fec71e385b80820b1a5900000300'], 'value': 1100}, {'coinbase': False, 'prevout_hash': '7ddf1b8c2fbba194e9259e037e4f360fab1eb10333a14795911fa8a0c806fe39', 'prevout_n': 1, 'type': 'p2pkh', 'num_sig': 1, 'height': 525572, 'address': <Address 1HxFqPcJAW2HLy4pHxuNfaoHgrXB1tmfb4>, 'signatures': [None], 'x_pubkeys': ['ff0488b21e00000000000000000083f9baccf67870c86cd7903c45326b6a4bb122fb99e9236d5a8f5739def0125402b1975b7edae8a63ef6e8ed37e300d60637dd1b991487fec71e385b80820b1a5901000100");
		TxIn txin = new TxIn(walletAddress1, "d96eecd4edaac5836dcd829ac752164f267f952316a7d2e02d6f4c747cb4248d", 1,new BigInteger("94040") );
		txin.setSignature(null);
		
		WalletAddress walletAddress2 = new WalletAddress("1PaaGaj6GqsY83NUQMm89iA1YZmyigHxf7", 525572, true, "ff0488b21e00000000000000000083f9baccf67870c86cd7903c45326b6a4bb122fb99e9236d5a8f5739def0125402b1975b7edae8a63ef6e8ed37e300d60637dd1b991487fec71e385b80820b1a5900000300'], 'value': 1100}, {'coinbase': False, 'prevout_hash': '7ddf1b8c2fbba194e9259e037e4f360fab1eb10333a14795911fa8a0c806fe39', 'prevout_n': 1, 'type': 'p2pkh', 'num_sig': 1, 'height': 525572, 'address': <Address 1HxFqPcJAW2HLy4pHxuNfaoHgrXB1tmfb4>, 'signatures': [None], 'x_pubkeys': ['ff0488b21e00000000000000000083f9baccf67870c86cd7903c45326b6a4bb122fb99e9236d5a8f5739def0125402b1975b7edae8a63ef6e8ed37e300d60637dd1b991487fec71e385b80820b1a5901000100");
		TxIn txin1 = new TxIn(walletAddress2, "7ddf1b8c2fbba194e9259e037e4f360fab1eb10333a14795911fa8a0c806fe39", 0,new BigInteger("3300") );
		txin1.setSignature(null);
		
		WalletAddress walletAddress3 = new WalletAddress("1ECiyNygHMUCxGNn38jcP3AU4KrCPtaRNt", 529636, true, "ff0488b21e00000000000000000083f9baccf67870c86cd7903c45326b6a4bb122fb99e9236d5a8f5739def0125402b1975b7edae8a63ef6e8ed37e300d60637dd1b991487fec71e385b80820b1a5901000200'], 'value': 1100}, {'coinbase': False, 'prevout_hash': '7ddf1b8c2fbba194e9259e037e4f360fab1eb10333a14795911fa8a0c806fe39', 'prevout_n': 1, 'type': 'p2pkh', 'num_sig': 1, 'height': 525572, 'address': <Address 1HxFqPcJAW2HLy4pHxuNfaoHgrXB1tmfb4>, 'signatures': [None], 'x_pubkeys': ['ff0488b21e00000000000000000083f9baccf67870c86cd7903c45326b6a4bb122fb99e9236d5a8f5739def0125402b1975b7edae8a63ef6e8ed37e300d60637dd1b991487fec71e385b80820b1a5901000100");
		TxIn txin2 = new TxIn(walletAddress3, "d96eecd4edaac5836dcd829ac752164f267f952316a7d2e02d6f4c747cb4248d", 0,new BigInteger("840") );
		txin2.setSignature(null);
		
		Set set = new HashSet();
		set.add(txin);
		set.add(txin1);
		set.add(txin2);
		
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
		for(int i=0;i<out.length;i++) {
			TxOut txout = (TxOut)arr[i];
			spent_amount = spent_amount.add(txout.getAmount());
		}
		
		BigInteger sum = BigInteger.ZERO;
		int fee = 78;
		for(int size=0;size < arr.length;size++) {
			TxIn txin = (TxIn)arr[size];
			fee += 180 * (size+1);
			sum = sum.add(txin.getAmount()).add(new BigInteger(String.valueOf(fee)));
		}
		System.out.println(sum);
		if(spent_amount.compareTo(sum) > 0) {
			throw new Exception("Amount Can not be paid");
		}
		else {
			transaction.setInputs(coins);
			Set change = change_outputs(transaction, change_addr);
			transaction.setOutputs(change);
			
		}
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
		
		Set spendBucket = new HashSet();
		Object[] out = transaction.getOutputs().toArray();
		BigInteger tmp = BigInteger.ZERO;
		for(int j=0; j < size;j++) {
			tmp = tmp.add(((TxOut)out[j]).getAmount());
		}
		
		for(int i=1;i<=in.length;i++) {
			TxIn txin = (TxIn)in[i-1];
			if(tmp.compareTo(txin.getAmount())>0) {
				int fee = 178 * i + 78;
				tmp = tmp.add(new BigInteger(String.valueOf(fee)));
				spendBucket.add(txin.getAmount().subtract(tmp));
				return spendBucket;
			}		
		}
		
		return spendBucket;
	}

	public Set bucket(Set coins, BigInteger amount) {
		Set spendBucket = new HashSet();
		Object[] arr = coins.toArray();
		
		BigInteger tmp = BigInteger.ZERO;
		for(int i=0;i<arr.length;i++) {
			TxIn txin = (TxIn)arr[i];
			tmp = tmp.add(txin.getAmount());
			if(txin.getAmount().compareTo(amount) >=0 ) {
				spendBucket.add(txin.getAmount());
			}
		}
		return spendBucket;
	}
	
}
