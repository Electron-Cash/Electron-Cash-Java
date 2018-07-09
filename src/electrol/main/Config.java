package electrol.main;

import java.util.Date;

import electrol.java.util.HashMap;
import electrol.java.util.Iterator;
import electrol.java.util.Map;

public class Config {
	private long last_time_fee_estimates_requested;
	private Date date;
	private Map fee_estimates = new HashMap();
	private Map fee_estimates_last_updated = new HashMap();
	public Config() {
		date = new Date();
	}
	
	public long requested_fee_estimates() {
		last_time_fee_estimates_requested = date.getTime();
		return last_time_fee_estimates_requested;
	}

	public void update_fee_estimates(String i, Integer fee) {
		fee_estimates.put(i, fee);
		fee_estimates_last_updated.put(i, new Long(date.getTime()));
	}

	public boolean is_fee_estimates_update_required() {
		/*Checks time since last requested and updated fee estimates.
        Returns True if an update should be requested.*/
        long now = date.getTime();
        Iterator prev_updates = fee_estimates_last_updated.values().iterator();
        long oldest_fee_time = 0;
        long tmp = Long.MAX_VALUE;
        if(fee_estimates_last_updated.size() > 0) {
        	while (prev_updates.hasNext()) {
        		long type = ((Long) prev_updates.next()).longValue();
        		if(tmp > type) {
        			tmp = type;
        		}
        	}
        	oldest_fee_time = tmp;
        }
        boolean stale_fees = now - oldest_fee_time > 7200;
        boolean old_request = now - last_time_fee_estimates_requested > 60;
        return stale_fees && old_request;
	}
	public Map get_fee_estimates() {
		return fee_estimates;
	}
}
