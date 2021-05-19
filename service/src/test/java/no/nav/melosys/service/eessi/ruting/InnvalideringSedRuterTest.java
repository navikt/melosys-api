package no.nav.melosys.service.eessi.ruting;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import no.finn.unleash.FakeUnleash;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.eessi.BucInformasjon;
import no.nav.melosys.domain.eessi.SedInformasjon;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.medl.MedlPeriodeService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InnvalideringSedRuterTest {

    @Mock
    private FagsakService fagsakService;
    @Mock
    private ProsessinstansService prosessinstansService;
    @Mock
    private OppgaveService oppgaveService;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private MedlPeriodeService medlPeriodeService;
    @Mock
    private EessiService eessiService;

    private FakeUnleash fakeUnleash = new FakeUnleash();

    private InnvalideringSedRuter innvalideringSedRuter;

    private final long behandlingID = 111;
    private final long arkivsakID = 123321;
    private final Prosessinstans prosessinstans = new Prosessinstans();
    private final MelosysEessiMelding melosysEessiMelding = new MelosysEessiMelding();
    private final String rinaSaksnummer = "1233333";
    private final String sedID = "2414";

    @BeforeEach
    void setup() {
        innvalideringSedRuter = new InnvalideringSedRuter(fagsakService, prosessinstansService, oppgaveService,
            behandlingsresultatService, medlPeriodeService, eessiService, fakeUnleash);

        melosysEessiMelding.setAktoerId("12312412");
        melosysEessiMelding.setRinaSaksnummer("143141");
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);
    }

    @Test
    void gjelderSedTyper_featureTogglePå_collectionMedX008() {
        fakeUnleash.enableAll();
        assertThat(innvalideringSedRuter.gjelderSedTyper()).containsExactly(SedType.X008);
    }

    @Test
    void gjelderSedTyper_featureToggleAv_tomCollection() {
        fakeUnleash.disableAll();
        assertThat(innvalideringSedRuter.gjelderSedTyper()).isEmpty();
    }

    @Test
    void rutSedTilBehandling_arkivsaksIdErNull_opprettJournalFøringsOppgave(){
        innvalideringSedRuter.rutSedTilBehandling(prosessinstans, null);
        verify(oppgaveService).opprettJournalføringsoppgave(melosysEessiMelding.getJournalpostId(), melosysEessiMelding.getAktoerId());

    }

    @Test
    void rutSedTilBehandling_finnesIngenTilhørendeFagsak_opprettesJfrOppgave() {
        innvalideringSedRuter.rutSedTilBehandling(prosessinstans, arkivsakID);
        verify(oppgaveService).opprettJournalføringsoppgave(melosysEessiMelding.getJournalpostId(), melosysEessiMelding.getAktoerId());
    }

    @Test
    void rutSedTilBehandling_tilhørendeFagsakFinnesOgBehandlingErNorgeUtpekt_opprettesJfrProsessOgBehandlingsoppgave() {
        when(fagsakService.finnFagsakFraArkivsakID(arkivsakID)).thenReturn(Optional.of(lagFagsak(Behandlingstema.BESLUTNING_LOVVALG_NORGE, Behandlingsstatus.UNDER_BEHANDLING)));
        innvalideringSedRuter.rutSedTilBehandling(prosessinstans, arkivsakID);

        verify(oppgaveService).opprettEllerGjenbrukBehandlingsoppgave(any(Behandling.class), eq(melosysEessiMelding.getJournalpostId()), eq(melosysEessiMelding.getAktoerId()), isNull());
    }

    @Test
    void rutSedTilBehandling_behandlingErUtlandUtpektOgAvsluttetHarMedlPeriode_oppdaterSaksstatusAnnullertOgOpphørMEDLPeriode(){
        var fagsak = lagFagsak(Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND, Behandlingsstatus.AVSLUTTET);
        fagsak.hentSistAktiveBehandling().getSaksopplysninger().add(lagSedDokument());
        var behandlingsresultat = lagBehandlingsresultat(true);

        when(eessiService.hentTilknyttedeBucer(arkivsakID, List.of())).thenReturn(lagBucInformasjon("AVBRUTT"));
        when(fagsakService.finnFagsakFraArkivsakID(arkivsakID)).thenReturn(Optional.of(fagsak));
        when(behandlingsresultatService.hentBehandlingsresultat(behandlingID)).thenReturn(behandlingsresultat);

        innvalideringSedRuter.rutSedTilBehandling(prosessinstans, arkivsakID);

        verify(fagsakService).oppdaterStatus(fagsak, Saksstatuser.ANNULLERT);
        verify(medlPeriodeService).avvisPeriodeOpphørt(behandlingsresultat.hentValidertLovvalgsperiode().getMedlPeriodeID());
    }

    @Test
    void rutSedTilBehandling_behandlingErUtstasjoneringOgAktiv_oppdaterSaksstatusAnnullert(){
        var fagsak = lagFagsak(Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING, Behandlingsstatus.UNDER_BEHANDLING);
        fagsak.hentSistAktiveBehandling().getSaksopplysninger().add(lagSedDokument());

        when(eessiService.hentTilknyttedeBucer(arkivsakID, List.of())).thenReturn(lagBucInformasjon("AVBRUTT"));
        when(fagsakService.finnFagsakFraArkivsakID(arkivsakID)).thenReturn(Optional.of(fagsak));
        when(behandlingsresultatService.hentBehandlingsresultat(behandlingID)).thenReturn(lagBehandlingsresultat(false));

        innvalideringSedRuter.rutSedTilBehandling(prosessinstans, arkivsakID);

        verify(fagsakService).avsluttFagsakOgBehandling(fagsak, Saksstatuser.ANNULLERT);

    }

    @Test
    void rutSedTilBehandling_behandlingErUnntakNorskTrygvØvrigAktivSedIkkeAnnullert_oppretterBehandlingsoppgave(){
        var fagsak = lagFagsak(Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE, Behandlingsstatus.UNDER_BEHANDLING);
        fagsak.hentSistAktiveBehandling().getSaksopplysninger().add(lagSedDokument());

        when(eessiService.hentTilknyttedeBucer(arkivsakID, List.of())).thenReturn(lagBucInformasjon("ÅPEN"));
        when(fagsakService.finnFagsakFraArkivsakID(arkivsakID)).thenReturn(Optional.of(fagsak));

        innvalideringSedRuter.rutSedTilBehandling(prosessinstans, arkivsakID);
        verify(oppgaveService).opprettEllerGjenbrukBehandlingsoppgave(any(Behandling.class),
            eq(melosysEessiMelding.getJournalpostId()), eq(melosysEessiMelding.getAktoerId()), isNull());
    }



    private Behandlingsresultat lagBehandlingsresultat(boolean medMedlperiode) {
        var behandlingsresultat = new Behandlingsresultat();

        var lovvalgsperiode = new Lovvalgsperiode();
        if (medMedlperiode) {
            lovvalgsperiode.setMedlPeriodeID(123L);
        }

        behandlingsresultat.getLovvalgsperioder().add(lovvalgsperiode);
        return behandlingsresultat;
    }

    private List<BucInformasjon> lagBucInformasjon(String status) {
        return List.of(new BucInformasjon(
            rinaSaksnummer,
            "LA_BUC_04",
            LocalDate.now(),
            Set.of(),
            List.of(new SedInformasjon(rinaSaksnummer, sedID, null, null, null, status, null))
        ));
    }

    private Saksopplysning lagSedDokument() {
        var sedDokument = new SedDokument();
        sedDokument.setRinaSaksnummer(rinaSaksnummer);
        sedDokument.setRinaDokumentID(sedID);

        var saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.SEDOPPL);
        saksopplysning.setDokument(sedDokument);
        return saksopplysning;
    }

    private Behandling lagBehandling(Fagsak fagsak, Behandlingstema behandlingstema, Behandlingsstatus behandlingsstatus) {
        var behandling = new Behandling();
        behandling.setId(behandlingID);
        behandling.setTema(behandlingstema);
        behandling.setEndretDato(Instant.now());
        behandling.setFagsak(fagsak);
        behandling.setStatus(behandlingsstatus);
        return behandling;
    }

    private Fagsak lagFagsak(Behandlingstema behandlingstema, Behandlingsstatus behandlingsstatus) {
        var fagsak = new Fagsak();
        fagsak.setBehandlinger(List.of(lagBehandling(fagsak, behandlingstema, behandlingsstatus)));
        return fagsak;
    }
}
