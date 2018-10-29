package no.nav.melosys.service.dokument.brev;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;

import no.nav.dok.brevdata.felles.v1.navfelles.Mottaker;
import no.nav.dok.brevdata.felles.v1.navfelles.Organisasjon;
import no.nav.dok.brevdata.felles.v1.navfelles.Person;
import no.nav.melosys.domain.*;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.doksys.DokumentbestillingMetadata;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.integrasjon.tps.TpsService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.w3c.dom.Element;

import static no.nav.melosys.service.dokument.DokumentType.MELDING_FORVENTET_SAKSBEHANDLINGSTID;
import static no.nav.melosys.service.dokument.DokumentType.MELDING_MANGLENDE_OPPLYSNINGER;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class BrevDataServiceTest {

    private BrevDataService service;

    private static final String FNR = "Fnr";
    private static final String ORGNR = "Org-Nr";

    @Before
    public void setUp() throws IkkeFunnetException, TekniskException {
        TpsFasade tpsFasade = mock(TpsService.class);
        service = spy(new BrevDataService(tpsFasade));

        when(tpsFasade.hentFagsakIdentMedRolleType(any(), any())).thenCallRealMethod();
        when(tpsFasade.hentIdentForAktørId(any())).thenReturn(FNR);
    }

    @Test
    public void lagForvaltningsmelding() throws TekniskException {
        Behandling behandling = lagBehandling();
        BrevDataDto brevDataDto = new BrevDataDto();
        brevDataDto.saksbehandler = "TEST";

        DokumentbestillingMetadata metadata = service.lagBestillingMetadata(MELDING_FORVENTET_SAKSBEHANDLINGSTID, behandling, brevDataDto);

        assertThat(metadata.bruker).isEqualTo(FNR);
        assertThat(metadata.mottaker).isEqualTo(FNR);

        Element element = service.lagBrevXML(MELDING_FORVENTET_SAKSBEHANDLINGSTID, lagBehandling(), brevDataDto);

        assertThat(element).isNotNull();
    }

    @Test
    public void lagMangelbrevXml_mottakerErBruker() throws TekniskException {
        Behandling behandling = lagBehandling();
        BrevDataDto brevDataDto = new BrevDataDto();
        brevDataDto.saksbehandler = "TEST";
        brevDataDto.mottaker = RolleType.BRUKER;
        brevDataDto.fritekst = "Test";

        DokumentbestillingMetadata metadata = service.lagBestillingMetadata(MELDING_MANGLENDE_OPPLYSNINGER, behandling, brevDataDto);

        assertThat(metadata.bruker).isEqualTo(FNR);
        assertThat(metadata.mottaker).isEqualTo(FNR);

        doAnswer(answer -> {
            Mottaker mottaker = (Mottaker) answer.callRealMethod();
            assertThat(mottaker).isNotNull();
            assertThat(mottaker).isInstanceOf(Person.class);
            return mottaker;
        }).when(service).lagMottaker(any(), any());

        Element element = service.lagBrevXML(MELDING_MANGLENDE_OPPLYSNINGER, behandling, brevDataDto);

        assertThat(element).isNotNull();
    }

    @Test
    public void lagMangelbrevXml_mottakerErArbeidsgiver() throws TekniskException {
        Behandling behandling = lagBehandling();
        BrevDataDto brevDataDto = new BrevDataDto();
        brevDataDto.saksbehandler = "TEST";
        brevDataDto.mottaker = RolleType.ARBEIDSGIVER;
        brevDataDto.fritekst = "Test";

        DokumentbestillingMetadata metadata = service.lagBestillingMetadata(MELDING_MANGLENDE_OPPLYSNINGER, behandling, brevDataDto);

        assertThat(metadata.bruker).isEqualTo(FNR);
        assertThat(metadata.mottaker).isEqualTo(ORGNR);

        doAnswer(answer -> {
            Mottaker mottaker = (Mottaker) answer.callRealMethod();
            assertThat(mottaker).isNotNull();
            assertThat(mottaker).isInstanceOf(Organisasjon.class);
            return mottaker;
        }).when(service).lagMottaker(any(), any());

        Element element = service.lagBrevXML(MELDING_MANGLENDE_OPPLYSNINGER, behandling, brevDataDto);

        assertThat(element).isNotNull();
    }

    private Behandling lagBehandling() {
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MOCK-1");
        fagsak.setGsakSaksnummer(123L);

        Aktoer bruker = new Aktoer();
        bruker.setAktørId("Aktør-Id");
        bruker.setRolle(RolleType.BRUKER);

        Aktoer arbeidsgiver = new Aktoer();
        arbeidsgiver.setOrgnr(ORGNR);
        arbeidsgiver.setRolle(RolleType.ARBEIDSGIVER);

        fagsak.setAktører(new HashSet<>(Arrays.asList(bruker, arbeidsgiver)));

        Behandling behandling = new Behandling();
        behandling.setRegistrertDato(Instant.now());
        behandling.setType(Behandlingstype.SØKNAD);
        behandling.setFagsak(fagsak);

        return behandling;
    }
}
