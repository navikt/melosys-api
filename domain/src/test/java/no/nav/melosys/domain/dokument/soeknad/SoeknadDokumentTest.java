package no.nav.melosys.domain.dokument.soeknad;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

public class SoeknadDokumentTest {

    @Ignore //Testen bruktes til å validere json søknaden som kom fra frontend
    @Test
    public void lesSoeknad() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.registerModule(new JavaTimeModule());

        mapper.enable(DeserializationFeature.UNWRAP_ROOT_VALUE);
        mapper.enable(SerializationFeature.WRAP_ROOT_VALUE);


        String json = "{\"soknadDokument\":{\"arbeidUtland\":{\"arbeidsland\":[\"ES\",\"DK\"],\"arbeidsperiode\":{\"fom\":\"2018-01-01\",\"tom\":\"2018-06-01\"},\"arbeidsandelNorge\":33.3,\"arbeidsandelUtland\":66.6,\"arbeidsstedUtland\":null,\"bostedsland\":\"SE\",\"erstatterTidligereUtsendt\":false},\"foretakUtland\":{\"foretakUtlandNavn\":\"Volkswagen AG\",\"foretakUtlandOrgnr\":\"1122334444\",\"foretakUtlandAdresse\":null},\"oppholdUtland\":{\"oppholdsland\":\"DE\",\"oppholdsPeriode\":{\"fom\":\"2018-01-01\",\"tom\":\"2018-06-01\"},\"studentIEOS\":false,\"studentFinansiering\":\"Beskrivelse av hvordan studiene finansieres\",\"studentSemester\":\"2018/2019\",\"studieLand\":\"SE\"},\"arbeidNorge\":{\"arbeidsforholdOpprettholdIHelePerioden\":true,\"brukerErSelvstendigNaeringsdrivende\":true,\"selvstendigFortsetterEtterArbeidIUtlandet\":true,\"brukerArbeiderIVikarbyra\":\"false\",\"vikarOrgnr\":\"Ola Nordmann 22334455\",\"flyendePersonellHjemmebase\":\"Flybasen Int. Airport, ....\",\"ansattPaSokkelEllerSkip\":\"sokkel | skip\",\"navnSkipEllerSokkel\":\"Trym-sokkelen\",\"sokkelLand\":\"SE\",\"skipFartsomrade\":\"Europeisk fart\",\"skipFlaggLand\":\"SE\"},\"juridiskArbeidsgiverNorge\":{\"antallAnsatte\":350,\"antallAdminAnsatte\":250,\"antallAdminAnsatteEOS\":75,\"andelOmsetningINorge\":78.5,\"andelKontrakterINorge\":50.5,\"erBemanningsbyra\":false,\"hattDriftSiste12Mnd\":true,\"antallUtsendte\":30},\"arbeidsinntekt\":{\"inntektNorskIPerioden\":5500,\"inntektUtenlandskIPerioden\":2000,\"inntektNaeringIPerioden\":0,\"inntektNaturalYtelser\":[\"Fri bolig\",\"Fri bil\"],\"inntektErInnrapporteringspliktig\":true,\"inntektTrygdeavgiftBlirTrukket\":true},\"tilleggsopplysninger\":\"Lang utgreiing om utsendelsen som egentlig ikke er relevant for saksbehandlingen...\"}}";

        SoeknadDokument dokument = mapper.readValue(json, SoeknadDokument.class);

        String pp = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(dokument);
        System.out.println(pp);

    }

}