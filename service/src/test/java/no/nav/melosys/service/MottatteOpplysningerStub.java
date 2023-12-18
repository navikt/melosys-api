package no.nav.melosys.service;

import java.util.ArrayList;
import java.util.List;

import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData;
import no.nav.melosys.domain.mottatteopplysninger.Soeknad;
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.FysiskArbeidssted;
import no.nav.melosys.domain.mottatteopplysninger.data.ForetakUtland;
import no.nav.melosys.domain.mottatteopplysninger.data.SelvstendigForetak;
import no.nav.melosys.domain.kodeverk.Mottatteopplysningertyper;

public final class MottatteOpplysningerStub {


    public static MottatteOpplysninger lagMottatteOpplysninger(List<String> selvstendigeForetak, List<ForetakUtland> foretakUtland, List<String> ekstraArbeidsgivere) {
        MottatteOpplysninger mottatteOpplysninger = new MottatteOpplysninger();
        mottatteOpplysninger.setType(Mottatteopplysningertyper.SØKNAD_A1_YRKESAKTIVE_EØS);
        mottatteOpplysninger.setMottatteOpplysningerData(lagMottatteOpplysningerdata(selvstendigeForetak, foretakUtland, ekstraArbeidsgivere));
        return mottatteOpplysninger;
    }

    private static MottatteOpplysningerData lagMottatteOpplysningerdata(List<String> selvstendigeForetak, List<ForetakUtland> foretakUtland, List<String> ekstraArbeidsgivere) {
        Soeknad søknad = new Soeknad();
        for (String orgnr : selvstendigeForetak) {
            SelvstendigForetak selvstendigForetak = new SelvstendigForetak();
            selvstendigForetak.orgnr = orgnr;
            søknad.selvstendigArbeid.selvstendigForetak.add(selvstendigForetak);
        }

        FysiskArbeidssted fysiskArbeidssted = new FysiskArbeidssted();
        fysiskArbeidssted.adresse.setLandkode("DE");
        søknad.arbeidPaaLand.fysiskeArbeidssteder = new ArrayList<>();
        søknad.arbeidPaaLand.fysiskeArbeidssteder.add(fysiskArbeidssted);
        søknad.juridiskArbeidsgiverNorge.ekstraArbeidsgivere = ekstraArbeidsgivere;
        søknad.foretakUtland = foretakUtland;
        søknad.soeknadsland.landkoder.add("DE");

        return søknad;
    }
}
