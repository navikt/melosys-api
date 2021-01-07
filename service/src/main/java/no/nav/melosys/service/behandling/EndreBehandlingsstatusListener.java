package no.nav.melosys.service.behandling;

import java.time.Instant;
import java.time.Period;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.service.dokument.DokumentBestiltEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class EndreBehandlingsstatusListener {

    private static final int DOKUMENTASJON_SVARFRIST_UKER_MANGELBREV = 4;

    private final BehandlingService behandlingService;

    public EndreBehandlingsstatusListener(BehandlingService behandlingService) {
        this.behandlingService = behandlingService;
    }

    @Transactional
    @TransactionalEventListener
    public void dokumentBestilt(DokumentBestiltEvent dokumentBestiltEvent) throws IkkeFunnetException {
        if (dokumentBestiltEvent.getProduserbaredokumenter() == Produserbaredokumenter.MELDING_MANGLENDE_OPPLYSNINGER) {
            Behandling behandling = behandlingService.hentBehandlingUtenSaksopplysninger(dokumentBestiltEvent.getBehandlingID());
            if (behandling.erAktiv()) {
                behandling.setStatus(Behandlingsstatus.AVVENT_DOK_PART);
                behandling.setDokumentasjonSvarfristDato(Instant.now().plus(Period.ofWeeks(DOKUMENTASJON_SVARFRIST_UKER_MANGELBREV)));
                behandlingService.lagre(behandling);
            }
        }
    }
}
