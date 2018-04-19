package electrol1;

import electrol.java.util.Set;

public class MessagesCallbackTuple {
	private Callback callback;
	private Set methodParamsTuple;
	public Callback getCallback() {
		return callback;
	}
	public void setCallback(Callback callback) {
		this.callback = callback;
	}
	public Set getMethodParamsTuple() {
		return methodParamsTuple;
	}
	public void setMethodParamsTuple(Set methodParamsTuple) {
		this.methodParamsTuple = methodParamsTuple;
	}
	
}
