package io;

import java.io.IOException;
import java.io.OutputStream;

public class FileOutputBuffer extends OutputBuffer {
	private OutputStream out;

	public FileOutputBuffer(OutputStream os) {
		super();
		out = os;
	}

	@Override
	public void flush() throws IOException {
		if (currByte == 0 && currOffset == 0) {
			return;
		}

		if (currOffset > 0) {
			data[currByte] &= (1 << currOffset) - 1;
			out.write(data, 0, currByte + 1);
		} else {
			out.write(data, 0, currByte);
		}
	}

	@Override
	public void close() throws IOException {
		flush();
		out.close();
	}
}
