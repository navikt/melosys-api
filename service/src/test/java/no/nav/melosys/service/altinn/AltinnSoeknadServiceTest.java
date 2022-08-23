package no.nav.melosys.service.altinn;

import java.net.URL;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import no.finn.unleash.FakeUnleash;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Kontaktopplysning;
import no.nav.melosys.domain.kodeverk.Representerer;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.integrasjon.altinn.SoknadMottakConsumer;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.sak.OpprettSakRequest;
import no.nav.melosys.soknad_altinn.MedlemskapArbeidEOSM;
import no.nav.melosys.soknad_altinn.ObjectFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AltinnSoeknadServiceTest {
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
    private final FakeUnleash unleash = new FakeUnleash();

    private AltinnSoeknadService altinnSoeknadService;

    private final String soknadID = "13423";
    private final String aktørID = "123321123";

    @Captor
    private ArgumentCaptor<OpprettSakRequest> captor;

    @BeforeEach
    void setup() {
        altinnSoeknadService = new AltinnSoeknadService(soknadMottakConsumer, fagsakService,
            behandlingsgrunnlagService, persondataFasade, avklarteVirksomheterService, unleash);
    }

    @Test
    void opprettFagsakOgBehandlingFraAltinnSøknad_soeknadEksisterer_verifiserFagsakBehandlingOgBehandlinggrunnlagOpprettet() {
        final Fagsak fagsak = lagFagsak();
        final MedlemskapArbeidEOSM søknad = lagMedlemskapArbeidEOSM();

        when(soknadMottakConsumer.hentSøknad(soknadID)).thenReturn(søknad);
        when(fagsakService.nyFagsakOgBehandling(captor.capture())).thenReturn(fagsak);
        when(persondataFasade.hentAktørIdForIdent(anyString())).thenReturn(aktørID);

        assertThat(altinnSoeknadService.opprettFagsakOgBehandlingFraAltinnSøknad(soknadID)).isEqualTo(fagsak.hentAktivBehandling());

        OpprettSakRequest req = captor.getValue();
        assertThat(req.getSakstype()).isEqualTo(Sakstyper.EU_EOS);
        assertThat(req.getSakstema()).isEqualTo(Sakstemaer.MEDLEMSKAP_LOVVALG);
        assertThat(req.getBehandlingstema()).isEqualTo(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        assertThat(req.getBehandlingstype()).isEqualTo(Behandlingstyper.SOEKNAD);
        assertThat(req.getArbeidsgiver()).isEqualTo(søknad.getInnhold().getArbeidsgiver().getVirksomhetsnummer());
        assertThat(req.getAktørID()).isEqualTo(aktørID);

        verify(behandlingsgrunnlagService).opprettSøknadUtsendteArbeidstakereEøs(eq(1L), anyString(), any(),
            eq(soknadID));
    }

    @Test
    void opprettFagsakOgBehandlingFraAltinnSøknad_soeknadEksistererArbeidsgiverOffentlig_verifiserBehandlingstemaArbeidsEttLandØvrig() {
        unleash.enable("melosys.behandle_alle_saker");
        final Fagsak fagsak = lagFagsak();
        final MedlemskapArbeidEOSM søknad = lagMedlemskapArbeidEOSM();

        søknad.getInnhold().getArbeidsgiver().setOffentligVirksomhet(Boolean.TRUE);

        when(soknadMottakConsumer.hentSøknad(soknadID)).thenReturn(søknad);
        when(fagsakService.nyFagsakOgBehandling(captor.capture())).thenReturn(fagsak);
        when(persondataFasade.hentAktørIdForIdent(anyString())).thenReturn(aktørID);


        assertThat(altinnSoeknadService.opprettFagsakOgBehandlingFraAltinnSøknad(soknadID)).isEqualTo(fagsak.hentAktivBehandling());

        OpprettSakRequest req = captor.getValue();
        assertThat(req.getBehandlingstema()).isEqualTo(Behandlingstema.ARBEID_TJENESTEPERSON_ELLER_FLY);
        assertThat(req.getBehandlingstype()).isEqualTo(Behandlingstyper.SOEKNAD);
        assertThat(req.getArbeidsgiver()).isEqualTo(søknad.getInnhold().getArbeidsgiver().getVirksomhetsnummer());
        assertThat(req.getAktørID()).isEqualTo(aktørID);
    }

    @Test
    void opprettSakFraAltinnSøknad_rådgivningsfirmaErFullmektig_lagerFullmektig() {
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
    void opprettSakFraAltinnSøknad_fullmaktUtenRådgivningsfirma_lagerArbeidsgiverSomFullmektig() {
        final Fagsak fagsak = lagFagsak();
        final MedlemskapArbeidEOSM søknad = lagMedlemskapArbeidEOSM();
        søknad.getInnhold().getFullmakt().setFullmektigVirksomhetsnummer(null);
        søknad.getInnhold().getFullmakt().setFullmaktFraArbeidstaker(true);

        when(soknadMottakConsumer.hentSøknad(soknadID)).thenReturn(søknad);
        when(fagsakService.nyFagsakOgBehandling(captor.capture())).thenReturn(fagsak);
        when(persondataFasade.hentAktørIdForIdent(anyString())).thenReturn(aktørID);

        altinnSoeknadService.opprettFagsakOgBehandlingFraAltinnSøknad(soknadID);

        OpprettSakRequest req = captor.getValue();
        String fullmektigVirksomhetsnummer = søknad.getInnhold().getArbeidsgiver().getVirksomhetsnummer();
        assertThat(req.getFullmektig().getRepresentantID()).isEqualTo(fullmektigVirksomhetsnummer);
        assertThat(req.getFullmektig().getRepresenterer()).isEqualTo(Representerer.BEGGE);
    }

    @Test
    void opprettSakFraAltinnSøknad_kontaktpersonNavnFinnes_lagerKontaktopplysninger() {
        final Fagsak fagsak = lagFagsak();
        final MedlemskapArbeidEOSM søknad = lagMedlemskapArbeidEOSM();
        søknad.getInnhold().getArbeidsgiver().getKontaktperson().setKontaktpersonNavn("Ola");

        when(soknadMottakConsumer.hentSøknad(soknadID)).thenReturn(søknad);
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
    void opprettSakFraAltinnSøknad_arbeidstakerHarUtenlandskIDnummer_utenlandskPersonIdBlirSatt() {
        final String utenlandskPersonId = "utenlandskPersonId";
        final Fagsak fagsak = lagFagsak();
        final MedlemskapArbeidEOSM søknad = lagMedlemskapArbeidEOSM();
        søknad.getInnhold().getArbeidstaker().setUtenlandskIDnummer(utenlandskPersonId);

        when(soknadMottakConsumer.hentSøknad(soknadID)).thenReturn(søknad);
        when(fagsakService.nyFagsakOgBehandling(captor.capture())).thenReturn(fagsak);

        altinnSoeknadService.opprettFagsakOgBehandlingFraAltinnSøknad(soknadID);

        OpprettSakRequest req = captor.getValue();
        assertThat(req.getUtenlandskPersonId()).isEqualTo(utenlandskPersonId);
    }

    @Test
    void opprettFagsakOgBehandlingFraAltinnSøknad_virksomhetLagresSomAvklartFakta() {
        final Fagsak fagsak = lagFagsak();
        final MedlemskapArbeidEOSM søknad = lagMedlemskapArbeidEOSM();

        when(soknadMottakConsumer.hentSøknad(soknadID)).thenReturn(søknad);
        when(fagsakService.nyFagsakOgBehandling(any(OpprettSakRequest.class))).thenReturn(fagsak);

        altinnSoeknadService.opprettFagsakOgBehandlingFraAltinnSøknad(soknadID);

        verify(avklarteVirksomheterService).lagreVirksomhetSomAvklartfakta(
            søknad.getInnhold().getArbeidsgiver().getVirksomhetsnummer(), fagsak.hentAktivBehandling().getId());
    }

    private MedlemskapArbeidEOSM lagMedlemskapArbeidEOSM() {
        JAXBContext jaxbContext;
        MedlemskapArbeidEOSM medlemskapArbeidEOSM;
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
