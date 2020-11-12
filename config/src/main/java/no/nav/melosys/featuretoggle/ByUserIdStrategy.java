package no.nav.melosys.featuretoggle;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import no.finn.unleash.strategy.Strategy;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import org.apache.commons.lang3.StringUtils;

class ByUserIdStrategy implements Strategy {

    @Override
    public String getName() {
        return "byUserId";
    }

    @Override
    public boolean isEnabled(Map<String, String> parameters) {
        final String userID = SubjectHandler.getInstance().getUserID();

        return StringUtils.isNotEmpty(userID) && Optional.ofNullable(parameters.get("user"))
            .map(users -> Arrays.asList(users.split(",")))
            .stream().anyMatch(users -> users.contains(userID));
    }
}
