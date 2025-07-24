package com.mycompany.myapp.aop.logging;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import tech.jhipster.config.JHipsterConstants;

@ExtendWith(MockitoExtension.class)
class LoggingAspectTest {

    private static final String TEST_CLASS_NAME = "com.mycompany.myapp.service.TestService";
    private static final String TEST_METHOD_NAME = "testMethod";

    @Mock
    private Environment environment;

    @Mock
    private JoinPoint joinPoint;

    @Mock
    private ProceedingJoinPoint proceedingJoinPoint;

    @Mock
    private Signature signature;

    @InjectMocks
    private LoggingAspect loggingAspect;

    @BeforeEach
    void setUp() {
        lenient().when(joinPoint.getSignature()).thenReturn(signature);
        lenient().when(proceedingJoinPoint.getSignature()).thenReturn(signature);
        lenient().when(signature.getDeclaringTypeName()).thenReturn(TEST_CLASS_NAME);
        lenient().when(signature.getName()).thenReturn(TEST_METHOD_NAME);
    }

    @Test
    void constructor_WithEnvironment_ShouldCreateInstance() {
        // Given
        Environment env = mock(Environment.class);

        // When
        LoggingAspect aspect = new LoggingAspect(env);
        // Then
        // Constructor should complete successfully
        // No assertions needed as constructor success is implicit
    }

    @Test
    void logAfterThrowing_InDevelopmentProfile_ShouldLogDetailedException() {
        // Given
        Exception testException = new RuntimeException("Test exception");
        Throwable cause = new IllegalArgumentException("Cause exception");
        testException.initCause(cause);

        lenient().when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(true);
        lenient().when(joinPoint.getArgs()).thenReturn(new Object[] { "arg1", "arg2" });

        // When
        loggingAspect.logAfterThrowing(joinPoint, testException);

        // Then
        verify(environment).acceptsProfiles(any(Profiles.class));
        // Logger is created internally, so we can't verify the exact log call
        // But we can verify that the joinPoint was accessed for logging
        verify(joinPoint, times(2)).getSignature(); // Called in logger() method and logAfterThrowing()
        verify(signature, atLeastOnce()).getName();
    }

    @Test
    void logAfterThrowing_InProductionProfile_ShouldLogSimplifiedException() {
        // Given
        Exception testException = new RuntimeException("Test exception");
        Throwable cause = new IllegalArgumentException("Cause exception");
        testException.initCause(cause);

        when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(false);

        // When
        loggingAspect.logAfterThrowing(joinPoint, testException);

        // Then
        verify(environment).acceptsProfiles(any(Profiles.class));
        verify(joinPoint, times(2)).getSignature(); // Called in logger() method and logAfterThrowing()
        verify(signature, atLeastOnce()).getName();
    }

    @Test
    void logAfterThrowing_WithNullCause_ShouldHandleGracefully() {
        // Given
        Exception testException = new RuntimeException("Test exception");
        // No cause set - cause will be null

        when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(true);

        // When
        loggingAspect.logAfterThrowing(joinPoint, testException);

        // Then
        verify(environment).acceptsProfiles(any(Profiles.class));
        verify(joinPoint, times(2)).getSignature(); // Called in logger() method and logAfterThrowing()
        verify(signature, atLeastOnce()).getName();
    }

    @Test
    void logAround_WithDebugEnabled_ShouldLogEntryAndExit() throws Throwable {
        // Given
        Object expectedResult = "test result";
        Object[] methodArgs = new Object[] { "arg1", 123, true };

        lenient().when(proceedingJoinPoint.getArgs()).thenReturn(methodArgs);
        lenient().when(proceedingJoinPoint.proceed()).thenReturn(expectedResult);

        // We can't easily mock the logger.isDebugEnabled() since logger is created internally
        // So we test the happy path where proceed() works

        // When
        Object result = loggingAspect.logAround(proceedingJoinPoint);

        // Then
        verify(proceedingJoinPoint).proceed();
        verify(proceedingJoinPoint, atLeastOnce()).getSignature();
        // getArgs() is only called if debug logging is enabled, which we can't control in tests
        assert result == expectedResult;
    }

    @Test
    void logAround_WithIllegalArgumentException_ShouldLogAndRethrow() throws Throwable {
        // Given
        IllegalArgumentException expectedException = new IllegalArgumentException("Invalid argument");
        Object[] methodArgs = new Object[] { "invalid_arg" };

        lenient().when(proceedingJoinPoint.getArgs()).thenReturn(methodArgs);
        lenient().when(proceedingJoinPoint.proceed()).thenThrow(expectedException);

        // When & Then
        try {
            loggingAspect.logAround(proceedingJoinPoint);
            assert false : "Expected IllegalArgumentException to be thrown";
        } catch (IllegalArgumentException e) {
            assert e == expectedException;
            verify(proceedingJoinPoint).proceed();
            verify(proceedingJoinPoint, atLeastOnce()).getSignature();
            verify(proceedingJoinPoint, atLeastOnce()).getArgs();
        }
    }

    @Test
    void logAround_WithRuntimeException_ShouldRethrowWithoutSpecialHandling() throws Throwable {
        // Given
        RuntimeException expectedException = new RuntimeException("Runtime error");
        Object[] methodArgs = new Object[] { "arg1" };

        lenient().when(proceedingJoinPoint.getArgs()).thenReturn(methodArgs);
        lenient().when(proceedingJoinPoint.proceed()).thenThrow(expectedException);

        // When & Then
        try {
            loggingAspect.logAround(proceedingJoinPoint);
            assert false : "Expected RuntimeException to be thrown";
        } catch (RuntimeException e) {
            assert e == expectedException;
            verify(proceedingJoinPoint).proceed();
        }
    }

    @Test
    void logAround_WithNullResult_ShouldHandleGracefully() throws Throwable {
        // Given
        lenient().when(proceedingJoinPoint.getArgs()).thenReturn(new Object[] {});
        lenient().when(proceedingJoinPoint.proceed()).thenReturn(null);

        // When
        Object result = loggingAspect.logAround(proceedingJoinPoint);

        // Then
        verify(proceedingJoinPoint).proceed();
        assert result == null;
    }

    @Test
    void logAround_WithEmptyArguments_ShouldHandleGracefully() throws Throwable {
        // Given
        Object expectedResult = "result";
        lenient().when(proceedingJoinPoint.getArgs()).thenReturn(new Object[] {});
        lenient().when(proceedingJoinPoint.proceed()).thenReturn(expectedResult);

        // When
        Object result = loggingAspect.logAround(proceedingJoinPoint);

        // Then
        verify(proceedingJoinPoint).proceed();
        verify(proceedingJoinPoint, atLeastOnce()).getSignature();
        // getArgs() is only called if debug logging is enabled, which we can't control in tests
        assert result == expectedResult;
    }

    @Test
    void logAround_WithNullArguments_ShouldHandleGracefully() throws Throwable {
        // Given
        Object expectedResult = "result";
        lenient().when(proceedingJoinPoint.getArgs()).thenReturn(null);
        lenient().when(proceedingJoinPoint.proceed()).thenReturn(expectedResult);

        // When
        Object result = loggingAspect.logAround(proceedingJoinPoint);

        // Then
        verify(proceedingJoinPoint).proceed();
        verify(proceedingJoinPoint, atLeastOnce()).getSignature();
        // getArgs() is only called if debug logging is enabled, which we can't control in tests
        assert result == expectedResult;
    }

    @Test
    void springBeanPointcut_ShouldBeCallable() {
        // Given/When
        loggingAspect.springBeanPointcut();
        // Then
        // Method should complete without exception
        // Pointcut methods are typically empty by design
    }

    @Test
    void applicationPackagePointcut_ShouldBeCallable() {
        // Given/When
        loggingAspect.applicationPackagePointcut();
        // Then
        // Method should complete without exception
        // Pointcut methods are typically empty by design
    }

    @Test
    void logger_ShouldReturnLoggerForCorrectClass() throws Exception {
        // This test verifies the logger creation indirectly by ensuring
        // the signature is accessed properly for logger creation

        // Given
        String testClassName = "com.mycompany.myapp.test.TestClass";
        when(signature.getDeclaringTypeName()).thenReturn(testClassName);
        when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(true);

        // When
        loggingAspect.logAfterThrowing(joinPoint, new Exception("test"));

        // Then
        verify(signature).getDeclaringTypeName();
        // The logger should be created for the correct class name
        // We can't directly test logger creation, but we verify the class name is retrieved
    }

    @Test
    void logAfterThrowing_WithDifferentExceptionTypes_ShouldLogAppropriately() {
        // Given
        when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(true);

        // Test with different exception types
        Exception[] exceptions = {
            new RuntimeException("Runtime exception"),
            new IllegalArgumentException("Illegal argument"),
            new NullPointerException("Null pointer"),
            new IllegalStateException("Illegal state"),
        };

        for (Exception exception : exceptions) {
            // When
            loggingAspect.logAfterThrowing(joinPoint, exception);

            // Then
            verify(joinPoint, atLeastOnce()).getSignature();
        }
    }
}
