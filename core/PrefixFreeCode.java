package core;

public class PrefixFreeCode {
	static final long INF = 1L << 48;
	static long[][] CURR = new long[129][256];
	static long[][] PREV = new long[129][256];
	static int[][][] BEST = new int[129][256][256];
	static int[] F;
	static int L;

	static void initDp(long[][] T) {
		for (int i = 0; i <= 128; i++) {
			T[i][0] = 0;
			for (int j = 1; j < 256; j++) {
				T[i][j] = INF;
			}
		}
	}

	static void dp(int N, int R, int D) {
		long best = INF;
		long diff = 0;

		int maxI = Math.min(R, 2 * N);
		for (int i = 1; i <= maxI; i++) {
			diff += F[L - R - i] + (D + 1) * F[L - R - 1 + i];
			long test = diff + PREV[i][R - i];
			if (test < best) {
				best = test;
				BEST[N][R][D] = i;
			}
		}

		CURR[N][R] = best;
	}

	public static int[] getDepths(int[] freqs) {
		F = freqs;
		L = F.length;

		for (int D = 255; D >= 1; D--) {
			long[][] temp = CURR;
			CURR = PREV;
			PREV = temp;
			initDp(CURR);

			for (int N = 1; N < 128; N++) {
				for (int R = 1; 2 * N + R <= L; R++) {
					dp(N, R, D);
				}
			}
		}

		int[] G = new int[L];
		G[0] = G[1] = 1;
		for (int D = 1, N = 1, R = L - 2; (N = BEST[N][R][D]) > 0; D++) {
			for (int i = 1; i <= N; i++) {
				G[L - R - i] = G[L - R - 1 + i] = D + 1;
			}
			R -= N;
		}

		return G;
	}

	public static int[] getDepths(int[][] symbolList, int symbolCount) {
		if (symbolCount == 1) {
			return new int[] { 1 };
		}

		int[] freqs = new int[symbolCount];
		for (int i = 0; i < symbolCount; i++) {
			freqs[i] = symbolList[i][1];
		}

		return getDepths(freqs);
	}
}
