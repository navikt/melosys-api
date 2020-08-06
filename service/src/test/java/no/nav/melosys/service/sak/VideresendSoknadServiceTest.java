package no.nav.melosys.service.sak;


import java.util.List;
import java.util.Set;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.Bosted;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.dokument.LandvelgerService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class VideresendSoknadServiceTest {

    @Mock
    private EessiService eessiService;
    @Mock
    private FagsakService fagsakService;
    @Mock
    private OppgaveService oppgaveService;
    @Mock
    private BehandlingService behandlingService;
    @Mock
    private LandvelgerService landvelgerService;
    @Mock
    private ProsessinstansService prosessinstansService;

    private VideresendSoknadService videresendSoknadService;

    private Fagsak fagsak = new Fagsak();
    private Behandling behandling = new Behandling();
    private BehandlingsgrunnlagData behandlingsgrunnlagData = new BehandlingsgrunnlagData();
    private PersonDokument personDokument = new PersonDokument();

    private final String saksnummer = "MEL-2222";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setup() throws IkkeFunnetException {
        videresendSoknadService = new VideresendSoknadService(
            fagsakService, behandlingService, prosessinstansService, landvelgerService, eessiService, oppgaveService
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

        when(behandlingService.hentBehandlingUtenSaksopplysninger(eq(behandling.getId()))).thenReturn(behandling);
        when(fagsakService.hentFagsak(eq(saksnummer))).thenReturn(fagsak);
    }

    @Test
    public void henleggOgVideresend_bostedsLandSpaniaErSøknad_prosessinstansBlirOpprettet() throws MelosysException {
        final Set<String> validerteMottakere = Set.of("ES:mottakerID123");
        when(landvelgerService.hentBostedsland(eq(behandling))).thenReturn(Landkoder.ES);
        when(eessiService.validerOgAvklarMottakerInstitusjonerForBuc(any(), eq(List.of(Landkoder.ES)), eq(BucType.LA_BUC_03)))
            .thenReturn(validerteMottakere);

        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);

        videresendSoknadService.videresend(saksnummer, "", "fritekst");

        verify(fagsakService).oppdaterStatus(fagsak, Saksstatuser.VIDERESENDT);
        verify(prosessinstansService).opprettProsessinstansVideresendSoknad(eq(behandling),
            eq(validerteMottakere.iterator().next()), eq("fritekst"));
        verify(oppgaveService).ferdigstillOppgaveMedSaksnummer(eq(saksnummer));
    }

    @Test
    public void henleggOgVideresend_ikkeSøknad_kasterException() throws MelosysException {
        when(landvelgerService.hentBostedsland(eq(behandling))).thenReturn(Landkoder.ES);
        behandling.setTema(Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING);

        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage("er ikke behandling av en søknad");

        videresendSoknadService.videresend(saksnummer, "", "");
    }

    @Test
    public void henleggOgVideresend_bostedsLandNorgeErSøknad_kasterException() throws MelosysException {
        when(landvelgerService.hentBostedsland(eq(behandling))).thenReturn(Landkoder.NO);
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);

        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage("til Norge");

        videresendSoknadService.videresend(saksnummer, "", "");
    }

    @Test
    public void henleggOgVideresend_bostedsLanIkkeAvklartErSøknad_kasterException() throws MelosysException {
        when(landvelgerService.hentBostedsland(eq(behandling))).thenReturn(null);
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);

        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage("Bostedsland ikke avklart");

        videresendSoknadService.videresend(saksnummer, "", "");
    }

    @Test
    public void henleggOgVideresend_ikkeLagretBostedsadresseISøknadEllerTPS_kasterException() throws MelosysException {
        when(landvelgerService.hentBostedsland(eq(behandling))).thenReturn(Landkoder.SE);
        behandling.setTema(Behandlingstema.ARBEID_FLERE_LAND);
        behandlingsgrunnlagData.bosted = new Bosted();

        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage("mangler bostedsadresse");

        videresendSoknadService.videresend(saksnummer, "", "");
    }

}