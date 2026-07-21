package no.nav.melosys.audit;

import java.util.Optional;

import no.nav.melosys.sikkerhet.tilgang.SaksflytSubjektHolder;
import no.nav.melosys.sikkerhet.tilgang.SubjectHandler;
import org.springframework.data.domain.AuditorAware;
import org.springframework.util.ObjectUtils;

public class AuditorProvider implements AuditorAware<String> {

    private static final String MELOSYS = "MELOSYS";

    @Override
    public Optional<String> getCurrentAuditor() {
        String auditor = SubjectHandler.getInstance().getUserID();
        if (auditor != null) {
            return Optional.of(auditor);
        }

        String saksbehandler = SaksflytSubjektHolder.get();
        if (!ObjectUtils.isEmpty(saksbehandler)) {
            return Optional.of(saksbehandler);
        }

        return Optional.of(MELOSYS);
    }
}