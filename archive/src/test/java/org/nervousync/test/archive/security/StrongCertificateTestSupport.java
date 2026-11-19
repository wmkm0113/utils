/*
 * Licensed to the Nervousync Studio (NSYC) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.nervousync.test.archive.security;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.params.provider.Arguments;
import org.nervousync.archive.ArchiveFile;
import org.nervousync.archive.commons.ZipGlobals;
import org.nervousync.archive.core.ResourceReader;
import org.nervousync.archive.core.ResourceWriter;
import org.nervousync.archive.data.LocalDataFile;
import org.nervousync.archive.enumerations.zip.StrongAccessMode;
import org.nervousync.archive.options.ArchiveOptions;
import org.nervousync.archive.security.certificate.CertificateCredentials;
import org.nervousync.archive.security.certificate.CertificateProvider;
import org.nervousync.archive.security.password.PasswordProvider;
import org.nervousync.archive.zip.ZipOptions;
import org.nervousync.archive.zip.crypto.ZipCrypto;
import org.nervousync.archive.zip.crypto.strong.StrongInputStream;
import org.nervousync.archive.zip.crypto.strong.StrongOutputStream;
import org.nervousync.archive.zip.models.crypto.StrongEncryptionInfo;
import org.nervousync.archive.zip.models.header.StrongDecryptionHeader;

import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Provider;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

/** Runtime-generated RSA fixtures; no external keystore or expiring checked-in certificate. */
final class StrongCertificateTestSupport {

	static final String PASSWORD = "archive-certificate-test-password";
	static final String ENTRY_PATH = "目录/payload.bin";
	static final int AES256 = ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_AES_256;
	static final int RC2 = ZipGlobals.ZIP64_HEADER_ENC_ALGORITHM_RC2_NEW;
	static final byte[] PAYLOAD = payload(196731);
	private static Fixtures fixtures;

	enum Layout { SINGLE, NATIVE, NUMBERED }

	private StrongCertificateTestSupport() {
	}

	static synchronized Fixtures fixtures() throws Exception {
		if (fixtures == null) {
			fixtures = new Fixtures();
		}
		return fixtures;
	}

	static Stream<Arguments> algorithms() {
		return Stream.of(
				Arguments.of("DES-56", 0x6601, 56),
				Arguments.of("DES-64", 0x6601, 64),
				Arguments.of("3DES-112", 0x6609, 112),
				Arguments.of("3DES-168", 0x6603, 168),
				Arguments.of("AES-128", 0x660E, 128),
				Arguments.of("AES-192", 0x660F, 192),
				Arguments.of("AES-256", 0x6610, 256),
				Arguments.of("RC2-40", 0x6702, 40),
				Arguments.of("RC2-64", 0x6702, 64),
				Arguments.of("RC2-128", 0x6702, 128),
				Arguments.of("RC4-40", 0x6801, 40),
				Arguments.of("RC4-64", 0x6801, 64),
				Arguments.of("RC4-128", 0x6801, 128),
				Arguments.of("Blowfish-32", 0x6720, 32),
				Arguments.of("Blowfish-128", 0x6720, 128),
				Arguments.of("Blowfish-320", 0x6720, 320),
				Arguments.of("Twofish-128", 0x6721, 128),
				Arguments.of("Twofish-192", 0x6721, 192),
				Arguments.of("Twofish-256", 0x6721, 256));
	}

	static byte[] payload(final int size) {
		byte[] bytes = new byte[size];
		new Random(17L + size).nextBytes(bytes);
		return bytes;
	}

	static ArchiveOptions passwordOptions() {
		return ArchiveOptions.newBuilder()
				.passwordProvider(PasswordProvider.fixed(PASSWORD.toCharArray())).build();
	}

	static ArchiveOptions certificateOptions() throws Exception {
		return ArchiveOptions.newBuilder().certificateProvider(fixtures().secondProvider()).build();
	}

	static ArchiveOptions writeOptions(final StrongAccessMode mode) throws Exception {
		return mode == StrongAccessMode.CERTIFICATE ? certificateOptions() : passwordOptions();
	}

	static ZipOptions.Builder zipBuilder(final int algorithm, final int bits,
	                                     final StrongAccessMode mode, final Layout layout) throws Exception {
		List<X509Certificate> recipients = mode.useCertificate() ? fixtures().recipients() : List.of();
		ZipOptions.Builder builder = ZipOptions.newBuilder()
				.strongCrypto(algorithm, bits, mode, recipients)
				.centralDirectoryCrypto(algorithm, bits, mode, recipients)
				.enableCentralDirectoryCompress()
				.centralDirectoryHash(ZipGlobals.ZIP64_HEADER_HASH_ALGORITHM_SHA256);
		if (layout != Layout.SINGLE) {
			builder.splitArchive(true, 65536)
					.splitType(layout == Layout.NATIVE ? ZipOptions.SplitType.NATIVE : ZipOptions.SplitType.NUMBERED);
		}
		return builder;
	}

	static void writeEntry(final ArchiveFile archive, final String path, final byte[] bytes) throws Exception {
		try (ResourceWriter writer = archive.openWriter(path, "").orElseThrow()) {
			try (OutputStream output = writer.openStream(0)) {
				output.write(bytes);
			}
			writer.finish();
		}
	}

	static void createArchive(final Path target, final ZipOptions options,
	                          final StrongAccessMode mode) throws Exception {
		try (ArchiveFile archive = ArchiveFile.zipFile(
				new LocalDataFile(target.toString(), "rw"), writeOptions(mode), options)) {
			writeEntry(archive, ENTRY_PATH, PAYLOAD);
		}
	}

	static byte[] readEntry(final Path target, final ZipOptions zip,
	                        final ArchiveOptions credentials, final String entryPath) throws Exception {
		try (ArchiveFile archive = ArchiveFile.zipFile(new LocalDataFile(target.toString(), "r"), credentials, zip);
		     ResourceReader reader = archive.openReader(archive.entry(entryPath).orElseThrow()).orElseThrow();
		     InputStream input = reader.openStream()) {
			return input.readAllBytes();
		}
	}

	static Path readable(final Path target, final Layout layout) {
		return layout == Layout.NUMBERED ? Path.of(target + ".001") : target;
	}

	static Stored encrypt(final int algorithm, final int bits, final StrongAccessMode mode,
	                       final byte[] data) throws Exception {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		StrongEncryptionInfo info;
		try (StrongOutputStream output = StrongOutputStream.create(bytes, algorithm, bits, mode,
				mode.usePassword() ? PASSWORD.getBytes(StandardCharsets.UTF_8) : null,
				mode.useCertificate() ? fixtures().recipients() : List.of())) {
			output.write(data);
			output.finish();
			info = output.getEncryptionInfo();
		}
		return new Stored(bytes.toByteArray(), info);
	}

	static byte[] decrypt(final Stored stored, final PasswordProvider passwordProvider,
	                      final CertificateProvider certificateProvider) throws Exception {
		ByteArrayInputStream bytes = new ByteArrayInputStream(stored.bytes);
		StrongDecryptionHeader header = ZipCrypto.readStrongDecryptionHeader(
				new DataInputStream(bytes), 1024L * 1024L);
		try (InputStream input = StrongInputStream.entry("test.zip", ENTRY_PATH, bytes, stored.bytes.length,
				passwordProvider, certificateProvider, stored.info, header)) {
			return input.readAllBytes();
		}
	}

	static final class Stored {
		final byte[] bytes;
		final StrongEncryptionInfo info;

		Stored(final byte[] bytes, final StrongEncryptionInfo info) {
			this.bytes = bytes;
			this.info = info;
		}
	}

	static final class Fixtures {
		final KeyPair firstKey;
		final KeyPair secondKey;
		final X509Certificate first;
		final X509Certificate second;
		private final Provider bc = new BouncyCastleProvider();

		Fixtures() throws Exception {
			if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
				Security.addProvider(this.bc);
			}
			KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
			generator.initialize(2048);
			this.firstKey = generator.generateKeyPair();
			this.secondKey = generator.generateKeyPair();
			this.first = certificate(this.firstKey, "first", 1);
			this.second = certificate(this.secondKey, "second", 2);
		}

		List<X509Certificate> recipients() {
			return List.of(this.first, this.second);
		}

		CertificateProvider secondProvider() throws Exception {
			return CertificateProvider.fixed(new CertificateCredentials(this.second, this.secondKey.getPrivate(), null));
		}

		private X509Certificate certificate(final KeyPair key, final String name, final long serial) throws Exception {
			Instant now = Instant.now();
			X500Name subject = new X500Name("CN=Archive test " + name);
			JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(subject,
					BigInteger.valueOf(serial), Date.from(now.minusSeconds(86400)),
					Date.from(now.plusSeconds(30L * 86400L)), subject, key.getPublic());
			builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyEncipherment));
			return new JcaX509CertificateConverter().setProvider(this.bc).getCertificate(builder.build(
					new JcaContentSignerBuilder("SHA256withRSA").setProvider(this.bc).build(key.getPrivate())));
		}
	}
}
