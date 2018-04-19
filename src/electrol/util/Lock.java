package electrol.util;

public final class Lock {
	private Thread owner;
	private int nestCount;

	public synchronized void lock() throws InterruptedException {
		for(;;) {
			if(tryLock()) return;
			wait();
		}
	}
	public synchronized boolean tryLock() {
		Thread me=Thread.currentThread();
		if(owner!=me) {
			if(nestCount!=0) return false;
			owner=me;
		}
		nestCount++;
		return true;
	}
	public synchronized void unlock() {
		if(owner!=Thread.currentThread())
			throw new IllegalMonitorStateException();
		if(--nestCount == 0) {
			owner=null;
			notify();
		}
	}
}