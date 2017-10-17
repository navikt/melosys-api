package no.nav.melosys.tjenester.gui;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringReader;
import java.io.StringWriter;

import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.modelmapper.convention.MatchingStrategies;

import no.nav.melosys.domain.dokument.arbeidsforhold.Arbeidsforhold;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.integrasjon.aareg.AaregService;
import no.nav.melosys.integrasjon.aareg.arbeidsforhold.ArbeidsforholdMock;
import no.nav.melosys.integrasjon.ereg.EregService;
import no.nav.melosys.integrasjon.ereg.organisasjon.OrganisasjonMock;
import no.nav.melosys.integrasjon.tps.TpsService;
import no.nav.melosys.integrasjon.tps.person.PersonMock;
import no.nav.melosys.tjenester.gui.dto.ArbeidsforholdDto;
import no.nav.melosys.tjenester.gui.dto.view.ArbeidsforholdView;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.FinnArbeidsforholdPrArbeidstakerSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.FinnArbeidsforholdPrArbeidstakerUgyldigInput;


public class ArbeidsforholdDokumentTest {

    ArbeidsforholdRestTjeneste restTjeneste;

    @Before
    public void setUp() throws JAXBException {

        TpsService tps = new TpsService(null, new PersonMock(), null);
        AaregService aareg = new AaregService(new ArbeidsforholdMock(), null);
        EregService ereg = new EregService(new OrganisasjonMock(), null);

        restTjeneste = new ArbeidsforholdRestTjeneste(tps, aareg, ereg);
    }

    // Generer intern aareg xml ut fra ArbeidsforholdRestTjeneste
    @Ignore
    @Test
    public void arbeidsforholdDokumentXmlTest() throws JAXBException, FinnArbeidsforholdPrArbeidstakerSikkerhetsbegrensning, FinnArbeidsforholdPrArbeidstakerUgyldigInput {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.createTypeMap(ArbeidsforholdDto.class, Arbeidsforhold.class);

        PropertyMap<ArbeidsforholdDto, Arbeidsforhold> map1 = new PropertyMap<ArbeidsforholdDto, Arbeidsforhold>() {
            @Override
            protected void configure() {
                map().setOpplysningspliktigID(source.getOpplysningspliktig().getOrgnummer());
                map().setArbeidsgiverID(source.getArbeidsgiver().getOrgnummer());
            }
        };
        modelMapper.addMappings(map1);
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.LOOSE);

        Response response = restTjeneste.hentArbeidsforhold("88888888885");
        ArbeidsforholdView entity = (ArbeidsforholdView) response.getEntity();
        ArbeidsforholdDokument dokument = modelMapper.map(entity, ArbeidsforholdDokument.class);

        JAXBContext jaxbContext = JAXBContext.newInstance(ArbeidsforholdDokument.class);
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        StringWriter writer = new StringWriter();
        marshaller.marshal(dokument, writer);
        System.out.println(writer.toString());

        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        ArbeidsforholdDokument dok2 = (ArbeidsforholdDokument) unmarshaller.unmarshal(new StringReader(writer.toString()));

        assertThat(dok2).isEqualToComparingFieldByFieldRecursively(dokument);
        modelMapper.validate();
    }

}