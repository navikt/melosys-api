package no.nav.melosys.service.altinn;

import java.net.URL;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

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
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
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
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AltinnSoeknadServiceTest {
    @Mock
    private SoknadMottakConsumer soknadMottakConsumer;
    @Mock
    private FagsakService fagsakService;
    @Mock
    private BehandlingsgrunnlagService behandlingsgrunnlagService;
    @Mock
    private PersondataFasade persondataFasade;
    @Mock
    private AvklarteVirksomheterService avklarteVirksomheterService;

    private AltinnSoeknadService altinnSoeknadService;

    private final String soknadID = "13423";
    private final String aktørID = "123321123";

    @Captor
    private ArgumentCaptor<OpprettSakRequest> captor;

    @Before
    public void setup() {
        altinnSoeknadService = new AltinnSoeknadService(soknadMottakConsumer, fagsakService,
            behandlingsgrunnlagService, persondataFasade, avklarteVirksomheterService);
    }

    @Test
    public void opprettFagsakOgBehandlingFraAltinnSøknad_soeknadEksisterer_verifiserFagsakBehandlingOgBehandlinggrunnlagOpprettet()
        throws FunksjonellException, TekniskException {
        final Fagsak fagsak = lagFagsak();
        final MedlemskapArbeidEOSM søknad = lagMedlemskapArbeidEOSM();

        when(soknadMottakConsumer.hentSøknad(eq(soknadID))).thenReturn(søknad);
        when(fagsakService.nyFagsakOgBehandling(captor.capture())).thenReturn(fagsak);
        when(persondataFasade.hentAktørIdForIdent(anyString())).thenReturn(aktørID);

        assertThat(altinnSoeknadService.opprettFagsakOgBehandlingFraAltinnSøknad(soknadID)).isEqualTo(fagsak.hentAktivBehandling());

        OpprettSakRequest req = captor.getValue();
        assertThat(req.getBehandlingstema()).isEqualTo(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        assertThat(req.getBehandlingstype()).isEqualTo(Behandlingstyper.SOEKNAD);
        assertThat(req.getArbeidsgiver()).isEqualTo(søknad.getInnhold().getArbeidsgiver().getVirksomhetsnummer());
        assertThat(req.getAktørID()).isEqualTo(aktørID);

        verify(behandlingsgrunnlagService).opprettSøknadUtsendteArbeidstakereEøs(eq(1L), anyString(), any(), eq(soknadID));
    }

    @Test
    public void opprettFagsakOgBehandlingFraAltinnSøknad_soeknadEksistererArbeidsgiverOffentlig_verifiserBehandlingstemaArbeidsEttLandØvrig()
        throws FunksjonellException, TekniskException {
        final Fagsak fagsak = lagFagsak();
        final MedlemskapArbeidEOSM søknad = lagMedlemskapArbeidEOSM();

        søknad.getInnhold().getArbeidsgiver().setOffentligVirksomhet(Boolean.TRUE);

        when(soknadMottakConsumer.hentSøknad(eq(soknadID))).thenReturn(søknad);
        when(fagsakService.nyFagsakOgBehandling(captor.capture())).thenReturn(fagsak);
        when(persondataFasade.hentAktørIdForIdent(anyString())).thenReturn(aktørID);

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

        when(soknadMottakConsumer.hentSøknad(soknadID)).thenReturn(søknad);
        when(fagsakService.nyFagsakOgBehandling(captor.capture())).thenReturn(fagsak);
        when(persondataFasade.hentAktørIdForIdent(anyString())).thenReturn(aktørID);

        altinnSoeknadService.opprettFagsakOgBehandlingFraAltinnSøknad(soknadID);

        OpprettSakRequest req = captor.getValue();
        String fullmektigVirksomhetsnummer = søknad.getInnhold().getFullmakt().getFullmektigVirksomhetsnummer();
        assertThat(req.getFullmektig().getRepresentantID()).isEqualTo(fullmektigVirksomhetsnummer);
        assertThat(req.getFullmektig().getRepresenterer()).isEqualTo(Representerer.BEGGE);
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
        when(persondataFasade.hentAktørIdForIdent(anyString())).thenReturn(aktørID);

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
        when(persondataFasade.hentAktørIdForIdent(anyString())).thenReturn(aktørID);

        altinnSoeknadService.opprettFagsakOgBehandlingFraAltinnSøknad(soknadID);

        OpprettSakRequest req = captor.getValue();
        assertThat(req.getKontaktopplysninger()).isNotEmpty();
        Kontaktopplysning kontaktopplysning = req.getKontaktopplysninger().iterator().next();
        assertThat(kontaktopplysning.getKontaktNavn())
            .isEqualTo(søknad.getInnhold().getArbeidsgiver().getKontaktperson().getKontaktpersonNavn());
    }

    @Test
    public void opprettSakFraAltinnSøknad_arbeidstakerHarUtenlandskIDnummer_utenlandskPersonIdBlirSatt()
        throws FunksjonellException, TekniskException {
        final String utenlandskPersonId = "utenlandskPersonId";
        final Fagsak fagsak = lagFagsak();
        final MedlemskapArbeidEOSM søknad = lagMedlemskapArbeidEOSM();
        søknad.getInnhold().getArbeidstaker().setUtenlandskIDnummer(utenlandskPersonId);

        when(soknadMottakConsumer.hentSøknad(eq(soknadID))).thenReturn(søknad);
        when(fagsakService.nyFagsakOgBehandling(captor.capture())).thenReturn(fagsak);

        altinnSoeknadService.opprettFagsakOgBehandlingFraAltinnSøknad(soknadID);

        OpprettSakRequest req = captor.getValue();
        assertThat(req.getUtenlandskPersonId()).isEqualTo(utenlandskPersonId);
    }

    @Test
    public void opprettFagsakOgBehandlingFraAltinnSøknad_virksomhetLagresSomAvklartFakta()
        throws FunksjonellException, TekniskException {
        final Fagsak fagsak = lagFagsak();
        final MedlemskapArbeidEOSM søknad = lagMedlemskapArbeidEOSM();

        when(soknadMottakConsumer.hentSøknad(eq(soknadID))).thenReturn(søknad);
        when(fagsakService.nyFagsakOgBehandling(any(OpprettSakRequest.class))).thenReturn(fagsak);

        altinnSoeknadService.opprettFagsakOgBehandlingFraAltinnSøknad(soknadID);

        verify(avklarteVirksomheterService).lagreVirksomhetSomAvklartfakta(
            eq(søknad.getInnhold().getArbeidsgiver().getVirksomhetsnummer()), eq(fagsak.hentAktivBehandling().getId()));
    }

    private MedlemskapArbeidEOSM lagMedlemskapArbeidEOSM() {
        JAXBContext jaxbContext = null;
        MedlemskapArbeidEOSM medlemskapArbeidEOSM = null;
        try {
            jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
            URL url = getClass().getClassLoader().getResource("altinn/NAV_MedlemskapArbeidEOS.xml");
            medlemskapArbeidEOSM = ((JAXBElement<MedlemskapArbeidEOSM>) jaxbContext.createUnmarshaller().unmarshal(
                url)).getValue();
        } catch (JAXBException e) {
            throw new IllegalStateException(e);
        }
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