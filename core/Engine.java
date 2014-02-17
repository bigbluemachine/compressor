package core;

import io.FileInputBuffer;
import io.FileOutputBuffer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

public class Engine {
	static class Table {
		int[] depth;
		long[] vec;

		Table(Node root) {
			depth = new int[256];
			vec = new long[256];
			Arrays.fill(depth, -1);
			traverse(root, 0, 0);
		}

		void traverse(Node n, int d, long v) {
			if (n.l == null && n.r == null) {
				depth[n.symbol] = d;
				vec[n.symbol] = v;
				return;
			}

			if (n.l != null) {
				traverse(n.l, d + 1, v);
			}

			if (n.r != null) {
				traverse(n.r, d + 1, v | 1L << d);
			}
		}
	}

	static class Node {
		Node l, r;
		int symbol, id;

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

		void collect(ArrayList<Node> L) {
			L.add(this);

			if (l != null) {
				l.collect(L);
			}

			if (r != null) {
				r.collect(L);
			}
		}
	}

	static class Cache {
		Node dest;
		ArrayList<Integer> symbols;

		Cache(Node d, ArrayList<Integer> s) {
			dest = d;
			symbols = s;
		}
	}

	public static int[][] getSymbols(File file) throws IOException {
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

		ArrayList<int[]> L = new ArrayList<int[]>();
		for (int i = 0; i < 256; i++) {
			if (F[i] > 0) {
				L.add(new int[] { i, F[i] });
			}
		}
		Collections.sort(L, new Comparator<int[]>() {
			@Override
			public int compare(int[] p, int[] q) {
				int d = q[1] - p[1];
				return d == 0 ? p[0] - q[0] : d;
			}
		});

		return L.toArray(new int[0][]);
	}

	public static void encode(File inFile, File outFile) throws IOException {
		// Do nothing with empty file
		if (inFile.length() == 0) {
			FileOutputStream os = new FileOutputStream(outFile);
			os.close();
			return;
		}

		// Get prefix-free code
		int[][] S = getSymbols(inFile);
		int symbolCount = S.length;
		int[] depths = PrefixFreeCode.getDepths(S, symbolCount);

		// Build tree
		Node root = new Node();
		int bodySize = 0;
		for (int i = 0; i < symbolCount; i++) {
			bodySize += depths[i] * S[i][1];
			root.add(depths[i], S[i][0]);
		}

		// Initialize output
		FileOutputBuffer ob = new FileOutputBuffer(new FileOutputStream(outFile));

		// Emit table information
		ob.append(symbolCount - 1, 8);
		for (int i = 0; i < symbolCount; i++) {
			ob.append(S[i][0], 8);
			ob.append(depths[i], 8);
		}

		// Emit padding
		int padding = (int) ((bodySize + 3) % 8);
		if (padding != 0) {
			padding = 8 - padding;
		}
		ob.append(padding, 3);
		ob.append(0, padding);

		// Emit body
		encodeBody(new Table(root), inFile, ob);
		ob.close();
	}

	public static void decode(File inFile, File outFile) throws IOException {
		// Do nothing with empty file
		if (inFile.length() == 0) {
			FileOutputStream os = new FileOutputStream(outFile);
			os.close();
			return;
		}

		// Initialize input
		FileInputBuffer ib = new FileInputBuffer(new FileInputStream(inFile));
		ib.fetch();

		// Read table
		Node root = new Node();
		int tableLength = ib.read(8) + 1;
		for (int i = 0; i < tableLength; i++) {
			int symbol = ib.read(8);
			int depth = ib.read(8);
			root.add(depth, symbol);
		}

		// Skip padding
		int padding = ib.read(3);
		ib.read(padding);

		// Decode body
		decodeBody(root, outFile, ib);
		ib.close();
	}

	private static void encodeBody(Table lookupTable, File inFile, FileOutputBuffer ob) throws IOException {
		int l;
		byte[] buf = new byte[1024];
		FileInputStream in = new FileInputStream(inFile);
		while ((l = in.read(buf)) != -1) {
			for (int i = 0; i < l; i++) {
				int b = buf[i] & 0xff;
				ob.append(lookupTable.vec[b], lookupTable.depth[b]);
			}
		}
		in.close();
	}

	private static void decodeBody(Node root, File outFile, FileInputBuffer ib) throws IOException {
		// Calculate alignment
		int alignment = ib.getOffset();
		if (alignment != 0) {
			alignment = 8 - alignment;
		}

		// Initialize output
		FileOutputBuffer ob = new FileOutputBuffer(new FileOutputStream(outFile));
		Node curr = root;

		// Read until aligned
		for (int i = 0, v = ib.read(alignment); i < alignment; i++) {
			if ((v & (1 << i)) > 0) {
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

		// Set IDs
		ArrayList<Node> L = new ArrayList<Node>();
		root.collect(L);
		for (int i = 0; i < L.size(); i++) {
			L.get(i).id = i;
		}

		// Read rest
		Cache[] T = new Cache[131072];
		while (ib.hasMore()) {
			int v = ib.read(8);
			int key = curr.id << 8 | v;

			// Cache current position
			if (T[key] == null) {
				ArrayList<Integer> symbols = new ArrayList<Integer>();
				for (int i = 0; i < 8; i++) {
					if ((v & (1 << i)) > 0) {
						if (curr.r == null) {
							symbols.add(curr.symbol);
							curr = root;
						}
						curr = curr.r;
					} else {
						if (curr.l == null) {
							symbols.add(curr.symbol);
							curr = root;
						}
						curr = curr.l;
					}
				}
				T[key] = new Cache(curr, symbols);
			}

			// Emit symbols
			curr = T[key].dest;
			for (int s : T[key].symbols) {
				ob.append(s, 8);
			}
		}

		// Does it add up?
		if (curr.l != null) {
			ib.close();
			ob.close();
			throw new Error("Invalid encoding!");
		}

		// Add last symbol
		ob.append(curr.symbol, 8);
		ob.close();
	}
}
