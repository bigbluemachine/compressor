package io;

import java.io.IOException;
import java.io.InputStream;

public class FileInputBuffer extends InputBuffer {
	private InputStream in;

	public FileInputBuffer(InputStream is) throws IOException {
		super();
		in = is;
	}

	@Override
	public int fetch() throws IOException {
		int bytesRead = in.read(data);

		currByte = 0;
		currOffset = 0;

		return bytesRead;
	}

	@Override
	public void close() throws IOException {
		in.close();
	}
}
