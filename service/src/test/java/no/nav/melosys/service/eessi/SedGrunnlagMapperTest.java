package no.nav.melosys.service.eessi;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.domain.behandlingsgrunnlag.SedGrunnlag;
import no.nav.melosys.domain.eessi.sed.SedGrunnlagDto;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class SedGrunnlagMapperTest {

    @Test
    void mapSedGrunnlag() throws IOException, URISyntaxException {
        SedGrunnlag sedGrunnlag = SedGrunnlagMapper.tilSedGrunnlag(lagSedGrunnlag("eessi/sedGrunnlag.json"));

        assertThat(sedGrunnlag)
            .isNotNull()
            .isInstanceOf(SedGrunnlag.class);

        assertThat(sedGrunnlag.bosted.oppgittAdresse)
            .extracting(
                strukturertAdresse -> strukturertAdresse.getLandkode(),
                strukturertAdresse -> strukturertAdresse.getPostnummer(),
                strukturertAdresse -> strukturertAdresse.getPoststed())
            .containsExactly("BE", "Testpostkode", "Testby");

        assertThat(sedGrunnlag.personOpplysninger.utenlandskIdent)
            .extracting(
                utenlandskIdent -> utenlandskIdent.ident,
                utenlandskIdent -> utenlandskIdent.landkode)
            .containsExactly(tuple("15225345345", "BG"));

        assertThat(sedGrunnlag.arbeidPaaLand.fysiskeArbeidssteder)
            .extracting(arbeidssted -> arbeidssted.virksomhetNavn)
            .containsExactlyInAnyOrder(
                "Testarbeidsstednavn",
                "Testarbeidsstednavn2"
            );

        assertThat(sedGrunnlag.juridiskArbeidsgiverNorge.ekstraArbeidsgivere)
            .containsExactlyInAnyOrder(
                "115511",
                "226622",
                "finner ikke orgnummer så vi sender uten"
            );

        assertThat(sedGrunnlag.foretakUtland)
            .extracting(foretakUtland -> foretakUtland.orgnr)
            .containsExactly(
                "923609016",
                "123321",
                "123",
                "Testselvstendignummer"
            );
    }

    @Test
    void lagSedGrunnlagA001() throws Exception{
        SedGrunnlag sedGrunnlag = SedGrunnlagMapper.tilSedGrunnlag(lagSedGrunnlag("eessi/sedGrunnlagA001.json"));

        assertThat(sedGrunnlag)
            .isNotNull()
            .isInstanceOf(SedGrunnlag.class);

        assertThat(sedGrunnlag.bosted.oppgittAdresse)
            .extracting(
                strukturertAdresse -> strukturertAdresse.getLandkode(),
                strukturertAdresse -> strukturertAdresse.getPostnummer(),
                strukturertAdresse -> strukturertAdresse.getPoststed())
            .containsExactly("BE", "Testpostkode", "Testby");

        assertThat(sedGrunnlag.personOpplysninger.utenlandskIdent)
            .extracting(
                utenlandskIdent -> utenlandskIdent.ident,
                utenlandskIdent -> utenlandskIdent.landkode)
            .containsExactly(tuple("15225345345", "BG"));

        assertThat(sedGrunnlag.arbeidPaaLand.fysiskeArbeidssteder)
            .extracting(arbeidssted -> arbeidssted.virksomhetNavn)
            .containsExactlyInAnyOrder(
                "Testarbeidsstednavn",
                "Testarbeidsstednavn2"
            );

        assertThat(sedGrunnlag.foretakUtland)
            .extracting(foretakUtland -> foretakUtland.orgnr)
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
