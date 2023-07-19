package no.nav.melosys.integrasjon.inntk.inntekt

import no.nav.melosys.domain.Saksopplysning
import no.nav.melosys.domain.dokument.inntekt.ArbeidsInntektMaaned
import no.nav.melosys.domain.dokument.inntekt.Avvik
import no.nav.melosys.domain.dokument.inntekt.InntektDokument

class InntektKonverter {

    fun lagSaksopplysning(inntektResponse: InntektResponse): Saksopplysning {
        return Saksopplysning().apply {
            dokument = InntektDokument().apply {
                arbeidsInntektMaanedListe = inntektResponse.arbeidsInntektMaaned?.map {
                    ArbeidsInntektMaaned().apply {
                        aarMaaned = it.aarMaaned
                        avvikListe = it.avvikListe?.map {
                            Avvik().apply {
                                ident = it.ident?.identifikator
                                opplysningspliktigID = it.opplysningspliktig?.identifikator
                                virksomhetID = it.virksomhet?.identifikator
                                avvikPeriode = it.avvikPeriode
                                tekst = it.tekst
                            }
                        }
                    }
                }
            }
        }
    }
}
