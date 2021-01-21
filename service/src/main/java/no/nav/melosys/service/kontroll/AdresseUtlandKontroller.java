package no.nav.melosys.service.kontroll;

import java.util.ArrayList;
import java.util.List;

import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.behandlingsgrunnlag.data.FysiskArbeidssted;
import no.nav.melosys.domain.behandlingsgrunnlag.data.ForetakUtland;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.service.validering.Kontrollfeil;

public abstract class AdresseUtlandKontroller {

    static final String ARBEID_UTLAND_NAVN = "behandlingsgrunnlag.arbeidUtland[%d].foretakNavn";
    static final String ARBEID_UTLAND_LAND = "behandlingsgrunnlag.arbeidUtland[%d].adresse.landkode";
    static final String FORETAK_UTLAND_NAVN = "behandlingsgrunnlag.foretakUtland[%d].navn";
    static final String FORETAK_UTLAND_LAND = "behandlingsgrunnlag.foretakUtland[%d].adresse.landkode";

    public static Kontrollfeil arbeidsstedManglerFelter(BehandlingsgrunnlagData behandlingsgrunnlagData) {
        List<FysiskArbeidssted> fysiskArbeidsstedListe = behandlingsgrunnlagData.fysiskeArbeidsstederUtland;
        List<String> felter = new ArrayList<>();

        for (int i = 0; i < fysiskArbeidsstedListe.size(); i++) {
            FysiskArbeidssted fysiskArbeidssted = fysiskArbeidsstedListe.get(i);
            if (fysiskArbeidssted.foretakNavn == null) {
                felter.add(String.format(ARBEID_UTLAND_NAVN, i));
            }
            if (fysiskArbeidssted.adresse.landkode == null) {
                felter.add(String.format(ARBEID_UTLAND_LAND, i));
            }
        }
        return felter.isEmpty() ? null
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
        return felter.isEmpty() ? null
            : new Kontrollfeil(Kontroll_begrunnelser.MANGLENDE_OPPL_ANDRE_ARBEIDSFORHOLD_UTL, felter);
    }
}
