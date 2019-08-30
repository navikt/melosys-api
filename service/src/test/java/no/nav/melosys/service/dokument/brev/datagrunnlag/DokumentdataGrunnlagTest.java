package no.nav.melosys.service.dokument.brev.datagrunnlag;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.felles.StrukturertAdresse;
import no.nav.melosys.domain.dokument.person.Bostedsadresse;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.MaritimtArbeid;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.yrker.Yrkesgrupper;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.avklartefakta.AvklartMaritimtArbeid;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DokumentdataGrunnlagTest {
    @Mock
    private KodeverkService kodeverkService;
    @Mock
    private AvklartefaktaService avklartefaktaService;
    @Mock
    private AvklarteVirksomheterService avklarteVirksomheterService;

    private PersonDokument person;
    private SoeknadDokument søknad;
    private Behandling behandling;
    private DokumentdataGrunnlag dataGrunnlag;

    @Before
    public void setUp() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        when(kodeverkService.dekod(any(), any(), any())).thenReturn("Oslo");

        AvklartVirksomhet arbeidsgiver = lagNorskVirksomhet();
        when(avklarteVirksomheterService.hentAlleNorskeVirksomheter(any(), any())).thenReturn(Arrays.asList(arbeidsgiver));

        Bostedsadresse boAdresseFraRegister = new Bostedsadresse();
        boAdresseFraRegister.getGateadresse().setGatenavn("Hjemgata");
        boAdresseFraRegister.getGateadresse().setHusnummer(23);
        boAdresseFraRegister.setPostnr("0165");
        boAdresseFraRegister.setPoststed("Oslo");
        boAdresseFraRegister.setLand(new Land(Land.NORGE));

        person = new PersonDokument();
        person.bostedsadresse = boAdresseFraRegister;

        søknad = new SoeknadDokument();
        behandling = lagBehandling(søknad, person);

        dataGrunnlag = new DokumentdataGrunnlag(behandling, kodeverkService, avklarteVirksomheterService, avklartefaktaService);
    }

    private Behandling lagBehandling(SoeknadDokument søknad, PersonDokument person) {
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.getSaksopplysninger().add(lagSoeknadssaksopplysning(søknad));
        behandling.getSaksopplysninger().add(lagPersonsaksopplysning(person));
        return behandling;
    }

    @Test(expected = TekniskException.class)
    public void hentBostedsadresse_manglerOppgittOgTpsBostedsadresse_girUnntak() throws TekniskException {
        person.bostedsadresse = new Bostedsadresse();
        søknad.bosted.oppgittAdresse = new StrukturertAdresse();
        dataGrunnlag.getBostedGrunnlag().hentBostedsadresse();
    }

    @Test
    public void hentBostedsadresse_brukerBostedFraPersonDokument() throws TekniskException {
        StrukturertAdresse bostedsadresse = dataGrunnlag.getBostedGrunnlag().hentBostedsadresse();
        assertThat(bostedsadresse.gatenavn).isEqualTo("Hjemgata");
        assertThat(bostedsadresse.husnummer).isEqualTo("23");
        assertThat(bostedsadresse.postnummer).isEqualTo("0165");
        assertThat(bostedsadresse.poststed).isEqualTo("Oslo");
        assertThat(bostedsadresse.landkode).isEqualTo(Landkoder.NO.getKode());
    }

    @Test
    public void hentBostedsadresse_oppgittAdresseOverstyrerTPS_nårOppgittAdresseISøknad() throws TekniskException {
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
    public void hentArbeidssteder_medMaritimtArbeid_girMaritimeArbeidssteder() throws TekniskException {
        MaritimtArbeid maritimtArbeidISøknad = lagMaritimtArbeid();
        this.søknad.maritimtArbeid.add(maritimtArbeidISøknad);

        AvklartMaritimtArbeid avklartMaritimtArbeid = lagAvklartMaritimtArbeid();
        when(avklartefaktaService.hentAlleMaritimeAvklartfakta(anyLong())).thenReturn(Collections.singletonMap("Dunfjæder", avklartMaritimtArbeid));

        List<Arbeidssted> arbeidssteder = dataGrunnlag.getArbeidssteder().hentArbeidssteder();
        assertThat(arbeidssteder.size()).isEqualTo(1);

        MaritimtArbeidssted arbeidssted = (MaritimtArbeidssted) arbeidssteder.get(0);
        assertThat(arbeidssted.getNavn()).isEqualTo(maritimtArbeidISøknad.foretakNavn);
        assertThat(arbeidssted.getIdnummer()).isEqualTo(maritimtArbeidISøknad.foretakOrgnr);
        assertThat(arbeidssted.getOmråde()).isEqualTo(avklartMaritimtArbeid.getLand());
        assertThat(arbeidssted.getYrkesgruppe().getKode()).isEqualTo(Yrkesgrupper.SOKKEL_ELLER_SKIP.getKode());
    }

    @Test
    public void hentArbeidssteder_medMaritimtArbeidUtenForetak_girMaritimeArbeidssteder() throws TekniskException {
        MaritimtArbeid maritimtArbeidISøknad = lagMaritimtArbeid();
        maritimtArbeidISøknad.foretakOrgnr = null;
        maritimtArbeidISøknad.foretakNavn = null;
        this.søknad.maritimtArbeid.add(maritimtArbeidISøknad);

        AvklartMaritimtArbeid avklartMaritimtArbeid = lagAvklartMaritimtArbeid();
        when(avklartefaktaService.hentAlleMaritimeAvklartfakta(anyLong())).thenReturn(Collections.singletonMap("Dunfjæder", avklartMaritimtArbeid));

        List<Arbeidssted> arbeidssteder = dataGrunnlag.getArbeidssteder().hentArbeidssteder();
        assertThat(arbeidssteder.size()).isEqualTo(1);

        MaritimtArbeidssted arbeidssted = (MaritimtArbeidssted) arbeidssteder.get(0);
        assertThat(arbeidssted.getNavn()).isNullOrEmpty();
        assertThat(arbeidssted.getIdnummer()).isNullOrEmpty();
        assertThat(arbeidssted.getOmråde()).isEqualTo("GB");
    }

    @Test
    public void hentArbeidssteder_medMaritimtArbeidUtenAvklartMaritimtArbeid_girTomListe() throws TekniskException {
        MaritimtArbeid maritimtArbeidISøknad = lagMaritimtArbeid();
        this.søknad.maritimtArbeid.add(maritimtArbeidISøknad);

        when(avklartefaktaService.hentAlleMaritimeAvklartfakta(anyLong())).thenReturn(Collections.emptyMap());

        Collection<Arbeidssted> arbeidssteder = dataGrunnlag.getArbeidssteder().hentArbeidssteder();
        assertThat(arbeidssteder).isEmpty();
    }

    @Test
    public void hentAlleNorskeVirksomheter_foreventerEnVirksomhet() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        Collection<AvklartVirksomhet> norskeVirksomheter = dataGrunnlag.getAvklarteVirksomheterGrunnlag().hentAlleNorskeVirksomheterMedAdresse();
        assertThat(norskeVirksomheter).hasSize(1);
        dataGrunnlag.getAvklarteVirksomheterGrunnlag().hentAlleNorskeVirksomheterMedAdresse();
        verify(avklarteVirksomheterService, times(1)).hentAlleNorskeVirksomheter(any(), any());
    }

    @Test
    public void hentHovedvirksomhet_medEnNorskVirksomhet_girNorskHovedvirksomhet() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        AvklartVirksomhet norskVirksomhet = lagNorskVirksomhet();
        when(avklarteVirksomheterService.hentAlleNorskeVirksomheter(any(), any())).thenReturn(Collections.singletonList(norskVirksomhet));

        AvklartVirksomhet avklartVirksomhet = dataGrunnlag.getAvklarteVirksomheterGrunnlag().hentHovedvirksomhet();
        assertThat(avklartVirksomhet).isEqualTo(norskVirksomhet);
    }

    @Test
    public void hentHovedvirksomhet_medNorskOgUtenlandskVirksomhet_girNorskHovedvirksomhet() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        AvklartVirksomhet norskVirksomhet = lagNorskVirksomhet();
        when(avklarteVirksomheterService.hentAlleNorskeVirksomheter(any(), any())).thenReturn(Collections.singletonList(norskVirksomhet));

        AvklartVirksomhet utenlandskAvklartVirksomhet = new AvklartVirksomhet(lagForetakUtland());
        when(avklarteVirksomheterService.hentUtenlandskeVirksomheter(any())).thenReturn(Collections.singletonList(utenlandskAvklartVirksomhet));

        AvklartVirksomhet hovedvirksomhet = dataGrunnlag.getAvklarteVirksomheterGrunnlag().hentHovedvirksomhet();
        Collection<AvklartVirksomhet> bivirksomhet = dataGrunnlag.getAvklarteVirksomheterGrunnlag().hentBivirksomheter();
        assertThat(hovedvirksomhet).isEqualTo(norskVirksomhet);
    }

    @Test
    public void hentHovedvirksomhet_medKunUtenlandskVirksomhet_girUtenlandskVirksomhet() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        AvklartVirksomhet forventetUtenlandskVirksomhet = new AvklartVirksomhet(lagForetakUtland());
        when(avklarteVirksomheterService.hentUtenlandskeVirksomheter(any())).thenReturn(Collections.singletonList(forventetUtenlandskVirksomhet));

        when(avklarteVirksomheterService.hentAlleNorskeVirksomheter(any(), any())).thenReturn(Collections.emptyList());

        AvklartVirksomhet hovedvirksomhet = dataGrunnlag.getAvklarteVirksomheterGrunnlag().hentHovedvirksomhet();
        assertThat(hovedvirksomhet).isEqualToComparingFieldByField(forventetUtenlandskVirksomhet);
    }

    @Test
    public void hentBivirksomheter_medEnNorskVirksomhet_girIngenBivirksomheter() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        AvklartVirksomhet norskVirksomhet = lagNorskVirksomhet();
        when(avklarteVirksomheterService.hentAlleNorskeVirksomheter(any(), any())).thenReturn(Collections.singletonList(norskVirksomhet));

        Collection<AvklartVirksomhet> bivirksomheter = dataGrunnlag.getAvklarteVirksomheterGrunnlag().hentBivirksomheter();
        assertThat(bivirksomheter).isEmpty();
    }

    @Test
    public void hentBivirksomheter_medEnUtenlandskVirksomhet_girIngenBivirksomheter() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        when(avklarteVirksomheterService.hentAlleNorskeVirksomheter(any(), any())).thenReturn(Collections.emptyList());

        AvklartVirksomhet forventetUtenlandskVirksomhet = new AvklartVirksomhet(lagForetakUtland());
        when(avklarteVirksomheterService.hentUtenlandskeVirksomheter(any())).thenReturn(Collections.singletonList(forventetUtenlandskVirksomhet));

        Collection<AvklartVirksomhet> bivirksomheter = dataGrunnlag.getAvklarteVirksomheterGrunnlag().hentBivirksomheter();
        assertThat(bivirksomheter).isEmpty();
    }

    @Test
    public void hentBivirksomheter_medToNorskeVirksomheter_girEnNorskBivirksomhet() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        AvklartVirksomhet norskVirksomhet = lagNorskVirksomhet();
        when(avklarteVirksomheterService.hentAlleNorskeVirksomheter(any(), any())).thenReturn(Arrays.asList(norskVirksomhet, norskVirksomhet));

        Collection<AvklartVirksomhet> bivirksomheter = dataGrunnlag.getAvklarteVirksomheterGrunnlag().hentBivirksomheter();
        assertThat(bivirksomheter).containsExactly(norskVirksomhet);
    }

    @Test
    public void hentHovedvirksomhet_medNorskOgUtenlandskVirksomhet_girUtenlandskBivirksomhet() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        AvklartVirksomhet forventetUtenlandskVirksomhet = new AvklartVirksomhet(lagForetakUtland());

        AvklartVirksomhet norskVirksomhet = lagNorskVirksomhet();
        when(avklarteVirksomheterService.hentAlleNorskeVirksomheter(any(), any())).thenReturn(Collections.singletonList(norskVirksomhet));
        when(avklarteVirksomheterService.hentUtenlandskeVirksomheter(any())).thenReturn(Collections.singletonList(forventetUtenlandskVirksomhet));

        Collection<AvklartVirksomhet> bivirksomheter = dataGrunnlag.getAvklarteVirksomheterGrunnlag().hentBivirksomheter();
        assertThat(bivirksomheter).hasSize(1);

        assertThat(bivirksomheter.iterator().next()).isEqualToComparingFieldByField(forventetUtenlandskVirksomhet);
    }
}