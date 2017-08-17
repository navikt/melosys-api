package no.nav.melosys.integrasjon.test;

import java.io.IOException;

import no.nav.vedtak.isso.MockServerInfo;
import no.nav.vedtak.isso.OpenAMHelper;
import no.nav.vedtak.sikkerhet.context.SubjectHandlerUtils;
import no.nav.vedtak.sikkerhet.domene.IdTokenAndRefreshToken;

public class MidlertidigOpenAMInnlogging {

    public static final String USERID_1 = "Z990998";

    public MidlertidigOpenAMInnlogging() {
        // skjult
    }

    public static void setupSecurity(MockServerInfo serverInfo) throws IOException {

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

    public static void verifiserSystemProperty(String propKey) {
        if(System.getProperty(propKey)==null){
            throw new IllegalStateException("Mangler System.property: " + propKey);
        }
    }
}
