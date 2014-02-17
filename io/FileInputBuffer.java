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
	public void fetch() throws IOException {
		bytesRead = in.read(data);
	}

	@Override
	public void close() throws IOException {
		in.close();
	}
}
