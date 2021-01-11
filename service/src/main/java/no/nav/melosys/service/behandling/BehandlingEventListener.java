package no.nav.melosys.service.behandling;

import java.time.Instant;
import java.time.Period;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.dokument.DokumentBestiltEvent;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.exception.IkkeFunnetException;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BehandlingEventListener {

    private static final int DOKUMENTASJON_SVARFRIST_UKER_MANGELBREV = 4;

    private final BehandlingService behandlingService;

    public BehandlingEventListener(BehandlingService behandlingService) {
        this.behandlingService = behandlingService;
    }

    @Transactional
    @EventListener
    public void dokumentBestilt(DokumentBestiltEvent dokumentBestiltEvent) throws IkkeFunnetException {
        if (dokumentBestiltEvent.getProduserbaredokumenter() == Produserbaredokumenter.MELDING_MANGLENDE_OPPLYSNINGER) {
            Behandling behandling = behandlingService.hentBehandlingUtenSaksopplysninger(dokumentBestiltEvent.getBehandlingID());
            if (behandling.erAktiv()) {
                behandlingService.oppdaterStatusOgSvarfrist(
                    behandling,
                    Behandlingsstatus.AVVENT_DOK_PART,
                    Instant.now().plus(Period.ofWeeks(DOKUMENTASJON_SVARFRIST_UKER_MANGELBREV))
                );
            }
        }
    }
}
