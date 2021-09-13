package no.nav.melosys.service.sak;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import no.finn.unleash.Unleash;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Bostedsland;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.arkiv.DokumentReferanse;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.joark.HentJournalposterTilknyttetSakRequest;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.service.LandvelgerService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.kontroll.PersonKontroller;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VideresendSoknadService {
    private static final Logger log = LoggerFactory.getLogger(VideresendSoknadService.class);

    private final BehandlingsresultatService behandlingsresultatService;
    private final EessiService eessiService;
    private final FagsakService fagsakService;
    private final JoarkFasade joarkFasade;
    private final LandvelgerService landvelgerService;
    private final OppgaveService oppgaveService;
    private final PersondataFasade persondataFasade;
    private final ProsessinstansService prosessinstansService;
    private final Unleash unleash;

    public VideresendSoknadService(BehandlingsresultatService behandlingsresultatService, EessiService eessiService,
                                   FagsakService fagsakService, JoarkFasade joarkFasade,
                                   LandvelgerService landvelgerService, OppgaveService oppgaveService,
                                   PersondataFasade persondataFasade, ProsessinstansService prosessinstansService,
                                   Unleash unleash) {
        this.behandlingsresultatService = behandlingsresultatService;
        this.eessiService = eessiService;
        this.fagsakService = fagsakService;
        this.joarkFasade = joarkFasade;
        this.landvelgerService = landvelgerService;
        this.oppgaveService = oppgaveService;
        this.persondataFasade = persondataFasade;
        this.prosessinstansService = prosessinstansService;
        this.unleash = unleash;
    }

    @Transactional
    public void videresend(String saksnummer,
                           String mottakerinstitusjon,
                           String fritekst,
                           Set<DokumentReferanse> vedleggReferanser) {
        final Fagsak fagsak = fagsakService.hentFagsak(saksnummer);
        final Behandling behandling = fagsak.hentAktivBehandling();
        log.info("Videresender søknad for sak: {} behandling: {}", behandling.getFagsak().getSaksnummer(), behandling.getId());

        final Bostedsland bostedsland = landvelgerService.hentBostedsland(behandling);
        validerBehandlingOgBosted(behandling, bostedsland);
        joarkFasade.validerDokumenterTilhørerSakOgHarTilgang(new HentJournalposterTilknyttetSakRequest(fagsak.getGsakSaksnummer(), saksnummer), vedleggReferanser);

        fagsakService.avsluttFagsakOgBehandling(fagsak, Saksstatuser.VIDERESENDT);
        behandlingsresultatService.oppdaterBehandlingsresultattype(behandling.getId(), Behandlingsresultattyper.HENLEGGELSE);

        final Set<String> avklarteEessiMottakere = eessiService.validerOgAvklarMottakerInstitusjonerForBuc(
            mottakerinstitusjon != null ? Set.of(mottakerinstitusjon) : Collections.emptySet(),
            List.of(bostedsland.getLandkodeobjekt()),
            BucType.LA_BUC_03
        );

        prosessinstansService.opprettProsessinstansVideresendSoknad(behandling,
            avklarteEessiMottakere.stream().findFirst().orElse(null),
            fritekst,
            vedleggReferanser
        );
        oppgaveService.ferdigstillOppgaveMedSaksnummer(behandling.getFagsak().getSaksnummer());
    }

    private void validerBehandlingOgBosted(Behandling behandling, Bostedsland bostedsland) {
        if (!behandling.erBehandlingAvSøknad()) {
            throw new FunksjonellException("Behandling " + behandling.getId() + " er ikke behandling av en søknad!");
        }
        if (bostedsland == null) {
            throw new FunksjonellException("Bostedsland ikke avklart for behandling " + behandling.getId());
        }
        if (bostedsland.getLandkodeobjekt() == Landkoder.NO) {
            throw new FunksjonellException("Kan ikke videresende søknad tilknyttet behandling " + behandling.getId() + " til Norge");
        }
        if (!PersonKontroller.harRegistrertBostedsadresse(hentPersondata(behandling), behandling.getBehandlingsgrunnlag().getBehandlingsgrunnlagdata())) {
            throw new FunksjonellException("Behandlingen mangler bostedsadresse!");
        }
    }

    private Persondata hentPersondata(Behandling behandling) {
        if (unleash.isEnabled("melosys.kontroller.pdl")) {
            return persondataFasade.hentPerson(behandling.getFagsak().hentAktørID());
        }
        return behandling.hentPersonDokument();
    }
}
