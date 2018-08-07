package electrol.java.neo;

public abstract class Buffer {
	final int _capacity;

	int _limit;

	int _position;

	int _mark;

	Buffer(int capacity, int limit, int position, int mark) {
		_capacity = capacity;
		_limit = limit;
		_position = position;
		_mark = mark;
	}

	public final int capacity() {
		return _capacity;
	}

	public final Buffer clear() {
		_limit = _capacity;
		_position = 0;
		_mark = -1;
		return this;
	}

	public final Buffer flip() {
		_limit = _position;
		_position = 0;
		_mark = -1;
		return this;
	}

	public final boolean hasRemaining() {
		return _limit - _position > 0;
	}

	public boolean isReadOnly() {
		return false;
	}

	public final int limit() {
		return _limit;
	}

	public final Buffer limit(int newLimit) {
		if ((newLimit < 0) || (newLimit > _capacity)) {
			throw new IllegalArgumentException();
		}
		if (newLimit < _mark) {
			_mark = -1;
		}
		if (_position > newLimit) {
			_position = newLimit;
		}
		_limit = newLimit;
		return this;
	}

	public final Buffer mark() {
		_mark = _position;
		return this;
	}

	public final int position() {
		return _position;
	}

	public final Buffer position(int newPosition) {
		if ((newPosition < 0) || (newPosition > _limit)) {
			throw new IllegalArgumentException();
		}

		if (newPosition <= _mark) {
			_mark = -1;
		}
		_position = newPosition;
		return this;
	}

	public final int remaining() {
		return _limit - _position;
	}

	public final Buffer reset() throws InvalidMarkException {
		if (_mark == -1) {
			throw new InvalidMarkException();
		}
		_position = _mark;
		return this;
	}

	public final Buffer rewind() {
		_position = 0;
		_mark = -1;
		return this;
	}
} 