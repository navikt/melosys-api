package no.nav.melosys.domain.dokument.soeknad;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.Ignore;
import org.junit.Test;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class SoeknadDokumentTest {

    //Testen bruktes til å validere json søknaden som kom fra frontend
    @Ignore
    @Test
    public void lesSoeknad() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.registerModule(new JavaTimeModule());

        mapper.enable(DeserializationFeature.UNWRAP_ROOT_VALUE);
        mapper.enable(SerializationFeature.WRAP_ROOT_VALUE);


        String json = "{\"soknadDokument\":{\"arbeidUtland\":{\"arbeidsland\":[\"ES\",\"DK\"],\"arbeidsperiode\":{\"fom\":\"2018-01-01\",\"tom\":\"2018-06-01\"},\"arbeidsandelNorge\":33.3,\"arbeidsandelUtland\":66.6,\"arbeidsstedUtland\":null,\"bostedsland\":\"SE\",\"erstatterTidligereUtsendt\":false},\"foretakUtland\":{\"foretakUtlandNavn\":\"Volkswagen AG\",\"foretakUtlandOrgnr\":\"1122334444\",\"foretakUtlandAdresse\":null},\"oppholdUtland\":{\"oppholdsland\":\"DE\",\"oppholdsPeriode\":{\"fom\":\"2018-01-01\",\"tom\":\"2018-06-01\"},\"studentIEOS\":false,\"studentFinansiering\":\"Beskrivelse av hvordan studiene finansieres\",\"studentSemester\":\"2018/2019\",\"studieLand\":\"SE\"},\"arbeidNorge\":{\"arbeidsforholdOpprettholdIHelePerioden\":true,\"brukerErSelvstendigNaeringsdrivende\":true,\"selvstendigFortsetterEtterArbeidIUtlandet\":true,\"brukerArbeiderIVikarbyra\":\"false\",\"vikarOrgnr\":\"Ola Nordmann 22334455\",\"flyendePersonellHjemmebase\":\"Flybasen Int. Airport, ....\",\"ansattPaSokkelEllerSkip\":\"sokkel | skip\",\"navnSkipEllerSokkel\":\"Trym-sokkelen\",\"sokkelLand\":\"SE\",\"skipFartsomrade\":\"Europeisk fart\",\"skipFlaggLand\":\"SE\"},\"juridiskArbeidsgiverNorge\":{\"antallAnsatte\":350,\"antallAdminAnsatte\":250,\"antallAdminAnsatteEOS\":75,\"andelOmsetningINorge\":78.5,\"andelKontrakterINorge\":50.5,\"erBemanningsbyra\":false,\"hattDriftSiste12Mnd\":true,\"antallUtsendte\":30},\"arbeidsinntekt\":{\"inntektNorskIPerioden\":5500,\"inntektUtenlandskIPerioden\":2000,\"inntektNaeringIPerioden\":0,\"inntektNaturalytelser\": {\"friBolig\": true, \"friBil\": false, \"friAnnet\": null }, \"inntektErInnrapporteringspliktig\":true,\"inntektTrygdeavgiftBlirTrukket\":true},\"tilleggsopplysninger\":\"Lang utgreiing om utsendelsen som egentlig ikke er relevant for saksbehandlingen...\",\"familiemedlemmer\":[{\"fnr\":\"99999999997\",\"navn\":\"STOR BÆREPOSE\",\"familierelasjon\":\"BARN\"}],\"sivilstand\":\"Gift, lever adskilt\"}}";

        SoeknadDokument dokument = mapper.readValue(json, SoeknadDokument.class);

        String pp = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(dokument);
        System.out.println(pp);

    }

    @Test
    public void hentAllePersonnumre() {
        String personnummer1 = "12345678910";
        String personnummer2 = "10987654321";

        SoeknadDokument soeknadDokument = new SoeknadDokument();
        soeknadDokument.personOpplysninger.medfolgendeAndre = personnummer1;
        soeknadDokument.personOpplysninger.medfolgendeFamilie = Arrays.asList(personnummer2);

        Set<String> personnumre = soeknadDokument.hentAllePersonnumre();
        assertThat(personnumre.size()).isEqualTo(2);
        assertThat(personnumre.containsAll(Arrays.asList(personnummer1, personnummer2)));
    }

    @Test
    public void hentAllePersonnumreKunFamilie() {
        String personnummer1 = "12345678910";

        SoeknadDokument soeknadDokument = new SoeknadDokument();
        soeknadDokument.personOpplysninger.medfolgendeFamilie = Arrays.asList(personnummer1);

        Set<String> personnumre = soeknadDokument.hentAllePersonnumre();
        assertThat(personnumre.size()).isEqualTo(1);
        assertThat(personnumre.contains(personnummer1));
    }

    @Test
    public void hentAlleOrganisasjonsnumre() {
        SelvstendigForetak selvstendigForetak = new SelvstendigForetak();
        selvstendigForetak.orgnr = "12345678910";

        SoeknadDokument soeknadDokument = new SoeknadDokument();
        soeknadDokument.selvstendigArbeid.selvstendigForetak = Arrays.asList(selvstendigForetak);

        String orgNr2 = "10987654321";
        soeknadDokument.juridiskArbeidsgiverNorge.ekstraArbeidsgivere.add("10987654321");

        Set<String> organisasjonsnumre = soeknadDokument.hentAlleOrganisasjonsnumre();
        assertThat(organisasjonsnumre.size()).isEqualTo(2);
        assertThat(organisasjonsnumre.contains(selvstendigForetak.orgnr));
        assertThat(organisasjonsnumre.contains(orgNr2));
    }
}