package no.nav.melosys.domain.audit;

import no.nav.melosys.sikkerhet.context.SubjectHandler;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

@Component
public class MelosysAuditorAware implements AuditorAware<String> {

    private static final String MELOSYS = "MELOSYS";

    private String saksbehanlderBrukerID;

    @Override
    public String getCurrentAuditor() {
        String auditor = SubjectHandler.getInstance().getUserID();

        if (auditor != null) {
            return auditor;
        } else if (saksbehanlderBrukerID != null) {
            return saksbehanlderBrukerID;
        } else
            return MELOSYS;
    }

    public void setSaksbehanlderBrukerID(String saksbehanlderBrukerID) {
        this.saksbehanlderBrukerID = saksbehanlderBrukerID;
    }
}
