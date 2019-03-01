package no.nav.melosys.service.dokument.brev;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;

import no.nav.dok.brevdata.felles.v1.navfelles.Mottaker;
import no.nav.dok.brevdata.felles.v1.navfelles.Organisasjon;
import no.nav.dok.brevdata.felles.v1.navfelles.Person;
import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.Produserbaredokumenter;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.doksys.DokumentbestillingMetadata;
import no.nav.melosys.integrasjon.doksys.MottakerType;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.integrasjon.tps.TpsService;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.w3c.dom.Element;

import static no.nav.melosys.domain.kodeverk.Produserbaredokumenter.*;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class BrevDataServiceTest {

    private BrevDataService service;

    private static final String FNR = "Fnr";
    private static final String ORGNR = "Org-Nr";
    private static final String REPRESENTANT = "Representant";

    private TpsFasade tpsFasade;

    @Before
    public void setUp() throws IkkeFunnetException, TekniskException {
        BehandlingsresultatRepository behandlingsresultatRepository = mock(BehandlingsresultatRepository.class);
        tpsFasade = mock(TpsService.class);
        service = spy(new BrevDataService(tpsFasade, behandlingsresultatRepository));

        when(tpsFasade.hentFagsakIdentMedRolleType(any(), any())).thenCallRealMethod();
        when(tpsFasade.hentIdentForAktørId(any())).thenReturn(FNR);
        when(behandlingsresultatRepository.findById(anyLong())).thenReturn(Optional.of(new Behandlingsresultat()));
    }

    @Test
    public void lagForvaltningsmelding_representantErNull_tilBruker() throws TekniskException {
        Behandling behandling = lagBehandling();
        BrevData brevData = new BrevData("Z123456");

        DokumentbestillingMetadata metadata = service.lagBestillingMetadata(MELDING_FORVENTET_SAKSBEHANDLINGSTID, behandling, brevData);

        assertThat(metadata.bruker).isEqualTo(FNR);
        assertThat(metadata.mottakerID).isEqualTo(FNR);

        Element element = service.lagBrevXML(MELDING_FORVENTET_SAKSBEHANDLINGSTID, lagBehandling(), brevData);

        assertThat(element).isNotNull();
    }

    @Test
    public void lagForvaltningsmelding_representantIkkeNull_tilRepresentant() throws TekniskException {

        Aktoer representant = new Aktoer();
        representant.setAktørId("Representant");
        representant.setRolle(Aktoersroller.REPRESENTANT);

        Behandling behandling = lagBehandling();
        behandling.getFagsak().getAktører().add(representant);
        BrevData brevData = new BrevData("Z123456");

        when(tpsFasade.hentFagsakIdentMedRolleType(behandling.getFagsak(), Aktoersroller.REPRESENTANT)).thenReturn(REPRESENTANT);

        DokumentbestillingMetadata metadata = service.lagBestillingMetadata(MELDING_FORVENTET_SAKSBEHANDLINGSTID, behandling, brevData);

        assertThat(metadata.bruker).isEqualTo(FNR);
        assertThat(metadata.mottakerID).isEqualTo(REPRESENTANT);

        Element element = service.lagBrevXML(MELDING_FORVENTET_SAKSBEHANDLINGSTID, lagBehandling(), brevData);

        assertThat(element).isNotNull();
    }

    @Test
    public void lagMangelbrevXml_mottakerErBruker() throws TekniskException {
        Behandling behandling = lagBehandling();
        BrevData brevData = new BrevData("Z123456");
        brevData.mottaker = Aktoersroller.BRUKER;
        brevData.fritekst = "Test";

        DokumentbestillingMetadata metadata = service.lagBestillingMetadata(MELDING_MANGLENDE_OPPLYSNINGER, behandling, brevData);

        assertThat(metadata.bruker).isEqualTo(FNR);
        assertThat(metadata.mottakerID).isEqualTo(FNR);

        doAnswer(answer -> {
            Mottaker mottaker = (Mottaker) answer.callRealMethod();
            assertThat(mottaker).isNotNull();
            assertThat(mottaker).isInstanceOf(Person.class);
            return mottaker;
        }).when(service).lagMottaker(any(), any());

        Element element = service.lagBrevXML(MELDING_MANGLENDE_OPPLYSNINGER, behandling, brevData);

        assertThat(element).isNotNull();
    }

    @Test
    public void lagMangelbrevXml_mottakerErArbeidsgiver() throws TekniskException {
        Behandling behandling = lagBehandling();
        BrevData brevData = new BrevData("Z123456");
        brevData.mottaker = Aktoersroller.ARBEIDSGIVER;
        brevData.fritekst = "Test";

        DokumentbestillingMetadata metadata = service.lagBestillingMetadata(MELDING_MANGLENDE_OPPLYSNINGER, behandling, brevData);

        assertThat(metadata.bruker).isEqualTo(FNR);
        assertThat(metadata.mottakerID).isEqualTo(ORGNR);

        doAnswer(answer -> {
            Mottaker mottaker = (Mottaker) answer.callRealMethod();
            assertThat(mottaker).isNotNull();
            assertThat(mottaker).isInstanceOf(Organisasjon.class);
            return mottaker;
        }).when(service).lagMottaker(any(), any());

        Element element = service.lagBrevXML(MELDING_MANGLENDE_OPPLYSNINGER, behandling, brevData);

        assertThat(element).isNotNull();
    }

    @Test
    public void lagMetadataForInnvilgelsesbrevAngirDokTypeLikInnvilgelseYrkesaktiv() throws Exception {
        testLagDokumentMetadata(INNVILGELSE_YRKESAKTIV, Aktoersroller.BRUKER);
    }

    @Test
    public void lagMetadataForMangelbrevAngirDokTypeLikMangelbrev() throws Exception {
        testLagDokumentMetadata(MELDING_MANGLENDE_OPPLYSNINGER, Aktoersroller.ARBEIDSGIVER);
    }

    @Test
    public void lagMetadataForMangelbrevAngirDokTypeLikHenleggelse() throws Exception {
        testLagDokumentMetadata(MELDING_HENLAGT_SAK, Aktoersroller.BRUKER);
    }

    @Test
    public void lagMetadataUtenMottakerKasterUnntak() {
        Throwable unntak = catchThrowable(() -> service.lagBestillingMetadata(INNVILGELSE_YRKESAKTIV, lagBehandling(), new BrevData()));
        assertThat(unntak).isInstanceOf(TekniskException.class)
            .hasMessageContaining("finnes ingen mottaker")
            .hasNoCause();
    }

    private void testLagDokumentMetadata(Produserbaredokumenter doktype, Aktoersroller rolle) throws Exception {
        DokumentbestillingMetadata resultat = service.lagBestillingMetadata(doktype, lagBehandling(), lagBrevData(rolle));
        DokumentbestillingMetadata forventet = lagDokumentbestillingMetadata(doktype, rolle);
        assertThat(resultat).isEqualToComparingFieldByFieldRecursively(forventet);
    }

    private static DokumentbestillingMetadata lagDokumentbestillingMetadata(Produserbaredokumenter doktype, Aktoersroller rolle) throws TekniskException {
        DokumentbestillingMetadata forventet = new DokumentbestillingMetadata();
        forventet.bruker = FNR;
        forventet.mottakersRolle = rolle;
        if (rolle == Aktoersroller.BRUKER) {
            forventet.mottakerType = MottakerType.PERSON;
            forventet.mottakerID = FNR;
        } else {
            forventet.mottakerType = MottakerType.ORGANISASJON;
            forventet.mottakerID = ORGNR;
        }

        forventet.dokumenttypeID = DokumenttypeIdMapper.hentID(doktype);
        forventet.fagområde = "MED";
        forventet.journalsakID = "123";
        forventet.saksbehandler = "TEST";
        return forventet;
    }

    private static BrevData lagBrevData(Aktoersroller rolle) {
        BrevData brevDataDto = new BrevData();
        brevDataDto.saksbehandler = "TEST";
        brevDataDto.mottaker = rolle;
        brevDataDto.fritekst = "Test";
        return brevDataDto;
    }

    private static Behandling lagBehandling() {
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MOCK-1");
        fagsak.setGsakSaksnummer(123L);

        Aktoer bruker = new Aktoer();
        bruker.setAktørId("Aktør-Id");
        bruker.setRolle(Aktoersroller.BRUKER);

        Aktoer arbeidsgiver = new Aktoer();
        arbeidsgiver.setOrgnr(ORGNR);
        arbeidsgiver.setRolle(Aktoersroller.ARBEIDSGIVER);

        fagsak.setAktører(new HashSet<>(Arrays.asList(bruker, arbeidsgiver)));

        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setRegistrertDato(Instant.now());
        behandling.setType(Behandlingstyper.SOEKNAD);
        behandling.setFagsak(fagsak);

        return behandling;
    }

}
