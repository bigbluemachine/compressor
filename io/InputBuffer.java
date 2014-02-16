package io;

import java.io.Closeable;
import java.io.IOException;

public abstract class InputBuffer implements Closeable {
	private final static int DEFAULT_SIZE = 1024;

	protected byte[] data;
	protected int bytesRead;
	protected int currByte;
	protected int currOffset;

	public InputBuffer() {
		this(DEFAULT_SIZE);
	}

	public InputBuffer(int n) {
		data = new byte[n];
		bytesRead = 0;
		currByte = 0;
		currOffset = 0;
	}

	public boolean hasMore() throws IOException {
		if (currByte < bytesRead) {
			return true;
		}

		return (bytesRead = fetch()) != -1;
	}

	public boolean read() throws IOException {
		int value = data[currByte] & 1 << currOffset;

		if (++currOffset == 8) {
			currOffset = 0;
			++currByte;
		}

		return value > 0;
	}

	public int readInt(int n) throws IOException {
		int v = 0;

		for (int i = 0; i < n; i++) {
			if (read()) {
				v |= 1 << i;
			}
		}

		return v;
	}

	public long readLong(int n) throws IOException {
		long v = 0;

		for (int i = 0; i < n; i++) {
			if (read()) {
				v |= 1L << i;
			}
		}

		return v;
	}

	public abstract int fetch() throws IOException;
}
