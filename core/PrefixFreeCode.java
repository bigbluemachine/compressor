package core;

import java.util.Arrays;

public class PrefixFreeCode {
	static int[] F;
	static int L;
	static long[][][] T = new long[128][256][256];
	static int[][][] B = new int[128][256][256];

	static long M(int R, int N, int D) {
		if (T[N - 1][R][D - 1] >= 0) {
			return T[N - 1][R][D - 1];
		}

		long best = Long.MAX_VALUE;
		long diff = 0;

		int maxI = Math.min(R, 2 * N);
		for (int i = 1; i <= maxI; i++) {
			diff += F[L - R - i] + (D + 1) * F[L - R - 1 + i];
			long test = diff + M(R - i, i, D + 1);
			if (test < best) {
				best = test;
				B[N - 1][R][D - 1] = i;
			}
		}

		return T[N - 1][R][D - 1] = best;
	}

	public static int[] depths(int[] freq) {
		F = freq;
		L = F.length;

		for (long[][] U : T) {
			for (long[] V : U) {
				Arrays.fill(V, -1);
			}
		}

		for (int i = 0; i < 128; i++) {
			for (int j = 0; j < 256; j++) {
				T[i][0][j] = 0;
			}
		}

		int R = L - 2, N = 1, D = 1;
		M(R, N, D);

		int[] G = new int[L];
		G[0] = G[1] = 1;
		int lastIndex = 1;

		for (;;) {
			int i = B[N - 1][R][D - 1];
			if (i == 0) {
				break;
			}

			for (int j = 0; j < i; j++) {
				G[lastIndex - j] = D + 1;
				G[lastIndex + j + 1] = D + 1;
			}

			lastIndex += i;
			R -= i;
			N = i;
			D++;
		}

		return G;
	}
}
