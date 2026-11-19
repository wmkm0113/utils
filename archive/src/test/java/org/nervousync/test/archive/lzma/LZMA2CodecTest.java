package org.nervousync.test.archive.lzma;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.nervousync.archive.io.lzma.LZMA2InputStream;
import org.nervousync.archive.io.lzma.LZMA2OutputStream;
import org.tukaani.xz.FinishableWrapperOutputStream;
import org.tukaani.xz.LZMA2Options;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h2 class="en-US">Raw LZMA2 interoperability and stream contract tests.</h2>
 * <h2 class="zh-CN">原始 LZMA2 互操作和流接口契约测试。</h2>
 */
final class LZMA2CodecTest {
    private static byte[] resource(String name) throws IOException {
        try (InputStream input=LZMA2CodecTest.class.getResourceAsStream("/lzma2/"+name)) {
            return input.readAllBytes();
        }
    }
    private static byte[] concat(byte[]... arrays) throws IOException {
        ByteArrayOutputStream output=new ByteArrayOutputStream();
        for (byte[] bytes:arrays) {
            output.write(bytes);
        }
        return output.toByteArray();
    }
    private static byte[] withoutEnd(byte[] bytes) {
        assertEquals(0,bytes[bytes.length-1]);
        return Arrays.copyOf(bytes,bytes.length-1);
    }
    private static void size(ByteArrayOutputStream output,int size) {
        output.write((size-1)>>>8);
        output.write((size-1)&255);
    }
    private static byte[] stored(byte[] bytes,boolean reset) throws IOException {
        ByteArrayOutputStream output=new ByteArrayOutputStream();
        output.write(reset?1:2);
        size(output,bytes.length);
        output.write(bytes);
        return output.toByteArray();
    }
    private static LZMA2Options options(int lc,int lp,int pb) throws IOException {
        LZMA2Options options=new LZMA2Options(1);
        options.setDictSize(65536);
        options.setLcLp(lc,lp);
        options.setPb(pb);
        return options;
    }
    private static byte[] independent(byte[] bytes,LZMA2Options options,boolean flush) throws IOException {
        ByteArrayOutputStream output=new ByteArrayOutputStream();
        try (var encoder=options.getOutputStream(new FinishableWrapperOutputStream(output))) {
            if (flush) {
                encoder.write(bytes,0,bytes.length/2);
                encoder.flush();
                encoder.write(bytes,bytes.length/2,bytes.length-bytes.length/2);
            } else {
                encoder.write(bytes);
            }
        }
        return output.toByteArray();
    }
    private static void assertBoth(byte[] stream,byte[] expected) throws IOException {
        try (InputStream input=new org.tukaani.xz.LZMA2InputStream(new ByteArrayInputStream(stream),65536)) {
            assertArrayEquals(expected,input.readAllBytes(),"independent decoder");
        }
        try (InputStream input=new LZMA2InputStream(new ByteArrayInputStream(stream),65536)) {
            assertArrayEquals(expected,input.readAllBytes(),"project decoder");
        }
    }
    private static List<Integer> controls(byte[] stream) {
        List<Integer> result=new ArrayList<>();
        int offset=0;
        while (true) {
            int control=stream[offset++]&255;
            result.add(control);
            if (control==0) {
                return result;
            }
            int size=((stream[offset]&255)<<8)+(stream[offset+1]&255)+1;
            offset+=2;
            if (control<128) {
                offset+=size;
            } else {
                int compressed=((stream[offset]&255)<<8)+(stream[offset+1]&255)+1;
                offset+=2;
                if (control>=192) {
                    offset++;
                }
                offset+=compressed;
            }
        }
    }

    /**
     * <h4 class="en-US">Decode Python/liblzma fixtures for several presets.</h4>
     * <h4 class="zh-CN">解码 Python/liblzma 生成的多个等级样本。</h4>
     */
    @ParameterizedTest
    @ValueSource(ints={
            0,1,6,9
        })
    void readsPythonStreams(int preset) throws Exception {
        assertBoth(resource("python-"+preset+".lzma2"),resource("payload.bin"));
    }

    /**
     * <h4 class="en-US">Decode independent uncompressed chunks.</h4>
     * <h4 class="zh-CN">解码独立实现生成的未压缩分块。</h4>
     */
    @Test
    void readsIndependentStoredChunks() throws Exception {
        byte[] packed=resource("python-noise.lzma2");
        assertTrue(controls(packed).stream().anyMatch(c -> c==1||c==2));
        assertBoth(packed,resource("noise.bin"));
    }

    /**
     * <h4 class="en-US">Verify every encoding level with an independent decoder.</h4>
     * <h4 class="zh-CN">使用独立解码器验证所有编码等级。</h4>
     */
    @ParameterizedTest
    @ValueSource(ints={
            0,1,2,3,4,5,6,7,8,9
        })
    void everyLevelInteroperatesWithReferenceDecoder(int level) throws Exception {
        for (byte[] expected : new byte[][]{
                new byte[0],
                {42},
                new byte[65535],
                new byte[65536],
                new byte[65537],
                resource("payload.bin"),
                resource("noise.bin")
        }) {
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();
            try (LZMA2OutputStream encoder=new LZMA2OutputStream(bytes,level)) {
                assertEquals(65536,encoder.getDictionarySize());
                for (int offset=0;offset<expected.length;offset+=1009) {
                    encoder.write(expected,offset,Math.min(1009,expected.length-offset));
                }
            }
            assertBoth(bytes.toByteArray(),expected);
            assertTrue(bytes.size()<=expected.length+((expected.length+65535)/65536)*3+1);
        }
    }

    /**
     * <h4 class="en-US">Preserve models across range-only chunk boundaries.</h4>
     * <h4 class="zh-CN">跨仅重置区间编码器的块边界保留概率模型。</h4>
     */
    @Test
    void preservesStateAcrossRangeOnlyChunks() throws Exception {
        byte[] expected=new byte[100000];
        Arrays.fill(expected,(byte)'A');
        byte[] packed=independent(expected,options(3,0,2),true);
        assertTrue(controls(packed).stream().anyMatch(c -> c>=0x80&&c<0xA0),"must exercise state retention");
        assertBoth(packed,expected);
    }

    /**
     * <h4 class="en-US">Verify state, property and dictionary transitions.</h4>
     * <h4 class="zh-CN">验证状态、属性和字典的转换。</h4>
     */
    @Test
    void supportsStatePropertiesAndDictionaryResets() throws Exception {
        byte[] first=new byte[4096];
        byte[] second=new byte[8192];
        for (int i=0;i<second.length;i++) {
            second[i]=(byte)(i%17);
        }
        byte[] a=withoutEnd(independent(first,options(3,0,2),false));
        byte[] b=withoutEnd(independent(second,options(3,0,2),false));
        assertEquals(0xE0,b[0]&0xE0);
        byte[] stateOnly=concat(new byte[]{
                (byte)(0xA0|(b[0]&31))
            }
            ,Arrays.copyOfRange(b,1,5),Arrays.copyOfRange(b,6,b.length));
        assertBoth(concat(a,stateOnly,new byte[]{
                0
            }),concat(first,second));
        byte[] c=withoutEnd(independent(second,options(2,1,1),false));
        c[0]=(byte)(0xC0|(c[0]&31));
        assertBoth(concat(a,c,new byte[]{
                0
            }),concat(first,second));
        assertBoth(concat(a,b,new byte[]{
                0
            }),concat(first,second));
    }

    /**
     * <h4 class="en-US">Allow compressed matches to reference stored history.</h4>
     * <h4 class="zh-CN">允许压缩匹配引用未压缩块形成的历史。</h4>
     */
    @Test
    void storedHistoryIsAvailableToFollowingCompressedMatches() throws Exception {
        byte[] prefix=new byte[16000];
        new Random(319).nextBytes(prefix);
        byte[] following=concat(prefix,prefix,prefix);
        LZMA2Options options=options(3,0,2);
        options.setPresetDict(prefix);
        byte[] body=independent(following,options,false);
        assertTrue((body[0]&255)>=0xC0&&(body[0]&255)<0xE0);
        assertBoth(concat(stored(prefix,true),body),concat(prefix,following));
    }

    /**
     * <h4 class="en-US">Verify stored continuation and property initialization.</h4>
     * <h4 class="zh-CN">验证未压缩块续接及属性初始化。</h4>
     */
    @Test
    void supportsStoredContinuationAndPropertyReinitialization() throws Exception {
        byte[] first=new byte[4096],second=new byte[128],third=new byte[3000];
        Arrays.fill(third,(byte)37);
        byte[] compressed=withoutEnd(independent(third,options(3,0,2),false));
        compressed[0]=(byte)(0xC0|(compressed[0]&31));
        assertBoth(concat(stored(first,true),stored(second,false),compressed,new byte[]{
                0
            }),concat(first,second,third));
        byte[] a=withoutEnd(independent(first,options(3,0,2),false));
        byte[] b=concat(new byte[]{
                (byte)(0xA0|(compressed[0]&31))
            }
            ,Arrays.copyOfRange(compressed,1,5),Arrays.copyOfRange(compressed,6,compressed.length));
        assertBoth(concat(a,stored(first,false),b,new byte[]{
                0
            }),concat(first,first,third));
    }
    private static byte[] compressedChunk(byte[] payload,boolean lzmaEos) throws Exception {
        ByteArrayOutputStream range=new ByteArrayOutputStream();
        try (OutputStream encoder=new org.tukaani.xz.LZMAOutputStream(range,options(3,0,2),lzmaEos)) {
            encoder.write(payload);
        }
        assertTrue(range.size()<=65536);
        ByteArrayOutputStream output=new ByteArrayOutputStream();
        output.write(0xE0|((payload.length-1)>>>16));
        size(output,payload.length);
        size(output,range.size());
        output.write(93);
        range.writeTo(output);
        output.write(0);
        return output.toByteArray();
    }

    /**
     * <h4 class="en-US">Decode maximum chunk sizes and dictionary wraparound.</h4>
     * <h4 class="zh-CN">解码最大分块并验证字典回绕。</h4>
     */
    @Test
    void acceptsMaximumUncompressedChunkAndDictionaryWrap() throws Exception {
        byte[] expected=new byte[2*1024*1024];
        Arrays.fill(expected,(byte)'Z');
        byte[] packed=compressedChunk(expected,false);
        assertEquals(255,packed[0]&255);
        assertBoth(packed,expected);
        byte[] noise=Arrays.copyOf(resource("noise.bin"),65536);
        assertBoth(concat(stored(noise,true),new byte[]{
                0
            }),noise);
    }
    private static void reject(byte[] packed) {
        assertThrows(IOException.class,() -> {
                try (InputStream input=new LZMA2InputStream(new ByteArrayInputStream(packed),65536)) {
                    input.readAllBytes();
                }
            });
    }

    /**
     * <h4 class="en-US">Reject missing resets, reserved controls and invalid properties.</h4>
     * <h4 class="zh-CN">拒绝缺少重置、保留控制值和非法属性。</h4>
     */
    @Test
    void rejectsMissingResetsReservedControlsAndInvalidProperties() throws Exception {
        for (int value=3;value<128;value++) {
            reject(new byte[]{
                    (byte)value
                });
        }
        for (int value:new int[]{
                2,0x80,0xA0,0xC0
            }) {
            reject(new byte[]{
                    (byte)value,0,0,0,4,93,0,0,0,0,0,0
                });
        }
        byte[] stored=stored(new byte[]{
                0
            }
            ,true);
        reject(concat(stored,new byte[]{
                (byte)0x80,0,0,0,4,0,0,0,0,0,0
            }));
        for (int property:new int[]{
                111,225,255
            }) {
            reject(new byte[]{
                    (byte)0xE0,0,0,0,4,(byte)property,0,0,0,0,0,0
                });
        }
        reject(new byte[]{
                (byte)0xE0,0,0,0,3,93,0,0,0,0,0
            });
    }

    /**
     * <h4 class="en-US">Reject truncation, incorrect sizes and internal LZMA1 EOS.</h4>
     * <h4 class="zh-CN">拒绝截断、大小错误和块内 LZMA1 EOS。</h4>
     */
    @Test
    void rejectsTruncationWrongSizesAndInternalLzmaEos() throws Exception {
        byte[] good=compressedChunk(new byte[1000],false);
        for (int cut=0;cut<good.length;cut++) {
            reject(Arrays.copyOf(good,cut));
        }
        for (int count:new int[]{
                999,1001
            }) {
            byte[] wrong=good.clone();
            wrong[1]=(byte)((count-1)>>>8);
            wrong[2]=(byte)(count-1);
            reject(wrong);
        }
        byte[] trailing=concat(Arrays.copyOf(good,good.length-1),new byte[]{
                42,0
            });
        int size=((trailing[3]&255)<<8)+(trailing[4]&255)+1;
        trailing[3]=(byte)(size>>>8);
        trailing[4]=(byte)size;
        reject(trailing);
        reject(compressedChunk(new byte[1000],true));
        reject(new byte[]{
                1,0,1,42
            });
    }

    /**
     * <h4 class="en-US">Verify dictionary limits, read boundaries and source ownership.</h4>
     * <h4 class="zh-CN">验证字典限制、读取边界和输入流所有权。</h4>
     */
    @Test
    void dictionaryBoundsAndStreamOwnershipAreExplicit() throws Exception {
        assertThrows(IOException.class,() -> new LZMA2InputStream(new ByteArrayInputStream(new byte[]{
                0
            }),67108865));
        assertThrows(IllegalArgumentException.class,() -> new LZMA2InputStream(new ByteArrayInputStream(new byte[]{
                0
            }),4095));
        assertThrows(IllegalArgumentException.class,() -> new LZMA2OutputStream(new ByteArrayOutputStream(),10));
        class Source extends ByteArrayInputStream {
            int closes;
            Source(byte[] bytes){
                super(bytes);
            }
            @Override
            public void close(){
                this.closes++;
            }
        }
        byte[] packed=resource("python-6.lzma2");
        Source source=new Source(concat(packed,new byte[]{
                91
            }));
        LZMA2InputStream input=new LZMA2InputStream(source,65536);
        assertEquals(packed.length+1,source.available());
        assertEquals(0,input.read(new byte[0]));
        assertThrows(IndexOutOfBoundsException.class,() -> input.read(new byte[1],-1,1));
        assertEquals(10000,input.skip(10000));
        assertEquals(160000,input.readAllBytes().length);
        assertEquals(91,source.read());
        input.close();
        input.close();
        assertEquals(1,source.closes);
        assertThrows(IOException.class,input::read);
        Source early=new Source(packed);
        LZMA2InputStream unopened=new LZMA2InputStream(early,65536);
        unopened.close();
        assertEquals(packed.length,early.available());
    }

    /**
     * <h4 class="en-US">Verify flush continuation and idempotent finish.</h4>
     * <h4 class="zh-CN">验证 flush 后续写及 finish 幂等性。</h4>
     */
    @Test
    void flushCompletesChunksAndFinishIsIdempotent() throws Exception {
        class Destination extends ByteArrayOutputStream {
            int closes;
            @Override
            public void close(){
                this.closes++;
            }
        }
        Destination bytes=new Destination();
        LZMA2OutputStream output=new LZMA2OutputStream(bytes,5);
        output.write(new byte[1000]);
        output.flush();
        try (InputStream input=new org.tukaani.xz.LZMA2InputStream(new ByteArrayInputStream(bytes.toByteArray()),65536)) {
            assertArrayEquals(new byte[1000],input.readNBytes(1000));
        }
        output.write(99);
        output.finish();
        int size=bytes.size();
        output.finish();
        assertEquals(size,bytes.size());
        assertEquals(0,bytes.closes);
        assertBoth(bytes.toByteArray(),concat(new byte[1000],new byte[]{
                99
            }));
        assertThrows(IOException.class,() -> output.write(1));
        output.close();
        output.close();
        assertEquals(1,bytes.closes);
    }

    /**
     * <h4 class="en-US">Retain decoding failures and secondary close errors.</h4>
     * <h4 class="zh-CN">保留解码失败及次要关闭异常。</h4>
     */
    @Test
    void retainedFailuresAndSuppressedCloseErrorsAreStable() throws Exception {
        LZMA2InputStream input=new LZMA2InputStream(new ByteArrayInputStream(new byte[]{
                3
            }),65536);
        IOException error=assertThrows(IOException.class,input::read);
        assertSame(error,assertThrows(IOException.class,input::read));
        input.close();
        class Broken extends OutputStream {
            int closes;
            @Override
            public void write(int value)throws IOException{
                throw new IOException("write");
            }
            @Override
            public void close()throws IOException{
                this.closes++;
                throw new IOException("close");
            }
        }
        Broken parent=new Broken();
        LZMA2OutputStream output=new LZMA2OutputStream(parent,1);
        output.write(42);
        IOException failure=assertThrows(IOException.class,output::close);
        assertEquals("write",failure.getMessage());
        assertEquals(1,failure.getSuppressed().length);
        assertEquals(1,parent.closes);
        output.close();
    }
}
