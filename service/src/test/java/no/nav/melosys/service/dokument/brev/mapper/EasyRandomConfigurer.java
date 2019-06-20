package no.nav.melosys.service.dokument.brev.mapper;

import org.jeasy.random.EasyRandomParameters;
import org.jeasy.random.EasyRandom;
import no.nav.dok.brevdata.felles.v1.navfelles.Mottaker;
import no.nav.dok.brevdata.felles.v1.navfelles.NorskPostadresse;
import no.nav.dok.brevdata.felles.v1.navfelles.Person;
import no.nav.dok.brevdata.felles.v1.simpletypes.AktoerType;
import no.nav.dok.brevdata.felles.v1.simpletypes.Spraakkode;

public enum EasyRandomConfigurer {
    ;

    private static final Mottaker MOTTAKER = Person.builder()
        .withId("foobar")
        .withTypeKode(AktoerType.PERSON)
        .withNavn("Foobar Zot")
        .withKortNavn("fbz")
        .withSpraakkode(Spraakkode.NN)
        .withMottakeradresse(NorskPostadresse.builder()
            .withAdresselinje1("Gate 1")
            .withPostnummer("1234")
            .withPoststed("Sted")
            .withLand("NO")
            .build())
        .build();

    static EasyRandomParameters paramForDokProd() {
        return new EasyRandomParameters()
            .randomize(Mottaker.class, () -> MOTTAKER);
    }

    static EasyRandom randomForDokProd() {
        return new EasyRandom(paramForDokProd());
    }
}
