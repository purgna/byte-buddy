package net.bytebuddy.utility;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class ClassFileExtraction {

    private static final int ASM_MANUAL = 0;

    public static byte[] extract(Class<?> type) throws IOException {
        ClassReader classReader = new ClassReader(type.getName());
        ClassWriter classWriter = new ClassWriter(classReader, ASM_MANUAL);
        classReader.accept(classWriter, ASM_MANUAL);
        return classWriter.toByteArray();
    }

    private static class Foo {
        /* empty */
    }

    private static final int CA = 0xCA, FE = 0xFE, BA = 0xBA, BE = 0xBE;

    @Test
    public void testClassFileExtraction() throws Exception {
        byte[] binaryFoo = extract(Foo.class);
        assertThat(binaryFoo.length > 4, is(true));
        assertThat(binaryFoo[0], is(new Integer(CA).byteValue()));
        assertThat(binaryFoo[1], is(new Integer(FE).byteValue()));
        assertThat(binaryFoo[2], is(new Integer(BA).byteValue()));
        assertThat(binaryFoo[3], is(new Integer(BE).byteValue()));
    }
}