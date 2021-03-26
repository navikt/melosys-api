package no.nav.melosys.service.sak;


import java.util.Collections;
import java.util.List;
import java.util.Set;

import no.nav.melosys.domain.behandling.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.arkiv.DokumentReferanse;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.behandlingsgrunnlag.data.Bosted;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.service.LandvelgerService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VideresendSoknadServiceTest {
    @Mock
    private EessiService eessiService;
    @Mock
    private FagsakService fagsakService;
    @Mock
    private OppgaveService oppgaveService;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private LandvelgerService landvelgerService;
    @Mock
    private ProsessinstansService prosessinstansService;

    private VideresendSoknadService videresendSoknadService;

    private final Fagsak fagsak = new Fagsak();
    private final Behandling behandling = new Behandling();
    private final BehandlingsgrunnlagData behandlingsgrunnlagData = new BehandlingsgrunnlagData();
    private final PersonDokument personDokument = new PersonDokument();

    private final String saksnummer = "MEL-2222";

    @BeforeEach
    public void setup() throws IkkeFunnetException {
        videresendSoknadService = new VideresendSoknadService(
            fagsakService, behandlingsresultatService, prosessinstansService, landvelgerService, eessiService, oppgaveService
        );

        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        behandling.setFagsak(fagsak);
        behandling.setId(11L);
        fagsak.getBehandlinger().add(behandling);
        fagsak.setStatus(Saksstatuser.OPPRETTET);
        fagsak.setSaksnummer(saksnummer);

        behandling.setBehandlingsgrunnlag(new Behandlingsgrunnlag());
        behandling.getBehandlingsgrunnlag().setBehandlingsgrunnlagdata(behandlingsgrunnlagData);

        behandlingsgrunnlagData.bosted.oppgittAdresse.gatenavn = "gate 123";

        Saksopplysning personOpplysning = new Saksopplysning();
        personOpplysning.setType(SaksopplysningType.PERSOPL);
        personOpplysning.setDokument(personDokument);
        behandling.getSaksopplysninger().add(personOpplysning);

        when(fagsakService.hentFagsak(saksnummer)).thenReturn(fagsak);
    }

    @Test
    void henleggOgVideresend_bostedsLandSpaniaErSøknad_prosessinstansBlirOpprettet() throws MelosysException {
        final Set<String> validerteMottakere = Set.of("ES:mottakerID123");
        when(landvelgerService.hentBostedsland(behandling)).thenReturn(Landkoder.ES);
        when(eessiService.validerOgAvklarMottakerInstitusjonerForBuc(any(), eq(List.of(Landkoder.ES)), eq(BucType.LA_BUC_03)))
            .thenReturn(validerteMottakere);
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        DokumentReferanse dokumentReferanse = new DokumentReferanse("jpID", "dokID");

        videresendSoknadService.videresend(saksnummer, "", "fritekst", Set.of(dokumentReferanse));

        verify(fagsakService).avsluttFagsakOgBehandling(fagsak, Saksstatuser.VIDERESENDT);
        verify(prosessinstansService).opprettProsessinstansVideresendSoknad(behandling,
            validerteMottakere.iterator().next(), "fritekst", Set.of(dokumentReferanse));
        verify(oppgaveService).ferdigstillOppgaveMedSaksnummer(saksnummer);
        verify(behandlingsresultatService).oppdaterBehandlingsresultattype(behandling.getId(), Behandlingsresultattyper.HENLEGGELSE);
    }

    @Test
    void henleggOgVideresend_ikkeSøknad_kasterException() {
        when(landvelgerService.hentBostedsland(behandling)).thenReturn(Landkoder.ES);
        behandling.setTema(Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> videresendSoknadService.videresend(saksnummer, "", "", Collections.emptySet()))
            .withMessageContaining("er ikke behandling av en søknad");
    }

    @Test
    void henleggOgVideresend_bostedsLandNorgeErSøknad_kasterException() {
        when(landvelgerService.hentBostedsland(behandling)).thenReturn(Landkoder.NO);
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> videresendSoknadService.videresend(saksnummer, "", "", Collections.emptySet()))
            .withMessageContaining("til Norge");
    }

    @Test
    void henleggOgVideresend_bostedsLanIkkeAvklartErSøknad_kasterException() {
        when(landvelgerService.hentBostedsland(behandling)).thenReturn(null);
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> videresendSoknadService.videresend(saksnummer, "", "", Collections.emptySet()))
            .withMessageContaining("Bostedsland ikke avklart");
    }

    @Test
    void henleggOgVideresend_ikkeLagretBostedsadresseISøknadEllerTPS_kasterException() {
        when(landvelgerService.hentBostedsland(behandling)).thenReturn(Landkoder.SE);
        behandling.setTema(Behandlingstema.ARBEID_FLERE_LAND);
        behandlingsgrunnlagData.bosted = new Bosted();

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> videresendSoknadService.videresend(saksnummer, "", "", Collections.emptySet()))
            .withMessageContaining("mangler bostedsadresse");
    }

}