package no.nav.melosys.service.dokument.brev.mapper;

import no.nav.dok.brevdata.felles.v1.navfelles.*;
import no.nav.dok.brevdata.felles.v1.simpletypes.AktoerType;
import no.nav.dok.brevdata.felles.v1.simpletypes.Spraakkode;
import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType;
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles;
import no.nav.melosys.domain.kodeverk.Landkoder;

import static no.nav.melosys.service.dokument.brev.BrevDataUtils.lagKontaktInformasjon;

class BrevMappingTestUtils {

    static MelosysNAVFelles lagNAVFelles() {
        MelosysNAVFelles melosysNAVFelles = new MelosysNAVFelles();
        melosysNAVFelles.setMottaker(lagMottaker());
        melosysNAVFelles.setSakspart(lagSakspart());

        NavEnhet navEnhet = new NavEnhet()
            .withEnhetsId("4567")
            .withEnhetsNavn("MEL");
        melosysNAVFelles.setBehandlendeEnhet(navEnhet);

        NavAnsatt navAnsatt = new NavAnsatt()
            .withAnsattId("A94840")
            .withNavn("Aleksander Z");

        Saksbehandler saksbehandler = new Saksbehandler()
            .withNavEnhet(navEnhet)
            .withNavAnsatt(navAnsatt);
        melosysNAVFelles.setSignerendeSaksbehandler(saksbehandler);
        melosysNAVFelles.setSignerendeBeslutter(saksbehandler);
        melosysNAVFelles.setKontaktinformasjon(lagKontaktInformasjon());
        return melosysNAVFelles;
    }

    private static Mottaker lagMottaker() {
        Mottaker mottaker = new Person();
        mottaker.setId("ID");
        mottaker.setTypeKode(AktoerType.PERSON);
        mottaker.setKortNavn("Nvn");
        mottaker.setNavn("Navn");
        mottaker.setMottakeradresse(lagAdresse());
        mottaker.setSpraakkode(Spraakkode.NB);
        return mottaker;
    }

    private static Adresse lagAdresse() {
        return new NorskPostadresse()
        .withAdresselinje1("Gate")
        .withAdresselinje2("12B")
        .withPoststed("Sted")
        .withPostnummer("4321")
        .withLand(Landkoder.BG.getKode());
    }

    private static Sakspart lagSakspart() {
        Sakspart sakspart = new Sakspart();
        sakspart.setId("AktørID");
        sakspart.setTypeKode(AktoerType.PERSON);
        sakspart.setNavn("Navn");
        return sakspart;
    }

    public static FellesType lagFellesType() {
        return new FellesType()
            .withFagsaksnummer("MELTEST-1");
    }
}
