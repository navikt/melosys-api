package no.nav.melosys.service.sak;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Bostedsland;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.arkiv.DokumentReferanse;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.kodeverk.Land_iso2;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.joark.HentJournalposterTilknyttetSakRequest;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.saksflytapi.ProsessinstansService;
import no.nav.melosys.service.LandvelgerService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.kontroll.regler.PersonRegler;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.persondata.PersondataFasade;
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

    public VideresendSoknadService(EessiService eessiService, FagsakService fagsakService, BehandlingsresultatService behandlingsresultatService,
                                   JoarkFasade joarkFasade, LandvelgerService landvelgerService, OppgaveService oppgaveService,
                                   PersondataFasade persondataFasade, ProsessinstansService prosessinstansService) {
        this.behandlingsresultatService = behandlingsresultatService;
        this.eessiService = eessiService;
        this.fagsakService = fagsakService;
        this.joarkFasade = joarkFasade;
        this.landvelgerService = landvelgerService;
        this.oppgaveService = oppgaveService;
        this.persondataFasade = persondataFasade;
        this.prosessinstansService = prosessinstansService;
    }

    @Transactional
    public void videresend(String saksnummer,
                           String mottakerinstitusjon,
                           String fritekst,
                           Set<DokumentReferanse> vedleggReferanser) {
        final Fagsak fagsak = fagsakService.hentFagsak(saksnummer);
        final Behandling behandling = fagsak.finnAktivBehandlingIkkeÅrsavregning();
        log.info("Videresender søknad for sak: {} behandling: {}", behandling.getFagsak().getSaksnummer(), behandling.getId());

        final Bostedsland bostedsland = landvelgerService.hentBostedsland(behandling);
        valider(behandling, bostedsland);
        validerDokumenterTilhørerSakOgTilgang(vedleggReferanser, fagsak);

        fagsakService.avsluttFagsakOgBehandling(fagsak, Saksstatuser.VIDERESENDT);
        behandlingsresultatService.oppdaterBehandlingsresultattype(behandling.getId(), Behandlingsresultattyper.HENLEGGELSE);

        final Set<String> avklarteEessiMottakere = eessiService.validerOgAvklarMottakerInstitusjonerForBuc(
            mottakerinstitusjon != null ? Set.of(mottakerinstitusjon) : Collections.emptySet(),
            List.of(Land_iso2.valueOf(bostedsland.landkode())),
            BucType.LA_BUC_03
        );

        prosessinstansService.opprettProsessinstansVideresendSoknad(behandling,
            avklarteEessiMottakere.stream().findFirst().orElse(null),
            fritekst,
            vedleggReferanser
        );
        oppgaveService.ferdigstillOppgaveMedBehandlingID(behandling.getId());
    }

    private void valider(Behandling behandling, Bostedsland bostedsland) {
        validerBehandlingstemaErArbeidFlereLand(behandling);
        validerBostedsland(behandling, bostedsland);
        validerAdresse(behandling);
    }

    private void validerDokumenterTilhørerSakOgTilgang(Set<DokumentReferanse> vedleggReferanser, Fagsak fagsak) {
        joarkFasade.validerDokumenterTilhørerSakOgHarTilgang(
            new HentJournalposterTilknyttetSakRequest(fagsak.getGsakSaksnummer(), fagsak.getSaksnummer()), vedleggReferanser
        );
    }

    private void validerBehandlingstemaErArbeidFlereLand(Behandling behandling) {
        if (!Behandlingstema.ARBEID_FLERE_LAND.equals(behandling.getTema())) {
            throw new FunksjonellException("Behandling " + behandling.getId() + " har ikke behandlingstema 'ARBEID_FLERE_LAND' og kan ikke videresendes");
        }
    }

    private void validerBostedsland(Behandling behandling, Bostedsland bostedsland) {
        if (bostedsland == null) {
            throw new FunksjonellException("Bostedsland ikke avklart for behandling " + behandling.getId());
        }
        if (bostedsland.getLandkodeobjekt() == Landkoder.NO) {
            throw new FunksjonellException("Kan ikke videresende søknad tilknyttet behandling " + behandling.getId() + " til Norge");
        }
    }

    private void validerAdresse(Behandling behandling) {
        if (!PersonRegler.harRegistrertAdresse(hentPersondata(behandling), behandling.getMottatteOpplysninger().getMottatteOpplysningerData())) {
            throw new FunksjonellException("Behandlingen mangler adresse!");
        }
    }

    private Persondata hentPersondata(Behandling behandling) {
        return persondataFasade.hentPerson(behandling.getFagsak().hentBrukersAktørID());
    }
}
