package no.nav.melosys.service.eessi;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.getunleash.FakeUnleash;
import no.nav.melosys.domain.mottatteopplysninger.SedGrunnlag;
import no.nav.melosys.domain.eessi.sed.SedGrunnlagDto;
import no.nav.melosys.domain.mottatteopplysninger.data.ForetakUtland;
import no.nav.melosys.domain.mottatteopplysninger.data.UtenlandskIdent;
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.FysiskArbeidssted;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class SedGrunnlagMapperTest {
    private final FakeUnleash fakeUnleash = new FakeUnleash();

    @Test
    void mapSedGrunnlag() throws IOException, URISyntaxException {
        SedGrunnlag sedGrunnlag = SedGrunnlagMapper.tilSedGrunnlag(lagSedGrunnlag("eessi/sedGrunnlag.json"), fakeUnleash);

        assertThat(sedGrunnlag)
            .isNotNull()
            .isInstanceOf(SedGrunnlag.class);

        assertThat(sedGrunnlag.personOpplysninger.getUtenlandskIdent())
            .extracting(
                UtenlandskIdent::getIdent,
                UtenlandskIdent::getLandkode)
            .containsExactly(tuple("15225345345", "BG"));

        assertThat(sedGrunnlag.arbeidPaaLand.getFysiskeArbeidssteder())
            .extracting(FysiskArbeidssted::getVirksomhetNavn)
            .containsExactlyInAnyOrder(
                "Testarbeidsstednavn",
                "Testarbeidsstednavn2"
            );

        assertThat(sedGrunnlag.juridiskArbeidsgiverNorge.getEkstraArbeidsgivere())
            .containsExactlyInAnyOrder(
                "115511",
                "226622",
                "finner ikke orgnummer så vi sender uten"
            );

        assertThat(sedGrunnlag.foretakUtland)
            .extracting(foretakUtland -> foretakUtland.getOrgnr())
            .containsExactly(
                "923609016",
                "123321",
                "123",
                "Testselvstendignummer"
            );
    }

    @Test
    void lagSedGrunnlagA001() throws Exception{
        SedGrunnlag sedGrunnlag = SedGrunnlagMapper.tilSedGrunnlag(lagSedGrunnlag("eessi/sedGrunnlagA001.json"), fakeUnleash);

        assertThat(sedGrunnlag)
            .isNotNull()
            .isInstanceOf(SedGrunnlag.class);

        assertThat(sedGrunnlag.personOpplysninger.getUtenlandskIdent())
            .extracting(
                UtenlandskIdent::getIdent,
                UtenlandskIdent::getLandkode)
            .containsExactly(tuple("15225345345", "BG"));

        assertThat(sedGrunnlag.arbeidPaaLand.getFysiskeArbeidssteder())
            .extracting(FysiskArbeidssted::getVirksomhetNavn)
            .containsExactlyInAnyOrder(
                "Testarbeidsstednavn",
                "Testarbeidsstednavn2"
            );

        assertThat(sedGrunnlag.foretakUtland)
            .extracting(ForetakUtland::getOrgnr)
            .containsExactly(
                "TestOrgnummer",
                "Testselvstendignummer"
            );
    }

    private SedGrunnlagDto lagSedGrunnlag(String filename) throws IOException, URISyntaxException {
        URI uri = Objects.requireNonNull(getClass().getClassLoader().getResource(filename)).toURI();
        String json = new String(Files.readAllBytes(Paths.get(uri)));
        return new ObjectMapper().readValue(json, SedGrunnlagDto.class);
    }
}
