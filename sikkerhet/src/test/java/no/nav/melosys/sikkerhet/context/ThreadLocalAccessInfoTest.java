package no.nav.melosys.sikkerhet.context;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThreadLocalAccessInfoTest {

    private final ListAppender<ILoggingEvent> listAppender = new ListAppender<>();

    @BeforeEach
    void setUp() {
        Logger logger = (Logger) LoggerFactory.getLogger("no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo");
        listAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        logger.setLevel(Level.WARN);
        logger.addAppender(listAppender);
        listAppender.start();
    }

    @Test
    void isProcessCall_callIsUnregistered_logAndFallbacktoUseSystemToken() {
        assertTrue(ThreadLocalAccessInfo.isProcessCall());
        assertThat(listAppender.list)
            .singleElement()
            .matches(iLoggingEvent -> iLoggingEvent.getMessage()
                .contains("Call have not been registret from RestController or Prosess")
            );
    }
}
