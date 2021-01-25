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
import no.nav.melosys.exception.MelosysException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class SedGrunnlagMapperTest {

    @Test
    void mapSedGrunnlag() throws MelosysException, IOException, URISyntaxException {
        SedGrunnlag sedGrunnlag = SedGrunnlagMapper.tilSedGrunnlag(lagSedGrunnlag());

        assertThat(sedGrunnlag)
            .isNotNull()
            .isInstanceOf(SedGrunnlag.class);

        assertThat(sedGrunnlag.bosted.oppgittAdresse)
            .extracting(
                strukturertAdresse -> strukturertAdresse.landkode,
                strukturertAdresse -> strukturertAdresse.postnummer,
                strukturertAdresse -> strukturertAdresse.poststed)
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

    private SedGrunnlagDto lagSedGrunnlag() throws IOException, URISyntaxException {
        URI uri = Objects.requireNonNull(getClass().getClassLoader().getResource("eessi/sedGrunnlag.json")).toURI();
        String json = new String(Files.readAllBytes(Paths.get(uri)));
        return new ObjectMapper().readValue(json, SedGrunnlagDto.class);
    }
}