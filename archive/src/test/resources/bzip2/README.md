# Legacy randomized BZip2 fixture

Expected output: 400000 bytes, output[i] = i % 251.
The fixed 512-entry BZip2 randomization table was obtained from Apache Commons
Compress 1.28.0 Rand.java (Apache License 2.0). The table wraps at least once.
The input has no RLE1 runs before or after randomization. CPython bz2 compresses
the randomized bytes at level 9; the randomized flag is then set and the block
and combined CRCs are replaced with the original bytes' independently computed
MSB-first CRC-32/BZIP2 (polynomial 0x04C11DB7, initial/final XOR 0xFFFFFFFF).
CPython bz2.decompress and the test's Commons Compress decoder both verify it.
No project encoder or CRC implementation is used to create this fixture.

SHA-256: 6e531203e1c81b951adfc8322a9efe76ad268d7272dd321a5b78a09e14dd2b2c
