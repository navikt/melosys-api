package no.nav.melosys.service.vedtak;

import java.time.LocalDate;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.DokgenService;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import static no.nav.melosys.service.vedtak.VedtaksfattingFasade.FRIST_KLAGE_UKER;

@Service
public class FtrlVedtakService {
    private static final Logger log = LoggerFactory.getLogger(FtrlVedtakService.class);

    private final BehandlingsresultatService behandlingsresultatService;
    private final BehandlingService behandlingService;
    private final ProsessinstansService prosessinstansService;
    private final OppgaveService oppgaveService;
    private final DokgenService dokgenService;

    public FtrlVedtakService(BehandlingsresultatService behandlingsresultatService,
                             BehandlingService behandlingService,
                             ProsessinstansService prosessinstansService,
                             OppgaveService oppgaveService,
                             DokgenService dokgenService) {
        this.behandlingsresultatService = behandlingsresultatService;
        this.behandlingService = behandlingService;
        this.prosessinstansService = prosessinstansService;
        this.oppgaveService = oppgaveService;
        this.dokgenService = dokgenService;
    }

    public void fattVedtak(Behandling behandling, FattVedtakRequest request) {
        long behandlingID = behandling.getId();

        log.info("Fatter vedtak for (FTRL) sak: {} behandling: {}", behandling.getFagsak().getSaksnummer(), behandlingID);

        oppdaterBehandlingsresultat(behandlingID, request);

        if (prosessinstansService.harVedtakInstans(behandlingID)) {
            throw new FunksjonellException("Det finnes allerede en vedtak-prosess for behandling " + behandling);
        }

        behandling.getFagsak().setStatus(Saksstatuser.MEDLEMSKAP_AVKLART);
        behandlingService.endreStatus(behandling, Behandlingsstatus.IVERKSETTER_VEDTAK);

        prosessinstansService.opprettProsessinstansIverksettVedtakFTRL(behandling, request);
        dokgenService.produserOgDistribuerBrev(behandlingID, lagBrevbestilling(request));
        oppgaveService.ferdigstillOppgaveMedSaksnummer(behandling.getFagsak().getSaksnummer());
    }

    private BrevbestillingDto lagBrevbestilling(FattVedtakRequest request) {
        if (request.getBehandlingsresultatTypeKode() == Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL) {
            return lagAvslagMangledeOpplysningerBrevbestilling(request);
        }
        return lagInnvilgelseFolketrygdloven(request);
    }

    private BrevbestillingDto lagAvslagMangledeOpplysningerBrevbestilling(FattVedtakRequest request) {
        var brevbestillingDto = new BrevbestillingDto();
        brevbestillingDto.setProduserbardokument(Produserbaredokumenter.AVSLAG_MANGLENDE_OPPLYSNINGER);
        brevbestillingDto.setMottaker(Aktoersroller.BRUKER);
        brevbestillingDto.setBestillersId(request.getBestillersId());
        brevbestillingDto.setFritekst(request.getFritekst());
        return brevbestillingDto;
    }

    private BrevbestillingDto lagInnvilgelseFolketrygdloven(FattVedtakRequest request) {
        var brevbestillingDto = new BrevbestillingDto();
        brevbestillingDto.setProduserbardokument(Produserbaredokumenter.INNVILGELSE_FOLKETRYGDLOVEN_2_8);
        brevbestillingDto.setMottaker(Aktoersroller.BRUKER);
        brevbestillingDto.setKopiMottakere(request.getKopiMottakere());
        brevbestillingDto.setInnledningFritekst(request.getInnledningFritekst());
        brevbestillingDto.setBegrunnelseFritekst(request.getBegrunnelseFritekst());
        brevbestillingDto.setEktefelleFritekst(request.getEktefelleFritekst());
        brevbestillingDto.setBarnFritekst(request.getBarnFritekst());
        brevbestillingDto.setBestillersId(request.getBestillersId());
        return brevbestillingDto;
    }

    private void oppdaterBehandlingsresultat(long behandlingID, FattVedtakRequest request) throws IkkeFunnetException {
        var behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);
        behandlingsresultat.setType(request.getBehandlingsresultatTypeKode());
        behandlingsresultat.settVedtakMetadata(request.getVedtakstype(), request.getNyVurderingBakgrunn(), LocalDate.now().plusWeeks(FRIST_KLAGE_UKER));
        behandlingsresultat.setBegrunnelseFritekst(request.getBegrunnelseFritekst());
        behandlingsresultat.setInnledningFritekst(request.getInnledningFritekst());
        behandlingsresultat.setFastsattAvLand(Landkoder.NO);

        behandlingsresultatService.lagre(behandlingsresultat);
    }
}
