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

	private InputBuffer(int n) {
		data = new byte[n];
		bytesRead = 0;
		currByte = 0;
		currOffset = 0;
	}

	public boolean hasMore() throws IOException {
		if (currByte < bytesRead) {
			return true;
		}

		fetch();
		currByte = 0;
		currOffset = 0;
		return bytesRead != -1;
	}

	public int getOffset() {
		return currOffset;
	}

	public int read(int n) throws IOException {
		int v = 0;

		if (currOffset != 0) {
			v = (data[currByte] & 0xff) >> currOffset;
			v &= (1 << n) - 1;

			if (currOffset + n < 8) {
				currOffset += n;
				return v;
			} else {
				n -= (8 - currOffset);
				checkFetch();
			}
		}

		for (; n >= 8; n -= 8) {
			v <<= 8;
			v |= data[currByte] & 0xff;
			checkFetch();
		}

		v <<= n;
		v |= data[currByte] & ((1 << n) - 1);
		currOffset = n;

		return v;
	}

	/**
	 * After each call, sets currByte and currOffset to 0. Expects bytesRead to
	 * be set to the number of bytes read, or -1 if it is impossible to read.
	 * @throws IOException
	 */
	public abstract void fetch() throws IOException;

	private void checkFetch() throws IOException {
		currOffset = 0;
		if (++currByte == bytesRead) {
			fetch();
			currByte = 0;
		}
	}
}
