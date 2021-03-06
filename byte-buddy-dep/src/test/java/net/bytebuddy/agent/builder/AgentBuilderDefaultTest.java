package net.bytebuddy.agent.builder;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.inline.MethodRebaseResolver;
import net.bytebuddy.instrumentation.LoadedTypeInitializer;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.*;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.*;

public class AgentBuilderDefaultTest {

    private static final String FOO = "foo";

    private static final byte[] QUX = new byte[]{1, 2, 3}, BAZ = new byte[]{4, 5, 6};

    private static final Class<?> REDEFINED = Object.class;

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private Instrumentation instrumentation;

    @Mock
    private ClassLoader classLoader;

    @Mock
    private ByteBuddy byteBuddy;

    @Mock
    private DynamicType.Builder<?> builder;

    @Mock
    private DynamicType.Unloaded<?> unloaded;

    @Mock
    private TypeDescription typeDescription;

    @Mock
    private LoadedTypeInitializer loadedTypeInitializer;

    @Mock
    private AgentBuilder.RawMatcher rawMatcher;

    @Mock
    private AgentBuilder.Transformer transformer;

    @Mock
    private ProtectionDomain protectionDomain;

    @Mock
    private AgentBuilder.BinaryLocator binaryLocator;

    @Mock
    private AgentBuilder.BinaryLocator.Initialized initialized;

    @Mock
    private TypePool typePool;

    @Mock
    private TypePool.Resolution resolution;

    @Mock
    private AgentBuilder.Listener listener;

    private List<ClassFileTransformer> instrumentations;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        instrumentations = new LinkedList<ClassFileTransformer>();
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return instrumentations.add((ClassFileTransformer) invocation.getArguments()[0]);
            }
        }).when(instrumentation).addTransformer(any(ClassFileTransformer.class), anyBoolean());
        when(byteBuddy.rebase(any(TypeDescription.class), any(ClassFileLocator.class), any(MethodRebaseResolver.MethodNameTransformer.class)))
                .thenReturn((DynamicType.Builder) builder);
        when(builder.make()).thenReturn((DynamicType.Unloaded) unloaded);
        when(unloaded.getTypeDescription()).thenReturn(typeDescription);
        Map<TypeDescription, LoadedTypeInitializer> loadedTypeInitializers = new HashMap<TypeDescription, LoadedTypeInitializer>();
        loadedTypeInitializers.put(typeDescription, loadedTypeInitializer);
        when(unloaded.getLoadedTypeInitializers()).thenReturn(loadedTypeInitializers);
        when(transformer.transform(builder, typeDescription)).thenReturn((DynamicType.Builder) builder);
        when(binaryLocator.initialize(FOO, QUX, classLoader)).thenReturn(initialized);
        when(initialized.getTypePool()).thenReturn(typePool);
        when(typePool.describe(FOO)).thenReturn(resolution);
    }

    @Test
    public void testSuccessful() throws Exception {
        when(unloaded.getBytes()).thenReturn(BAZ);
        when(resolution.resolve()).thenReturn(typeDescription);
        when(rawMatcher.matches(typeDescription, classLoader, REDEFINED, protectionDomain)).thenReturn(true);
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .disableSelfInitialization()
                .withBinaryLocator(binaryLocator)
                .withListener(listener)
                .rebase(rawMatcher).transform(transformer)
                .installOn(instrumentation);
        assertThat(instrumentations.size(), is(1));
        assertThat(instrumentations.get(0).transform(classLoader, FOO, REDEFINED, protectionDomain, QUX), is(BAZ));
        verify(listener).onTransformation(typeDescription, unloaded);
        verify(listener).onComplete(FOO);
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer, false);
        verifyNoMoreInteractions(instrumentation);
    }

    @Test
    public void testSuccessfulWithRetransformation() throws Exception {
        when(unloaded.getBytes()).thenReturn(BAZ);
        when(resolution.resolve()).thenReturn(typeDescription);
        when(rawMatcher.matches(typeDescription, classLoader, REDEFINED, protectionDomain)).thenReturn(true);
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .disableSelfInitialization()
                .allowRetransformation()
                .withBinaryLocator(binaryLocator)
                .withListener(listener)
                .rebase(rawMatcher).transform(transformer)
                .installOn(instrumentation);
        assertThat(instrumentations.size(), is(1));
        assertThat(instrumentations.get(0).transform(classLoader, FOO, REDEFINED, protectionDomain, QUX), is(BAZ));
        verify(listener).onTransformation(typeDescription, unloaded);
        verify(listener).onComplete(FOO);
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer, true);
        verifyNoMoreInteractions(instrumentation);
    }

    @Test
    public void testWithError() throws Exception {
        when(unloaded.getBytes()).thenReturn(BAZ);
        RuntimeException exception = mock(RuntimeException.class);
        when(resolution.resolve()).thenThrow(exception);
        when(rawMatcher.matches(typeDescription, classLoader, REDEFINED, protectionDomain)).thenReturn(true);
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .disableSelfInitialization()
                .withBinaryLocator(binaryLocator)
                .withListener(listener)
                .rebase(rawMatcher).transform(transformer)
                .installOn(instrumentation);
        assertThat(instrumentations.size(), is(1));
        assertThat(instrumentations.get(0).transform(classLoader, FOO, REDEFINED, protectionDomain, QUX), nullValue(byte[].class));
        verify(listener).onError(FOO, exception);
        verify(listener).onComplete(FOO);
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer, false);
        verifyNoMoreInteractions(instrumentation);
    }

    @Test
    public void testIgnored() throws Exception {
        when(unloaded.getBytes()).thenReturn(BAZ);
        when(resolution.resolve()).thenReturn(typeDescription);
        when(rawMatcher.matches(typeDescription, classLoader, REDEFINED, protectionDomain)).thenReturn(false);
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default(byteBuddy)
                .disableSelfInitialization()
                .withBinaryLocator(binaryLocator)
                .withListener(listener)
                .rebase(rawMatcher).transform(transformer)
                .installOn(instrumentation);
        assertThat(instrumentations.size(), is(1));
        assertThat(instrumentations.get(0).transform(classLoader, FOO, REDEFINED, protectionDomain, QUX), nullValue(byte[].class));
        verify(listener).onIgnored(FOO);
        verify(listener).onComplete(FOO);
        verifyNoMoreInteractions(listener);
        verify(instrumentation).addTransformer(classFileTransformer, false);
        verifyNoMoreInteractions(instrumentation);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyPrefixThrowsException() throws Exception {
        new AgentBuilder.Default(byteBuddy).withNativeMethodPrefix("");
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(AgentBuilder.Default.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.Default.Matched.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.Default.Transformation.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.Default.ExecutingTransformer.class)
                .apply(new AgentBuilder.Default().new ExecutingTransformer());
        ObjectPropertyAssertion.of(AgentBuilder.Default.InitializationStrategy.SelfInjection.class).apply();
        final Iterator<Class<?>> iterator = Arrays.asList(Object.class, AgentBuilderDefaultTest.class).iterator();
        ObjectPropertyAssertion.of(AgentBuilder.Default.InitializationStrategy.SelfInjection.Nexus.class).create(new ObjectPropertyAssertion.Creator<Class<?>>() {
            @Override
            public Class<?> create() {
                return iterator.next();
            }
        }).apply();
    }
}
