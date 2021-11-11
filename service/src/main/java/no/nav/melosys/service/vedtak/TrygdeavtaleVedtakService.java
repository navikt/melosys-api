package no.nav.melosys.service.vedtak;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

import static no.nav.melosys.service.vedtak.VedtakServiceFasade.FRIST_KLAGE_UKER;

@Service
public class TrygdeavtaleVedtakService {
    private static final Logger log = LoggerFactory.getLogger(TrygdeavtaleVedtakService.class);

    private final BehandlingsresultatService behandlingsresultatService;
    private final BehandlingService behandlingService;
    private final ProsessinstansService prosessinstansService;
    private final OppgaveService oppgaveService;

    @Autowired
    public TrygdeavtaleVedtakService(BehandlingsresultatService behandlingsresultatService,
                                     BehandlingService behandlingService,
                                     ProsessinstansService prosessinstansService,
                                     OppgaveService oppgaveService) {
        this.behandlingsresultatService = behandlingsresultatService;
        this.behandlingService = behandlingService;
        this.prosessinstansService = prosessinstansService;
        this.oppgaveService = oppgaveService;
    }

    public void fattVedtak(Behandling behandling, FattTrygdeavtaleVedtakRequest request) {
        long behandlingID = behandling.getId();

        String saksnummer = behandling.getFagsak().getSaksnummer();
        log.info("Fatter vedtak for (Trygdeavtale) sak: {} behandling: {}", saksnummer, behandlingID);

        oppdaterBehandlingsresultat(behandlingID, request);

        if (prosessinstansService.harVedtakInstans(behandlingID)) {
            throw new FunksjonellException("Det finnes allerede en vedtak-prosess for behandling " + behandling);
        }

        behandling.getFagsak().setStatus(Saksstatuser.MEDLEMSKAP_AVKLART);
        behandling.setStatus(Behandlingsstatus.IVERKSETTER_VEDTAK);
        behandlingService.lagre(behandling);

        prosessinstansService.opprettProsessinstansIverksettVedtakTrygdeavtale(behandling, request);
        oppgaveService.ferdigstillOppgaveMedSaksnummer(saksnummer);
    }

    private void oppdaterBehandlingsresultat(long behandlingID, FattTrygdeavtaleVedtakRequest request) throws IkkeFunnetException {
        var behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);
        behandlingsresultat.setType(request.getBehandlingsresultatTypeKode());
        behandlingsresultat.settVedtakMetadata(request.getVedtakstype(), LocalDate.now().plusWeeks(FRIST_KLAGE_UKER));
        behandlingsresultat.setBegrunnelseFritekst(request.getFritekstBegrunnelse());
        behandlingsresultat.setFastsattAvLand(Landkoder.NO);

        behandlingsresultatService.lagre(behandlingsresultat);
    }
}
