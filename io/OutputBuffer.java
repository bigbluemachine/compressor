package io;

import java.io.Closeable;
import java.io.IOException;

public abstract class OutputBuffer implements Closeable {
	private final static int DEFAULT_SIZE = 1024;

	protected byte[] data;
	protected int size;
	protected int currByte;
	protected int currOffset;

	public OutputBuffer() {
		this(DEFAULT_SIZE);
	}

	public OutputBuffer(int n) {
		data = new byte[size = n];
		currByte = 0;
		currOffset = 0;
	}

	public void append(boolean on) throws IOException {
		if (on) {
			data[currByte] |= 1 << currOffset;
		} else {
			data[currByte] &= ~(1 << currOffset);
		}

		if (++currOffset == 8) {
			currOffset = 0;
			if (++currByte == size) {
				flush();
				currByte = 0;
			}
		}
	}

	public void append(long v, int n) throws IOException {
		for (int i = 0; i < n; i++) {
			append((v & (1 << i)) > 0);
		}
	}

	public abstract int flush() throws IOException;
}
