package org.electroncash.util;

import java.io.PrintStream;
import java.util.Random;


public class Coinchooser
{
  private Storage storage;
  
  public Coinchooser(Storage storage)
  {
    this.storage = storage;
  }
  
  public Set keys(Set coins) {
    Iterator it = coins.iterator();
    Set key = new HashSet();
    while (it.hasNext()) {
      TxIn txin = (TxIn)it.next();
      key.add(txin.getAddr());
    }
    return key;
  }
  
  public void bucketize_coins(Set coins) {
    Map bucket = new HashMap();
    Iterator it = coins.iterator();
    while (it.hasNext()) {
      TxIn txin = (TxIn)it.next();
      bucket.put(txin.getAddr(), txin);
    }
  }
  
  public Transaction make_tx(Set coins, Set outputs, Set change_addr) throws Exception {
    Transaction transaction = Transaction.from_io(new HashSet(), outputs, storage);
    long spent_amount = 0L;
    Object[] out = outputs.toArray();
    
    for (int i = 0; i < out.length; i++) {
      TxOut txout = (TxOut)out[i];
      spent_amount += txout.getAmount();
    }
    
    coins = bucket(coins, spent_amount);
    Iterator it = coins.iterator();
    while (it.hasNext()) {
      TxIn txin = (TxIn)it.next();
      System.out.println(txin.getAmount());
    }
    transaction.addInputs(coins);
    Set change = change_outputs(transaction, change_addr);
    
    transaction.addOutputs(change);
    return transaction;
  }
  

  private Set change_outputs(Transaction transaction, Set change_addr)
  {
    Set txout = new HashSet();
    Object[] amounts = change_amounts(transaction, change_addr.size()).toArray();
    Object[] change_addrs = change_addr.toArray();
    for (int i = 0; i < change_addrs.length; i++) {
      TxOut out = new TxOut((String)change_addrs[i], Long.parseLong((String)amounts[i]));
      txout.add(out);
    }
    return txout;
  }
  
  private Set change_amounts(Transaction transaction, int size) {
    System.out.println(transaction.getInputs().size());
    
    Object[] out = transaction.getOutputs().toArray();
    
    long max = -2147483648L;
    
    for (int j = 0; j < size; j++) {
      if (max < ((TxOut)out[j]).getAmount()) {
        max = ((TxOut)out[j]).getAmount();
      }
    }
    double max_change = Math.max(max * 1.25D, 2000000.0D);
    
    int[] zeros = new int[size];
    long output_value = 0L;
    for (int j = 0; j < size; j++) {
      long o = ((TxOut)out[j]).getAmount();
      zeros[j] = trailing_zeroes(String.valueOf(o));
      output_value += ((TxOut)out[j]).getAmount();
    }
    

    int max_zero = Integer.MIN_VALUE;
    int min_zero = Integer.MAX_VALUE;
    for (int i = 0; i < zeros.length; i++) {
      if (zeros[i] > max_zero) {
        max_zero = zeros[i];
      }
      if (zeros[i] < min_zero) {
        min_zero = zeros[i];
      }
    }
    


    long input_value = 0L;
    Iterator it = transaction.getInputs().iterator();
    while (it.hasNext()) {
      TxIn txin = (TxIn)it.next();
      input_value += txin.getAmount();
    }
    
    long change_amount = 0L;
    long fee = input_value - output_value;
    int n = 1;
    for (n = 1; n < size + 1; n++) {
      change_amount = Math.max(0L, fee - (180 * n + 78));
      if (change_amount / n <= max_change) {
        break;
      }
    }
    long remaining = change_amount;
    Set amounts = new HashSet();
    
    System.out.println(n);
    while (n > 1) {
      float average = (float)(remaining / n);
      int amount = randint((int)(average * 0.7D), (int)(average * 1.3D));
      remaining -= amount;
      System.out.println(amount);
      amounts.add(new BigInteger(String.valueOf(amount)));
      n--;
    }
    
    int N = 10;
    for (int i = 0; i < Math.min(2, Math.max(0, min_zero - 1)); i++) {
      N *= N;
    }
    int amount = (int)(remaining / N * N);
    amounts.add(new BigInteger(String.valueOf(amount)));
    
    return amounts;
  }
  
  int trailing_zeroes(String val) {
    int s = val.length();
    int count = 0;
    for (int i = 0; i < val.length(); i++) {
      if (val.charAt(s - i - 1) != '0') break;
      count++;
    }
    


    return count;
  }
  
  private int randint(int i, int j) {
    return i + new Random().nextInt(j - i);
  }
  
  public Set bucket(Set coins, long amount) throws Exception {
    Set spendBucket = new HashSet();
    Object[] arr = coins.toArray();
    TxIn in = null;
    for (int i = 0; i < arr.length; i++) {
      in = (TxIn)arr[i];
      
      if (in.getAmount() > amount + 258L) {
        System.out.println("txin used " + in);
        spendBucket.add(in);
        return spendBucket;
      }
    }
    
    long tmp = 0L;
    boolean check = true;
    for (int i = 0; i < arr.length; i++) {
      int fee = 78 + 180 * (i + 1);
      in = (TxIn)arr[i];
      tmp += in.getAmount();
      if (tmp < amount + fee) {
        spendBucket.add(in);
      }
      else {
        check = false;
      }
    }
    if (check) {
      throw new Exception("not able to fullfill");
    }
    return spendBucket;
  }
}
