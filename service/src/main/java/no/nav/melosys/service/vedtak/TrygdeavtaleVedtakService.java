package no.nav.melosys.service.vedtak;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.kodeverk.Land_iso2;
import no.nav.melosys.domain.kodeverk.Mottakerroller;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.Sakstyper;
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
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import no.nav.melosys.service.kontroll.feature.ferdigbehandling.FerdigbehandlingKontrollFacade;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import no.nav.melosys.service.validering.Kontrollfeil;
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
    private final SaksbehandlingRegler saksbehandlingRegler;


    public TrygdeavtaleVedtakService(BehandlingsresultatService behandlingsresultatService,
                                     BehandlingService behandlingService,
                                     ProsessinstansService prosessinstansService,
                                     OppgaveService oppgaveService,
                                     DokgenService dokgenService,
                                     FerdigbehandlingKontrollFacade ferdigbehandlingKontrollFacade,
                                     SaksbehandlingRegler saksbehandlingRegler) {
        this.behandlingsresultatService = behandlingsresultatService;
        this.behandlingService = behandlingService;
        this.prosessinstansService = prosessinstansService;
        this.oppgaveService = oppgaveService;
        this.dokgenService = dokgenService;
        this.ferdigbehandlingKontrollFacade = ferdigbehandlingKontrollFacade;
        this.saksbehandlingRegler = saksbehandlingRegler;
    }

    public void fattVedtak(Behandling behandling, FattVedtakRequest request) throws ValideringException {
        long behandlingID = behandling.getId();

        String saksnummer = behandling.getFagsak().getSaksnummer();
        log.info("Fatter vedtak for (Trygdeavtale) sak: {} behandling: {}", saksnummer, behandlingID);

        var behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);
        behandlingsresultat.setType(request.getBehandlingsresultatTypeKode());

        if (behandlingsresultat.erInnvilgelse()) {
            Collection<Kontrollfeil> kontrollfeil = ferdigbehandlingKontrollFacade.kontrollerVedtakMedRegisteropplysninger(
                behandling,
                behandlingsresultat,
                Sakstyper.TRYGDEAVTALE,
                Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN,
                null
            );

            if (!kontrollfeil.isEmpty()) {
                throw new ValideringException("Feil i validering. Kan ikke fatte vedtak.",
                    kontrollfeil.stream().map(Kontrollfeil::tilDto).toList());
            }
        }

        if (prosessinstansService.harVedtakInstans(behandlingID)) {
            throw new FunksjonellException("Det finnes allerede en vedtak-prosess for behandling " + behandling);
        }

        behandlingService.endreStatus(behandling, Behandlingsstatus.IVERKSETTER_VEDTAK);

        if(saksbehandlingRegler.harIkkeYrkesaktivFlyt(behandling.getFagsak().getType(), behandling.getTema())) {
            behandlingsresultat.setFastsattAvLand(Land_iso2.NO);
            prosessinstansService.opprettProsessinstansIverksettIkkeYreksaktiv(behandling);
        } else {
            behandling.getFagsak().setStatus(Saksstatuser.MEDLEMSKAP_AVKLART); // TODO: Egen oppgave for fjerne denne som ikke brukes
            oppdaterBehandlingsresultat(behandlingsresultat, request);
            prosessinstansService.opprettProsessinstansIverksettVedtakTrygdeavtale(behandling, request);
            BrevbestillingDto brevbestillingDto = lagBrevbestilling(behandling, request);
            dokgenService.produserOgDistribuerBrev(behandlingID, brevbestillingDto);
        }

        oppgaveService.ferdigstillOppgaveMedSaksnummer(saksnummer);
    }

    private BrevbestillingDto lagBrevbestilling(Behandling behandling, FattVedtakRequest request) {
        if (request.getBehandlingsresultatTypeKode() == Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL) {
            return lagAvslagMangledeOpplysningerBrevbestilling(request);
        }
        Optional<Produserbaredokumenter> produserbaredokumenter = behandling.getMottatteOpplysninger().getMottatteOpplysningerData().soeknadsland.landkoder.stream()
            .map(Land_iso2::valueOf)
            .findFirst()
            .map(this::utledProduserbartTrygdeavtaleDokument);
        return lagTrygdeavtaleBrevbestilling(request, produserbaredokumenter.get());
    }

    private BrevbestillingDto lagAvslagMangledeOpplysningerBrevbestilling(FattVedtakRequest request) {
        var brevbestillingDto = new BrevbestillingDto();
        brevbestillingDto.setProduserbardokument(Produserbaredokumenter.AVSLAG_MANGLENDE_OPPLYSNINGER);
        brevbestillingDto.setMottaker(Mottakerroller.BRUKER);
        brevbestillingDto.setBestillersId(request.getBestillersId());
        brevbestillingDto.setFritekst(request.getFritekst());
        return brevbestillingDto;
    }

    private BrevbestillingDto lagTrygdeavtaleBrevbestilling(FattVedtakRequest request, Produserbaredokumenter produserbaredokumenter) {
        var brevbestillingDto = new BrevbestillingDto();
        brevbestillingDto.setProduserbardokument(produserbaredokumenter);
        brevbestillingDto.setMottaker(Mottakerroller.BRUKER);
        brevbestillingDto.setKopiMottakere(request.getKopiMottakere());
        brevbestillingDto.setInnledningFritekst(request.getInnledningFritekst());
        brevbestillingDto.setBegrunnelseFritekst(request.getBegrunnelseFritekst());
        brevbestillingDto.setEktefelleFritekst(request.getEktefelleFritekst());
        brevbestillingDto.setBarnFritekst(request.getBarnFritekst());
        brevbestillingDto.setBestillersId(request.getBestillersId());
        brevbestillingDto.setNyVurderingBakgrunn(request.getNyVurderingBakgrunn());
        return brevbestillingDto;
    }

    private Produserbaredokumenter utledProduserbartTrygdeavtaleDokument(Land_iso2 soeknadsland) {
        return switch (soeknadsland) {
            case GB -> Produserbaredokumenter.TRYGDEAVTALE_GB;
            case US -> Produserbaredokumenter.TRYGDEAVTALE_US;
            case CA -> Produserbaredokumenter.TRYGDEAVTALE_CAN;
            case AU -> Produserbaredokumenter.TRYGDEAVTALE_AU;
            default -> throw new TekniskException("Søknadsland er ikke implementert som produsertbart dokument : " + soeknadsland);
        };
    }

    private void oppdaterBehandlingsresultat(Behandlingsresultat behandlingsresultat, FattVedtakRequest request) throws IkkeFunnetException {
        behandlingsresultat.settVedtakMetadata(request.getVedtakstype(), LocalDate.now().plusWeeks(FRIST_KLAGE_UKER));
        behandlingsresultat.setNyVurderingBakgrunn(request.getNyVurderingBakgrunn());
        behandlingsresultat.setBegrunnelseFritekst(request.getBegrunnelseFritekst());
        behandlingsresultat.setInnledningFritekst(request.getInnledningFritekst());
        behandlingsresultat.setFastsattAvLand(Land_iso2.NO);

        behandlingsresultatService.lagre(behandlingsresultat);
    }
}
