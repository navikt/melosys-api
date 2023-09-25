package no.nav.melosys.integrasjon.inntk.inntekt

import no.nav.melosys.domain.Saksopplysning
import no.nav.melosys.domain.dokument.inntekt.*
import no.nav.melosys.domain.dokument.inntekt.ArbeidsInntektInformasjon
import no.nav.melosys.domain.dokument.inntekt.ArbeidsInntektMaaned
import no.nav.melosys.domain.dokument.inntekt.Avvik
import no.nav.melosys.domain.dokument.inntekt.Inntekt
import no.nav.melosys.domain.dokument.inntekt.tillegsinfo.*
import no.nav.melosys.integrasjon.inntk.inntekt.InntektResponse.TilleggsinformasjonDetaljerType.*
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
                                        it.leveringstidspunkt.year, it.leveringstidspunkt.month, 1, 0, 0
                                    )
                                    opptjeningsland = it.opptjeningsland
                                    skattemessigBosattLand = it.skattemessigBosattLand
                                    utbetaltIPeriode = it.utbetaltIMaaned
                                    opplysningspliktigID = it.opplysningspliktig?.identifikator
                                    // Denne finnes ikke i restApi, og tror ikke den er i bruk
                                    // https://nav-it.slack.com/archives/CLMJJ882W/p1689859452879089?thread_ts=1689168417.802869&cid=CLMJJ882W
                                    inntektsinnsenderID = null
                                    virksomhetID = it.virksomhet?.identifikator
                                    tilleggsinformasjon = Tilleggsinformasjon().apply {
                                        kategori = it.tilleggsinformasjon?.kategori
                                        tilleggsinformasjonDetaljer = mapTilleggsinformasjonDetaljer(
                                            it.tilleggsinformasjon?.tilleggsinformasjonDetaljer
                                        )
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


    private fun mapTilleggsinformasjonDetaljer(tilleggsinformasjonDetaljer: InntektResponse.TilleggsinformasjonDetaljer?): TilleggsinformasjonDetaljer? =
        when (tilleggsinformasjonDetaljer?.detaljerType) {
            ALDERSUFOEREETTERLATTEAVTALEFESTETOGKRIGSPENSJON ->
                PensjonOgUfoere().apply {
                    (tilleggsinformasjonDetaljer as InntektResponse.AldersUfoereEtterlatteAvtalefestetOgKrigspensjon).let {
                        grunnpensjonbeløp = it.grunnpensjonbeloep
                        heravEtterlattepensjon = it.heravEtterlattepensjon
                        pensjonsgrad = it.pensjonsgrad
                        tidsrom = Periode().apply {
                            fom = it.tidsromFom
                            tom = it.tidsromTom
                        }
                    }
                }

            BARNEPENSJONOGUNDERHOLDSBIDRAG ->
                BarnepensjonOgUnderholdsbidrag().apply {
                    (tilleggsinformasjonDetaljer as InntektResponse.BarnepensjonOgUnderholdsbidrag).let {
                        forsørgersFødselnummer = it.forsoergersFoedselnummer
                        tidsrom = Periode().apply {
                            fom = it.tidsromFom
                            tom = it.tidsromTom
                        }
                    }
                }

            BONUSFRAFORSVARET -> BonusFraForsvaret().apply {
                (tilleggsinformasjonDetaljer as InntektResponse.BonusFraForsvaret).let {
                    åretUtbetalingenGjelderFor = it.aaretUtbetalingenGjelderFor
                }
            }

            ETTERBETALINGSPERIODE -> Etterbetalingsperiode().apply {
                (tilleggsinformasjonDetaljer as InntektResponse.Etterbetalingsperiode).let {
                    etterbetalingsperiode = Periode().apply {
                        fom = it.etterbetalingsperiodeFom
                        tom = it.etterbetalingsperiodeTom
                    }
                }
            }

            INNTJENINGSFORHOLD -> Inntjeningsforhold().apply {
                (tilleggsinformasjonDetaljer as InntektResponse.Inntjeningsforhold).let {
                    inntjeningsforhold = it.spesielleInntjeningsforhold
                }
            }

            SVALBARDINNTEKT -> Svalbardinntekt().apply {
                (tilleggsinformasjonDetaljer as InntektResponse.Svalbardinntekt).let {
                    antallDager = it.antallDager
                    betaltTrygdeavgift = it.betaltTrygdeavgift
                }
            }

            REISEKOSTOGLOSJI -> ReiseKostOgLosji().apply {
                (tilleggsinformasjonDetaljer as InntektResponse.ReiseKostOgLosji).let {
                    persontype = it.persontype
                }
            }
            else -> null
        }
}
