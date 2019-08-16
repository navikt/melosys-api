package no.nav.melosys.service.dokument.brev;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper;
import no.nav.melosys.domain.kodeverk.yrker.Yrkesgrupper;
import no.nav.melosys.service.avklartefakta.AvklartMaritimtArbeid;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.felles.StrukturertAdresse;
import no.nav.melosys.domain.dokument.person.Bostedsadresse;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.ForetakUtland;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Yrkesgrupper;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.avklartefakta.AvklartMaritimtArbeid;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.AbstraktDokumentDataBygger;
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.Arbeidssted;
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.MaritimtArbeidssted;
import no.nav.melosys.service.kodeverk.KodeverkService;
import org.junit.Before;
import org.junit.Test;

import static no.nav.melosys.service.dokument.brev.BrevDataTestUtils.lagForetakUtland;
import static no.nav.melosys.service.dokument.brev.BrevDataTestUtils.lagNorskVirksomhet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

public class AbstraktDokumentDataByggerTest {

    private SoeknadDokument søknad;
    private PersonDokument person;

    private BrevDatabyggerbaseImpl brevDatabyggerbase;

    private AvklartefaktaService avklartefaktaService;
    private AvklarteVirksomheterService avklarteVirksomheterService;

    class BrevDatabyggerbaseImpl extends AbstraktDokumentDataBygger {

        BrevDatabyggerbaseImpl(KodeverkService kodeverkService,
                                         AvklartefaktaService avklartefaktaService,
                                         AvklarteVirksomheterService avklarteVirksomheterService,
                                         PersonDokument person,
                                         SoeknadDokument søknad) {
            super(kodeverkService, mock(LovvalgsperiodeService.class), avklartefaktaService, avklarteVirksomheterService);
            this.person = person;
            this.søknad = søknad;
            this.behandling = mock(Behandling.class);
        }

        public StrukturertAdresse hentBostedsadresse() throws TekniskException {
            return super.hentBostedsadresse();
        }

        public List<AvklartVirksomhet> hentAlleNorskeVirksomheter() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
            return super.hentAlleNorskeVirksomheterMedAdresse();
        }

        public AvklartVirksomhet hentHovedvirksomhet() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException { return super.hentHovedvirksomhet(); }
        public Collection<AvklartVirksomhet> hentBivirksomheter() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException { return super.hentBivirksomheter(); }

        public List<Arbeidssted> hentArbeidssteder() {
            return super.hentArbeidssteder();
        }

        public List<AvklartVirksomhet> hentUtenlandskeVirksomheter() {
            return super.hentUtenlandskeVirksomheter();
        }
    }

    @Before
    public void setUp() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        KodeverkService kodeverkService = mock(KodeverkService.class);
        when(kodeverkService.dekod(any(), any(), any())).thenReturn("Oslo");

        avklartefaktaService = mock(AvklartefaktaService.class);
        avklarteVirksomheterService = mock(AvklarteVirksomheterService.class);

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

        brevDatabyggerbase = new BrevDatabyggerbaseImpl(kodeverkService, avklartefaktaService, avklarteVirksomheterService, person, søknad);
    }

    @Test(expected = TekniskException.class)
    public void hentBostedsadresse_manglerOppgittOgTpsBostedsadresse_girUnntak() throws TekniskException {
        person.bostedsadresse = new Bostedsadresse();
        søknad.bosted.oppgittAdresse = new StrukturertAdresse();
        brevDatabyggerbase.hentBostedsadresse();
    }

    @Test
    public void hentBostedsadresse_brukerBostedFraPersonDokument() throws TekniskException {
        StrukturertAdresse bostedsadresse = brevDatabyggerbase.hentBostedsadresse();
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

        StrukturertAdresse bostedsadresse = brevDatabyggerbase.hentBostedsadresse();
        assertThat(bostedsadresse.gatenavn).isEqualTo("HerBorJegGata");
        assertThat(bostedsadresse.husnummer).isEqualTo("123");
        assertThat(bostedsadresse.postnummer).isEqualTo("0166");
        assertThat(bostedsadresse.poststed).isEqualTo("Oslo");
        assertThat(bostedsadresse.region).isEqualTo("Østlandet");
        assertThat(bostedsadresse.landkode).isEqualTo(Landkoder.NO.getKode());
    }

    @Test
    public void hentArbeidssteder_medArbeidsstedPåUtenlandskForetaksAdresse() {
        ForetakUtland foretakUtland = lagForetakUtland();
        foretakUtland.adresseErOgsåArbeidssted = true;
        søknad.foretakUtland.add(foretakUtland);

        List<Arbeidssted> arbeidssteder = brevDatabyggerbase.hentArbeidssteder();
        assertThat(arbeidssteder.get(0).getNavn()).isEqualTo(foretakUtland.navn);
        assertThat(arbeidssteder.get(0).getLandkode()).isEqualTo(foretakUtland.adresse.landkode);
    }

    @Test
    public void hentArbeidssteder_medMaritimtArbeid_girMaritimeArbeidssteder() {
        ForetakUtland foretakUtland = lagForetakUtland();
        foretakUtland.adresse.landkode = null;
        this.søknad.foretakUtland.add(foretakUtland);

        Avklartefakta avklartefakta = new Avklartefakta();
        avklartefakta.setType(Avklartefaktatyper.ARBEIDSLAND);
        avklartefakta.setFakta("BG");
        avklartefakta.setReferanse("INSTALLASJON_ARBEIDSLAND");
        avklartefakta.setSubjekt("Dunfjæder");

        AvklartMaritimtArbeid avklartMaritimtArbeid = new AvklartMaritimtArbeid("Dunfjæder");
        avklartMaritimtArbeid.leggTilFakta(avklartefakta);

        when(avklartefaktaService.hentMaritimeAvklartfakta(anyLong())).thenReturn(Collections.singletonList(avklartMaritimtArbeid));

        List<Arbeidssted> arbeidssteder = brevDatabyggerbase.hentArbeidssteder();

        assertThat(arbeidssteder.size()).isEqualTo(1);
        MaritimtArbeidssted arbeidssted = (MaritimtArbeidssted) arbeidssteder.get(0);
        assertThat(arbeidssted.getNavn()).isEqualTo("Dunfjæder");
        assertThat(arbeidssted.getOmråde()).isEqualTo("BG");
        assertThat(arbeidssted.getYrkesgruppe().getKode()).isEqualTo(Yrkesgrupper.SOKKEL_ELLER_SKIP.getKode());
    }

    @Test
    public void hentAlleNorskeVirksomheter() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        List<AvklartVirksomhet> norskeVirksomheter = brevDatabyggerbase.hentAlleNorskeVirksomheter();
        assertThat(norskeVirksomheter).hasSize(1);
        brevDatabyggerbase.hentAlleNorskeVirksomheter();
        verify(avklarteVirksomheterService, times(1)).hentAlleNorskeVirksomheter(any(), any());
    }

    @Test
    public void hentHovedvirksomhet_medEnNorskVirksomhet_girNorskHovedvirksomhet() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        AvklartVirksomhet norskVirksomhet = lagNorskVirksomhet();
        when(avklarteVirksomheterService.hentAlleNorskeVirksomheter(any(), any())).thenReturn(Collections.singletonList(norskVirksomhet));

        AvklartVirksomhet avklartVirksomhet = brevDatabyggerbase.hentHovedvirksomhet();
        assertThat(avklartVirksomhet).isEqualTo(norskVirksomhet);
    }

    @Test
    public void hentHovedvirksomhet_medNorskOgUtenlandskVirksomhet_girNorskHovedvirksomhet() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        AvklartVirksomhet norskVirksomhet = lagNorskVirksomhet();
        søknad.foretakUtland = Collections.singletonList(lagForetakUtland());
        when(avklarteVirksomheterService.hentAlleNorskeVirksomheter(any(), any())).thenReturn(Collections.singletonList(norskVirksomhet));

        AvklartVirksomhet hovedvirksomhet = brevDatabyggerbase.hentHovedvirksomhet();
        assertThat(hovedvirksomhet).isEqualTo(norskVirksomhet);
    }

    @Test
    public void hentHovedvirksomhet_medKunUtenlandskVirksomhet_girUtenlandskVirksomhet() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        ForetakUtland utenlandskVirksomhet = lagForetakUtland();
        søknad.foretakUtland = Collections.singletonList(utenlandskVirksomhet);
        when(avklarteVirksomheterService.hentAlleNorskeVirksomheter(any(), any())).thenReturn(Collections.emptyList());

        AvklartVirksomhet hovedvirksomhet = brevDatabyggerbase.hentHovedvirksomhet();
        AvklartVirksomhet forventetUtenlandskVirksomhet = new AvklartVirksomhet(utenlandskVirksomhet);
        assertThat(hovedvirksomhet).isEqualToComparingFieldByField(forventetUtenlandskVirksomhet);
    }

    @Test
    public void hentBivirksomheter_medEnNorskVirksomhet_girIngenBivirksomheter() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        AvklartVirksomhet norskVirksomhet = lagNorskVirksomhet();
        when(avklarteVirksomheterService.hentAlleNorskeVirksomheter(any(), any())).thenReturn(Collections.singletonList(norskVirksomhet));

        Collection<AvklartVirksomhet> bivirksomheter = brevDatabyggerbase.hentBivirksomheter();
        assertThat(bivirksomheter).isEmpty();
    }

    @Test
    public void hentBivirksomheter_medEnUtenlandskVirksomhet_girIngenBivirksomheter() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        when(avklarteVirksomheterService.hentAlleNorskeVirksomheter(any(), any())).thenReturn(Collections.emptyList());
        ForetakUtland utenlandskVirksomhet = lagForetakUtland();
        søknad.foretakUtland = Collections.singletonList(utenlandskVirksomhet);

        Collection<AvklartVirksomhet> bivirksomheter = brevDatabyggerbase.hentBivirksomheter();
        assertThat(bivirksomheter).isEmpty();
    }

    @Test
    public void hentBivirksomheter_medToNorskeVirksomheter_girEnNorskBivirksomhet() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        AvklartVirksomhet norskVirksomhet = lagNorskVirksomhet();
        when(avklarteVirksomheterService.hentAlleNorskeVirksomheter(any(), any())).thenReturn(Arrays.asList(norskVirksomhet, norskVirksomhet));

        Collection<AvklartVirksomhet> bivirksomheter = brevDatabyggerbase.hentBivirksomheter();
        assertThat(bivirksomheter).containsExactly(norskVirksomhet);
    }

    @Test
    public void hentHovedvirksomhet_medNorskOgUtenlandskVirksomhet_girUtenlandskBivirksomhet() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        ForetakUtland utenlandskVirksomhet = lagForetakUtland();
        søknad.foretakUtland = Collections.singletonList(utenlandskVirksomhet);

        AvklartVirksomhet norskVirksomhet = lagNorskVirksomhet();
        when(avklarteVirksomheterService.hentAlleNorskeVirksomheter(any(), any())).thenReturn(Collections.singletonList(norskVirksomhet));

        Collection<AvklartVirksomhet> bivirksomheter = brevDatabyggerbase.hentBivirksomheter();
        assertThat(bivirksomheter).hasSize(1);

        AvklartVirksomhet forventetUtenlandskVirksomhet = new AvklartVirksomhet(utenlandskVirksomhet);
        assertThat(bivirksomheter.iterator().next()).isEqualToComparingFieldByField(forventetUtenlandskVirksomhet);
    }
}