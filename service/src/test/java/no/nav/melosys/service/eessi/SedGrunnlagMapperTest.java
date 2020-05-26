package no.nav.melosys.service.eessi;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.domain.behandlingsgrunnlag.SedGrunnlag;
import no.nav.melosys.domain.eessi.sed.SedGrunnlagDto;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SedGrunnlagMapperTest {

    @Mock
    private EregFasade eregFasade;

    private SedGrunnlagMapper sedGrunnlagMapper;

    @Before
    public void setUp() {
        sedGrunnlagMapper = new SedGrunnlagMapper(eregFasade);
    }

    @Test
    public void mapSedGrunnlag() throws MelosysException, IOException, URISyntaxException {
        SedGrunnlag sedGrunnlag = sedGrunnlagMapper.mapSedGrunnlag(lagSedGrunnlag());

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

    @Test
    public void mapSedGrunnlag_orgnummerFinnes_forventJuridiskArbeidsgiverNorge() throws IntegrasjonException, IOException, URISyntaxException, FunksjonellException {
        when(eregFasade.organisasjonFinnes(eq("115511"))).thenReturn(true);

        SedGrunnlag sedGrunnlag = sedGrunnlagMapper.mapSedGrunnlag(lagSedGrunnlag());

        verify(eregFasade).organisasjonFinnes(eq("115511"));
        verify(eregFasade).organisasjonFinnes(eq("226622"));

        assertThat(sedGrunnlag.juridiskArbeidsgiverNorge).isNotNull();
        assertThat(sedGrunnlag.juridiskArbeidsgiverNorge.hentManueltRegistrerteArbeidsgiverOrgnumre()).containsExactly("115511");
        assertThat(sedGrunnlag.foretakUtland).hasSize(6);
        assertThat(sedGrunnlag.foretakUtland).extracting("orgnr").doesNotContain("115511");
    }

    @Test
    public void mapSedGrunnlag_orgnummerFinnesIkke_forventUtenlandskeForetak() throws IntegrasjonException, IOException, URISyntaxException, FunksjonellException {
        SedGrunnlag sedGrunnlag = sedGrunnlagMapper.mapSedGrunnlag(lagSedGrunnlag());

        verify(eregFasade).organisasjonFinnes(eq("115511"));
        verify(eregFasade).organisasjonFinnes(eq("226622"));

        assertThat(sedGrunnlag.juridiskArbeidsgiverNorge).isNotNull();
        assertThat(sedGrunnlag.juridiskArbeidsgiverNorge.hentManueltRegistrerteArbeidsgiverOrgnumre()).isEmpty();
        assertThat(sedGrunnlag.foretakUtland).hasSize(7);
        assertThat(sedGrunnlag.foretakUtland).extracting("orgnr").containsAll(List.of("115511", "226622"));
    }

    private SedGrunnlagDto lagSedGrunnlag() throws IOException, URISyntaxException {
        URI uri = Objects.requireNonNull(getClass().getClassLoader().getResource("eessi/sedGrunnlag.json")).toURI();
        String json = new String(Files.readAllBytes(Paths.get(uri)));
        return new ObjectMapper().readValue(json, SedGrunnlagDto.class);
    }
}