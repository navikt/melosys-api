package no.nav.melosys.service.dokument;

import java.util.Optional;

import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.behandlingsgrunnlag.Soeknad;
import no.nav.melosys.domain.behandlingsgrunnlag.data.Bosted;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.kodeverk.KodeverkService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

class BostedGrunnlagTest {
    private final Soeknad soeknad = new Soeknad();
    private BostedGrunnlag bostedGrunnlag;
    private final KodeverkService kodeverkService = mock(KodeverkService.class);

    @BeforeEach
    public void setup() {
        bostedGrunnlag = new BostedGrunnlag(soeknad, null, kodeverkService);
    }

    @Test
    void hentBostedsadresse_forventStrukturertAdresse() {
        soeknad.bosted = new Bosted();
        soeknad.bosted.oppgittAdresse = new StrukturertAdresse();
        soeknad.bosted.oppgittAdresse.setLandkode("SE");
        soeknad.bosted.oppgittAdresse.setGatenavn("gate");

        StrukturertAdresse strukturertAdresse = bostedGrunnlag.hentBostedsadresse();

        assertThat(strukturertAdresse.getGatenavn()).isEqualTo("gate");
        assertThat(strukturertAdresse.getLandkode()).isEqualTo("SE");
    }

    @Test
    void hentBostedsadresse_ingenAdresse_forventException() {
        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> bostedGrunnlag.hentBostedsadresse())
            .withMessageContaining("finnes ikke eller mangler landkode");
    }

    @Test
    void finnBostedsadresse_harBostedsadresse_forventBostedsadresse() {
        soeknad.bosted = new Bosted();
        soeknad.bosted.oppgittAdresse = new StrukturertAdresse();
        soeknad.bosted.oppgittAdresse.setLandkode("SE");
        soeknad.bosted.oppgittAdresse.setGatenavn("gate");

        Optional<StrukturertAdresse> strukturertAdresse = bostedGrunnlag.finnBostedsadresse();

        assertThat(strukturertAdresse).isPresent();
        assertThat(strukturertAdresse.get().getGatenavn()).isEqualTo("gate");
        assertThat(strukturertAdresse.get().getLandkode()).isEqualTo("SE");
    }

    @Test
    void finnBostedsadresse_harBostedsadresseIRegister_forventBostedsadresse() {
        var bostedsadresse = new no.nav.melosys.domain.person.adresse.Bostedsadresse(
            new StrukturertAdresse("gate", null, null, null, null, "SE"),
            null, null, null, null, null, false);
        var bostedGrunnlag = new BostedGrunnlag(soeknad, bostedsadresse, kodeverkService);

        Optional<StrukturertAdresse> strukturertAdresse = bostedGrunnlag.finnBostedsadresse();

        assertThat(strukturertAdresse).isPresent();
        assertThat(strukturertAdresse.get().getGatenavn()).isEqualTo("gate");
        assertThat(strukturertAdresse.get().getLandkode()).isEqualTo("SE");
    }

    @Test
    void finnBostedsadresse_ingenAdresse_forventTomOptional() {
        Optional<StrukturertAdresse> strukturertAdresse = bostedGrunnlag.finnBostedsadresse();
        assertThat(strukturertAdresse).isEmpty();
    }
}
