package electrol.main;



import java.util.Random;

import electrol.java.util.HashMap;
import electrol.java.util.HashSet;
import electrol.java.util.Iterator;
import electrol.java.util.Map;
import electrol.java.util.Set;
import electrol.util.BigInteger;

public class Coinchooser {
	
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
		Transaction transaction = Transaction.from_io(new HashSet(), outputs);
		BigInteger spent_amount = BigInteger.ZERO;
		Object[] out = outputs.toArray();
		
		for(int i=0;i < out.length;i++) {
			TxOut txout = (TxOut)out[i];
			spent_amount = spent_amount.add(txout.getAmount());
		}
		
		coins = bucket(coins, spent_amount.intValue());
		Iterator it = coins.iterator();
		while(it.hasNext()) {
			TxIn txin = (TxIn)it.next();
			System.out.println(txin.getAmount());
		}
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
		System.out.println(transaction.getInputs().size());

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
		Iterator it = transaction.getInputs().iterator();
		while(it.hasNext()) {
			TxIn txin = (TxIn)it.next();
			input_value = input_value.add(txin.getAmount());
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
		TxIn in = null;
		for(int i=0;i<arr.length;i++) {
			in = (TxIn)arr[i];
			
			if(in.getAmount().intValue() > (amount + (180+78))) {
				System.out.println("txin used "+in);
				spendBucket.add(in);
				return spendBucket;
			}
		}
		
		int tmp = 0;
		boolean check = true;
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
