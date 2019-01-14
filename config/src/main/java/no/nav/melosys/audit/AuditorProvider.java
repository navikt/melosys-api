package no.nav.melosys.audit;

import java.util.Optional;

import no.nav.melosys.sikkerhet.context.SubjectHandler;
import org.springframework.data.domain.AuditorAware;

public class AuditorProvider implements AuditorAware<String> {

    private static final String MELOSYS = "MELOSYS";

    private String saksbehandlerID;

    @Override
    public Optional<String> getCurrentAuditor() {
        String auditor = SubjectHandler.getInstance().getUserID();
        if (auditor != null) {
            return Optional.of(auditor);
        } else if (saksbehandlerID != null) {
            return Optional.of(saksbehandlerID);
        } else {
            return Optional.of(MELOSYS);
        }
    }

    public void setSaksbehanlderID(String saksbehanlderID) {
        this.saksbehandlerID = saksbehanlderID;
    }
}
