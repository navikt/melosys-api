package no.nav.melosys.sikkerhet.context;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ThreadLocalAccessInfoTest {

    private final ListAppender<ILoggingEvent> listAppender = new ListAppender<>();

    @BeforeAll
    void setUp() {
        Logger logger = (Logger) LoggerFactory.getLogger("no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo");
        listAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        logger.setLevel(Level.WARN);
        logger.addAppender(listAppender);
        listAppender.start();
    }

    @Test
    void isProcessCall_callIsUnregistered_logAndFallbackToReturnTrue() {
        assertTrue(ThreadLocalAccessInfo.useSystemToken());
        assertThat(listAppender.list)
            .singleElement()
            .matches(iLoggingEvent -> iLoggingEvent.getMessage()
                .contains("Call have not been registret from RestController or Prosess")
            );
    }

    @Test
    void isProcessCall_callIsRegistered_returnTrue() {
        UUID uuid = UUID.randomUUID();
        ThreadLocalAccessInfo.beforeExecuteProcess(uuid, "Test");

        assertTrue(ThreadLocalAccessInfo.useSystemToken());
        assertThat(listAppender.list).isEmpty();

        ThreadLocalAccessInfo.afterExecuteProcess(uuid);
    }

    @Test
    void isProcessCall_webCallIsRegistered_returnFalse() {
        ThreadLocalAccessInfo.beforeControllerRequest("test", false);

        assertFalse(ThreadLocalAccessInfo.useSystemToken());
        assertThat(listAppender.list).isEmpty();

        ThreadLocalAccessInfo.afterControllerRequest("test");
    }

    @Test
    void isProcessCall_adminCallIsRegistrered_returnTrue() {
        ThreadLocalAccessInfo.beforeControllerRequest("Test", true);

        assertTrue(ThreadLocalAccessInfo.useSystemToken());

        ThreadLocalAccessInfo.afterControllerRequest("Test");
    }

    @Test
    void isFrontendCall_callIsUnregistered_returnFalse() {
        assertFalse(ThreadLocalAccessInfo.useOidcToken());
    }

    @Test
    void isFrontendCall_callIsRegistered_returnTrue() {
        ThreadLocalAccessInfo.beforeControllerRequest("Test", false);

        assertTrue(ThreadLocalAccessInfo.useOidcToken());

        ThreadLocalAccessInfo.afterControllerRequest("Test");
    }

    @Test
    void isFrontendCall_webCallIsRegistered_returnFalse() {
        UUID uuid = UUID.randomUUID();
        ThreadLocalAccessInfo.beforeExecuteProcess(uuid, "Test");

        assertFalse(ThreadLocalAccessInfo.useOidcToken());

        ThreadLocalAccessInfo.afterExecuteProcess(uuid);
    }
}
