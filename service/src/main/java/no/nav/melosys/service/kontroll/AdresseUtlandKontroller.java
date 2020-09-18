package no.nav.melosys.service.kontroll;

import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.dokument.soeknad.ArbeidUtland;
import no.nav.melosys.domain.dokument.soeknad.ForetakUtland;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.service.validering.Kontrollfeil;

import java.util.ArrayList;
import java.util.List;

public abstract class AdresseUtlandKontroller {

    static final String ARBEID_UTLAND_NAVN = "behandlingsgrunnlag.arbeidUtland[%d].foretakNavn";
    static final String ARBEID_UTLAND_LAND = "behandlingsgrunnlag.arbeidUtland[%d].adresse.landkode";
    static final String FORETAK_UTLAND_NAVN = "behandlingsgrunnlag.foretakUtland[%d].navn";
    static final String FORETAK_UTLAND_LAND = "behandlingsgrunnlag.foretakUtland[%d].adresse.landkode";

    public static Kontrollfeil arbeidsstedManglerFelter(BehandlingsgrunnlagData behandlingsgrunnlagData) {
        List<ArbeidUtland> arbeidUtlandListe = behandlingsgrunnlagData.arbeidUtland;
        List<String> felter = new ArrayList<>();

        for (int i = 0; i < arbeidUtlandListe.size(); i++) {
            ArbeidUtland arbeidUtland = arbeidUtlandListe.get(i);
            if (arbeidUtland.foretakNavn == null) {
                felter.add(String.format(ARBEID_UTLAND_NAVN, i));
            }
            if (arbeidUtland.adresse.landkode == null) {
                felter.add(String.format(ARBEID_UTLAND_LAND, i));
            }
        }
        return felter.size() == 0 ? null
            : new Kontrollfeil(Kontroll_begrunnelser.MANGLENDE_OPPL_ARBEIDSSTED, felter);
    }

    public static Kontrollfeil foretakUtlandManglerFelter(BehandlingsgrunnlagData behandlingsgrunnlagData) {
        List<ForetakUtland> foretakUtlandListe = behandlingsgrunnlagData.foretakUtland;
        List<String> felter = new ArrayList<>();

        for (int i = 0; i < foretakUtlandListe.size(); i++) {
            ForetakUtland foretakUtland = foretakUtlandListe.get(i);
            if (foretakUtland.navn == null) {
                felter.add(String.format(FORETAK_UTLAND_NAVN, i));
            }
            if (foretakUtland.adresse.landkode == null) {
                felter.add(String.format(FORETAK_UTLAND_LAND, i));
            }
        }
        return felter.size() == 0 ? null
            : new Kontrollfeil(Kontroll_begrunnelser.MANGLENDE_OPPL_ANDRE_ARBEIDSFORHOLD_UTL, felter);
    }
}
