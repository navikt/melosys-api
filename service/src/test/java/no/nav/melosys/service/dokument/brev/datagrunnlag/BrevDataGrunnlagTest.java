package no.nav.melosys.service.dokument.brev.datagrunnlag;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.avklartefakta.AvklartFamiliemedlem;
import no.nav.melosys.domain.avklartefakta.AvklarteMedfolgendeFamiliemedlemmer;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.brev.Brevbestilling;
import no.nav.melosys.domain.dokument.adresse.StrukturertAdresse;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.Bostedsadresse;
import no.nav.melosys.domain.dokument.person.Familiemedlem;
import no.nav.melosys.domain.dokument.person.Familierelasjon;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.behandlingsgrunnlag.soeknad.MaritimtArbeid;
import no.nav.melosys.domain.behandlingsgrunnlag.Soeknad;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.begrunnelser.Medfolgende_barn_begrunnelser;
import no.nav.melosys.domain.kodeverk.yrker.Yrkesgrupper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.avklartefakta.AvklartMaritimtArbeid;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.brev.IkkeOmfattetBarn;
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.Arbeidssted;
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.MaritimtArbeidssted;
import no.nav.melosys.service.kodeverk.KodeverkService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.service.dokument.brev.BrevDataTestUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class BrevDataGrunnlagTest {
    @Mock
    private KodeverkService kodeverkService;
    @Mock
    private AvklartefaktaService avklartefaktaService;

    private Brevbestilling brevbestilling;
    private Behandling behandling;
    private PersonDokument person;
    private Soeknad søknad;
    private BrevDataGrunnlag dataGrunnlag;

    @Before
    public void setUp() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
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
        person.bostedsadresse = boAdresseFraRegister;

        søknad = new Soeknad();
        behandling = lagBehandling(søknad, person);

        brevbestilling = new Brevbestilling.Builder().medBehandling(behandling).build();
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

    @Test(expected = FunksjonellException.class)
    public void hentBostedsadresse_manglerOppgittOgTpsBostedsadresse_girUnntak() throws FunksjonellException {
        person.bostedsadresse = new Bostedsadresse();
        søknad.bosted.oppgittAdresse = new StrukturertAdresse();
        dataGrunnlag.getBostedGrunnlag().hentBostedsadresse();
    }

    @Test
    public void hentBostedsadresse_brukerBostedFraPersonDokument() throws FunksjonellException {
        StrukturertAdresse bostedsadresse = dataGrunnlag.getBostedGrunnlag().hentBostedsadresse();
        assertThat(bostedsadresse.gatenavn).isEqualTo("Hjemgata");
        assertThat(bostedsadresse.husnummer).isEqualTo("23");
        assertThat(bostedsadresse.postnummer).isEqualTo("0165");
        assertThat(bostedsadresse.poststed).isEqualTo("Oslo");
        assertThat(bostedsadresse.landkode).isEqualTo(Landkoder.NO.getKode());
    }

    @Test
    public void hentBostedsadresse_oppgittAdresseOverstyrerTPS_nårOppgittAdresseISøknad() throws FunksjonellException {
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
    public void hentArbeidssteder_medMaritimtArbeid_girMaritimeArbeidssteder() {
        MaritimtArbeid maritimtArbeidISøknad = lagMaritimtArbeid();
        this.søknad.maritimtArbeid.add(maritimtArbeidISøknad);

        List<Arbeidssted> arbeidssteder = dataGrunnlag.getArbeidsstedGrunnlag().hentArbeidssteder();
        assertThat(arbeidssteder.size()).isEqualTo(1);

        MaritimtArbeidssted arbeidssted = (MaritimtArbeidssted) arbeidssteder.get(0);
        assertThat(arbeidssted.getForetakNavn()).isEqualTo(maritimtArbeidISøknad.foretakNavn);
        assertThat(arbeidssted.getEnhetNavn()).isEqualTo(maritimtArbeidISøknad.enhetNavn);
        assertThat(arbeidssted.getIdnummer()).isEqualTo(maritimtArbeidISøknad.foretakOrgnr);
        assertThat(arbeidssted.getYrkesgruppe().getKode()).isEqualTo(Yrkesgrupper.SOKKEL_ELLER_SKIP.getKode());
    }

    @Test
    public void hentArbeidssteder_medMaritimtArbeidUtenForetak_girMaritimeArbeidssteder() {
        MaritimtArbeid maritimtArbeidISøknad = lagMaritimtArbeid();
        maritimtArbeidISøknad.foretakOrgnr = null;
        maritimtArbeidISøknad.foretakNavn = null;
        this.søknad.maritimtArbeid.add(maritimtArbeidISøknad);

        List<Arbeidssted> arbeidssteder = dataGrunnlag.getArbeidsstedGrunnlag().hentArbeidssteder();
        assertThat(arbeidssteder.size()).isEqualTo(1);

        MaritimtArbeidssted arbeidssted = (MaritimtArbeidssted) arbeidssteder.get(0);
        assertThat(arbeidssted.getForetakNavn()).isNullOrEmpty();
        assertThat(arbeidssted.getEnhetNavn()).isEqualTo("Dunfjæder");
        assertThat(arbeidssted.getIdnummer()).isNullOrEmpty();
    }

    @Test
    public void hentArbeidssteder_medMaritimtArbeidUtenAvklartMaritimtArbeid_girTomListe() throws TekniskException {
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

    @Test
    public void hentMedfølgendeFamilie() throws TekniskException {
        final Familiemedlem omfattet1 = lagBarn("123", "Per R. Omfattet");
        final Familiemedlem omfattet2 = lagBarn("456", "Pål R. Omfattet");
        final Familiemedlem utelatt = lagBarn("789", "Espen R. Utelatt");

        PersonDokument personMedFamilie = new PersonDokument();
        personMedFamilie.familiemedlemmer = List.of(omfattet1, omfattet2, utelatt);

        when(avklartefaktaService.hentAvklarteMedfølgendeFamiliemedlemmer(anyLong())).thenReturn(
            new AvklarteMedfolgendeFamiliemedlemmer(
                List.of(
                    new AvklartFamiliemedlem(omfattet1.fnr, null),
                    new AvklartFamiliemedlem(omfattet2.fnr, null),
                    new AvklartFamiliemedlem(utelatt.fnr, Medfolgende_barn_begrunnelser.OVER_18_AR.getKode())
                ),
                "Begrunnelse"
            )
        );

        Brevbestilling brevbestilling = new Brevbestilling.Builder().medBehandling(lagBehandling(søknad, personMedFamilie)).build();
        BrevDataGrunnlag dataGrunnlag = new BrevDataGrunnlag(brevbestilling, kodeverkService, mock(AvklarteVirksomheterService.class), avklartefaktaService);

        assertThat(dataGrunnlag.getMedfolgendeFamilieGrunnlag().hentOmfattedeBarn()).containsExactly(omfattet1.navn, omfattet2.navn);
        assertThat(dataGrunnlag.getMedfolgendeFamilieGrunnlag().hentIkkeOmfattedeBarn())
            .extracting(IkkeOmfattetBarn::getNavn, IkkeOmfattetBarn::getBegrunnelse)
            .containsExactly(tuple(utelatt.navn, Medfolgende_barn_begrunnelser.OVER_18_AR));
        assertThat(dataGrunnlag.getMedfolgendeFamilieGrunnlag().hentBegrunnelse()).isEqualTo("Begrunnelse");
    }

    private Familiemedlem lagBarn(String fnr, String navn) {
        Familiemedlem familiemedlem = new Familiemedlem();
        familiemedlem.fnr = fnr;
        familiemedlem.navn = navn;
        familiemedlem.familierelasjon = Familierelasjon.BARN;
        return familiemedlem;
    }
}