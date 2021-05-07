package no.nav.melosys.service.unntak;

import java.util.*;
import java.util.stream.Collectors;

import no.nav.melosys.domain.AnmodningsperiodeSvar;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.arkiv.DokumentReferanse;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.exception.ValideringException;
import no.nav.melosys.service.LandvelgerService;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.kontroll.unntak.AnmodningUnntakKontrollService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import no.nav.melosys.service.validering.Kontrollfeil;
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

    public AnmodningUnntakService(BehandlingService behandlingService,
                                  BehandlingsresultatService behandlingsresultatService, OppgaveService oppgaveService,
                                  ProsessinstansService prosessinstansService,
                                  AnmodningsperiodeService anmodningsperiodeService,
                                  LovvalgsperiodeService lovvalgsperiodeService,
                                  LandvelgerService landvelgerService,
                                  EessiService eessiService,
                                  AnmodningUnntakKontrollService anmodningUnntakKontrollService) {
        this.behandlingService = behandlingService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.oppgaveService = oppgaveService;
        this.prosessinstansService = prosessinstansService;
        this.anmodningsperiodeService = anmodningsperiodeService;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
        this.landvelgerService = landvelgerService;
        this.eessiService = eessiService;
        this.anmodningUnntakKontrollService = anmodningUnntakKontrollService;
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void anmodningOmUnntak(long behandlingID, String mottakerinstitusjon,
                                  Set<DokumentReferanse> vedleggReferanser, String ytterligereInformasjonSed) throws MelosysException, ValideringException {
        Set<String> mottakerinstitusjoner = validerMottakerInstitusjon(behandlingID, mottakerinstitusjon);

        Behandling behandling = behandlingService.hentBehandling(behandlingID);
        log.info("Anmodning om unntak for sak: {} behandling: {}", behandling.getFagsak().getSaksnummer(), behandlingID);

        kontrollerAnmodningOmUnntak(behandlingID);
        behandlingsresultatService.oppdaterBehandlingsresultattype(behandlingID, Behandlingsresultattyper.ANMODNING_OM_UNNTAK);

        prosessinstansService.opprettProsessinstansAnmodningOmUnntak(behandling, mottakerinstitusjoner,
            vedleggReferanser, ytterligereInformasjonSed);
        oppgaveService.leggTilbakeOppgaveMedSaksnummer(behandling.getFagsak().getSaksnummer());
    }

    private Set<String> validerMottakerInstitusjon(long behandlingID, String mottakerinstitusjon) throws MelosysException {
        Landkoder landkode = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID).stream().findFirst()
            .orElseThrow(() -> new FunksjonellException("Finner ikke utenlandsk myndighet for behandling " + behandlingID));

        return eessiService.validerOgAvklarMottakerInstitusjonerForBuc(
            StringUtils.isEmpty(mottakerinstitusjon) ? Collections.emptySet() : Set.of(mottakerinstitusjon),
            List.of(landkode), BucType.LA_BUC_01);
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void anmodningOmUnntakSvar(long behandlingID) throws FunksjonellException, TekniskException {
        Behandling behandling = behandlingService.hentBehandlingUtenSaksopplysninger(behandlingID);
        validerBehandlingstemaUnntak(behandling);
        validerSvar(behandling);
        prosessinstansService.opprettProsessinstansAnmodningOmUnntakMottakSvar(behandling);
        oppgaveService.ferdigstillOppgaveMedSaksnummer(behandling.getFagsak().getSaksnummer());
    }

    private static void validerBehandlingstemaUnntak(Behandling behandling) throws FunksjonellException {
        if (behandling.getTema() != Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL) {
            throw new FunksjonellException("Behandling er ikke av tema ANMODNING_OM_UNNTAK_HOVEDREGEL");
        } else if (behandling.getStatus() == Behandlingsstatus.AVSLUTTET) {
            throw new FunksjonellException("Behandlingen er avsluttet");
        }
    }

    private void validerSvar(Behandling behandling) throws FunksjonellException {
        Optional<AnmodningsperiodeSvar> anmodningsperiodeSvar = anmodningsperiodeService
            .hentAnmodningsperiodeSvarForBehandling(behandling.getId()).stream().findFirst();
        if (anmodningsperiodeSvar.isEmpty()) {
            throw new FunksjonellException("Finner ingen AnmodningsperiodeSvar for behandling " + behandling.getId());
        }

        if (lovvalgsperiodeService.hentLovvalgsperioder(behandling.getId()).isEmpty()) {
            throw new FunksjonellException("Finner ingen Lovvalgsperioder for behandling " + behandling.getId());
        }

        if (anmodningsperiodeSvar.get().erAvslag()) {
            validerFritekstLengde(anmodningsperiodeSvar.get());
        }
    }

    private void validerFritekstLengde(AnmodningsperiodeSvar anmodningsperiodeSvar) throws FunksjonellException {
        if (anmodningsperiodeSvar.getBegrunnelseFritekst() != null && anmodningsperiodeSvar.getBegrunnelseFritekst().length() > 255) {
            throw new FunksjonellException("Kan ikke ha fritekst lengre enn 255 for avslag på anmodning om unntak");
        }
    }

    private void kontrollerAnmodningOmUnntak(long behandlingID) throws ValideringException {
        Collection<Kontrollfeil> feilValideringer = anmodningUnntakKontrollService.utførKontroller(behandlingID);
        if (!feilValideringer.isEmpty()) {
            throw new ValideringException("Feil i validering. Kan ikke sende anmodning om unntak.",
                feilValideringer.stream().map(Kontrollfeil::tilDto).collect(Collectors.toList()));
        }
    }
}
