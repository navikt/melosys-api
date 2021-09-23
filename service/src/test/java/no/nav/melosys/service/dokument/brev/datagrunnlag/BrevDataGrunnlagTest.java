package no.nav.melosys.service.dokument.brev.datagrunnlag;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import no.finn.unleash.FakeUnleash;
import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.Soeknad;
import no.nav.melosys.domain.behandlingsgrunnlag.data.arbeidssteder.MaritimtArbeid;
import no.nav.melosys.domain.brev.DoksysBrevbestilling;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.person.adresse.Bostedsadresse;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.yrker.Yrkesgrupper;
import no.nav.melosys.service.avklartefakta.AvklartMaritimtArbeid;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.Arbeidssted;
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.MaritimtArbeidssted;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.service.dokument.brev.BrevDataTestUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BrevDataGrunnlagTest {
    @Mock
    private KodeverkService kodeverkService;
    @Mock
    private AvklartefaktaService avklartefaktaService;
    @Mock
    private PersondataFasade persondataFasade;

    private final FakeUnleash fakeUnleash = new FakeUnleash();

    private DoksysBrevbestilling brevbestilling;
    private Soeknad søknad;
    private BrevDataGrunnlag dataGrunnlag;

    @BeforeEach
    public void setUp() {
        Bostedsadresse boAdresseFraRegister = new Bostedsadresse();
        boAdresseFraRegister.getGateadresse().setGatenavn("Hjemgata");
        boAdresseFraRegister.getGateadresse().setHusnummer(23);
        boAdresseFraRegister.setPostnr("0165");
        boAdresseFraRegister.setPoststed("Oslo");
        boAdresseFraRegister.setLand(new Land(Land.NORGE));

        PersonDokument person = new PersonDokument();
        person.setBostedsadresse(boAdresseFraRegister);

        søknad = new Soeknad();
        Behandling behandling = lagBehandling(søknad, person);

        brevbestilling = new DoksysBrevbestilling.Builder().medBehandling(behandling).build();
        dataGrunnlag = new BrevDataGrunnlag(brevbestilling, kodeverkService, mock(AvklarteVirksomheterService.class), avklartefaktaService, persondataFasade, fakeUnleash);
    }

    private Behandling lagBehandling(Soeknad søknad, PersonDokument person) {
        Aktoer aktoer = new Aktoer();
        aktoer.setRolle(Aktoersroller.BRUKER);
        aktoer.setAktørId("ident");

        Fagsak fagsak = new Fagsak();
        fagsak.setAktører(Set.of(aktoer));

        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setFagsak(fagsak);
        behandling.getSaksopplysninger().add(lagPersonsaksopplysning(person));

        Behandlingsgrunnlag behandlingsgrunnlag = new Behandlingsgrunnlag();
        behandlingsgrunnlag.setBehandlingsgrunnlagdata(søknad);
        behandling.setBehandlingsgrunnlag(behandlingsgrunnlag);
        return behandling;
    }

    @Test
    void hentBostedsadresse_brukerBostedFraPersonDokument() {
        when(kodeverkService.dekod(any(), any())).thenReturn("Oslo");
        StrukturertAdresse bostedsadresse = dataGrunnlag.getBostedGrunnlag().hentBostedsadresse();
        assertThat(bostedsadresse.getGatenavn()).isEqualTo("Hjemgata");
        assertThat(bostedsadresse.getHusnummerEtasjeLeilighet()).isEqualTo("23");
        assertThat(bostedsadresse.getPostnummer()).isEqualTo("0165");
        assertThat(bostedsadresse.getPoststed()).isEqualTo("Oslo");
        assertThat(bostedsadresse.getLandkode()).isEqualTo(Landkoder.NO.getKode());
    }

    @Test
    void hentBostedsadresse_oppgittAdresseOverstyrerTPS_nårOppgittAdresseISøknad() {
        StrukturertAdresse oppgittBosted = new StrukturertAdresse();
        oppgittBosted.setGatenavn("HerBorJegGata");
        oppgittBosted.setHusnummerEtasjeLeilighet("123");
        oppgittBosted.setPostnummer("0166");
        oppgittBosted.setPoststed("Oslo");
        oppgittBosted.setRegion("Østlandet");
        oppgittBosted.setLandkode("NO");
        søknad.bosted.oppgittAdresse = oppgittBosted;

        StrukturertAdresse bostedsadresse = dataGrunnlag.getBostedGrunnlag().hentBostedsadresse();
        assertThat(bostedsadresse.getGatenavn()).isEqualTo("HerBorJegGata");
        assertThat(bostedsadresse.getHusnummerEtasjeLeilighet()).isEqualTo("123");
        assertThat(bostedsadresse.getPostnummer()).isEqualTo("0166");
        assertThat(bostedsadresse.getPoststed()).isEqualTo("Oslo");
        assertThat(bostedsadresse.getRegion()).isEqualTo("Østlandet");
        assertThat(bostedsadresse.getLandkode()).isEqualTo(Landkoder.NO.getKode());
    }

    @Test
    void hentArbeidssteder_medMaritimtArbeid_girMaritimeArbeidssteder() {
        AvklartMaritimtArbeid maritimtArbeid = lagAvklartMaritimtArbeid();
        when(avklartefaktaService.hentMaritimeAvklartfaktaEtterSubjekt(anyLong()))
            .thenReturn(Collections.singletonMap("Dunfjæder", maritimtArbeid));
        dataGrunnlag = new BrevDataGrunnlag(brevbestilling, kodeverkService, mock(AvklarteVirksomheterService.class), avklartefaktaService, persondataFasade, fakeUnleash);

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
            avklartefaktaService,
            persondataFasade,
            fakeUnleash
        );

        Collection<Arbeidssted> arbeidssteder = dataGrunnlagUtenAvklartMaritimtArbeid.getArbeidsstedGrunnlag().hentArbeidssteder();
        assertThat(arbeidssteder).isEmpty();
    }
}
