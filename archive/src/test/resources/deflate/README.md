`deflate64.zip` is an independent fixture generated with 7-Zip 25.01:

    7z a -tzip -mm=Deflate64 deflate64.zip payload.bin

The 170000-byte payload is reproducible: generate 50000 bytes with
`(i * 37 + i / 7) & 255` (integer division), repeat those bytes once, then
append 70000 ASCII `A` bytes. The fixture provides independent Deflate64 interoperability and long-match coverage.
Exact 65536-byte distances and 65538-byte lengths are checked by explicit bit-level
fixtures in DeflateStreamTest.
