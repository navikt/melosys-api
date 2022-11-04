package no.nav.melosys.service.vedtak;

import java.time.LocalDate;
import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
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
import no.nav.melosys.service.kontroll.feature.ferdigbehandling.FerdigbehandlingKontrollFacade;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import static no.nav.melosys.service.vedtak.VedtaksfattingFasade.FRIST_KLAGE_UKER;

@Service
public class TrygdeavtaleVedtakService {
    private static final Logger log = LoggerFactory.getLogger(TrygdeavtaleVedtakService.class);

    private final BehandlingsresultatService behandlingsresultatService;
    private final BehandlingService behandlingService;
    private final ProsessinstansService prosessinstansService;
    private final OppgaveService oppgaveService;
    private final DokgenService dokgenService;
    private final FerdigbehandlingKontrollFacade ferdigbehandlingKontrollFacade;


    public TrygdeavtaleVedtakService(BehandlingsresultatService behandlingsresultatService,
                                     BehandlingService behandlingService,
                                     ProsessinstansService prosessinstansService,
                                     OppgaveService oppgaveService,
                                     DokgenService dokgenService,
                                     FerdigbehandlingKontrollFacade ferdigbehandlingKontrollFacade) {
        this.behandlingsresultatService = behandlingsresultatService;
        this.behandlingService = behandlingService;
        this.prosessinstansService = prosessinstansService;
        this.oppgaveService = oppgaveService;
        this.dokgenService = dokgenService;
        this.ferdigbehandlingKontrollFacade = ferdigbehandlingKontrollFacade;
    }

    public void fattVedtak(Behandling behandling, FattVedtakRequest request) throws ValideringException {
        long behandlingID = behandling.getId();

        String saksnummer = behandling.getFagsak().getSaksnummer();
        log.info("Fatter vedtak for (Trygdeavtale) sak: {} behandling: {}", saksnummer, behandlingID);

        var behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);
        behandlingsresultat.setType(request.getBehandlingsresultatTypeKode());

        if (behandlingsresultat.erInnvilgelse()) {
            ferdigbehandlingKontrollFacade.kontrollerVedtakMedRegisteropplysninger(behandling, behandlingsresultat, Sakstyper.TRYGDEAVTALE, Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN);
        }

        oppdaterBehandlingsresultat(behandlingsresultat, request);

        if (prosessinstansService.harVedtakInstans(behandlingID)) {
            throw new FunksjonellException("Det finnes allerede en vedtak-prosess for behandling " + behandling);
        }

        behandling.getFagsak().setStatus(Saksstatuser.MEDLEMSKAP_AVKLART);
        behandlingService.endreStatus(behandling, Behandlingsstatus.IVERKSETTER_VEDTAK);

        prosessinstansService.opprettProsessinstansIverksettVedtakTrygdeavtale(behandling, request);

        BrevbestillingRequest brevbestillingRequest = lagBrevbestilling(behandling, request);
        dokgenService.produserOgDistribuerBrev(behandlingID, brevbestillingRequest);
        oppgaveService.ferdigstillOppgaveMedSaksnummer(saksnummer);
    }

    private BrevbestillingRequest lagBrevbestilling(Behandling behandling, FattVedtakRequest request) {
        if (request.getBehandlingsresultatTypeKode() == Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL) {
            return lagAvslagMangledeOpplysningerBrevbestilling(request);
        }
        Optional<Produserbaredokumenter> produserbaredokumenter = behandling.getMottatteOpplysninger().getMottatteOpplysningerData().soeknadsland.landkoder.stream()
            .map(Land_iso2::valueOf)
            .findFirst()
            .map(this::utledProduserbartTrygdeavtaleDokument);
        return lagTrygdeavtaleBrevbestilling(request, produserbaredokumenter.get());
    }

    private BrevbestillingRequest lagAvslagMangledeOpplysningerBrevbestilling(FattVedtakRequest request) {
        return new BrevbestillingRequest.Builder()
            .medProduserbardokument(Produserbaredokumenter.AVSLAG_MANGLENDE_OPPLYSNINGER)
            .medMottaker(Aktoersroller.BRUKER)
            .medBestillersId(request.getBestillersId())
            .medFritekst(request.getFritekst())
            .build();
    }

    private BrevbestillingRequest lagTrygdeavtaleBrevbestilling(FattVedtakRequest request, Produserbaredokumenter produserbaredokumenter) {
        return new BrevbestillingRequest.Builder()
            .medProduserbardokument(produserbaredokumenter)
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

    private Produserbaredokumenter utledProduserbartTrygdeavtaleDokument(Land_iso2 soeknadsland) {
        return switch (soeknadsland) {
            case GB -> Produserbaredokumenter.STORBRITANNIA;
            case US -> Produserbaredokumenter.TRYGDEAVTALE_US;
            default ->
                throw new TekniskException("Søknadsland er ikke implementert som produsertbart dokument : " + soeknadsland);
        };
    }

    private void oppdaterBehandlingsresultat(Behandlingsresultat behandlingsresultat, FattVedtakRequest request) throws IkkeFunnetException {
        behandlingsresultat.settVedtakMetadata(request.getVedtakstype(), request.getNyVurderingBakgrunn(), LocalDate.now().plusWeeks(FRIST_KLAGE_UKER));
        behandlingsresultat.setBegrunnelseFritekst(request.getBegrunnelseFritekst());
        behandlingsresultat.setInnledningFritekst(request.getInnledningFritekst());
        behandlingsresultat.setFastsattAvLand(Landkoder.NO);

        behandlingsresultatService.lagre(behandlingsresultat);
    }
}
