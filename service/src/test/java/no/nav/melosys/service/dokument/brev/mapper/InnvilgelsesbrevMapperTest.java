package no.nav.melosys.service.dokument.brev.mapper;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import no.nav.dok.brevdata.felles.v1.navfelles.*;
import no.nav.dok.brevdata.felles.v1.simpletypes.AktoerType;
import no.nav.dok.brevdata.felles.v1.simpletypes.Spraakkode;
import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType;
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingstype;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Fagsakstype;
import no.nav.melosys.service.dokument.brev.BrevData;

import org.junit.Test;

import static no.nav.melosys.service.dokument.brev.BrevDataUtils.lagKontaktInformasjon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

public class InnvilgelsesbrevMapperTest {

    private final InnvilgelsesbrevMapper instans;

    public InnvilgelsesbrevMapperTest() {
        instans = new InnvilgelsesbrevMapper();
    }

    @Test
    public void mapTilBrevXmlGirIkkeTomXmlStreng() throws Exception {
        FellesType fellesType = lagFellesType();
        MelosysNAVFelles navFelles = LagMelosysNAVFelles();
        Behandling behandling = lagBehandling(lagFagsak());
        BrevData brevdata = new BrevData();
        String resultat = instans.mapTilBrevXML(fellesType, navFelles, behandling, null, brevdata);
        // TODO: Vurder å bruke XMLUnit e.l. til å sammenlikne XML-strengen
        // grundig mot forventninger.
        assertThat(resultat).matches("(?s)\\<\\?xml version=\"\\d\\.\\d+\" .*>\n.*");
    }

    @Test
    public void mapTilBrevXmlMedDysfunksjoneltDataTypeFactoryKasterUnntak() throws Exception {
        FellesType fellesType = lagFellesType();
        MelosysNAVFelles navFelles = LagMelosysNAVFelles();
        Behandling behandling = lagBehandling(lagFagsak());
        BrevData brevDataDto = new BrevData();
        String dataTypeFactoryClass = null;
        Throwable unntak;
        try {
            dataTypeFactoryClass = System.setProperty(DatatypeFactory.DATATYPEFACTORY_PROPERTY, "no.datatypefactory.here.Hahahahahha");
            unntak = catchThrowable(() -> instans.mapTilBrevXML(fellesType, navFelles, behandling, null, brevDataDto));
        } finally {
            if (dataTypeFactoryClass != null) {
                System.setProperty(DatatypeFactory.DATATYPEFACTORY_PROPERTY, dataTypeFactoryClass);
            } else {
                System.getProperties().remove(DatatypeFactory.DATATYPEFACTORY_PROPERTY);
            }
        }
        assertThat(unntak).isInstanceOf(IllegalStateException.class)
            .hasCauseInstanceOf(DatatypeConfigurationException.class)
            .hasMessageContaining("Kan ikke lage DatatypeConverterFactory");
    }

    private Fagsak lagFagsak() {
        Fagsak fagsak = new Fagsak();
        fagsak.setType(Fagsakstype.EU_EØS);
        return fagsak;
    }

    private static Behandling lagBehandling(Fagsak fagsak) {
        Behandling behandling = new Behandling();
        behandling.setType(Behandlingstype.KLAGE);
        behandling.setFagsak(fagsak);
        return behandling;
    }

    private static FellesType lagFellesType() {
        FellesType fellesType = FellesType.builder()
            .withFagsaksnummer("Sak 1")
            .build();
        return fellesType;
    }

    private static MelosysNAVFelles LagMelosysNAVFelles() {
        NavEnhet navEnhet = NavEnhet.builder()
            .withEnhetsId("Enhetsid")
            .withEnhetsNavn("Behandlende Enhet")
            .build();
        Saksbehandler saksbehandler = lagSaksbehandler(navEnhet);
        Person person = lagPerson();
        MelosysNAVFelles navFelles = MelosysNAVFelles.builder()
            .withSakspart(Sakspart.builder()
                .withId("123")
                .withTypeKode(AktoerType.INSTITUSJON)
                .withNavn("Institutten Tei")
                .build())
            .withMottaker(person)
            .withBehandlendeEnhet(navEnhet)
            .withSignerendeSaksbehandler(saksbehandler)
            .withSignerendeBeslutter(saksbehandler)
            .build();
        navFelles.setKontaktinformasjon(lagKontaktInformasjon());
        return navFelles;
    }

    private static Saksbehandler lagSaksbehandler(NavEnhet navEnhet) {
        Saksbehandler saksbehandler = Saksbehandler.builder()
            .withNavAnsatt(NavAnsatt.builder()
                .withAnsattId("Saksbehandler 1")
                .withNavn("Saksbehandler En")
                .build())
            .withNavEnhet(navEnhet)
            .build();
        return saksbehandler;
    }

    private static Person lagPerson() {
        Person person = Person.builder()
            .withId("2")
            .withTypeKode(AktoerType.PERSON)
            .withNavn("Nevn Navnet")
            .withKortNavn("NN")
            .withSpraakkode(Spraakkode.NB)
            .withMottakeradresse(NorskPostadresse.builder()
                .withAdresselinje1("Gate 1")
                .withPostnummer("1234")
                .withPoststed("Poststed")
                .build())
            .build();
        return person;
    }

}
