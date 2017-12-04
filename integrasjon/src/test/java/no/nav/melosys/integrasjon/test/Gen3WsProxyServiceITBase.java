package no.nav.melosys.integrasjon.test;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

/**
 * Baseklasse for integrasjonstest-klasser for 3. gen. web service klienter
 * som har proxy service for å veksle inn JWT (OICD) token til SAML token.
 */
@SuppressWarnings("deprecation")
@ContextConfiguration(classes = TestConfig.class)
@TestPropertySource("classpath:test.properties" )
public abstract class Gen3WsProxyServiceITBase {

    @BeforeClass
    public static void setupSecurity() throws Exception {

/*        Properties unitTestProperties = UnitTestConfiguration.getUnitTestProperties(Gen3WsProxyServiceITBase.class.getResource("/test.properties").toURI());
        UnitTestConfiguration.loadToSystemProperties(unitTestProperties, false);

        TestCertificates.setupKeyAndTrustStore();

        MDCOperations.putToMDC(MDCOperations.MDC_CALL_ID, MDCOperations.generateCallId());

        MockServerInfo serverInfo = new MockServerInfo("localhost", 8080, false, "/vedtak");

        MidlertidigOpenAMInnlogging.setupSecurity(serverInfo);*/
    }

    @AfterClass
    public static void unsetSubjectHandler() {
        //SubjectHandlerUtils.unsetSubjectHandler();
    }

}
