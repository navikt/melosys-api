package no.nav.melosys.service.kontroll.feature.arbeidutland.kontroll

import no.nav.melosys.domain.kodeverk.begrunnelser.Fartsomrader
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData
import no.nav.melosys.exception.KontrolldataFeilType
import no.nav.melosys.service.kontroll.regler.AdresseRegler
import no.nav.melosys.service.validering.Kontrollfeil


open class ArbeidUtlandKontroll {
    companion object {
        @JvmStatic
        fun arbeidsstedLandManglerFelter(mottatteOpplysningerData: MottatteOpplysningerData): Kontrollfeil? {
            val fysiskArbeidsstedListe = mottatteOpplysningerData.arbeidPaaLand.fysiskeArbeidssteder
            var erUfullstendigUtfyllt = false

            for (fysiskArbeidssted in fysiskArbeidsstedListe) {
                val manglerArbeidsstedVirksomhetsnavn = AdresseRegler.manglerArbeidsstedVirksomhetsnavn(fysiskArbeidssted)
                val manglerArbeidsstedLandkode = AdresseRegler.manglerArbeidsstedLandkode(fysiskArbeidssted)


                if (manglerArbeidsstedVirksomhetsnavn || manglerArbeidsstedLandkode) {
                    erUfullstendigUtfyllt = true
                }
            }
            return if (erUfullstendigUtfyllt) Kontrollfeil(Kontroll_begrunnelser.MANGLENDE_OPPL_ARBEIDSSTED_LAND, KontrolldataFeilType.FEIL)
            else null
        }

        @JvmStatic
        fun maritimtArbeidsstedManglerFelter(mottatteOpplysningerData: MottatteOpplysningerData): Kontrollfeil? {
            val maritimtArbeidListe = mottatteOpplysningerData.maritimtArbeid

            if (maritimtArbeidListe.isEmpty()) {
                return null
            }

            var erUfullstendigUtfyllt = false
            for (maritimtArbeid in maritimtArbeidListe) {

                val manglerArbeidsstedVirksomhetsnavnInnretningEnhetNavn =
                    AdresseRegler.manglerArbeidsstedVirksomhetsnavnInnretningEnhetNavn(maritimtArbeid)
                val manglerArbeidsstedVirksomhetsnavnInnretningLandsTerritorialFarvann =
                    AdresseRegler.manglerArbeidsstedVirksomhetsnavnInnretningLandsTerritorialFarvann(maritimtArbeid)
                val manglerArbeidsstedVirksomhetsnavnInnretningFlaggstat =
                    AdresseRegler.manglerArbeidsstedVirksomhetsnavnInnretningFlaggstat(maritimtArbeid)

                val erSkip = maritimtArbeid.fartsomradeKode != null

                if(erSkip) {
                    if (maritimtArbeid.fartsomradeKode == Fartsomrader.INNENRIKS) {
                        if (manglerArbeidsstedVirksomhetsnavnInnretningEnhetNavn || manglerArbeidsstedVirksomhetsnavnInnretningLandsTerritorialFarvann) {
                            erUfullstendigUtfyllt = true
                        }
                    } else {
                        if (manglerArbeidsstedVirksomhetsnavnInnretningEnhetNavn || manglerArbeidsstedVirksomhetsnavnInnretningFlaggstat) {
                            erUfullstendigUtfyllt = true
                        }
                    }
                }
            }

            return if (erUfullstendigUtfyllt) Kontrollfeil(Kontroll_begrunnelser.MANGLENDE_OPPL_ARBEIDSSTED_MARITIM, KontrolldataFeilType.FEIL)
            else null
        }

        @JvmStatic
        fun offshoreArbeidsstedManglerFelter(mottatteOpplysningerData: MottatteOpplysningerData): Kontrollfeil? {
            val maritimtArbeidListe = mottatteOpplysningerData.maritimtArbeid

            if (maritimtArbeidListe.isEmpty()) {
                return null
            }

            var erUfullstendigUtfyllt = false
            for (maritimtArbeid in maritimtArbeidListe) {

                val manglerArbeidsstedVirksomhetsnavnInnretningEnhetNavn =
                    AdresseRegler.manglerArbeidsstedVirksomhetsnavnInnretningEnhetNavn(maritimtArbeid)
                val manglerArbeidsstedVirksomhetsnavnInnretningLandssokkel =
                    AdresseRegler.manglerArbeidsstedVirksomhetsnavnInnretningLandssokkel(maritimtArbeid)

                val erSkip = maritimtArbeid.fartsomradeKode != null

                if (!erSkip) {
                    if (manglerArbeidsstedVirksomhetsnavnInnretningEnhetNavn || manglerArbeidsstedVirksomhetsnavnInnretningLandssokkel) {
                        erUfullstendigUtfyllt = true
                    }
                }
            }

            return if (erUfullstendigUtfyllt) Kontrollfeil(Kontroll_begrunnelser.MANGLENDE_OPPL_ARBEIDSSTED_OFFSHORE, KontrolldataFeilType.FEIL)
            else null
        }



        @JvmStatic
        fun luftfartArbeidsstedManglerFelter(mottatteOpplysningerData: MottatteOpplysningerData): Kontrollfeil? {
            val luftfartBaseListe = mottatteOpplysningerData.luftfartBaser

            if (luftfartBaseListe.isEmpty()) {
                return null
            }

            var erUfullstendigUtfyllt = false

            for (luftfartBase in luftfartBaseListe) {
                val manglerArbeidsstedVirksomhetsnavnLuftfartBaseHjemmebaseNavn =
                    AdresseRegler.manglerArbeidsstedVirksomhetsnavnLuftfartBaseHjemmebaseNavn(luftfartBase)
                val manglerArbeidsstedVirksomhetsnavnLuftfartBaseHjemmebaseLand =
                    AdresseRegler.manglerArbeidsstedVirksomhetsnavnLuftfartBaseHjemmebaseLand(luftfartBase)

                if (manglerArbeidsstedVirksomhetsnavnLuftfartBaseHjemmebaseNavn || manglerArbeidsstedVirksomhetsnavnLuftfartBaseHjemmebaseLand) {
                    erUfullstendigUtfyllt = true
                }
            }
            return if (erUfullstendigUtfyllt) Kontrollfeil(Kontroll_begrunnelser.MANGLENDE_OPPL_ARBEIDSSTED_LUFTFART, KontrolldataFeilType.FEIL)
            else null
        }

        @JvmStatic
        fun foretakUtlandManglerFelter(mottatteOpplysningerData: MottatteOpplysningerData): Kontrollfeil? {
            val foretakUtlandListe = mottatteOpplysningerData.foretakUtland

            var erUfullstendigUtfyllt = false

            for (foretakUtland in foretakUtlandListe) {
                val manglerForetakUtlandNavn = AdresseRegler.manglerForetakUtlandNavn(foretakUtland)
                val manglerForetakUtlandLandkode = AdresseRegler.manglerForetakUtlandLandkode(foretakUtland)
                val manglerForetakUtlandPoststed = AdresseRegler.manglerForetakUtlandPoststed(foretakUtland)
                val manglerForetakUtlandPostnummer = AdresseRegler.manglerForetakUtlandPostnummer(foretakUtland)
                val manglerForetakUtlandAdresse = AdresseRegler.manglerForetakUtlandAdresse(foretakUtland)


                if (manglerForetakUtlandNavn || manglerForetakUtlandLandkode || manglerForetakUtlandPoststed || manglerForetakUtlandPostnummer || manglerForetakUtlandAdresse) {
                    if(!foretakUtland.selvstendigNæringsvirksomhet) {
                        erUfullstendigUtfyllt = true
                    }
                }
            }

            return if (erUfullstendigUtfyllt) Kontrollfeil(Kontroll_begrunnelser.MANGLENDE_OPPL_ARBEIDSFORHOLD_UTL, KontrolldataFeilType.FEIL)
            else null
        }

        @JvmStatic
        fun selvstendigUtlandManglerFelter(mottatteOpplysningerData: MottatteOpplysningerData): Kontrollfeil? {
            val foretakUtlandListe = mottatteOpplysningerData.foretakUtland

            var erUfullstendigUtfyllt = false

            for (foretakUtland in foretakUtlandListe) {
                val manglerForetakUtlandNavn = AdresseRegler.manglerForetakUtlandNavn(foretakUtland)
                val manglerForetakUtlandLandkode = AdresseRegler.manglerForetakUtlandLandkode(foretakUtland)
                val manglerForetakUtlandPoststed = AdresseRegler.manglerForetakUtlandPoststed(foretakUtland)
                val manglerForetakUtlandPostnummer = AdresseRegler.manglerForetakUtlandPostnummer(foretakUtland)
                val manglerForetakUtlandAdresse = AdresseRegler.manglerForetakUtlandAdresse(foretakUtland)


                if (manglerForetakUtlandNavn || manglerForetakUtlandLandkode || manglerForetakUtlandPoststed || manglerForetakUtlandPostnummer || manglerForetakUtlandAdresse) {
                    if(foretakUtland.selvstendigNæringsvirksomhet) {
                        erUfullstendigUtfyllt = true
                    }
                }
            }

            return if (erUfullstendigUtfyllt) Kontrollfeil(Kontroll_begrunnelser.MANGLENDE_OPPL_SELVSTENDIG_ARBEIDSFORHOLD_UTL, KontrolldataFeilType.FEIL)
            else null
        }
    }
}
