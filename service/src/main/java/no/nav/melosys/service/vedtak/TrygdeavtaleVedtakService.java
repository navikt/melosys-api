package no.nav.melosys.service.vedtak;

import java.time.LocalDate;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.exception.ValideringException;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.DokgenService;
import no.nav.melosys.service.dokument.brev.BrevbestillingRequest;
import no.nav.melosys.service.kontroll.vedtak.VedtakKontrollService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static no.nav.melosys.service.vedtak.VedtakServiceFasade.FRIST_KLAGE_UKER;

@Service
public class TrygdeavtaleVedtakService {
    private static final Logger log = LoggerFactory.getLogger(TrygdeavtaleVedtakService.class);

    private final BehandlingsresultatService behandlingsresultatService;
    private final BehandlingService behandlingService;
    private final ProsessinstansService prosessinstansService;
    private final OppgaveService oppgaveService;
    private final DokgenService dokgenService;
    private final VedtakKontrollService vedtakKontrollService;


    @Autowired
    public TrygdeavtaleVedtakService(BehandlingsresultatService behandlingsresultatService,
                                     BehandlingService behandlingService,
                                     ProsessinstansService prosessinstansService,
                                     OppgaveService oppgaveService,
                                     DokgenService dokgenService,
                                     VedtakKontrollService vedtakKontrollService) {
        this.behandlingsresultatService = behandlingsresultatService;
        this.behandlingService = behandlingService;
        this.prosessinstansService = prosessinstansService;
        this.oppgaveService = oppgaveService;
        this.dokgenService = dokgenService;
        this.vedtakKontrollService = vedtakKontrollService;
    }

    public void fattAvslagPgaManglendePåOpplysninger(Behandling behandling, FattAvslagRequest request) throws ValideringException {
        throw new TekniskException("Ikke implementert");
    }

    public void fattVedtak(Behandling behandling, FattTrygdeavtaleVedtakRequest request) throws ValideringException {
        long behandlingID = behandling.getId();

        String saksnummer = behandling.getFagsak().getSaksnummer();
        log.info("Fatter vedtak for (Trygdeavtale) sak: {} behandling: {}", saksnummer, behandlingID);

        var behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);
        behandlingsresultat.setType(request.getBehandlingsresultatTypeKode());

        if (behandlingsresultat.erInnvilgelse()) {
            vedtakKontrollService.kontrollerInnvilgelse(behandling, behandlingsresultat, request.getVedtakstype(), Sakstyper.TRYGDEAVTALE);
        }

        oppdaterBehandlingsresultat(behandlingsresultat, request);

        if (prosessinstansService.harVedtakInstans(behandlingID)) {
            throw new FunksjonellException("Det finnes allerede en vedtak-prosess for behandling " + behandling);
        }

        behandling.getFagsak().setStatus(Saksstatuser.MEDLEMSKAP_AVKLART);
        behandlingService.endreStatus(behandling, Behandlingsstatus.IVERKSETTER_VEDTAK);

        prosessinstansService.opprettProsessinstansIverksettVedtakTrygdeavtale(behandling, request);
        dokgenService.produserOgDistribuerBrev(behandlingID, lagStorbritanniaBrevbestilling(request));
        oppgaveService.ferdigstillOppgaveMedSaksnummer(saksnummer);
    }

    private BrevbestillingRequest lagStorbritanniaBrevbestilling(FattTrygdeavtaleVedtakRequest request) {
        return new BrevbestillingRequest.Builder()
            .medProduserbardokument(Produserbaredokumenter.STORBRITANNIA)
            .medMottaker(Aktoersroller.BRUKER)
            .medKopiMottakere(request.getKopiMottakere())
            .medInnledningFritekst(request.getInnledningFritekst())
            .medBegrunnelseFritekst(request.getBegrunnelseFritekst())
            .medEktefelleFritekst(request.getEktefelleFritekst())
            .medBarnFritekst(request.getBarnFritekst())
            .medBestillersId(request.getBestillersId())
            .medNyVurderingBakgrunn(request.getNyVurderingBakgrunn())
            .build();
    }

    private void oppdaterBehandlingsresultat(Behandlingsresultat behandlingsresultat, FattTrygdeavtaleVedtakRequest request) throws IkkeFunnetException {
        behandlingsresultat.settVedtakMetadata(request.getVedtakstype(), request.getNyVurderingBakgrunn(), LocalDate.now().plusWeeks(FRIST_KLAGE_UKER));
        behandlingsresultat.setBegrunnelseFritekst(request.getBegrunnelseFritekst());
        behandlingsresultat.setInnledningFritekst(request.getInnledningFritekst());
        behandlingsresultat.setFastsattAvLand(Landkoder.NO);

        behandlingsresultatService.lagre(behandlingsresultat);
    }
}
