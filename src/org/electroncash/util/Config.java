package org.electroncash.util;

import java.util.Date;

public class Config {
  private long last_time_fee_estimates_requested;
  private Date date;
  private Map fee_estimates = new HashMap();
  private Map fee_estimates_last_updated = new HashMap();
  private Map item;
  
  public Config() { date = new Date();
    item = new HashMap();
  }
  
  public long requested_fee_estimates() {
    last_time_fee_estimates_requested = date.getTime();
    return last_time_fee_estimates_requested;
  }
  
  public void update_fee_estimates(String i, Integer fee) {
    fee_estimates.put(i, fee);
    fee_estimates_last_updated.put(i, new Long(date.getTime()));
  }
  

  public boolean is_fee_estimates_update_required()
  {
    long now = date.getTime();
    Iterator prev_updates = fee_estimates_last_updated.values().iterator();
    long oldest_fee_time = 0L;
    long tmp = Long.MAX_VALUE;
    if (fee_estimates_last_updated.size() > 0) {
      while (prev_updates.hasNext()) {
        long type = ((Long)prev_updates.next()).longValue();
        if (tmp > type) {
          tmp = type;
        }
      }
      oldest_fee_time = tmp;
    }
    boolean stale_fees = now - oldest_fee_time > 7200L;
    boolean old_request = now - last_time_fee_estimates_requested > 60L;
    return (stale_fees) && (old_request);
  }
  
  public Map get_fee_estimates() { return fee_estimates; }
  


  public void set(String key, Object value) { item.put(key, value); }
  
  public Object get(String key, Object defaultValue) {
    if (item.containsKey(key)) {
      return item.get(key);
    }
    
    return defaultValue;
  }
}
