package no.nav.melosys.service.dokument.brev;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.felles.StrukturertAdresse;
import no.nav.melosys.domain.dokument.person.Bostedsadresse;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.ForetakUtland;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.kodeverk.Avklartefaktatype;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Yrkesgrupper;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.AbstraktDokumentDataBygger;
import no.nav.melosys.service.dokument.brev.mapper.felles.Arbeidssted;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.service.kodeverk.KodeverkService;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AbstraktDokumentDataByggerTest {

    private SoeknadDokument søknad;
    private PersonDokument person;
    private Set<String> avklarteOrganisasjoner = new HashSet<>();

    private BrevDatabyggerbaseImpl brevDatabyggerbase;

    private AvklartefaktaService avklartefaktaService;

    class BrevDatabyggerbaseImpl extends AbstraktDokumentDataBygger {

        protected BrevDatabyggerbaseImpl(KodeverkService kodeverkService,
                                         AvklartefaktaService avklartefaktaService,
                                         PersonDokument person,
                                         SoeknadDokument søknad) {
            super(kodeverkService, mock(LovvalgsperiodeService.class), avklartefaktaService);
            this.person = person;
            this.søknad = søknad;
            this.behandling = mock(Behandling.class);
        }

        public StrukturertAdresse hentBostedsadresse() throws TekniskException {
            return super.hentBostedsadresse();
        }
        public List<Arbeidssted> hentArbeidssteder() {
            return super.hentArbeidssteder();
        }
        public List<AvklartVirksomhet> hentUtenlandskeVirksomheter() {
            return super.hentUtenlandskeVirksomheter();
        }
    }

    @Before
    public void setUp() {
        KodeverkService kodeverkService = mock(KodeverkService.class);
        avklartefaktaService = mock(AvklartefaktaService.class);
        when(kodeverkService.dekod(any(), any(), any())).thenReturn("Oslo");

        Bostedsadresse boAdresseFraRegister = new Bostedsadresse();
        boAdresseFraRegister.getGateadresse().setGatenavn("Hjemgata");
        boAdresseFraRegister.getGateadresse().setHusnummer(23);
        boAdresseFraRegister.setPostnr("0165");
        boAdresseFraRegister.setPoststed("Oslo");
        boAdresseFraRegister.setLand(new Land(Land.NORGE));

        person = new PersonDokument();
        person.bostedsadresse = boAdresseFraRegister;

        søknad = new SoeknadDokument();

        avklarteOrganisasjoner.add("12345678910");

        brevDatabyggerbase = new BrevDatabyggerbaseImpl(kodeverkService, avklartefaktaService, person, søknad);
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
    public void hentFysiskeArbeidsstedFraForetaketsAdresse() {
        ForetakUtland foretakUtland = new ForetakUtland();
        foretakUtland.orgnr = "12345678910";
        foretakUtland.navn = "Jarlsberg INTERNATIONAL";
        foretakUtland.adresse = new StrukturertAdresse();
        foretakUtland.adresse.landkode = "NO";
        søknad.foretakUtland.add(foretakUtland);

        List<Arbeidssted> arbeidssteder = brevDatabyggerbase.hentArbeidssteder();
        assertThat(arbeidssteder.get(0).navn).isEqualTo(foretakUtland.navn);
        assertThat(arbeidssteder.get(0).landkode).isEqualTo(foretakUtland.adresse.landkode);
    }

    @Test
    public void hentArbeidsstederForMartimtArbeid_listMedArbeidssteder() {
        this.søknad.foretakUtland.add(new ForetakUtland());
        Avklartefakta avklartefakta = new Avklartefakta();
        avklartefakta.setType(Avklartefaktatype.ARBEIDSLAND);
        avklartefakta.setFakta("BG");
        avklartefakta.setReferanse("INSTALLASJON_ARBEIDSLAND");
        avklartefakta.setSubjekt("Dunfjæder");

        when(avklartefaktaService.hentAlleAvklarteArbeidsland(anyLong())).thenReturn(new HashSet<>(Collections.singletonList((avklartefakta))));

        List<Arbeidssted> arbeidSteder = brevDatabyggerbase.hentArbeidssteder();

        assertThat(arbeidSteder.size()).isEqualTo(1);
        Arbeidssted arbeidssted = arbeidSteder.get(0);
        assertThat(arbeidssted.navn).isEqualTo("Dunfjæder");
        assertThat(arbeidssted.landkode).isEqualTo("BG");
        assertThat(arbeidssted.yrkesgruppe.getKode()).isEqualTo(Yrkesgrupper.SOKKEL_ELLER_SKIP.getKode());
    }
}