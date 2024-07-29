package no.nav.melosys.service.aareg

import no.nav.melosys.domain.FellesKodeverk
import no.nav.melosys.domain.Saksopplysning
import no.nav.melosys.domain.dokument.arbeidsforhold.*
import no.nav.melosys.domain.dokument.felles.Periode
import no.nav.melosys.integrasjon.aareg.arbeidsforhold.ArbeidsforholdResponse
import no.nav.melosys.integrasjon.aareg.arbeidsforhold.ArbeidsforholdResponse.AntallTimerForTimeloennet
import no.nav.melosys.integrasjon.aareg.arbeidsforhold.ArbeidsforholdResponse.PermisjonPermittering
import no.nav.melosys.service.kodeverk.KodeOppslag
import java.time.OffsetDateTime
import java.time.YearMonth
import java.util.*

class ArbeidsforholdKonverter(
    private val arbeidsforholdResponse: ArbeidsforholdResponse,
    private val kodeOppslag: KodeOppslag
) {
    fun createSaksopplysning() = Saksopplysning().apply {
        dokument = ArbeidsforholdDokument(
            arbeidsforholdResponse.arbeidsforhold.map { src ->
                Arbeidsforhold().apply {
                    arbeidsforholdID = src.arbeidsforholdId
                    arbeidsforholdIDnav = src.navArbeidsforholdId.toLong()
                    ansettelsesPeriode = getPeriode(src.periode)
                    arbeidsforholdstype = src.type
                    arbeidsavtaler = getArbeidsAvtaler(src.arbeidsavtaler)
                    permisjonOgPermittering = getPermisjonPermitteringer(src.permisjonPermitteringer)
                    utenlandsopphold = getUtenlandsopphold(src.utenlandsopphold)
                    arbeidsgivertype = Aktoertype.valueOf(src.arbeidsgiver.type.uppercase(Locale.getDefault()))
                    arbeidsgiverID = src.arbeidsgiver.organisasjonsnummer
                    arbeidstakerID = src.arbeidstaker.offentligIdent
                    opplysningspliktigtype = Aktoertype.valueOf(src.opplysningspliktigtype)
                    opplysningspliktigID = src.opplysningspliktig.organisasjonsnummer
                    arbeidsforholdInnrapportertEtterAOrdningen = src.innrapportertEtterAOrdningen
                    opprettelsestidspunkt = getOffsetDateTime(src.registrert)
                    sistBekreftet = getOffsetDateTime(src.sistBekreftet)
                    antallTimerForTimeloennet = getAntallTimerForTimeloennet(src.antallTimerForTimeloennet)
                }
            }
        )
    }

    private fun getAntallTimerForTimeloennet(antallTimerForTimeloennet: List<AntallTimerForTimeloennet>?): List<AntallTimerIPerioden> {
        if (antallTimerForTimeloennet == null) return emptyList()
        return antallTimerForTimeloennet.map {
            AntallTimerIPerioden(
                antallTimer = it.antallTimer,
                timelonnetPeriode = getPeriode(it.periode),
                rapporteringsAarMaaned = YearMonth.parse(it.rapporteringsperiode)
            )
        }
    }

    private fun getOffsetDateTime(date: String?): OffsetDateTime {
        if (date == null) return OffsetDateTime.now()
        return OffsetDateTime.parse(date + String.format("+%02d:00", getOffsetFraUTCForNorgeMedDTSavings()))
    }

    private fun getOffsetFraUTCForNorgeMedDTSavings(): Int {
        val tz = TimeZone.getTimeZone("Europe/Oslo")
        val cal = Calendar.getInstance(tz, Locale.forLanguageTag("nb_NO"))
        return cal.timeZone.dstSavings / (1000 * 60 * 24)
    }

    private fun getUtenlandsopphold(utenlandsopphold: List<ArbeidsforholdResponse.Utenlandsopphold>?): List<Utenlandsopphold> {
        if (utenlandsopphold == null) return emptyList()
        return utenlandsopphold.map {
            Utenlandsopphold().apply {
                land = it.landkode
                periode = getPeriode(it.periode)
                rapporteringsperiode = YearMonth.parse(it.rapporteringsperiode)
            }
        }
    }

    private fun getPermisjonPermitteringer(permisjonPermitteringer: List<PermisjonPermittering>?): List<PermisjonOgPermittering> {
        if (permisjonPermitteringer == null) return emptyList()
        return permisjonPermitteringer.map {
            PermisjonOgPermittering().apply {
                permisjonsId = it.permisjonPermitteringId
                permisjonsPeriode = getPeriode(it.periode)
                permisjonsprosent = it.prosent
                permisjonOgPermittering =
                    kodeOppslag.getTermFraKodeverk(FellesKodeverk.PERMISJONS_OG_PERMITTERINGS_BESKRIVELSE, it.type)
            }
        }
    }

    private fun getArbeidsAvtaler(arbeidsavtalerSrc: List<ArbeidsforholdResponse.Arbeidsavtale>?): List<Arbeidsavtale> {
        if (arbeidsavtalerSrc == null) return emptyList()
        return arbeidsavtalerSrc.map {
            Arbeidsavtale(
                yrke = Yrke(term = kodeOppslag.getTermFraKodeverk(FellesKodeverk.YRKER, it.yrke), kode = it.yrke),
                arbeidstidsordning = Arbeidstidsordning().apply { kode = it.arbeidstidsordning },
                avloenningstype = "", // Finnes ikke i nytt rest api
                gyldighetsperiode = getPeriode(it.gyldighetsperiode),
                beregnetAntallTimerPrUke = it.beregnetAntallTimerPrUke,
                stillingsprosent = it.stillingsprosent,
                sisteLoennsendringsdato = it.sistLoennsendring,
                endringsdatoStillingsprosent = it.sistStillingsendring,
                avtaltArbeidstimerPerUke = it.antallTimerPrUke,

                // Disse ikke er med i ny aareg rest api
                maritimArbeidsavtale = false,
                skipsregister = null,
                skipstype = null,
                fartsomraade = null)
        }
    }

    private fun getPeriode(periode: ArbeidsforholdResponse.Periode): Periode {
        return Periode(periode.fom, periode.tom)
    }
}
