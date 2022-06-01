package no.nav.melosys.service.unntak;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import no.nav.melosys.domain.AnmodningsperiodeSvar;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.arkiv.DokumentReferanse;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Medlemskapstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.ValideringException;
import no.nav.melosys.integrasjon.joark.HentJournalposterTilknyttetSakRequest;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.service.LandvelgerService;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import no.nav.melosys.service.kontroll.feature.unntak.AnmodningUnntakKontrollService;
import no.nav.melosys.service.validering.Kontrollfeil;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnmodningUnntakService {
    private static final Logger log = LoggerFactory.getLogger(AnmodningUnntakService.class);

    private final BehandlingService behandlingService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final OppgaveService oppgaveService;
    private final ProsessinstansService prosessinstansService;
    private final AnmodningsperiodeService anmodningsperiodeService;
    private final LovvalgsperiodeService lovvalgsperiodeService;
    private final LandvelgerService landvelgerService;
    private final EessiService eessiService;
    private final AnmodningUnntakKontrollService anmodningUnntakKontrollService;
    private final JoarkFasade joarkFasade;

    public AnmodningUnntakService(BehandlingService behandlingService,
                                  BehandlingsresultatService behandlingsresultatService, OppgaveService oppgaveService,
                                  ProsessinstansService prosessinstansService,
                                  AnmodningsperiodeService anmodningsperiodeService,
                                  LovvalgsperiodeService lovvalgsperiodeService,
                                  LandvelgerService landvelgerService,
                                  EessiService eessiService,
                                  AnmodningUnntakKontrollService anmodningUnntakKontrollService, JoarkFasade joarkFasade) {
        this.behandlingService = behandlingService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.oppgaveService = oppgaveService;
        this.prosessinstansService = prosessinstansService;
        this.anmodningsperiodeService = anmodningsperiodeService;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
        this.landvelgerService = landvelgerService;
        this.eessiService = eessiService;
        this.anmodningUnntakKontrollService = anmodningUnntakKontrollService;
        this.joarkFasade = joarkFasade;
    }

    @Transactional
    public void anmodningOmUnntak(long behandlingID, String mottakerinstitusjon,
                                  Set<DokumentReferanse> vedleggReferanser, String ytterligereInformasjonSed) throws ValideringException {
        Set<String> mottakerinstitusjoner = validerMottakerInstitusjon(behandlingID, mottakerinstitusjon);

        Behandling behandling = behandlingService.hentBehandlingMedSaksopplysninger(behandlingID);
        Fagsak fagsak = behandling.getFagsak();
        log.info("Anmodning om unntak for sak: {} behandling: {}", behandling.getFagsak().getSaksnummer(), behandlingID);

        kontrollerAnmodningOmUnntak(behandlingID);
        joarkFasade.validerDokumenterTilhørerSakOgHarTilgang(new HentJournalposterTilknyttetSakRequest(fagsak.getGsakSaksnummer(), fagsak.getSaksnummer()), vedleggReferanser);
        behandlingsresultatService.oppdaterBehandlingsresultattype(behandlingID, Behandlingsresultattyper.ANMODNING_OM_UNNTAK);

        anmodningsperiodeService.oppdaterAnmodetAvForBehandling(behandlingID, SubjectHandler.getInstance().getUserID());
        prosessinstansService.opprettProsessinstansAnmodningOmUnntak(behandling, mottakerinstitusjoner,
            vedleggReferanser, ytterligereInformasjonSed);
        oppgaveService.leggTilbakeOppgaveMedSaksnummer(behandling.getFagsak().getSaksnummer());
    }

    private Set<String> validerMottakerInstitusjon(long behandlingID, String mottakerinstitusjon) {
        Landkoder landkode = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID).stream().findFirst()
            .orElseThrow(() -> new FunksjonellException("Finner ikke utenlandsk myndighet for behandling " + behandlingID));

        return eessiService.validerOgAvklarMottakerInstitusjonerForBuc(
            StringUtils.isEmpty(mottakerinstitusjon) ? Collections.emptySet() : Set.of(mottakerinstitusjon),
            List.of(landkode), BucType.LA_BUC_01);
    }

    @Transactional
    public void anmodningOmUnntakSvar(long behandlingID, String ytterligereInfo) {
        Behandling behandling = behandlingService.hentBehandling(behandlingID);
        validerBehandlingstemaUnntak(behandling);
        validerBehandlingsstatus(behandling);

        AnmodningsperiodeSvar anmodningsperiodeSvar =
            anmodningsperiodeService.hentAnmodningsperiodeSvarForBehandling(behandlingID);
        validerSvar(anmodningsperiodeSvar);

        Lovvalgsperiode lovvalgsperiode = Lovvalgsperiode.av(anmodningsperiodeSvar, Medlemskapstyper.PLIKTIG);
        lovvalgsperiodeService.lagreLovvalgsperioder(behandlingID, Collections.singleton(lovvalgsperiode));

        prosessinstansService.opprettProsessinstansAnmodningOmUnntakMottakSvar(behandling, ytterligereInfo);
        oppgaveService.ferdigstillOppgaveMedSaksnummer(behandling.getFagsak().getSaksnummer());
    }

    private static void validerBehandlingstemaUnntak(Behandling behandling) {
        if (behandling.getTema() != Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL) {
            throw new FunksjonellException("Behandling er ikke av tema ANMODNING_OM_UNNTAK_HOVEDREGEL");
        }
    }

    private static void validerBehandlingsstatus(Behandling behandling) {
        if (behandling.getStatus() == Behandlingsstatus.AVSLUTTET) {
            throw new FunksjonellException("Behandlingen er avsluttet");
        }
    }

    private void validerSvar(AnmodningsperiodeSvar anmodningsperiodeSvar) {
        if (anmodningsperiodeSvar.erAvslag()) {
            validerFritekstLengde(anmodningsperiodeSvar);
        }
    }

    private void validerFritekstLengde(AnmodningsperiodeSvar anmodningsperiodeSvar) {
        if (anmodningsperiodeSvar.getBegrunnelseFritekst() != null && anmodningsperiodeSvar.getBegrunnelseFritekst().length() > 255) {
            throw new FunksjonellException("Kan ikke ha fritekst lengre enn 255 for avslag på anmodning om unntak");
        }
    }

    private void kontrollerAnmodningOmUnntak(long behandlingID) throws ValideringException {
        Collection<Kontrollfeil> feilValideringer = anmodningUnntakKontrollService.utførKontroller(behandlingID);
        if (!feilValideringer.isEmpty()) {
            throw new ValideringException("Feil i validering. Kan ikke sende anmodning om unntak.",
                feilValideringer.stream().map(Kontrollfeil::tilDto).toList());
        }
    }
}
