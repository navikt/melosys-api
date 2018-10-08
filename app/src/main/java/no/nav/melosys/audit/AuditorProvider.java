package no.nav.melosys.audit;

import no.nav.melosys.sikkerhet.context.SubjectHandler;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

@Component
public class AuditorProvider implements AuditorAware<String> {

    private static final String MELOSYS = "MELOSYS";

    @Override
    public String getCurrentAuditor() {
        String auditor = SubjectHandler.getInstance().getUserID();

        if (auditor != null) {
            return auditor;
        } else {
            return MELOSYS;
        }
    }
}
