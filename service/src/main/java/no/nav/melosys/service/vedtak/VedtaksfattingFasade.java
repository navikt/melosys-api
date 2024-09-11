package no.nav.melosys.service.vedtak;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.begrunnelser.Endretperiode;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.ValideringException;
import no.nav.melosys.service.behandling.BehandlingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.domain.kodeverk.Sakstyper.EU_EOS;

@Service
public class VedtaksfattingFasade {

    private final BehandlingService behandlingService;
    private final FattVedtakVelger fattVedtakVelger;
    public static final int FRIST_KLAGE_UKER = 6;
    private final EosVedtakService eosVedtakService;

    public VedtaksfattingFasade(BehandlingService behandlingService,
                                FattVedtakVelger fattVedtakVelger, EosVedtakService eosVedtakService) {
        this.behandlingService = behandlingService;
        this.eosVedtakService = eosVedtakService;
        this.fattVedtakVelger = fattVedtakVelger;
    }

    @Transactional(noRollbackFor = {ValideringException.class})
    public void fattVedtak(long behandlingID, FattVedtakRequest fattVedtakRequest) throws ValideringException {
        var behandling = behandlingService.hentBehandling(behandlingID);

        validerKanFattesVedtak(behandling);

        FattVedtakInterface fattVedtakInterface = fattVedtakVelger.getFattVedtakService(behandling);
        fattVedtakInterface.fattVedtak(behandling, fattVedtakRequest);
    }

    @Transactional(noRollbackFor = {ValideringException.class})
    public void endreVedtak(long behandlingID, Endretperiode endretperiode, String fritekst, String fritekstSed) {
        var behandling = behandlingService.hentBehandling(behandlingID);
        Sakstyper sakstype = behandling.getFagsak().getType();

        if (sakstype == EU_EOS) {
            eosVedtakService.endreVedtaksperiode(behandling, endretperiode, fritekst, fritekstSed);
        } else {
            throw new FunksjonellException("Vedtaksendring for sakstype " + sakstype + " er ikke støttet.");
        }
    }

    private void validerKanFattesVedtak(Behandling behandling) {
        if (!behandling.kanResultereIVedtak()) {
            throw new FunksjonellException("Kan ikke fatte vedtak ved behandlingstema " + behandling.getTema().getBeskrivelse());
        }
    }
}
