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
            selvstendigForetak.setOrgnr(orgnr);
            søknad.selvstendigArbeid.getSelvstendigForetak().add(selvstendigForetak);
        }

        FysiskArbeidssted fysiskArbeidssted = new FysiskArbeidssted();
        fysiskArbeidssted.getAdresse().setLandkode("DE");
        søknad.arbeidPaaLand.setFysiskeArbeidssteder(new ArrayList<>());
        søknad.arbeidPaaLand.getFysiskeArbeidssteder().add(fysiskArbeidssted);
        søknad.juridiskArbeidsgiverNorge.setEkstraArbeidsgivere(ekstraArbeidsgivere);
        søknad.foretakUtland = foretakUtland;
        søknad.soeknadsland.getLandkoder().add("DE");

        return søknad;
    }
}
