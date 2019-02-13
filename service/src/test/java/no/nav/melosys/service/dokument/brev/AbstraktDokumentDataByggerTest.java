package no.nav.melosys.service.dokument.brev;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.felles.StrukturertAdresse;
import no.nav.melosys.domain.dokument.person.Bostedsadresse;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.ArbeidUtland;
import no.nav.melosys.domain.dokument.soeknad.ForetakUtland;
import no.nav.melosys.domain.dokument.soeknad.SelvstendigForetak;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.kodeverk.Avklartefaktatype;
import no.nav.melosys.domain.kodeverk.Yrkesgrupper;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.AbstraktDokumentDataBygger;
import no.nav.melosys.service.dokument.brev.mapper.felles.Arbeidssted;
import no.nav.melosys.service.dokument.brev.mapper.felles.Virksomhet;
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
                                         SoeknadDokument søknad,
                                         Set<String> avklarteOrganisasjoner) {
            super(kodeverkService, mock(LovvalgsperiodeService.class), avklartefaktaService);
            this.person = person;
            this.søknad = søknad;
            this.avklarteOrganisasjoner = avklarteOrganisasjoner;
            this.behandling = mock(Behandling.class);
        }

        public Bostedsadresse hentBostedsadresse() {
            return super.hentBostedsadresse();
        }
        public Set<String> hentAvklarteSelvstendigeForetakOrgnumre() {
            return super.hentAvklarteSelvstendigeForetakOrgnumre();
        }
        public List<Arbeidssted> hentArbeidssteder() throws TekniskException {
            return super.hentArbeidssteder();
        }
        public List<Virksomhet> hentUtenlandskeVirksomheter() {
            return super.hentUtenlandskeVirksomheter();
        }
    }

    @Before
    public void setUp() {
        KodeverkService kodeverkService = mock(KodeverkService.class);
        avklartefaktaService = mock(AvklartefaktaService.class);
        when(kodeverkService.dekod(any(), any(), any())).thenReturn("Oslo");

        Bostedsadresse boAdresse = new Bostedsadresse();
        boAdresse.getGateadresse().setGatenavn("Gatenavn");
        boAdresse.getGateadresse().setHusnummer(23);
        boAdresse.setPostnr("0165");
        boAdresse.setPoststed("Oslo");
        boAdresse.setLand(new Land(Land.NORGE));

        person = new PersonDokument();
        person.bostedsadresse = boAdresse;

        søknad = new SoeknadDokument();

        avklarteOrganisasjoner.add("12345678910");

        brevDatabyggerbase = new BrevDatabyggerbaseImpl(kodeverkService, avklartefaktaService, person, søknad, avklarteOrganisasjoner);
    }

    @Test
    public void hentBostedsadresse() {
        Bostedsadresse bostedsadresse = brevDatabyggerbase.hentBostedsadresse();
        assertThat(bostedsadresse).isEqualTo(person.bostedsadresse);
        assertThat(bostedsadresse.getPoststed()).isEqualTo("Oslo");
    }

    @Test
    public void hentAvklarteSelvstendigeForetakOrgnumre() {
        SelvstendigForetak foretak = new SelvstendigForetak();
        foretak.orgnr = "12345678910";
        søknad.selvstendigArbeid.selvstendigForetak.add(foretak);

        SelvstendigForetak foretak1 = new SelvstendigForetak();
        foretak1.orgnr = "10987654321";
        søknad.selvstendigArbeid.selvstendigForetak.add(foretak1);

        Set<String> avklarteSelvstendigeOrgnumre =
                brevDatabyggerbase.hentAvklarteSelvstendigeForetakOrgnumre();

        assertThat(avklarteSelvstendigeOrgnumre).containsOnly("12345678910");
    }

    @Test
    public void hentFysiskeArbeidsstederMedForetaketsNavn() throws TekniskException {
        ForetakUtland foretakUtland = new ForetakUtland();
        foretakUtland.orgnr = "12345678910";
        foretakUtland.navn = "Jarlsberg INTERNATIONAL";
        søknad.foretakUtland.add(foretakUtland);

        ArbeidUtland arbeidUtland = new ArbeidUtland();
        arbeidUtland.adresse = new StrukturertAdresse();

        ArbeidUtland arbeidUtland1 = new ArbeidUtland();
        arbeidUtland1.adresse = new StrukturertAdresse();
        søknad.arbeidUtland = Arrays.asList(arbeidUtland, arbeidUtland1);

        List<Arbeidssted> arbeidssteder = brevDatabyggerbase.hentArbeidssteder();
        assertThat(arbeidssteder.stream().map(arbeidssted -> arbeidssted.navn))
                .containsOnly(foretakUtland.navn);
    }

    @Test
    public void hentFysiskeArbeidsstedFraForetaketsAdresse() throws TekniskException {
        ForetakUtland foretakUtland = new ForetakUtland();
        foretakUtland.orgnr = "12345678910";
        foretakUtland.navn = "Jarlsberg INTERNATIONAL";
        foretakUtland.adresse = new StrukturertAdresse();
        foretakUtland.adresse.landKode = "NO";
        søknad.foretakUtland.add(foretakUtland);

        List<Arbeidssted> arbeidssteder = brevDatabyggerbase.hentArbeidssteder();
        assertThat(arbeidssteder.get(0).navn).isEqualTo(foretakUtland.navn);
        assertThat(arbeidssteder.get(0).landKode).isEqualTo(foretakUtland.adresse.landKode);
    }

    @Test(expected = TekniskException.class)
    public void hentArbeidsstederKreverUtenlandskVirksomhet() throws TekniskException {
        brevDatabyggerbase.hentArbeidssteder();
    }

    @Test
    public void hentArbeidsstederForMartimtArbeid_listMedArbeidssteder() throws TekniskException {
        this.søknad.foretakUtland.add(new ForetakUtland());
        Avklartefakta avklartefakta = new Avklartefakta();
        avklartefakta.setType(Avklartefaktatype.ARBEIDSLAND);
        avklartefakta.setFakta("BG");
        avklartefakta.setReferanse("INSTALLASJON_ARBEIDSLAND");
        avklartefakta.setSubjekt("Dunfjæder");

        when(avklartefaktaService.hentAlleAvklarteArbeidsland(anyLong())).thenReturn(new HashSet<>(Arrays.asList((avklartefakta))));

        List<Arbeidssted> arbeidSteder = brevDatabyggerbase.hentArbeidssteder();

        assertThat(arbeidSteder.size()).isEqualTo(1);
        Arbeidssted arbeidssted = arbeidSteder.get(0);
        assertThat(arbeidssted.navn).isEqualTo("Dunfjæder");
        assertThat(arbeidssted.landKode).isEqualTo("BG");
        assertThat(arbeidssted.yrkesgruppe.getKode()).isEqualTo(Yrkesgrupper.SOKKEL_ELLER_SKIP.getKode());
    }
}