package no.nav.melosys.service.kontroll.regler

import no.nav.melosys.domain.mottatteopplysninger.data.ForetakUtland
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.FysiskArbeidssted
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.LuftfartBase
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.MaritimtArbeid
import org.apache.commons.lang.StringUtils


class AdresseRegler {
    companion object {
        fun manglerArbeidsstedLandkode(arbeidssted: FysiskArbeidssted?): Boolean {
            return arbeidssted?.adresse == null || StringUtils.isBlank(arbeidssted.adresse.landkode)
        }

        fun manglerArbeidsstedPoststed(arbeidssted: FysiskArbeidssted?): Boolean {
            return arbeidssted?.adresse == null || StringUtils.isBlank(arbeidssted.adresse.poststed)
        }

        fun manglerArbeidsstedVirksomhetsnavn(arbeidssted: FysiskArbeidssted?): Boolean {
            return arbeidssted == null || StringUtils.isBlank(arbeidssted.virksomhetNavn)
        }

        fun manglerArbeidsstedVirksomhetsnavnInnretningEnhetNavn(maritimtArbeid: MaritimtArbeid?): Boolean {
            return maritimtArbeid == null || StringUtils.isBlank(maritimtArbeid.enhetNavn)
        }

        fun manglerArbeidsstedVirksomhetsnavnInnretningLandssokkel(maritimtArbeid: MaritimtArbeid?): Boolean {
            return maritimtArbeid == null || StringUtils.isBlank(maritimtArbeid.innretningLandkode)
        }

        fun manglerArbeidsstedVirksomhetsnavnInnretningLandsTerritorialFarvann(maritimtArbeid: MaritimtArbeid?): Boolean {
            return maritimtArbeid == null || StringUtils.isBlank(maritimtArbeid.territorialfarvannLandkode)
        }

        fun manglerArbeidsstedVirksomhetsnavnInnretningFlaggstat(maritimtArbeid: MaritimtArbeid?): Boolean {
            return maritimtArbeid == null || StringUtils.isBlank(maritimtArbeid.flaggLandkode)
        }

        fun manglerArbeidsstedVirksomhetsnavnLuftfartBaseHjemmebaseNavn(luftfartBase: LuftfartBase?): Boolean {
            return luftfartBase == null || StringUtils.isBlank(luftfartBase.hjemmebaseNavn)
        }

        fun manglerArbeidsstedVirksomhetsnavnLuftfartBaseHjemmebaseLand(luftfartBase: LuftfartBase?): Boolean {
            return luftfartBase == null || StringUtils.isBlank(luftfartBase.hjemmebaseLand)
        }

        fun manglerForetakUtlandLandkode(foretakUtland: ForetakUtland): Boolean {
            return StringUtils.isBlank(foretakUtland.adresse.landkode)
        }

        fun manglerForetakUtlandNavn(foretakUtland: ForetakUtland?): Boolean {
            return foretakUtland == null || StringUtils.isBlank(foretakUtland.navn)
        }

        fun manglerForetakUtlandPoststed(foretakUtland: ForetakUtland?): Boolean {
            return foretakUtland == null || StringUtils.isBlank(foretakUtland.adresse.poststed)
        }

        fun manglerForetakUtlandPostnummer(foretakUtland: ForetakUtland?): Boolean {
            return foretakUtland == null || StringUtils.isBlank(foretakUtland.adresse.postnummer)
        }

        fun manglerForetakUtlandAdresse(foretakUtland: ForetakUtland?): Boolean {
            return foretakUtland == null || StringUtils.isBlank(foretakUtland.adresse.gatenavn)
        }



        fun manglerForetakUtlandRegistreringsnummer(foretakUtland: ForetakUtland?): Boolean {
            return foretakUtland == null || StringUtils.isBlank(foretakUtland.orgnr)
        }
    }
}
