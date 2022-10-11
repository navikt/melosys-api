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
        String userID;
        SubjectHandler instance = SubjectHandler.getInstance();
        if (instance != null && instance.getUserID() != null) {
            userID = instance.getUserID();
        } else {
            userID = ThreadLocalAccessInfo.getSaksbehandler();
        }

        if (userID == null) {
            throw new TekniskException("Unleash forventer en bruker");
        }

        return StringUtils.isNotEmpty(userID) && Optional.ofNullable(parameters.get("user"))
            .map(users -> Arrays.asList(users.split(",")))
            .stream().anyMatch(users -> users.contains(userID));
    }
}
