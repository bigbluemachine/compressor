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

	private OutputBuffer(int n) {
		data = new byte[size = n];
		currByte = 0;
		currOffset = 0;
	}

	public void append(long v, int n) throws IOException {
		if (currOffset != 0) {
			data[currByte] &= (1 << currOffset) - 1;
			data[currByte] |= v << currOffset;

			if (currOffset + n < 8) {
				currOffset += n;
				return;
			} else {
				n -= (8 - currOffset);
				v >>= (8 - currOffset);
				checkFlush();
			}
		}

		for (; n >= 8; n -= 8, v >>= 8) {
			data[currByte] = (byte) v;
			checkFlush();
		}

		data[currByte] = (byte) v;
		currOffset = n;
	}

	/**
	 * After each call, sets currByte and currOffset to 0.
	 * @throws IOException
	 */
	public abstract void flush() throws IOException;

	private void checkFlush() throws IOException {
		currOffset = 0;
		if (++currByte == size) {
			flush();
			currByte = 0;
		}
	}
}
