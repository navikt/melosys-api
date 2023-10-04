package no.nav.melosys.integrasjon.inntk.inntekt

import no.nav.melosys.domain.Saksopplysning
import no.nav.melosys.domain.dokument.inntekt.*
import no.nav.melosys.domain.dokument.inntekt.ArbeidsInntektInformasjon
import no.nav.melosys.domain.dokument.inntekt.ArbeidsInntektMaaned
import no.nav.melosys.domain.dokument.inntekt.Avvik
import no.nav.melosys.domain.dokument.inntekt.Inntekt
import no.nav.melosys.domain.dokument.inntekt.inntektstype.Loennsinntekt
import no.nav.melosys.domain.dokument.inntekt.inntektstype.Naeringsinntekt
import no.nav.melosys.domain.dokument.inntekt.inntektstype.PensjonEllerTrygd
import no.nav.melosys.domain.dokument.inntekt.inntektstype.YtelseFraOffentlige
import no.nav.melosys.domain.dokument.inntekt.tillegsinfo.*
import no.nav.melosys.exception.TekniskException
import no.nav.melosys.integrasjon.inntk.inntekt.InntektResponse.TilleggsinformasjonDetaljerType.*
import java.time.LocalDateTime
import javax.validation.Validation

class InntektKonverter {

    private val validator = Validation.buildDefaultValidatorFactory().validator

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
                        } ?: emptyList()
                        arbeidsInntektInformasjon = ArbeidsInntektInformasjon().apply {
                            inntektListe = aim.arbeidsInntektInformasjon?.inntektListe?.map {
                                lagSubtypeAvInntekt(it).apply {
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
                                    tilleggsinformasjon = if (it.tilleggsinformasjon != null)
                                        Tilleggsinformasjon().apply {
                                            kategori = it.tilleggsinformasjon.kategori
                                            tilleggsinformasjonDetaljer = mapTilleggsinformasjonDetaljer(
                                                it.tilleggsinformasjon.tilleggsinformasjonDetaljer
                                            )
                                        } else null
                                    inntektsmottakerID = it.inntektsmottaker?.identifikator
                                    inngaarIGrunnlagForTrekk = it.inngaarIGrunnlagForTrekk
                                    utloeserArbeidsgiveravgift = it.utloeserArbeidsgiveravgift
                                    informasjonsstatus = it.informasjonsstatus
                                    beskrivelse = it.beskrivelse
                                }
                            } ?: emptyList()
                            arbeidsforholdListe = aim.arbeidsInntektInformasjon?.arbeidsforholdListe?.map {
                                ArbeidsforholdFrilanser().apply {
                                    frilansPeriode = Periode().apply {
                                        fom = it.frilansPeriodeFom
                                        tom = it.frilansPeriodeTom
                                    }
                                    yrke = it.yrke
                                }
                            } ?: emptyList()
                        }
                    }
                }
            }
        }
    }

    private fun lagSubtypeAvInntekt(inntekt: InntektResponse.Inntekt): Inntekt {
        return when(inntekt.inntektType) {
            InntektResponse.InntektType.LOENNSINNTEKT -> Loennsinntekt()
            InntektResponse.InntektType.NAERINGSINNTEKT -> Naeringsinntekt()
            InntektResponse.InntektType.PENSJON_ELLER_TRYGD -> PensjonEllerTrygd()
            InntektResponse.InntektType.YTELSE_FRA_OFFENTLIGE -> YtelseFraOffentlige()
            null -> throw TekniskException("InntektType kan ikke være null")
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

    private inline fun <T> T.apply(block: T.() -> Unit): T {
        block()
        return this.validate()
    }

    private fun <T> T.validate(): T {
        validator.validate(this).run {
            if (isNotEmpty()) throw TekniskException(
                joinToString { "${it.rootBeanClass.simpleName}.${it.propertyPath} ${it.message}" }
            )
        }
        return this
    }
}
