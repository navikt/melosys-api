package no.nav.melosys.integrasjon.eessi.dto;

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

public class SedGrunnlagDtoTest {

    @Test
    public void tilDomene() throws MelosysException, IOException, URISyntaxException {
        SedGrunnlag sedGrunnlag = lagSedGrunnlag().tilDomene();

        assertThat(sedGrunnlag).isNotNull();
        assertThat(sedGrunnlag).isInstanceOf(SedGrunnlag.class);

        assertThat(sedGrunnlag.bosted)
            .extracting("oppgittAdresse")
            .extracting("landkode", "postnummer", "poststed")
            .containsExactly("BE", "Testpostkode", "Testby");

        assertThat(sedGrunnlag.personOpplysninger.utenlandskIdent)
            .extracting("ident", "landkode")
            .containsExactly(tuple("15225345345", "BG"));

        assertThat(sedGrunnlag.arbeidUtland)
            .extracting("foretakNavn", "foretakOrgnr")
            .containsExactlyInAnyOrder(
                tuple("Testarbeidsstednavn", null),
                tuple("Testarbeidsstednavn2", null)
            );
    }

    private SedGrunnlagDto lagSedGrunnlag() throws IOException, URISyntaxException {
        URI uri = Objects.requireNonNull(getClass().getClassLoader().getResource("mock/eux/sedGrunnlag.json")).toURI();
        String json = new String(Files.readAllBytes(Paths.get(uri)));
        return new ObjectMapper().readValue(json, SedGrunnlagDto.class);
    }
}
