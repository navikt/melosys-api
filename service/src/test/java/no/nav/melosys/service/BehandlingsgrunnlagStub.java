package no.nav.melosys.service;

import java.util.ArrayList;
import java.util.List;

import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.behandlingsgrunnlag.Soeknad;
import no.nav.melosys.domain.behandlingsgrunnlag.data.FysiskArbeidssted;
import no.nav.melosys.domain.behandlingsgrunnlag.data.ForetakUtland;
import no.nav.melosys.domain.behandlingsgrunnlag.data.SelvstendigForetak;
import no.nav.melosys.domain.kodeverk.Behandlingsgrunnlagtyper;

public final class BehandlingsgrunnlagStub {


    public static Behandlingsgrunnlag lagBehandlingsgrunnlag(List<String> selvstendigeForetak, List<ForetakUtland> foretakUtland, List<String> ekstraArbeidsgivere) {
        Behandlingsgrunnlag behandlingsgrunnlag = new Behandlingsgrunnlag();
        behandlingsgrunnlag.setType(Behandlingsgrunnlagtyper.SØKNAD_A1_YRKESAKTIVE_EØS);
        behandlingsgrunnlag.setBehandlingsgrunnlagdata(lagBehandlingsgrunnlagdata(selvstendigeForetak, foretakUtland, ekstraArbeidsgivere));
        return behandlingsgrunnlag;
    }

    private static BehandlingsgrunnlagData lagBehandlingsgrunnlagdata(List<String> selvstendigeForetak, List<ForetakUtland> foretakUtland, List<String> ekstraArbeidsgivere) {
        Soeknad søknad = new Soeknad();
        for (String orgnr : selvstendigeForetak) {
            SelvstendigForetak selvstendigForetak = new SelvstendigForetak();
            selvstendigForetak.orgnr = orgnr;
            søknad.selvstendigArbeid.selvstendigForetak.add(selvstendigForetak);
        }

        FysiskArbeidssted fysiskArbeidssted = new FysiskArbeidssted();
        fysiskArbeidssted.adresse.landkode = "DE";
        søknad.arbeidPaaLand.fysiskeArbeidssteder = new ArrayList<>();
        søknad.arbeidPaaLand.fysiskeArbeidssteder.add(fysiskArbeidssted);
        søknad.juridiskArbeidsgiverNorge.ekstraArbeidsgivere = ekstraArbeidsgivere;
        søknad.foretakUtland = foretakUtland;
        søknad.soeknadsland.landkoder.add("DE");

        return søknad;
    }
}
