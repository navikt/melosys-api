package no.nav.melosys.service.dokument.brev.datagrunnlag;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.Soeknad;
import no.nav.melosys.domain.behandlingsgrunnlag.data.arbeidssteder.MaritimtArbeid;
import no.nav.melosys.domain.brev.DoksysBrevbestilling;
import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.person.adresse.Bostedsadresse;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.yrker.Yrkesgrupper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.avklartefakta.AvklartMaritimtArbeid;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.Arbeidssted;
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.MaritimtArbeidssted;
import no.nav.melosys.service.kodeverk.KodeverkService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static no.nav.melosys.service.dokument.brev.BrevDataTestUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BrevDataGrunnlagTest {
    @Mock
    private KodeverkService kodeverkService;
    @Mock
    private AvklartefaktaService avklartefaktaService;

    private DoksysBrevbestilling brevbestilling;
    private Behandling behandling;
    private PersonDokument person;
    private Soeknad søknad;
    private BrevDataGrunnlag dataGrunnlag;

    @BeforeEach
    public void setUp() {
        AvklartMaritimtArbeid maritimtArbeid = lagAvklartMaritimtArbeid();
        when(avklartefaktaService.hentMaritimeAvklartfaktaEtterSubjekt(anyLong()))
            .thenReturn(Collections.singletonMap("Dunfjæder", maritimtArbeid));
        when(kodeverkService.dekod(any(), any(), any())).thenReturn("Oslo");

        Bostedsadresse boAdresseFraRegister = new Bostedsadresse();
        boAdresseFraRegister.getGateadresse().setGatenavn("Hjemgata");
        boAdresseFraRegister.getGateadresse().setHusnummer(23);
        boAdresseFraRegister.setPostnr("0165");
        boAdresseFraRegister.setPoststed("Oslo");
        boAdresseFraRegister.setLand(new Land(Land.NORGE));

        person = new PersonDokument();
        person.setBostedsadresse(boAdresseFraRegister);

        søknad = new Soeknad();
        behandling = lagBehandling(søknad, person);

        brevbestilling = new DoksysBrevbestilling.Builder().medBehandling(behandling).build();
        dataGrunnlag = new BrevDataGrunnlag(brevbestilling, kodeverkService, mock(AvklarteVirksomheterService.class), avklartefaktaService);
    }

    private Behandling lagBehandling(Soeknad søknad, PersonDokument person) {
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.getSaksopplysninger().add(lagPersonsaksopplysning(person));

        Behandlingsgrunnlag behandlingsgrunnlag = new Behandlingsgrunnlag();
        behandlingsgrunnlag.setBehandlingsgrunnlagdata(søknad);
        behandling.setBehandlingsgrunnlag(behandlingsgrunnlag);
        return behandling;
    }

    @Test
    void hentBostedsadresse_manglerOppgittOgTpsBostedsadresse_girUnntak() {
        person.setBostedsadresse(new Bostedsadresse());
        søknad.bosted.oppgittAdresse = new StrukturertAdresse();
        var bostedsgrunnlag = dataGrunnlag.getBostedGrunnlag();

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(bostedsgrunnlag::hentBostedsadresse)
            .withMessageContaining("finnes ikke");
    }

    @Test
    void hentBostedsadresse_brukerBostedFraPersonDokument() {
        StrukturertAdresse bostedsadresse = dataGrunnlag.getBostedGrunnlag().hentBostedsadresse();
        assertThat(bostedsadresse.gatenavn).isEqualTo("Hjemgata");
        assertThat(bostedsadresse.husnummer).isEqualTo("23");
        assertThat(bostedsadresse.postnummer).isEqualTo("0165");
        assertThat(bostedsadresse.poststed).isEqualTo("Oslo");
        assertThat(bostedsadresse.landkode).isEqualTo(Landkoder.NO.getKode());
    }

    @Test
    void hentBostedsadresse_oppgittAdresseOverstyrerTPS_nårOppgittAdresseISøknad() {
        StrukturertAdresse oppgittBosted = new StrukturertAdresse();
        oppgittBosted.gatenavn = "HerBorJegGata";
        oppgittBosted.husnummer = "123";
        oppgittBosted.postnummer = "0166";
        oppgittBosted.poststed = "Oslo";
        oppgittBosted.region = "Østlandet";
        oppgittBosted.landkode = "NO";
        søknad.bosted.oppgittAdresse = oppgittBosted;

        StrukturertAdresse bostedsadresse = dataGrunnlag.getBostedGrunnlag().hentBostedsadresse();
        assertThat(bostedsadresse.gatenavn).isEqualTo("HerBorJegGata");
        assertThat(bostedsadresse.husnummer).isEqualTo("123");
        assertThat(bostedsadresse.postnummer).isEqualTo("0166");
        assertThat(bostedsadresse.poststed).isEqualTo("Oslo");
        assertThat(bostedsadresse.region).isEqualTo("Østlandet");
        assertThat(bostedsadresse.landkode).isEqualTo(Landkoder.NO.getKode());
    }

    @Test
    void hentArbeidssteder_medMaritimtArbeid_girMaritimeArbeidssteder() {
        MaritimtArbeid maritimtArbeidISøknad = lagMaritimtArbeid();
        this.søknad.maritimtArbeid.add(maritimtArbeidISøknad);

        List<Arbeidssted> arbeidssteder = dataGrunnlag.getArbeidsstedGrunnlag().hentArbeidssteder();
        assertThat(arbeidssteder.size()).isEqualTo(1);

        MaritimtArbeidssted arbeidssted = (MaritimtArbeidssted) arbeidssteder.get(0);
        assertThat(arbeidssted.getEnhetNavn()).isEqualTo(maritimtArbeidISøknad.enhetNavn);
        assertThat(arbeidssted.getForetakNavn()).isNullOrEmpty();
        assertThat(arbeidssted.getIdnummer()).isNullOrEmpty();
        assertThat(arbeidssted.getYrkesgruppe().getKode()).isEqualTo(Yrkesgrupper.SOKKEL_ELLER_SKIP.getKode());
    }

    @Test
    void hentArbeidssteder_medMaritimtArbeidUtenAvklartMaritimtArbeid_girTomListe() {
        MaritimtArbeid maritimtArbeidISøknad = lagMaritimtArbeid();
        this.søknad.maritimtArbeid.add(maritimtArbeidISøknad);

        when(avklartefaktaService.hentMaritimeAvklartfaktaEtterSubjekt(anyLong())).thenReturn(Collections.emptyMap());
        final BrevDataGrunnlag dataGrunnlagUtenAvklartMaritimtArbeid = new BrevDataGrunnlag(
            brevbestilling,
            kodeverkService,
            mock(AvklarteVirksomheterService.class),
            avklartefaktaService
        );

        Collection<Arbeidssted> arbeidssteder = dataGrunnlagUtenAvklartMaritimtArbeid.getArbeidsstedGrunnlag().hentArbeidssteder();
        assertThat(arbeidssteder).isEmpty();
    }
}
