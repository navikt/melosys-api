package no.nav.melosys.service.kontroll.feature.arbeidutland.kontroll;

import java.util.ArrayList;
import java.util.List;

import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.behandlingsgrunnlag.data.ForetakUtland;
import no.nav.melosys.domain.behandlingsgrunnlag.data.arbeidssteder.FysiskArbeidssted;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.service.kontroll.regler.AdresseRegler;
import no.nav.melosys.service.validering.Kontrollfeil;

public class ArbeidUtlandKontroll {
    static String ARBEIDSSTED_FIRMANAVN = "behandlingsgrunnlag.arbeidPaaLand.fysiskeArbeidssteder[%d].virksomhetNavn";
    static String ARBEIDSSTED_LAND = "behandlingsgrunnlag.arbeidPaaLand.fysiskeArbeidssteder[%d].adresse.landkode";
    static String FORETAK_UTLAND_NAVN = "behandlingsgrunnlag.foretakUtland[%d].navn";
    static String FORETAK_UTLAND_LAND = "behandlingsgrunnlag.foretakUtland[%d].adresse.landkode";

    public static Kontrollfeil arbeidsstedManglerFelter(BehandlingsgrunnlagData behandlingsgrunnlagData) {
        List<FysiskArbeidssted> fysiskArbeidsstedListe = behandlingsgrunnlagData.arbeidPaaLand.fysiskeArbeidssteder;

        List<String> felterMedFeil = new ArrayList<>();
        for (int i = 0; i < fysiskArbeidsstedListe.size(); i++) {
            FysiskArbeidssted fysiskArbeidssted = fysiskArbeidsstedListe.get(i);
            if (AdresseRegler.manglerArbeidsstedVirksomhetsnavn(fysiskArbeidssted)) {
                felterMedFeil.add(String.format(ARBEIDSSTED_FIRMANAVN, i));
            }
            if (AdresseRegler.manglerArbeidsstedLandkode(fysiskArbeidssted)) {
                felterMedFeil.add(String.format(ARBEIDSSTED_LAND, i));
            }
        }
        return felterMedFeil.isEmpty() ? null
            : new Kontrollfeil(Kontroll_begrunnelser.MANGLENDE_OPPL_ARBEIDSSTED, felterMedFeil);
    }

    public static Kontrollfeil foretakUtlandManglerFelter(BehandlingsgrunnlagData behandlingsgrunnlagData) {
        List<ForetakUtland> foretakUtlandListe = behandlingsgrunnlagData.foretakUtland;

        List<String> felterMedFeil = new ArrayList<>();
        for (int i = 0; i < foretakUtlandListe.size(); i++) {
            ForetakUtland foretakUtland = foretakUtlandListe.get(i);
            if (AdresseRegler.manglerForetakUtlandNavn(foretakUtland)) {
                felterMedFeil.add(String.format(FORETAK_UTLAND_NAVN, i));
            }
            if (AdresseRegler.manglerForetakUtlandLandkode(foretakUtland)) {
                felterMedFeil.add(String.format(FORETAK_UTLAND_LAND, i));
            }
        }
        return felterMedFeil.isEmpty() ? null
            : new Kontrollfeil(Kontroll_begrunnelser.MANGLENDE_OPPL_ANDRE_ARBEIDSFORHOLD_UTL, felterMedFeil);
    }
}
