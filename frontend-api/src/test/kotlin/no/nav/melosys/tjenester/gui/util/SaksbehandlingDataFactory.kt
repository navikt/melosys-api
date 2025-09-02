package no.nav.melosys.tjenester.gui.util;

import java.time.LocalDate;
import java.util.ArrayList;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.FagsakTestFactory;
import no.nav.melosys.domain.mottatteopplysninger.Soeknad;
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.FysiskArbeidssted;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.Sakstyper;

import static no.nav.melosys.domain.kodeverk.Aktoersroller.BRUKER;

public final class SaksbehandlingDataFactory {

    public static Fagsak lagFagsak() {
        return FagsakTestFactory.builder()
            .medBruker()
            .medGsakSaksnummer()
            .build();
    }

    public static Soeknad lagSøknadDokument() {
        Soeknad soeknad = new Soeknad();
        FysiskArbeidssted fysiskArbeidssted = new FysiskArbeidssted();
        fysiskArbeidssted.getAdresse().setLandkode("SE");
        soeknad.soeknadsland.getLandkoder().add(Landkoder.DK.getKode());
        soeknad.soeknadsland.setFlereLandUkjentHvilke(false);
        soeknad.arbeidPaaLand.setFysiskeArbeidssteder(new ArrayList<>());
        soeknad.arbeidPaaLand.getFysiskeArbeidssteder().add(fysiskArbeidssted);
        soeknad.oppholdUtland.getOppholdslandkoder().add("FI");
        soeknad.periode = new no.nav.melosys.domain.mottatteopplysninger.data.Periode(
            LocalDate.of(2019,1,1), LocalDate.of(2019,2,1));
        return soeknad;
    }
}
