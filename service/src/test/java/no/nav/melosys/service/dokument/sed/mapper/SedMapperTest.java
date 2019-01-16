package no.nav.melosys.service.dokument.sed.mapper;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.Lists;
import no.nav.melosys.domain.dokument.felles.Adresse;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.felles.StrukturertAdresse;
import no.nav.melosys.domain.dokument.person.*;
import no.nav.melosys.domain.dokument.soeknad.ArbeidUtland;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.eux.model.SedType;
import no.nav.melosys.eux.model.medlemskap.Medlemskap;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.brev.mapper.felles.Arbeidssted;
import no.nav.melosys.service.dokument.brev.mapper.felles.Virksomhet;
import no.nav.melosys.service.dokument.sed.SedData;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class SedMapperTest {

    protected class SedMapperImpl extends SedMapper<MedlemskapImpl, SedDataImpl> {

        @Override
        protected MedlemskapImpl hentMedlemskap(SedDataImpl sedData) {
            return new MedlemskapImpl();
        }

        @Override
        protected SedType getSedType() {
            return SedType.A009;
        }
    }

    private final class SedDataImpl extends SedData {}
    private final class MedlemskapImpl extends Medlemskap {}

    private SedMapperImpl sedMapper = new SedMapperImpl();
    private SedDataImpl sedData;
    private ObjectMapper objectMapper;

    @Before
    public void setup() throws IOException, URISyntaxException {
        sedData = new SedDataImpl();
        objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        objectMapper.registerModule(new JavaTimeModule());

        URI søknadURI = (getClass().getClassLoader().getResource("soknad.json")).toURI();
        String json = new String(Files.readAllBytes(Paths.get(søknadURI)));
        SoeknadDokument soeknadDokument = objectMapper.readValue(json, SoeknadDokument.class);

        StrukturertAdresse strukturertAdresse = new StrukturertAdresse();
        strukturertAdresse.husnummer = "25";
        strukturertAdresse.gatenavn = "Gatenavn";
        strukturertAdresse.postnummer = "0165";
        strukturertAdresse.poststed = "Poststed";
        strukturertAdresse.region = "Region";
        strukturertAdresse.landKode = "Land";

        PersonDokument person = new PersonDokument();
        person.kjønn = new KjoennsType();
        person.kjønn.setKode("K");
        person.fornavn = "Ola";
        person.etternavn = "Nordmann";
        person.fødselsdato = LocalDate.now();
        person.fnr = "123456789";
        person.statsborgerskap = new Land();
        person.statsborgerskap.setKode("NOR");

        Familiemedlem far = new Familiemedlem();
        far.familierelasjon = Familierelasjon.FARA;
        far.navn = "Far-Fornavn Far-Etternavn";
        far.fnr = "1231231222";

        Familiemedlem mor = new Familiemedlem();
        mor.familierelasjon = Familierelasjon.MORA;
        mor.fnr = "1111111111";
        mor.navn = "Mor-UtenEtternavn";

        person.familiemedlemmer = Lists.newArrayList(far, mor);

        ArbeidUtland arbeidUtland = new ArbeidUtland();
        arbeidUtland.adresse = strukturertAdresse;
        SoeknadDokument søknad = new SoeknadDokument();
        søknad.arbeidUtland = Arrays.asList(arbeidUtland);

        sedData.setPersonDokument(person);
        sedData.setSøknadDokument(soeknadDokument);
        sedData.setArbeidssteder(Lists.newArrayList(new Arbeidssted("navn", "land")));
        sedData.setArbeidsgivendeVirkomsheter(Lists.newArrayList(new Virksomhet("navn", "orgnr", new StrukturertAdresse())));
        sedData.setBostedsadresse(new Bostedsadresse());
        sedData.getBostedsadresse().setGateadresse(new Gateadresse());
        sedData.setSelvstendigeVirksomheter(Collections.singletonList(
            new Virksomhet("navn", "orgnr", mock(Adresse.class))));
    }

    @Test
    public void hentNavObjektTest() throws TekniskException, IOException {
        JsonNode root = objectMapper.readTree(objectMapper.writeValueAsString(sedMapper.mapTilSed(sedData)));
        assertNotNull(root);
        assertEquals(sedMapper.getSedType().name(), root.get("sed").textValue());

        JsonNode nav = root.get("nav");
        assertNotNull(nav);

        JsonNode bruker = nav.get("bruker");
        assertNotNull(bruker);

        JsonNode person = bruker.get("person");
        assertEquals("Ola", person.get("fornavn").textValue());
        assertEquals("K", person.get("kjoenn").textValue());

    }
}
