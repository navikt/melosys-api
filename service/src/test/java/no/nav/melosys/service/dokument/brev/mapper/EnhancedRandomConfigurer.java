package no.nav.melosys.service.dokument.brev.mapper;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import io.github.benas.randombeans.api.Randomizer;
import no.nav.dok.brevdata.felles.v1.navfelles.Mottaker;
import no.nav.dok.brevdata.felles.v1.navfelles.NorskPostadresse;
import no.nav.dok.brevdata.felles.v1.navfelles.Person;
import no.nav.dok.brevdata.felles.v1.simpletypes.AktoerType;
import no.nav.dok.brevdata.felles.v1.simpletypes.Spraakkode;

public enum EnhancedRandomConfigurer {
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

    static EnhancedRandomBuilder builderForDokProd() {
        return EnhancedRandomBuilder
            .aNewEnhancedRandomBuilder()
            .randomize(Mottaker.class, (Randomizer<Mottaker>) () -> MOTTAKER);
    }

    static EnhancedRandom randomForDokProd() {
        return builderForDokProd().build();
    }
}
