package no.nav.melosys.service.unntak.kontroll;

import java.util.ArrayList;
import java.util.List;

import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.behandlingsgrunnlag.data.ForetakUtland;
import no.nav.melosys.domain.behandlingsgrunnlag.data.arbeidssteder.FysiskArbeidssted;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.service.validering.Kontrollfeil;
import org.apache.commons.lang.StringUtils;

public interface AdresseUtlandKontroller {
    String ARBEIDSSTED_FIRMANAVN = "behandlingsgrunnlag.arbeidPaaLand.fysiskeArbeidssteder[%d].virksomhetNavn";
    String ARBEIDSSTED_LAND = "behandlingsgrunnlag.arbeidPaaLand.fysiskeArbeidssteder[%d].adresse.landkode";
    String FORETAK_UTLAND_NAVN = "behandlingsgrunnlag.foretakUtland[%d].navn";
    String FORETAK_UTLAND_LAND = "behandlingsgrunnlag.foretakUtland[%d].adresse.landkode";

    static Kontrollfeil arbeidsstedManglerFelter(BehandlingsgrunnlagData behandlingsgrunnlagData) {
        List<FysiskArbeidssted> fysiskArbeidsstedListe = behandlingsgrunnlagData.arbeidPaaLand.fysiskeArbeidssteder;
        List<String> felter = new ArrayList<>();

        for (int i = 0; i < fysiskArbeidsstedListe.size(); i++) {
            FysiskArbeidssted fysiskArbeidssted = fysiskArbeidsstedListe.get(i);
            if (StringUtils.isBlank(fysiskArbeidssted.virksomhetNavn)) {
                felter.add(String.format(ARBEIDSSTED_FIRMANAVN, i));
            }
            if (StringUtils.isBlank(fysiskArbeidssted.adresse.getLandkode())) {
                felter.add(String.format(ARBEIDSSTED_LAND, i));
            }
        }
        return felter.isEmpty() ? null
            : new Kontrollfeil(Kontroll_begrunnelser.MANGLENDE_OPPL_ARBEIDSSTED, felter);
    }

    static Kontrollfeil foretakUtlandManglerFelter(BehandlingsgrunnlagData behandlingsgrunnlagData) {
        List<ForetakUtland> foretakUtlandListe = behandlingsgrunnlagData.foretakUtland;
        List<String> felter = new ArrayList<>();

        for (int i = 0; i < foretakUtlandListe.size(); i++) {
            ForetakUtland foretakUtland = foretakUtlandListe.get(i);
            if (StringUtils.isBlank(foretakUtland.navn)) {
                felter.add(String.format(FORETAK_UTLAND_NAVN, i));
            }
            if (StringUtils.isBlank(foretakUtland.adresse.getLandkode())) {
                felter.add(String.format(FORETAK_UTLAND_LAND, i));
            }
        }
        return felter.isEmpty() ? null
            : new Kontrollfeil(Kontroll_begrunnelser.MANGLENDE_OPPL_ANDRE_ARBEIDSFORHOLD_UTL, felter);
    }
}
