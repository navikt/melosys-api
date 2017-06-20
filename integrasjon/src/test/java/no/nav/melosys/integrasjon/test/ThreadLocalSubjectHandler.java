package no.nav.melosys.integrasjon.test;

import java.security.Principal;
import java.util.HashSet;

import javax.security.auth.Subject;

import no.nav.vedtak.sikkerhet.context.SubjectHandler;
import no.nav.vedtak.sikkerhet.domene.IdentType;
import no.nav.vedtak.sikkerhet.domene.OidcCredential;
import no.nav.vedtak.sikkerhet.domene.SluttBruker;

public class ThreadLocalSubjectHandler extends SubjectHandler {

    private static final String SYSPROP_PREFIX = "aktørklient.threadLocalSubjectHandler.";
    public static final String SYSPROP_TEST_USER_ID = SYSPROP_PREFIX + "userId";
    public static final String SYSPROP_TEST_ID_TOKEN = SYSPROP_PREFIX + "jwt";

    @Override
    public Subject getSubject() {
        HashSet<Principal> principals = new HashSet<>();
        String userId = getRequiredSystemProperty(SYSPROP_TEST_USER_ID);
        principals.add(new SluttBruker(userId, IdentType.InternBruker));

        String javaWebToken = getRequiredSystemProperty(SYSPROP_TEST_ID_TOKEN);
        OidcCredential oidcCredential = new OidcCredential(javaWebToken);
        HashSet<Object> pubCredentials = new HashSet<>();
        pubCredentials.add(oidcCredential);

        return new Subject(true, principals, pubCredentials, new HashSet<>());
    }

    protected String getRequiredSystemProperty(String name) {
        String value = System.getProperty(name);
        if (value == null) {
            throw new RuntimeException("System property " + name + " er null");
        }
        return value;
    }
}