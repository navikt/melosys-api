package no.nav.melosys.service;

import java.util.ArrayList;
import java.util.List;

import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsGrunnlagType;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.dokument.soeknad.ArbeidUtland;
import no.nav.melosys.domain.dokument.soeknad.ForetakUtland;
import no.nav.melosys.domain.dokument.soeknad.SelvstendigForetak;
import no.nav.melosys.domain.behandlingsgrunnlag.SoeknadDokument;

public final class BehandlingsgrunnlagStub {


    public static Behandlingsgrunnlag lagBehandlingsgrunnlag(List<String> selvstendigeForetak, List<ForetakUtland> foretakUtland, List<String> ekstraArbeidsgivere) {
        Behandlingsgrunnlag behandlingsgrunnlag = new Behandlingsgrunnlag();
        behandlingsgrunnlag.setType(BehandlingsGrunnlagType.SØKNAD);
        behandlingsgrunnlag.setBehandlingsgrunnlagdata(lagBehandlingsgrunnlagdata(selvstendigeForetak, foretakUtland, ekstraArbeidsgivere));
        return behandlingsgrunnlag;
    }

    private static BehandlingsgrunnlagData lagBehandlingsgrunnlagdata(List<String> selvstendigeForetak, List<ForetakUtland> foretakUtland, List<String> ekstraArbeidsgivere) {
        SoeknadDokument søknad = new SoeknadDokument();
        for (String orgnr : selvstendigeForetak) {
            SelvstendigForetak selvstendigForetak = new SelvstendigForetak();
            selvstendigForetak.orgnr = orgnr;
            søknad.selvstendigArbeid.selvstendigForetak.add(selvstendigForetak);
        }

        ArbeidUtland arbeidUtland = new ArbeidUtland();
        arbeidUtland.adresse.landkode = "DE";
        søknad.arbeidUtland = new ArrayList<>();
        søknad.arbeidUtland.add(arbeidUtland);
        søknad.juridiskArbeidsgiverNorge.ekstraArbeidsgivere = ekstraArbeidsgivere;
        søknad.foretakUtland = foretakUtland;
        søknad.soeknadsland.landkoder.add("DE");

        return søknad;
    }
}
