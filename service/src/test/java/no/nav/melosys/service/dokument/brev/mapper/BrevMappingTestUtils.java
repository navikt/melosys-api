package no.nav.melosys.service.dokument.brev.mapper;

import no.nav.dok.brevdata.felles.v1.navfelles.*;
import no.nav.dok.brevdata.felles.v1.simpletypes.AktoerType;
import no.nav.dok.brevdata.felles.v1.simpletypes.Spraakkode;
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles;

class BrevMappingTestUtils {

    static MelosysNAVFelles lagNAVFelles() {
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
