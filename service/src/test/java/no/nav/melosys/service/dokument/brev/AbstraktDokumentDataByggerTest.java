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
import no.nav.melosys.domain.kodeverk.Yrkesgrupper;
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
                                         SoeknadDokument søknad) {
            super(kodeverkService, mock(LovvalgsperiodeService.class), avklartefaktaService);
            this.person = person;
            this.søknad = søknad;
            this.behandling = mock(Behandling.class);
        }

        public Bostedsadresse hentBostedsadresse() {
            return super.hentBostedsadresse();
        }
        public List<Arbeidssted> hentArbeidssteder() {
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

        brevDatabyggerbase = new BrevDatabyggerbaseImpl(kodeverkService, avklartefaktaService, person, søknad);
    }

    @Test
    public void hentBostedsadresse() {
        Bostedsadresse bostedsadresse = brevDatabyggerbase.hentBostedsadresse();
        assertThat(bostedsadresse).isEqualTo(person.bostedsadresse);
        assertThat(bostedsadresse.getPoststed()).isEqualTo("Oslo");
    }

    @Test
    public void hentFysiskeArbeidsstedFraForetaketsAdresse() {
        ForetakUtland foretakUtland = new ForetakUtland();
        foretakUtland.orgnr = "12345678910";
        foretakUtland.navn = "Jarlsberg INTERNATIONAL";
        foretakUtland.adresse = new StrukturertAdresse();
        foretakUtland.adresse.landKode = "NO";
        søknad.foretakUtland.add(foretakUtland);

        assertThat(foretakUtland.navn).isEqualTo(foretakUtland.navn);
        assertThat(foretakUtland.adresse.landKode).isEqualTo(foretakUtland.adresse.landKode);
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
        assertThat(arbeidssted.landKode).isEqualTo("BG");
        assertThat(arbeidssted.yrkesgruppe.getKode()).isEqualTo(Yrkesgrupper.SOKKEL_ELLER_SKIP.getKode());
    }
}