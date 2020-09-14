package no.nav.melosys.service.altinn;

import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Kontaktopplysning;
import no.nav.melosys.domain.kodeverk.Representerer;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.altinn.SoknadMottakConsumer;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.sak.OpprettSakRequest;
import no.nav.melosys.soknad_altinn.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AltinnSoeknadServiceTest {
    @Mock
    private SoknadMottakConsumer soknadMottakConsumer;
    @Mock
    private FagsakService fagsakService;
    @Mock
    private BehandlingsgrunnlagService behandlingsgrunnlagService;
    @Mock
    private TpsFasade tpsFasade;

    private AltinnSoeknadService altinnSoeknadService;

    private final String soknadID = "13423";
    private final String aktørID = "123321123";

    @Captor
    private ArgumentCaptor<OpprettSakRequest> captor;

    @Before
    public void setup() {
        altinnSoeknadService = new AltinnSoeknadService(soknadMottakConsumer, fagsakService, behandlingsgrunnlagService, tpsFasade);
    }

    @Test
    public void opprettFagsakOgBehandlingFraAltinnSøknad_soeknadEksisterer_verifiserFagsakBehandlingOgBehandlinggrunnlagOpprettet()
        throws FunksjonellException, TekniskException {
        final Fagsak fagsak = lagFagsak();
        final MedlemskapArbeidEOSM søknad = lagMedlemskapArbeidEOSM();

        when(soknadMottakConsumer.hentSøknad(eq(soknadID))).thenReturn(søknad);
        when(fagsakService.nyFagsakOgBehandling(captor.capture())).thenReturn(fagsak);
        when(tpsFasade.hentAktørIdForIdent(anyString())).thenReturn(aktørID);

        assertThat(altinnSoeknadService.opprettFagsakOgBehandlingFraAltinnSøknad(soknadID)).isEqualTo(fagsak.hentAktivBehandling());

        OpprettSakRequest req = captor.getValue();
        assertThat(req.getBehandlingstema()).isEqualTo(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        assertThat(req.getBehandlingstype()).isEqualTo(Behandlingstyper.SOEKNAD);
        assertThat(req.getArbeidsgiver()).isEqualTo(søknad.getInnhold().getArbeidsgiver().getVirksomhetsnummer());
        assertThat(req.getAktørID()).isEqualTo(aktørID);
    }

    @Test
    public void opprettFagsakOgBehandlingFraAltinnSøknad_soeknadEksistererArbeidsgiverOffentlig_verifiserBehandlingstemaArbeidsEttLandØvrig()
        throws FunksjonellException, TekniskException {
        final Fagsak fagsak = lagFagsak();
        final MedlemskapArbeidEOSM søknad = lagMedlemskapArbeidEOSM();

        søknad.getInnhold().getArbeidsgiver().setOffentligVirksomhet(Boolean.TRUE);

        when(soknadMottakConsumer.hentSøknad(eq(soknadID))).thenReturn(søknad);
        when(fagsakService.nyFagsakOgBehandling(captor.capture())).thenReturn(fagsak);
        when(tpsFasade.hentAktørIdForIdent(anyString())).thenReturn(aktørID);

        assertThat(altinnSoeknadService.opprettFagsakOgBehandlingFraAltinnSøknad(soknadID)).isEqualTo(fagsak.hentAktivBehandling());

        OpprettSakRequest req = captor.getValue();
        assertThat(req.getBehandlingstema()).isEqualTo(Behandlingstema.ARBEID_ETT_LAND_ØVRIG);
        assertThat(req.getBehandlingstype()).isEqualTo(Behandlingstyper.SOEKNAD);
        assertThat(req.getArbeidsgiver()).isEqualTo(søknad.getInnhold().getArbeidsgiver().getVirksomhetsnummer());
        assertThat(req.getAktørID()).isEqualTo(aktørID);
    }

    @Test
    public void opprettSakFraAltinnSøknad_rådgivningsfirmaErFullmektig_lagerFullmektig()
        throws FunksjonellException, TekniskException {
        final Fagsak fagsak = lagFagsak();
        final MedlemskapArbeidEOSM søknad = lagMedlemskapArbeidEOSM();

        when(soknadMottakConsumer.hentSøknad(eq(soknadID))).thenReturn(søknad);
        when(fagsakService.nyFagsakOgBehandling(captor.capture())).thenReturn(fagsak);
        when(tpsFasade.hentAktørIdForIdent(anyString())).thenReturn(aktørID);

        altinnSoeknadService.opprettFagsakOgBehandlingFraAltinnSøknad(soknadID);

        OpprettSakRequest req = captor.getValue();
        String fullmektigVirksomhetsnummer = søknad.getInnhold().getFullmakt().getFullmektigVirksomhetsnummer();
        assertThat(req.getFullmektig().getRepresentantID()).isEqualTo(fullmektigVirksomhetsnummer);
        assertThat(req.getFullmektig().getRepresenterer()).isEqualTo(Representerer.ARBEIDSGIVER);
    }

    @Test
    public void opprettSakFraAltinnSøknad_fullmaktUtenRådgivningsfirma_lagerArbeidsgiverSomFullmektig()
        throws FunksjonellException, TekniskException {
        final Fagsak fagsak = lagFagsak();
        final MedlemskapArbeidEOSM søknad = lagMedlemskapArbeidEOSM();
        søknad.getInnhold().getFullmakt().setFullmektigVirksomhetsnummer(null);
        søknad.getInnhold().getFullmakt().setFullmaktFraArbeidstaker(true);

        when(soknadMottakConsumer.hentSøknad(eq(soknadID))).thenReturn(søknad);
        when(fagsakService.nyFagsakOgBehandling(captor.capture())).thenReturn(fagsak);
        when(tpsFasade.hentAktørIdForIdent(anyString())).thenReturn(aktørID);

        altinnSoeknadService.opprettFagsakOgBehandlingFraAltinnSøknad(soknadID);

        OpprettSakRequest req = captor.getValue();
        String fullmektigVirksomhetsnummer = søknad.getInnhold().getArbeidsgiver().getVirksomhetsnummer();
        assertThat(req.getFullmektig().getRepresentantID()).isEqualTo(fullmektigVirksomhetsnummer);
        assertThat(req.getFullmektig().getRepresenterer()).isEqualTo(Representerer.BEGGE);
    }

    @Test
    public void opprettSakFraAltinnSøknad_kontaktpersonNavnFinnes_lagerKontaktopplysninger()
        throws FunksjonellException, TekniskException {
        final Fagsak fagsak = lagFagsak();
        final MedlemskapArbeidEOSM søknad = lagMedlemskapArbeidEOSM();
        søknad.getInnhold().getArbeidsgiver().getKontaktperson().setKontaktpersonNavn("Ola");

        when(soknadMottakConsumer.hentSøknad(eq(soknadID))).thenReturn(søknad);
        when(fagsakService.nyFagsakOgBehandling(captor.capture())).thenReturn(fagsak);
        when(tpsFasade.hentAktørIdForIdent(anyString())).thenReturn(aktørID);

        altinnSoeknadService.opprettFagsakOgBehandlingFraAltinnSøknad(soknadID);

        OpprettSakRequest req = captor.getValue();
        assertThat(req.getKontaktopplysninger()).isNotEmpty();
        Kontaktopplysning kontaktopplysning = req.getKontaktopplysninger().iterator().next();
        assertThat(kontaktopplysning.getKontaktNavn())
            .isEqualTo(søknad.getInnhold().getArbeidsgiver().getKontaktperson().getKontaktpersonNavn());
    }

    private MedlemskapArbeidEOSM lagMedlemskapArbeidEOSM() {
        MedlemskapArbeidEOSM medlemskapArbeidEOSM = new MedlemskapArbeidEOSM();
        Innhold innhold = new Innhold();
        medlemskapArbeidEOSM.setInnhold(innhold);

        innhold.setFullmakt(new Fullmakt());
        innhold.getFullmakt().setFullmektigVirksomhetsnummer("123333");

        innhold.setArbeidsgiver(new Arbeidsgiver());
        innhold.getArbeidsgiver().setOffentligVirksomhet(Boolean.FALSE);
        innhold.getArbeidsgiver().setVirksomhetsnummer("53254352");
        innhold.getArbeidsgiver().setKontaktperson(new Kontaktperson());
        innhold.getArbeidsgiver().getKontaktperson().setKontaktpersonNavn("Navne Navnesen");

        innhold.setArbeidstaker(new Arbeidstaker());
        innhold.getArbeidstaker().setFoedselsnummer("12345612345");

        innhold.setMidlertidigUtsendt(new MidlertidigUtsendt());
        innhold.getMidlertidigUtsendt().setArbeidsland("PL");
        innhold.getMidlertidigUtsendt().setUtenlandsoppdraget(new Utenlandsoppdraget());
        innhold.getMidlertidigUtsendt().getUtenlandsoppdraget().setSamletUtsendingsperiode(new Tidsrom());
        return medlemskapArbeidEOSM;
    }

    private Fagsak lagFagsak() {
        Fagsak fagsak = new Fagsak();
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setStatus(Behandlingsstatus.OPPRETTET);
        fagsak.setBehandlinger(List.of(behandling));

        return fagsak;
    }
}