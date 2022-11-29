package no.nav.melosys.featuretoggle;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import no.finn.unleash.strategy.Strategy;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ByUserIdStrategy implements Strategy {

    private static final Logger log = LoggerFactory.getLogger(ByUserIdStrategy.class);

    @Override
    public String getName() {
        return "byUserId";
    }

    @Override
    public boolean isEnabled(Map<String, String> parameters) {
        Optional<String> userIDs = Optional.ofNullable(parameters.get("user"));
        if (StringUtils.isEmpty(getLoggedInUserID()) && userIDs.isPresent() && !userIDs.get().isBlank()) {
            log.warn("Finner ikke innlogget bruker i context, mens brukere er registert i unleash. Er sannsynligvis en SED prosess");
        }

        return StringUtils.isNotEmpty(getLoggedInUserID()) && userIDs
            .map(users -> Arrays.asList(users.split(",")))
            .stream().anyMatch(users -> users.contains(getLoggedInUserID()));
    }

    private static String getLoggedInUserID() {
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
