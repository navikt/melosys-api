package no.nav.melosys.regler.util;

import org.junit.Test;
import static org.junit.Assert.*;
import org.slf4j.Logger;

public class LoggerFactoryTest {

    @Test
    public void testRiktigLogger() {
        Logger logger = RegelLoggerFactory.getRegelLogger();
        assertEquals("util.LoggerFactoryTest", logger.getName());
    }
    
}
