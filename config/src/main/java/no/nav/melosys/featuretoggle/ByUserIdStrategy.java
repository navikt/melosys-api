package no.nav.melosys.featuretoggle;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import no.finn.unleash.strategy.Strategy;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo;
import org.apache.commons.lang3.StringUtils;

class ByUserIdStrategy implements Strategy {

    @Override
    public String getName() {
        return "byUserId";
    }

    @Override
    public boolean isEnabled(Map<String, String> parameters) {
        String userID = getUserID();
        if (userID == null) {
            throw new TekniskException("Unleash forventer en bruker");
        }

        return StringUtils.isNotEmpty(userID) && Optional.ofNullable(parameters.get("user"))
            .map(users -> Arrays.asList(users.split(",")))
            .stream().anyMatch(users -> users.contains(userID));
    }

    private static String getUserID() {
        String userID = null;
        SubjectHandler instance = SubjectHandler.getInstance();
        if (instance != null) {
            userID = instance.getUserID();
        }
        if (userID == null) {
            userID = ThreadLocalAccessInfo.getSaksbehandler();
        }
        return userID;
    }
}
