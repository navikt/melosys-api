package no.nav.melosys.service.dokument;

import java.util.Optional;

import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.behandlingsgrunnlag.Soeknad;
import no.nav.melosys.domain.behandlingsgrunnlag.data.Bosted;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.service.persondata.PersonopplysningerObjectFactory;
import org.junit.jupiter.api.Test;

import static no.nav.melosys.service.persondata.PersonopplysningerObjectFactory.lagPersonopplysninger;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

class BostedGrunnlagTest {
    private final Soeknad soeknad = new Soeknad();
    private final KodeverkService kodeverkService = mock(KodeverkService.class);

    private BostedGrunnlag bostedGrunnlag;

    @Test
    void hentBostedsadresse_forventStrukturertAdresse() {
        bostedGrunnlag = new BostedGrunnlag(soeknad, null, kodeverkService);

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
        bostedGrunnlag = new BostedGrunnlag(soeknad, null, kodeverkService);
        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> bostedGrunnlag.hentBostedsadresse())
            .withMessageContaining("finnes ikke eller mangler landkode");
    }

    @Test
    void finnBostedsadresse_oppgittAdresseOverstyrerRegister_nårOppgittAdresseISøknad() {
        bostedGrunnlag = new BostedGrunnlag(soeknad, PersonopplysningerObjectFactory.lagPersonopplysninger().bostedsadresse(), kodeverkService);
        StrukturertAdresse oppgittBosted = new StrukturertAdresse();
        oppgittBosted.setGatenavn("HerBorJegGata");
        oppgittBosted.setHusnummerEtasjeLeilighet("123");
        oppgittBosted.setPostnummer("0166");
        oppgittBosted.setPoststed("Oslo");
        oppgittBosted.setRegion("Østlandet");
        oppgittBosted.setLandkode("NO");
        soeknad.bosted = new Bosted();
        soeknad.bosted.oppgittAdresse = oppgittBosted;

        Optional<StrukturertAdresse> strukturertAdresse = bostedGrunnlag.finnBostedsadresse();

        assertThat(strukturertAdresse).isPresent();
        final StrukturertAdresse bostedsadresse = strukturertAdresse.get();
        assertThat(bostedsadresse.getGatenavn()).isEqualTo("HerBorJegGata");
        assertThat(bostedsadresse.getHusnummerEtasjeLeilighet()).isEqualTo("123");
        assertThat(bostedsadresse.getPostnummer()).isEqualTo("0166");
        assertThat(bostedsadresse.getPoststed()).isEqualTo("Oslo");
        assertThat(bostedsadresse.getRegion()).isEqualTo("Østlandet");
        assertThat(bostedsadresse.getLandkode()).isEqualTo(Landkoder.NO.getKode());
    }

    @Test
    void finnBostedsadresse_harBostedsadresseIRegister_forventBostedsadresse() {
        bostedGrunnlag = new BostedGrunnlag(soeknad, PersonopplysningerObjectFactory.lagPersonopplysninger().bostedsadresse(), kodeverkService);

        Optional<StrukturertAdresse> strukturertAdresse = bostedGrunnlag.finnBostedsadresse();

        assertThat(strukturertAdresse).isPresent();
        assertThat(strukturertAdresse.get().getGatenavn()).isEqualTo("gatenavnFraBostedsadresse");
        assertThat(strukturertAdresse.get().getLandkode()).isEqualTo("NO");
    }

    @Test
    void finnBostedsadresse_ingenAdresse_forventTomOptional() {
        bostedGrunnlag = new BostedGrunnlag(soeknad, null, kodeverkService);
        Optional<StrukturertAdresse> strukturertAdresse = bostedGrunnlag.finnBostedsadresse();
        assertThat(strukturertAdresse).isEmpty();
    }

    @Test
    void finnBostedsadresse_bostedsadresseFraPersonOpplysninger_forventBostedsadresse() {
        final var personopplysninger = lagPersonopplysninger();
        final var bostedGrunnlag = new BostedGrunnlag(null, personopplysninger.bostedsadresse(), kodeverkService);

        Optional<StrukturertAdresse> strukturertAdresse = bostedGrunnlag.finnBostedsadresse();

        assertThat(strukturertAdresse).isPresent();
        StrukturertAdresse adresse = strukturertAdresse.get();
        assertThat(adresse.getGatenavn()).isEqualTo("gatenavnFraBostedsadresse");
        assertThat(adresse.getLandkode()).isEqualTo("NO");
        assertThat(adresse.getPostnummer()).isEqualTo("1234");
        assertThat(adresse.getPoststed()).isEqualTo("Oslo");
        assertThat(adresse.getRegion()).isEqualTo("Norge");
    }
}
