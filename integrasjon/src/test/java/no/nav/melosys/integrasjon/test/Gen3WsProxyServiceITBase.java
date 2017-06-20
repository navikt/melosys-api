package no.nav.melosys.integrasjon.test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.util.Map;
import java.util.Properties;

import org.junit.BeforeClass;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;

import no.nav.melosys.integrasjon.felles.MDCOperations;
import no.nav.modig.testcertificates.TestCertificates;
import no.nav.vedtak.isso.MockServerInfo;
import no.nav.vedtak.isso.OpenAMHelper;
import no.nav.vedtak.sikkerhet.context.SubjectHandlerUtils;
import no.nav.vedtak.sikkerhet.domene.IdTokenAndRefreshToken;

/**
 * Baseklasse for integrasjonstest-klasser for 3. gen. web service klienter som har proxy service for å veksle inn JWT (OICD)
 * token til SAML token.
 */
@ContextConfiguration(classes = { TestConfig.class })
@Category(End2EndTest.class)
public abstract class Gen3WsProxyServiceITBase {

    private static final Logger log = LoggerFactory.getLogger(Gen3WsProxyServiceITBase.class);

    private static final String USERID_1 = "Z990405"; // TODO Francois Bruk egen bruker fra Ida

    @BeforeClass
    public static void setupOpenAMSecurity() throws Exception {

        Properties unitTestProperties = getUnitTestProperties(Gen3WsProxyServiceITBase.class.getResource("/integrasjon.properties").toURI());
        loadToSystemProperties(unitTestProperties, false);

        TestCertificates.setupKeyAndTrustStore();

        MDCOperations.putToMDC(MDCOperations.MDC_CALL_ID, MDCOperations.generateCallId());

        MockServerInfo serverInfo = new MockServerInfo("localhost", 8080, false, "/melosys");

        log.debug("username " + System.getProperty("systembruker.username"));
        log.debug("pass " + System.getProperty("systembruker.password"));

        setupOpenAMSecurity(serverInfo);
    }

    /*
     * @AfterClass public static void unsetSubjectHandler() { SubjectHandlerUtils.unsetSubjectHandler(); }
     */

    public static Properties getUnitTestProperties(URI uri) {
        Properties unitTestProps = new Properties();
        if (uri == null) {
            return unitTestProps;
        }
        try {
            URLConnection conn = uri.toURL().openConnection();
            try (InputStream is = conn.getInputStream();) {
                unitTestProps.load(is);
                return unitTestProps;
            }
        } catch (IOException e) {
            throw new IllegalStateException("Kunne ikke laste props fra URI=" + uri);
        }

    }

    // overstyrer eksisterende props, skriver ut underveis
    private static void loadToSystemProperties(Properties properties, boolean overwriteSystemProperties) {
        Properties systemProperties = System.getProperties();
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            if (overwriteSystemProperties || !systemProperties.containsKey(entry.getKey())) {
                log.info(entry.getKey() + " = " + entry.getValue());
                systemProperties.setProperty((String) entry.getKey(), (String) entry.getValue());
            }
        }
    }

    private static void setupOpenAMSecurity(MockServerInfo serverInfo) throws IOException {

        verifiserSystemProperty("systembruker.username");
        verifiserSystemProperty("systembruker.password");

        // Simuler subject handler satt opp i reell login module:
        SubjectHandlerUtils.useSubjectHandler(ThreadLocalSubjectHandler.class);

        OpenAMHelper openAMHelper = new OpenAMHelper(serverInfo);

        // Forutsetter at brukernavn og properties er satt
        IdTokenAndRefreshToken idTokenAndRefreshToken = openAMHelper.getToken();
        String idToken = idTokenAndRefreshToken.getIdToken().getToken();

        System.setProperty(ThreadLocalSubjectHandler.SYSPROP_TEST_USER_ID, USERID_1);
        System.setProperty(ThreadLocalSubjectHandler.SYSPROP_TEST_ID_TOKEN, idToken);
    }

    private static void verifiserSystemProperty(String propKey) {
        if (System.getProperty(propKey) == null) {
            throw new IllegalStateException("Mangler System.property: " + propKey);
        }
    }

}
