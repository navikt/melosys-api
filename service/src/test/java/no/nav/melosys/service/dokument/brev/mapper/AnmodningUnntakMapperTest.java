package no.nav.melosys.service.dokument.brev.mapper;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;

import no.nav.dok.brevdata.felles.v1.navfelles.*;
import no.nav.dok.brevdata.felles.v1.simpletypes.AktoerType;
import no.nav.dok.brevdata.felles.v1.simpletypes.Spraakkode;
import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType;
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.begrunnelse.Artikkel12_1;
import no.nav.melosys.domain.begrunnelse.Artikkel16_1_Anmodning;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.service.dokument.brev.BrevDataAnmodningUnntak;
import no.nav.melosys.service.dokument.brev.mapper.felles.Virksomhet;
import org.junit.Before;
import org.junit.Test;

import static no.nav.melosys.service.dokument.brev.BrevDataUtils.lagKontaktInformasjon;
import static no.nav.melosys.service.dokument.brev.BrevDataUtils.lagNorskPostadresse;
import static org.assertj.core.api.Assertions.assertThat;

public class AnmodningUnntakMapperTest {

    private AnmodningUnntakMapper mapper;

    @Before
    public void setUp() {
        mapper = new AnmodningUnntakMapper();
    }

    @Test
    public void mapTilBrevXML() throws Exception {
        FellesType fellesType = new FellesType();
        fellesType.setFagsaksnummer("MELTEST-1");

        MelosysNAVFelles navFelles = lagNAVFelles();
        navFelles.getMottaker().setMottakeradresse(lagNorskPostadresse());
        Kontaktinformasjon kontaktinformasjon = lagKontaktInformasjon();
        navFelles.setKontaktinformasjon(kontaktinformasjon);

        Behandling behandling = new Behandling();
        Fagsak fagsak = new Fagsak();
        fagsak.setType(Fagsakstype.EU_EØS);
        behandling.setFagsak(fagsak);

        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setDokument(new SoeknadDokument());
        saksopplysning.setType(SaksopplysningType.SØKNAD);
        behandling.setSaksopplysninger(Collections.singleton(saksopplysning));

        Behandlingsresultat resultat = new Behandlingsresultat();

        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setLovvalgsland(Landkoder.DE);
        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now());
        resultat.setLovvalgsperioder(Collections.singleton(lovvalgsperiode));

        resultat.setVilkaarsresultater(new HashSet<>());

        Vilkaarsresultat vilkaarsresultat12_1 = new Vilkaarsresultat();
        vilkaarsresultat12_1.setVilkaar(VilkaarType.FO_883_2004_ART12_1);
        VilkaarBegrunnelse begrunnelse12_1 = new VilkaarBegrunnelse();
        begrunnelse12_1.setKode(Artikkel12_1.IKKE_VESENTLIG_VIRKSOMHET.getKode());
        vilkaarsresultat12_1.setBegrunnelser(Collections.singleton(begrunnelse12_1));
        resultat.getVilkaarsresultater().add(vilkaarsresultat12_1);

        Vilkaarsresultat vilkaarsresultat12_2 = new Vilkaarsresultat();
        vilkaarsresultat12_2.setVilkaar(VilkaarType.FO_883_2004_ART12_2);
        resultat.getVilkaarsresultater().add(vilkaarsresultat12_2);

        Vilkaarsresultat vilkaarsresultat16_1 = new Vilkaarsresultat();
        vilkaarsresultat16_1.setVilkaar(VilkaarType.FO_883_2004_ART16_1);
        VilkaarBegrunnelse begrunnelse_16_1 = new VilkaarBegrunnelse();
        begrunnelse_16_1.setKode(Artikkel16_1_Anmodning.UTSENDELSE_MELLOM_24_MN_OG_5_AAR.getKode());
        vilkaarsresultat16_1.setBegrunnelser(Collections.singleton(begrunnelse_16_1));
        resultat.getVilkaarsresultater().add(vilkaarsresultat16_1);

        BrevDataAnmodningUnntak brevData = new BrevDataAnmodningUnntak("Z999999");
        brevData.hovedvirksomhet = new Virksomhet("Test AS", null, null);

        String xml = mapper.mapTilBrevXML(fellesType, navFelles, behandling, resultat, brevData);

        assertThat(xml).isNotNull();
    }

    private static MelosysNAVFelles lagNAVFelles() {
        MelosysNAVFelles melosysNAVFelles = new MelosysNAVFelles();
        melosysNAVFelles.setMottaker(lagMottaker());
        melosysNAVFelles.setSakspart(lagSakspart());
        NavEnhet navEnhet = NavEnhet.builder().withEnhetsId("4567").withEnhetsNavn("MEL").build();
        melosysNAVFelles.setBehandlendeEnhet(navEnhet);
        NavAnsatt navAnsatt = NavAnsatt.builder().withAnsattId("A94840").withNavn("Aleksander Z").build();
        Saksbehandler saksbehandler = Saksbehandler.builder().withNavEnhet(navEnhet).withNavAnsatt(navAnsatt).build();
        melosysNAVFelles.setSignerendeSaksbehandler(saksbehandler);
        melosysNAVFelles.setSignerendeBeslutter(saksbehandler);
        return melosysNAVFelles;
    }

    private static Mottaker lagMottaker() {
        Mottaker mottaker = new Person();
        mottaker.setId("ID");
        mottaker.setTypeKode(AktoerType.PERSON);
        mottaker.setKortNavn("Nvn");
        mottaker.setNavn("Navn");
        mottaker.setSpraakkode(Spraakkode.NB);
        return mottaker;
    }

    private static Sakspart lagSakspart() {
        Sakspart sakspart = new Sakspart();
        sakspart.setId("AktørID");
        sakspart.setTypeKode(AktoerType.PERSON);
        sakspart.setNavn("Navn");
        return sakspart;
    }
}