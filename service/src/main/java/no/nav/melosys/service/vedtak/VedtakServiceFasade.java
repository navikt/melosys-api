package no.nav.melosys.service.vedtak;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.Vedtakstyper;
import no.nav.melosys.domain.kodeverk.begrunnelser.Endretperiode;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.ValideringException;
import no.nav.melosys.service.behandling.BehandlingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.domain.kodeverk.Sakstyper.EU_EOS;

@Service
public class VedtakServiceFasade {

    private final BehandlingService behandlingService;
    private final EosVedtakService eosVedtakService;
    private final EosVedtakSystemService eosVedtakSystemService;
    private final FtrlVedtakService ftrlVedtakService;
    private final TrygdeavtaleVedtakService trygdeavtaleVedtakService;

    public static final int FRIST_KLAGE_UKER = 6;

    @Autowired
    public VedtakServiceFasade(BehandlingService behandlingService,
                               EosVedtakService eosVedtakService,
                               EosVedtakSystemService eosVedtakSystemService,
                               FtrlVedtakService ftrlVedtakService,
                               TrygdeavtaleVedtakService trygdeavtaleVedtakService
    ) {
        this.behandlingService = behandlingService;
        this.eosVedtakService = eosVedtakService;
        this.eosVedtakSystemService = eosVedtakSystemService;
        this.ftrlVedtakService = ftrlVedtakService;
        this.trygdeavtaleVedtakService = trygdeavtaleVedtakService;
    }

    @Transactional(noRollbackFor = {ValideringException.class})
    public void fattVedtak(long behandlingID, Behandlingsresultattyper behandlingsresultattype) throws ValideringException {
        var behandling = behandlingService.hentBehandling(behandlingID);

        eosVedtakSystemService.fattVedtak(behandling, behandlingsresultattype, Vedtakstyper.FØRSTEGANGSVEDTAK);
    }

    @Transactional(noRollbackFor = {ValideringException.class})
    public void fattVedtak(long behandlingID, FattVedtakRequest fattVedtakRequest) throws ValideringException {
        var behandling = behandlingService.hentBehandling(behandlingID);

        validerKanFattesVedtakAvTema(behandling);

        Sakstyper sakstype = behandling.getFagsak().getType();

        switch (sakstype) {
            case EU_EOS -> eosVedtakService.fattVedtak(behandling, fattVedtakRequest);
            case FTRL -> ftrlVedtakService.fattVedtak(behandling, fattVedtakRequest);
            case TRYGDEAVTALE -> trygdeavtaleVedtakService.fattVedtak(behandling, fattVedtakRequest);
            default -> throw new FunksjonellException("Vedtaksfatting for sakstype " + sakstype + " er ikke støttet.");
        }
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

    private void validerKanFattesVedtakAvTema(Behandling behandling) {
        if (!behandling.kanResultereIVedtak()) {
            throw new FunksjonellException("Kan ikke fatte vedtak ved behandlingstema " + behandling.getTema().getBeskrivelse());
        }
    }
}
