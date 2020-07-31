package no.nav.melosys.service.dokument;

import java.util.Optional;

import no.nav.melosys.domain.dokument.adresse.StrukturertAdresse;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.Gateadresse;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.Bosted;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.kodeverk.KodeverkService;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class BostedGrunnlagTest {
    private SoeknadDokument soeknadDokument = new SoeknadDokument();
    private PersonDokument personDokument = new PersonDokument();
    private BostedGrunnlag bostedGrunnlag;

    @Before
    public void setup() {
        KodeverkService kodeverkService = mock(KodeverkService.class);
        bostedGrunnlag = new BostedGrunnlag(soeknadDokument, personDokument, kodeverkService);
    }

    @Test
    public void hentBostedsadresse_forventStrukturertAdresse() throws FunksjonellException {
        soeknadDokument.bosted = new Bosted();
        soeknadDokument.bosted.oppgittAdresse = new StrukturertAdresse();
        soeknadDokument.bosted.oppgittAdresse.landkode = "SE";
        soeknadDokument.bosted.oppgittAdresse.gatenavn = "gate";

        StrukturertAdresse strukturertAdresse = bostedGrunnlag.hentBostedsadresse();

        assertThat(strukturertAdresse.gatenavn).isEqualTo("gate");
        assertThat(strukturertAdresse.landkode).isEqualTo("SE");
    }

    @Test(expected = FunksjonellException.class)
    public void hentBostedsadresse_ingenAdresse_forventException() throws  FunksjonellException {
        bostedGrunnlag.hentBostedsadresse();
    }

    @Test
    public void finnBostedsadresse_harBostedsadresse_forventBostedsadresse() {
        soeknadDokument.bosted = new Bosted();
        soeknadDokument.bosted.oppgittAdresse = new StrukturertAdresse();
        soeknadDokument.bosted.oppgittAdresse.landkode = "SE";
        soeknadDokument.bosted.oppgittAdresse.gatenavn = "gate";

        Optional<StrukturertAdresse> strukturertAdresse = bostedGrunnlag.finnBostedsadresse();

        assertThat(strukturertAdresse.isPresent()).isTrue();
        assertThat(strukturertAdresse.get().gatenavn).isEqualTo("gate");
        assertThat(strukturertAdresse.get().landkode).isEqualTo("SE");
    }

    @Test
    public void finnBostedsadresse_harBostedsadresseIRegister_forventBostedsadresse() {
        personDokument.bostedsadresse = new no.nav.melosys.domain.dokument.person.Bostedsadresse();
        personDokument.bostedsadresse.setLand(new Land("SWE"));
        personDokument.bostedsadresse.setGateadresse(new Gateadresse());
        personDokument.bostedsadresse.getGateadresse().setGatenavn("gate");

        Optional<StrukturertAdresse> strukturertAdresse = bostedGrunnlag.finnBostedsadresse();

        assertThat(strukturertAdresse.isPresent()).isTrue();
        assertThat(strukturertAdresse.get().gatenavn).isEqualTo("gate");
        assertThat(strukturertAdresse.get().landkode).isEqualTo("SE");
    }

    @Test
    public void finnBostedsadresse_ingenAdresse_forventTomOptional() {
        Optional<StrukturertAdresse> strukturertAdresse = bostedGrunnlag.finnBostedsadresse();
        assertThat(strukturertAdresse.isPresent()).isFalse();
    }
}