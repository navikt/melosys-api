package no.nav.melosys.service.eessi;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.eessi.BucInformasjon;
import no.nav.melosys.domain.eessi.SedInformasjon;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.saksflytapi.ProsessinstansService;
import no.nav.melosys.saksflytapi.domain.ProsessDataKey;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.medl.MedlPeriodeService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.sak.FagsakService;
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
class AdminInnvalideringSedRuterTest {

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
    @Mock
    private BehandlingService behandlingService;

    private AdminInnvalideringSedRuter adminInnvalideringSedRuter;

    private final long behandlingID = 111;
    private final long arkivsakID = 123321;
    private final Prosessinstans prosessinstans = new Prosessinstans();
    private final MelosysEessiMelding melosysEessiMelding = new MelosysEessiMelding();
    private final String rinaSaksnummer = "1233333";
    private final String sedID = "2414";

    @BeforeEach
    void setup() {
        adminInnvalideringSedRuter = new AdminInnvalideringSedRuter(fagsakService, prosessinstansService, oppgaveService,
            behandlingsresultatService, medlPeriodeService, eessiService, behandlingService);

        melosysEessiMelding.setAktoerId("12312412");
        melosysEessiMelding.setRinaSaksnummer("143141");
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, melosysEessiMelding);
    }

    @Test
    void gjelderSedTyper_featureTogglePå_collectionMedX008() {
        assertThat(adminInnvalideringSedRuter.gjelderSedTyper()).containsExactly(SedType.X008);
    }

    @Test
    void rutSedTilBehandling_arkivsaksIdErNull_opprettJournalFøringsOppgave() {
        adminInnvalideringSedRuter.rutSedTilBehandling(prosessinstans, null);
        verify(oppgaveService).opprettJournalføringsoppgave(melosysEessiMelding.getJournalpostId(), melosysEessiMelding.getAktoerId());
    }

    @Test
    void rutSedTilBehandling_finnesIngenTilhørendeFagsak_opprettesJfrOppgave() {
        adminInnvalideringSedRuter.rutSedTilBehandling(prosessinstans, arkivsakID);
        verify(oppgaveService).opprettJournalføringsoppgave(melosysEessiMelding.getJournalpostId(), melosysEessiMelding.getAktoerId());
    }

    @Test
    void rutSedTilBehandling_tilhørendeFagsakFinnesOgBehandlingErNorgeUtpektAktiv_behandlingsstausVURDER_DOKUMENT() {
        var fagsak = lagFagsak(Behandlingstema.BESLUTNING_LOVVALG_NORGE, Behandlingsstatus.UNDER_BEHANDLING);
        when(fagsakService.finnFagsakFraArkivsakID(arkivsakID)).thenReturn(Optional.of(fagsak));
        Behandling sistAktiveBehandling = fagsak.hentSistAktivBehandlingIkkeÅrsavregning();


        adminInnvalideringSedRuter.rutSedTilBehandling(prosessinstans, arkivsakID);


        verify(behandlingService).endreStatus(behandlingID, Behandlingsstatus.VURDER_DOKUMENT);
        verify(prosessinstansService).opprettProsessinstansSedJournalføring(sistAktiveBehandling, melosysEessiMelding);
    }

    @Test
    void rutSedTilBehandling_tilhørendeFagsakFinnesOgBehandlingErNorgeUtpektIkkeAktiv_journalføringsOppgaveLages() {
        when(fagsakService.finnFagsakFraArkivsakID(arkivsakID)).thenReturn(Optional.of(lagFagsak(Behandlingstema.BESLUTNING_LOVVALG_NORGE, Behandlingsstatus.AVSLUTTET)));


        adminInnvalideringSedRuter.rutSedTilBehandling(prosessinstans, arkivsakID);


        verify(oppgaveService).opprettJournalføringsoppgave(melosysEessiMelding.getJournalpostId(), melosysEessiMelding.getAktoerId());
    }

    @Test
    void rutSedTilBehandling_behandlingErUtlandUtpektOgAvsluttetHarMedlPeriode_oppdaterSaksstatusAnnullertOgOpphørMEDLPeriode() {
        var fagsak = lagFagsak(Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND, Behandlingsstatus.AVSLUTTET);
        fagsak.hentSistAktivBehandlingIkkeÅrsavregning().getSaksopplysninger().add(lagSedDokument());
        Behandling sistAktiveBehandling = fagsak.hentSistAktivBehandlingIkkeÅrsavregning();

        Behandlingsresultat behandlingsresultat = lagBehandlingsresultat(true);
        behandlingsresultat.setBehandling(sistAktiveBehandling);

        when(eessiService.hentTilknyttedeBucer(arkivsakID, List.of())).thenReturn(lagBucInformasjon("AVBRUTT"));
        when(fagsakService.finnFagsakFraArkivsakID(arkivsakID)).thenReturn(Optional.of(fagsak));
        when(behandlingsresultatService.hentBehandlingsresultat(behandlingID)).thenReturn(behandlingsresultat);

        adminInnvalideringSedRuter.rutSedTilBehandling(prosessinstans, arkivsakID);

        verify(fagsakService).oppdaterStatus(fagsak, Saksstatuser.ANNULLERT);
        verify(medlPeriodeService).avvisPeriodeOpphørt(behandlingsresultat.hentLovvalgsperiode().getMedlPeriodeID());
    }

    @Test
    void rutSedTilBehandling_behandlingErUtstasjoneringOgAktiv_oppdaterSaksstatusAnnullert() {
        var fagsak = lagFagsak(Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING, Behandlingsstatus.UNDER_BEHANDLING);
        fagsak.hentSistAktivBehandlingIkkeÅrsavregning().getSaksopplysninger().add(lagSedDokument());
        Behandling sistAktiveBehandling = fagsak.hentSistAktivBehandlingIkkeÅrsavregning();

        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setBehandling(sistAktiveBehandling);

        Anmodningsperiode periode = new Anmodningsperiode();
        behandlingsresultat.getAnmodningsperioder().add(periode);

        when(eessiService.hentTilknyttedeBucer(arkivsakID, List.of())).thenReturn(lagBucInformasjon("AVBRUTT"));
        when(fagsakService.finnFagsakFraArkivsakID(arkivsakID)).thenReturn(Optional.of(fagsak));
        when(behandlingsresultatService.hentBehandlingsresultat(behandlingID)).thenReturn(behandlingsresultat);

        adminInnvalideringSedRuter.rutSedTilBehandling(prosessinstans, arkivsakID);

        verify(fagsakService).avsluttFagsakOgBehandling(fagsak, Saksstatuser.ANNULLERT);

    }

    @Test
    void rutSedTilBehandling_behandlingErUnntakNorskTrygvØvrigAktivSedIkkeAnnullert_oppretterBehandlingsoppgave() {
        var fagsak = lagFagsak(Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE, Behandlingsstatus.UNDER_BEHANDLING);
        fagsak.hentSistAktivBehandlingIkkeÅrsavregning().getSaksopplysninger().add(lagSedDokument());

        when(eessiService.hentTilknyttedeBucer(arkivsakID, List.of())).thenReturn(lagBucInformasjon("ÅPEN"));
        when(fagsakService.finnFagsakFraArkivsakID(arkivsakID)).thenReturn(Optional.of(fagsak));

        adminInnvalideringSedRuter.rutSedTilBehandling(prosessinstans, arkivsakID);
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
            true,
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
        var fagsak = FagsakTestFactory.lagFagsak();
        var behandling = lagBehandling(fagsak, behandlingstema, behandlingsstatus);
        fagsak.leggTilBehandling(behandling);
        return fagsak;
    }
}
