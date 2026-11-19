package org.nervousync.test.archive.lzma;

import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * <h2 class="en-US">Raw LZMA2 interoperability and stream contract tests.</h2>
 * <h2 class="zh-CN">原始 LZMA2 互操作和流接口契约测试。</h2>
 */
final class LZMA2StreamTest {

    /**
     * <h4 class="en-US">Decode the independent preset-6 fixture.</h4>
     * <h4 class="zh-CN">解码独立生成的 preset-6 固定样本。</h4>
     */
    @Test
    void readsIndependentRawLzma2() throws Exception {
        Class<?> type=assertDoesNotThrow(() -> Class.forName("org.nervousync.archive.io.lzma.LZMA2InputStream"));
        try (InputStream source=this.getClass().getResourceAsStream("/lzma2/python-6.lzma2");
            InputStream input=(InputStream)type.getConstructor(InputStream.class,int.class).newInstance(source,65536);
            InputStream expected=this.getClass().getResourceAsStream("/lzma2/payload.bin")) {
            assertArrayEquals(expected.readAllBytes(),input.readAllBytes());
        }
    }
}
