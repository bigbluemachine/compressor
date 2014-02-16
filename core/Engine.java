package core;

import io.FileInputBuffer;
import io.FileOutputBuffer;
import io.OutputBuffer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

public class Engine {
	static class Pair {
		int a, b;

		Pair(int aa, int bb) {
			a = aa;
			b = bb;
		}
	}

	static class Table {
		int[] depth;
		long[] vec;

		Table(int n, Node root) {
			depth = new int[256];
			vec = new long[256];
			Arrays.fill(depth, -1);
			traverse(root, 0, 0);
		}

		void traverse(Node n, int d, long v) {
			if (n.l == null && n.r == null) {
				depth[n.symbol] = d;
				vec[n.symbol] = v;
			} else {
				traverse(n.l, d + 1, v);
				traverse(n.r, d + 1, v | 1L << d);
			}
		}
	}

	static class Node {
		Node l, r;
		int symbol;

		Node() {
			symbol = -1;
		}

		boolean add(int d, int s) {
			if (d == 0) {
				if (l == null && r == null && symbol == -1) {
					symbol = s;
					return true;
				}
				return false;
			}

			if (l == null && symbol == -1) {
				l = new Node();
				return l.add(d - 1, s);
			} else if (r == null && symbol == -1) {
				r = new Node();
				return r.add(d - 1, s);
			}

			return (l != null && l.add(d - 1, s)) || (r != null && r.add(d - 1, s));
		}
	}

	static Pair[] getSymbols(File file) throws Exception {
		int[] F = new int[256];

		int l;
		byte[] buf = new byte[1024];
		FileInputStream in = new FileInputStream(file);
		while ((l = in.read(buf)) != -1) {
			for (int i = 0; i < l; i++) {
				F[(int) buf[i] & 0xff]++;
			}
		}
		in.close();

		ArrayList<Pair> L = new ArrayList<Pair>();
		for (int i = 0; i < 256; i++) {
			if (F[i] > 0) {
				L.add(new Pair(i, F[i]));
			}
		}
		Collections.sort(L, new Comparator<Pair>() {
			@Override
			public int compare(Pair p, Pair q) {
				int d = q.b - p.b;
				return d == 0 ? p.a - q.a : d;
			}
		});

		return L.toArray(new Pair[0]);
	}

	static void encodeBody(Table T, File inFile, OutputBuffer ob) throws Exception {
		int l;
		byte[] buf = new byte[1024];
		FileInputStream in = new FileInputStream(inFile);
		while ((l = in.read(buf)) != -1) {
			for (int i = 0; i < l; i++) {
				int b = buf[i] & 0xff;
				ob.append(T.vec[b], T.depth[b]);
			}
		}
		in.close();
	}

	static void encode(File inFile, File outFile) throws Exception {
		// Read input
		Pair[] S = getSymbols(inFile);
		int symbolCount = S.length;
		if (symbolCount < 2) {
			throw new Error("Less than 2 symbols!");
		}

		// Get prefix-free code
		int[] symbols = new int[symbolCount];
		int[] freqs = new int[symbolCount];
		for (int i = 0; i < symbolCount; i++) {
			symbols[i] = S[i].a;
			freqs[i] = S[i].b;
		}
		int[] depths = PrefixFreeCode.depths(freqs);

		// Build tree
		Node root = new Node();
		long bodySize = 0;
		for (int i = 0; i < symbolCount; i++) {
			bodySize += (long) depths[i] * freqs[i];
			root.add(depths[i], symbols[i]);
		}

		// Initialize output
		FileOutputBuffer ob = new FileOutputBuffer(new FileOutputStream(outFile));

		// Emit table information
		ob.append(symbolCount - 1, 8);
		for (int i = 0; i < symbolCount; i++) {
			ob.append(symbols[i], 8);
			ob.append(depths[i], 8);
		}

		// Emit padding
		int padding = (int) ((bodySize + 3) % 8);
		if (padding > 0) {
			padding = 8 - padding;
		}
		ob.append(padding, 3);
		ob.append(0, padding);

		// Emit body
		encodeBody(new Table(symbolCount, root), inFile, ob);
		ob.close();
	}

	static void decode(File inFile, File outFile) throws Exception {
		FileInputBuffer ib = new FileInputBuffer(new FileInputStream(inFile));
		FileOutputBuffer ob = new FileOutputBuffer(new FileOutputStream(outFile));

		if (!ib.hasMore()) {
			ib.close();
			ob.close();
			throw new Error("File is empty!");
		}

		// Read table
		Node root = new Node();
		int tableLength = ib.readInt(8) + 1;
		for (int i = 0; i < tableLength; i++) {
			int symbol = ib.readInt(8);
			int depth = ib.readInt(8);
			root.add(depth, symbol);
		}

		// Skip padding
		int padding = ib.readInt(3);
		ib.readInt(padding);

		// Read body
		Node curr = root;
		while (ib.hasMore()) {
			boolean b = ib.read();
			if (b) {
				if (curr.r == null) {
					ob.append(curr.symbol, 8);
					curr = root;
				}
				curr = curr.r;
			} else {
				if (curr.l == null) {
					ob.append(curr.symbol, 8);
					curr = root;
				}
				curr = curr.l;
			}
		}

		// Does it add up?
		if (curr.l != null) {
			ib.close();
			ob.close();
			throw new Error("Invalid body!");
		}
		ob.append(curr.symbol, 8);

		ib.close();
		ob.close();
	}
}
