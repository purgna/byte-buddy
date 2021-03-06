package net.bytebuddy.matcher;

import net.bytebuddy.instrumentation.method.MethodList;
import net.bytebuddy.instrumentation.type.TypeDescription;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class DeclaringMethodMatcherTest extends AbstractElementMatcherTest<DeclaringMethodMatcher<?>> {

    @Mock
    private ElementMatcher<? super MethodList> methodMatcher;

    @Mock
    private TypeDescription typeDescription;

    @Mock
    private MethodList methodList;

    @SuppressWarnings("unchecked")
    public DeclaringMethodMatcherTest() {
        super((Class<DeclaringMethodMatcher<?>>) (Object) DeclaringMethodMatcher.class, "declaresMethods");
    }

    @Test
    public void testMatch() throws Exception {
        when(typeDescription.getDeclaredMethods()).thenReturn(methodList);
        when(methodMatcher.matches(methodList)).thenReturn(true);
        assertThat(new DeclaringMethodMatcher<TypeDescription>(methodMatcher).matches(typeDescription), is(true));
        verify(methodMatcher).matches(methodList);
        verifyNoMoreInteractions(methodMatcher);
        verify(typeDescription).getDeclaredMethods();
        verifyNoMoreInteractions(typeDescription);
    }

    @Test
    public void testNoMatch() throws Exception {
        when(typeDescription.getDeclaredMethods()).thenReturn(methodList);
        when(methodMatcher.matches(methodList)).thenReturn(false);
        assertThat(new DeclaringMethodMatcher<TypeDescription>(methodMatcher).matches(typeDescription), is(false));
        verify(methodMatcher).matches(methodList);
        verifyNoMoreInteractions(methodMatcher);
        verify(typeDescription).getDeclaredMethods();
        verifyNoMoreInteractions(typeDescription);
    }
}
