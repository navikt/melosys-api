package no.nav.melosys.service.altinn;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Fullmaktstype;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.msm.AltinnDokument;
import no.nav.melosys.integrasjon.altinn.SoknadMottakConsumer;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService;
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

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;

import java.net.URL;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;

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
    private MottatteOpplysningerService mottatteOpplysningerService;
    @Mock
    private PersondataFasade persondataFasade;
    @Mock
    private AvklarteVirksomheterService avklarteVirksomheterService;

    private AltinnSoeknadService altinnSoeknadService;

    private final String søknadID = "13423";
    private final String aktørID = "123321123";

    private final AltinnDokument søknadDokument = new AltinnDokument(
        søknadID, "dokID123", "tittel", AltinnDokument.AltinnDokumentType.SOKNAD.name(), "Base64EncodedPdf", Instant.EPOCH);

    @Captor
    private ArgumentCaptor<OpprettSakRequest> captor;

    @BeforeEach
    void setup() {
        altinnSoeknadService = new AltinnSoeknadService(soknadMottakConsumer, fagsakService,
            mottatteOpplysningerService, persondataFasade, avklarteVirksomheterService);
    }

    @Test
    void opprettFagsakOgBehandlingFraAltinnSøknad_soeknadEksisterer_verifiserFagsakBehandlingOgMottatteOpplysningerOpprettet() {
        final Fagsak fagsak = lagFagsak();
        final MedlemskapArbeidEOSM søknad = lagMedlemskapArbeidEOSM();

        when(soknadMottakConsumer.hentSøknad(søknadID)).thenReturn(søknad);
        when(soknadMottakConsumer.hentDokumenter(søknadID)).thenReturn(Set.of(søknadDokument));
        when(fagsakService.nyFagsakOgBehandling(captor.capture())).thenReturn(fagsak);
        when(persondataFasade.hentAktørIdForIdent(anyString())).thenReturn(aktørID);


        assertThat(altinnSoeknadService.opprettFagsakOgBehandlingFraAltinnSøknad(søknadID)).isEqualTo(fagsak.finnAktivBehandlingIkkeÅrsavregning());


        OpprettSakRequest req = captor.getValue();
        assertThat(req.getSakstype()).isEqualTo(Sakstyper.EU_EOS);
        assertThat(req.getSakstema()).isEqualTo(Sakstemaer.MEDLEMSKAP_LOVVALG);
        assertThat(req.getBehandlingstema()).isEqualTo(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        assertThat(req.getBehandlingstype()).isEqualTo(Behandlingstyper.FØRSTEGANG);
        assertThat(req.getBehandlingsårsaktype()).isEqualTo(Behandlingsaarsaktyper.SØKNAD);
        assertThat(req.getMottaksdato()).isEqualTo(LocalDate.ofInstant(søknadDokument.getInnsendtTidspunkt(), ZoneId.systemDefault()));
        assertThat(req.getArbeidsgiver()).isEqualTo(søknad.getInnhold().getArbeidsgiver().getVirksomhetsnummer());
        assertThat(req.getAktørID()).isEqualTo(aktørID);

        verify(mottatteOpplysningerService).opprettSøknadUtsendteArbeidstakereEøs(eq(1L), anyString(), any(),
            eq(søknadID));
    }

    @Test
    void opprettFagsakOgBehandlingFraAltinnSøknad_soeknadEksistererArbeidsgiverOffentlig_verifiserArbeidTjenestepersonEllerFly() {
        final Fagsak fagsak = lagFagsak();
        final MedlemskapArbeidEOSM søknad = lagMedlemskapArbeidEOSM();

        søknad.getInnhold().getArbeidsgiver().setOffentligVirksomhet(Boolean.TRUE);

        when(soknadMottakConsumer.hentSøknad(søknadID)).thenReturn(søknad);
        when(soknadMottakConsumer.hentDokumenter(søknadID)).thenReturn(Set.of(søknadDokument));
        when(fagsakService.nyFagsakOgBehandling(captor.capture())).thenReturn(fagsak);
        when(persondataFasade.hentAktørIdForIdent(anyString())).thenReturn(aktørID);


        assertThat(altinnSoeknadService.opprettFagsakOgBehandlingFraAltinnSøknad(søknadID)).isEqualTo(fagsak.finnAktivBehandlingIkkeÅrsavregning());


        OpprettSakRequest req = captor.getValue();
        assertThat(req.getBehandlingstema()).isEqualTo(Behandlingstema.ARBEID_TJENESTEPERSON_ELLER_FLY);
        assertThat(req.getBehandlingstype()).isEqualTo(Behandlingstyper.FØRSTEGANG);
        assertThat(req.getArbeidsgiver()).isEqualTo(søknad.getInnhold().getArbeidsgiver().getVirksomhetsnummer());
        assertThat(req.getAktørID()).isEqualTo(aktørID);
    }

    @Test
    void opprettSakFraAltinnSøknad_rådgivningsfirmaErFullmektig_lagerFullmektig() {
        final Fagsak fagsak = lagFagsak();
        final MedlemskapArbeidEOSM søknad = lagMedlemskapArbeidEOSM();

        when(soknadMottakConsumer.hentSøknad(søknadID)).thenReturn(søknad);
        when(soknadMottakConsumer.hentDokumenter(søknadID)).thenReturn(Set.of(søknadDokument));
        when(fagsakService.nyFagsakOgBehandling(captor.capture())).thenReturn(fagsak);
        when(persondataFasade.hentAktørIdForIdent(anyString())).thenReturn(aktørID);


        altinnSoeknadService.opprettFagsakOgBehandlingFraAltinnSøknad(søknadID);


        OpprettSakRequest req = captor.getValue();
        String fullmektigVirksomhetsnummer = søknad.getInnhold().getFullmakt().getFullmektigVirksomhetsnummer();
        assertThat(req.getFullmektig().getOrgnr()).isEqualTo(fullmektigVirksomhetsnummer);
        assertThat(req.getFullmektig().getFullmakter()).containsExactlyInAnyOrder(Fullmaktstype.FULLMEKTIG_SØKNAD, Fullmaktstype.FULLMEKTIG_ARBEIDSGIVER);
    }

    @Test
    void opprettSakFraAltinnSøknad_fullmaktUtenRådgivningsfirma_lagerArbeidsgiverSomFullmektig() {
        final Fagsak fagsak = lagFagsak();
        final MedlemskapArbeidEOSM søknad = lagMedlemskapArbeidEOSM();
        søknad.getInnhold().getFullmakt().setFullmektigVirksomhetsnummer(null);
        søknad.getInnhold().getFullmakt().setFullmaktFraArbeidstaker(true);

        when(soknadMottakConsumer.hentSøknad(søknadID)).thenReturn(søknad);
        when(soknadMottakConsumer.hentDokumenter(søknadID)).thenReturn(Set.of(søknadDokument));
        when(fagsakService.nyFagsakOgBehandling(captor.capture())).thenReturn(fagsak);
        when(persondataFasade.hentAktørIdForIdent(anyString())).thenReturn(aktørID);


        altinnSoeknadService.opprettFagsakOgBehandlingFraAltinnSøknad(søknadID);


        OpprettSakRequest req = captor.getValue();
        String fullmektigVirksomhetsnummer = søknad.getInnhold().getArbeidsgiver().getVirksomhetsnummer();
        assertThat(req.getFullmektig().getOrgnr()).isEqualTo(fullmektigVirksomhetsnummer);
        assertThat(req.getFullmektig().getFullmakter()).containsExactlyInAnyOrder(Fullmaktstype.FULLMEKTIG_SØKNAD, Fullmaktstype.FULLMEKTIG_ARBEIDSGIVER);
    }

    @Test
    void opprettSakFraAltinnSøknad_kontaktpersonNavnFinnes_lagerKontaktopplysninger() {
        final Fagsak fagsak = lagFagsak();
        final MedlemskapArbeidEOSM søknad = lagMedlemskapArbeidEOSM();
        søknad.getInnhold().getArbeidsgiver().getKontaktperson().setKontaktpersonNavn("Ola");

        when(soknadMottakConsumer.hentSøknad(søknadID)).thenReturn(søknad);
        when(soknadMottakConsumer.hentDokumenter(søknadID)).thenReturn(Set.of(søknadDokument));
        when(fagsakService.nyFagsakOgBehandling(captor.capture())).thenReturn(fagsak);
        when(persondataFasade.hentAktørIdForIdent(anyString())).thenReturn(aktørID);


        altinnSoeknadService.opprettFagsakOgBehandlingFraAltinnSøknad(søknadID);


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

        when(soknadMottakConsumer.hentSøknad(søknadID)).thenReturn(søknad);
        when(soknadMottakConsumer.hentDokumenter(søknadID)).thenReturn(Set.of(søknadDokument));
        when(fagsakService.nyFagsakOgBehandling(captor.capture())).thenReturn(fagsak);


        altinnSoeknadService.opprettFagsakOgBehandlingFraAltinnSøknad(søknadID);


        OpprettSakRequest req = captor.getValue();
        assertThat(req.getUtenlandskPersonId()).isEqualTo(utenlandskPersonId);
    }

    @Test
    void opprettFagsakOgBehandlingFraAltinnSøknad_virksomhetLagresSomAvklartFakta() {
        final Fagsak fagsak = lagFagsak();
        final MedlemskapArbeidEOSM søknad = lagMedlemskapArbeidEOSM();

        when(soknadMottakConsumer.hentSøknad(søknadID)).thenReturn(søknad);
        when(soknadMottakConsumer.hentDokumenter(søknadID)).thenReturn(Set.of(søknadDokument));
        when(fagsakService.nyFagsakOgBehandling(any(OpprettSakRequest.class))).thenReturn(fagsak);


        altinnSoeknadService.opprettFagsakOgBehandlingFraAltinnSøknad(søknadID);


        verify(avklarteVirksomheterService).lagreVirksomhetSomAvklartfakta(
            søknad.getInnhold().getArbeidsgiver().getVirksomhetsnummer(), fagsak.finnAktivBehandlingIkkeÅrsavregning().getId());
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
        Behandling behandling = BehandlingTestBuilder
            .builderWithDefaults()
            .medId(1L)
            .medStatus(Behandlingsstatus.OPPRETTET)
            .build();

        return FagsakTestFactory.builder()
            .behandlinger(behandling)
            .build();
    }
}
