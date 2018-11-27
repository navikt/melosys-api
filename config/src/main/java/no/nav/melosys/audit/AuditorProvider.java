package no.nav.melosys.audit;

import no.nav.melosys.sikkerhet.context.SubjectHandler;
import org.springframework.data.domain.AuditorAware;

public class AuditorProvider implements AuditorAware<String> {

    private static final String MELOSYS = "MELOSYS";

    private String saksbehandlerID;

    @Override
    public String getCurrentAuditor() {
        String auditor = SubjectHandler.getInstance().getUserID();
        if (auditor != null) {
            return auditor;
        } else if (saksbehandlerID != null) {
            return saksbehandlerID;
        } else
            return MELOSYS;
    }

    public void setSaksbehanlderID(String saksbehanlderID) {
        this.saksbehandlerID = saksbehanlderID;
    }
}
