package no.nav.melosys.service.sak;


import java.util.Collections;
import java.util.List;
import java.util.Set;

import no.finn.unleash.FakeUnleash;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Bostedsland;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.arkiv.DokumentReferanse;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.service.LandvelgerService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.persondata.PersonopplysningerObjectFactory;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.service.SaksbehandlingDataFactory.lagBehandling;
import static no.nav.melosys.service.SaksbehandlingDataFactory.lagFagsak;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VideresendSoknadServiceTest {
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private EessiService eessiService;
    @Mock
    private FagsakService fagsakService;
    @Mock
    private JoarkFasade joarkFasade;
    @Mock
    private LandvelgerService landvelgerService;
    @Mock
    private OppgaveService oppgaveService;
    @Mock
    private PersondataFasade persondataFasade;
    @Mock
    private ProsessinstansService prosessinstansService;

    private VideresendSoknadService videresendSoknadService;

    private final Bostedsland BOSTEDSLAND = new Bostedsland(Landkoder.ES);
    private static final String SAKSNUMMER = "MEL-2222";
    private final Fagsak fagsak = lagFagsak(SAKSNUMMER);
    private final BehandlingsgrunnlagData behandlingsgrunnlagData = new BehandlingsgrunnlagData();
    private final Behandling behandling = lagBehandling(behandlingsgrunnlagData);

    @BeforeEach
    public void setup() {
        final FakeUnleash unleash = new FakeUnleash();
        unleash.enable("melosys.pdl.aktiv");
        videresendSoknadService = new VideresendSoknadService(behandlingsresultatService, eessiService, fagsakService,
            joarkFasade, landvelgerService, oppgaveService, persondataFasade, prosessinstansService, unleash);

        behandling.setFagsak(fagsak);
        fagsak.getBehandlinger().add(behandling);

        when(fagsakService.hentFagsak(SAKSNUMMER)).thenReturn(fagsak);
    }

    @Test
    void henleggOgVideresend_bostedsLandSpaniaErSøknad_prosessinstansBlirOpprettet() {
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        final Set<String> validerteMottakere = Set.of("ES:mottakerID123");
        when(landvelgerService.hentBostedsland(behandling)).thenReturn(BOSTEDSLAND);
        when(eessiService.validerOgAvklarMottakerInstitusjonerForBuc(any(), eq(List.of(Landkoder.ES)), eq(BucType.LA_BUC_03)))
            .thenReturn(validerteMottakere);
        when(persondataFasade.hentPerson(anyString())).thenReturn(PersonopplysningerObjectFactory.lagPersonopplysninger());
        DokumentReferanse dokumentReferanse = new DokumentReferanse("jpID", "dokID");

        videresendSoknadService.videresend(SAKSNUMMER, "", "fritekst", Set.of(dokumentReferanse));

        verify(fagsakService).avsluttFagsakOgBehandling(fagsak, Saksstatuser.VIDERESENDT);
        verify(prosessinstansService).opprettProsessinstansVideresendSoknad(behandling,
            validerteMottakere.iterator().next(), "fritekst", Set.of(dokumentReferanse));
        verify(oppgaveService).ferdigstillOppgaveMedSaksnummer(SAKSNUMMER);
        verify(behandlingsresultatService).oppdaterBehandlingsresultattype(behandling.getId(), Behandlingsresultattyper.HENLEGGELSE);
    }

    @Test
    void henleggOgVideresend_ikkeSøknad_kasterException() {
        when(landvelgerService.hentBostedsland(behandling)).thenReturn(BOSTEDSLAND);
        behandling.setTema(Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> videresendSoknadService.videresend(SAKSNUMMER, "", "", Collections.emptySet()))
            .withMessageContaining("er ikke behandling av en søknad");
    }

    @Test
    void henleggOgVideresend_bostedsLandNorgeErSøknad_kasterException() {
        when(landvelgerService.hentBostedsland(behandling)).thenReturn(new Bostedsland(Landkoder.NO));
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> videresendSoknadService.videresend(SAKSNUMMER, "", "", Collections.emptySet()))
            .withMessageContaining("til Norge");
    }

    @Test
    void henleggOgVideresend_bostedslandIkkeAvklartErSøknad_kasterException() {
        when(landvelgerService.hentBostedsland(behandling)).thenReturn(null);
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> videresendSoknadService.videresend(SAKSNUMMER, "", "", Collections.emptySet()))
            .withMessageContaining("Bostedsland ikke avklart");
    }

    @Test
    void henleggOgVideresend_ingenAdresse_kasterException() {
        when(landvelgerService.hentBostedsland(behandling)).thenReturn(BOSTEDSLAND);
        behandling.setTema(Behandlingstema.ARBEID_FLERE_LAND);
        when(persondataFasade.hentPerson(anyString())).thenReturn(PersonopplysningerObjectFactory.lagPersonopplysningerUtenAdresser());

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> videresendSoknadService.videresend(SAKSNUMMER, "", "", Collections.emptySet()))
            .withMessageContaining("mangler adresse");
    }
}
