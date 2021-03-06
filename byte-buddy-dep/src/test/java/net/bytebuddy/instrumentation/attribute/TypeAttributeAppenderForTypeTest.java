package net.bytebuddy.instrumentation.attribute;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;
import org.mockito.asm.Type;

import java.util.Arrays;
import java.util.Iterator;

import static org.mockito.Mockito.*;

public class TypeAttributeAppenderForTypeTest extends AbstractTypeAttributeAppenderTest {

    @Test
    public void testTypeAnnotation() throws Exception {
        TypeAttributeAppender fieldAttributeAppender = new TypeAttributeAppender.ForType(FooBar.class);
        fieldAttributeAppender.apply(classVisitor, typeDescription);
        verify(classVisitor).visitAnnotation(Type.getDescriptor(Baz.class), true);
        verifyNoMoreInteractions(classVisitor);
        verifyZeroInteractions(typeDescription);
    }

    @Test
    public void testObjectProperties() throws Exception {
        final Iterator<Class<?>> iterator = Arrays.<Class<?>>asList(Void.class, String.class).iterator();
        ObjectPropertyAssertion.of(TypeAttributeAppender.ForType.class).create(new ObjectPropertyAssertion.Creator<Class<?>>() {
            @Override
            public Class<?> create() {
                return iterator.next();
            }
        }).apply();
    }

    @Baz
    @Qux
    @QuxBaz
    private static class FooBar {
        /* empty */
    }
}
