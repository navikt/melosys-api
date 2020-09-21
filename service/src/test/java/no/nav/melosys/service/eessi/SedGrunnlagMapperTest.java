package no.nav.melosys.service.eessi;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.domain.behandlingsgrunnlag.SedGrunnlag;
import no.nav.melosys.domain.eessi.SedOrganisasjon;
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

        assertThat(sedGrunnlag.arbeidUtland)
            .extracting(
                arbeidUtland -> arbeidUtland.foretakNavn,
                arbeidUtland -> arbeidUtland.foretakOrgnr)
            .containsExactlyInAnyOrder(
                tuple("Testarbeidsstednavn", null),
                tuple("Testarbeidsstednavn2", null)
            );

        assertThat(sedGrunnlag.norskeArbeidsgivere)
            .extracting(SedOrganisasjon::getNavn, SedOrganisasjon::getOrgnummer)
            .containsExactlyInAnyOrder(
                tuple("norsk", "115511"),
                tuple("annen norsk", "226622"),
                tuple("finner ikke orgnummer så vi sender uten", null)
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