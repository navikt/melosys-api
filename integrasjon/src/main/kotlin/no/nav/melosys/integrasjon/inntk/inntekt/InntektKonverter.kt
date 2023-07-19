package no.nav.melosys.integrasjon.inntk.inntekt

import no.nav.melosys.domain.Saksopplysning
import no.nav.melosys.domain.dokument.inntekt.*
import no.nav.melosys.domain.dokument.inntekt.tillegsinfo.Tilleggsinformasjon
import java.time.LocalDateTime

class InntektKonverter {

    fun lagSaksopplysning(inntektResponse: InntektResponse): Saksopplysning {
        return Saksopplysning().apply {
            dokument = InntektDokument().apply {
                arbeidsInntektMaanedListe = inntektResponse.arbeidsInntektMaaned.map { aim ->
                    ArbeidsInntektMaaned().apply {
                        aarMaaned = aim.aarMaaned
                        avvikListe = aim.avvikListe?.map {
                            Avvik().apply {
                                ident = it.ident?.identifikator
                                opplysningspliktigID = it.opplysningspliktig?.identifikator
                                virksomhetID = it.virksomhet?.identifikator
                                avvikPeriode = it.avvikPeriode
                                tekst = it.tekst
                            }
                        }
                        arbeidsInntektInformasjon = ArbeidsInntektInformasjon().apply {
                            inntektListe = aim.arbeidsInntektInformasjon?.inntektListe?.map {
                                Inntekt().apply {
                                    arbeidsforholdREF = it.arbeidsforholdREF
                                    beloep = it.beloep
                                    fordel = it.fordel
                                    inntektskilde = it.inntektskilde
                                    inntektsperiodetype = it.inntektsperiodetype
                                    inntektsstatus = it.inntektsstatus
                                    levereringstidspunkt = LocalDateTime.of(
                                        it.leveringstidspunkt.year,
                                        it.leveringstidspunkt.month,
                                        1,
                                        0,
                                        0
                                    )
                                    opptjeningsland = it.opptjeningsland
                                    skattemessigBosattLand = it.skattemessigBosattLand
                                    utbetaltIPeriode = it.utbetaltIMaaned // Er dette riktig?
                                    opplysningspliktigID = it.opplysningspliktig?.identifikator
                                    inntektsinnsenderID = null // TODO: Hva skal denne være?
                                    virksomhetID = it.virksomhet?.identifikator
                                    tilleggsinformasjon = Tilleggsinformasjon().apply {
                                        kategori = it.tilleggsinformasjon?.kategori
                                        // TODO: tilleggsinformasjonDetaljer blir litt mer komplisert å mappe, trenger vi dette?
                                        //tilleggsinformasjonDetaljer = it.tilleggsinformasjon.tilleggsinformasjonDetaljer
                                    }
                                    inntektsmottakerID = it.inntektsmottaker?.identifikator
                                    inngaarIGrunnlagForTrekk = it.inngaarIGrunnlagForTrekk
                                    utloeserArbeidsgiveravgift = it.utloeserArbeidsgiveravgift
                                    informasjonsstatus = it.informasjonsstatus
                                    beskrivelse = it.beskrivelse
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
