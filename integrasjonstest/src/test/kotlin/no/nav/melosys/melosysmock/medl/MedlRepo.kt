package no.nav.melosys.melosysmock.medl

import no.nav.melosys.integrasjon.medl.api.v1.MedlemskapsunntakForGet
import no.nav.melosys.integrasjon.medl.api.v1.MedlemskapsunntakForPost
import no.nav.melosys.integrasjon.medl.api.v1.MedlemskapsunntakForPut
import no.nav.melosys.integrasjon.medl.api.v1.Sporingsinformasjon
import no.nav.melosys.melosysmock.person.PersonRepo
import no.nav.melosys.melosysmock.utils.lagRandomLongId
import java.time.LocalDate
import java.time.LocalDateTime

typealias MedlRepository = MutableMap<Long?, MedlemskapsunntakForGet>

object MedlRepo {
    val repo: MedlRepository = mutableMapOf<Long?, MedlemskapsunntakForGet>().apply {
        // Add default test entry
        put(1242L, MedlemskapsunntakForGet().apply {
            unntakId = 1242L
            ident = "21075114491"
            fraOgMed = LocalDate.now()
            tilOgMed = LocalDate.now().plusYears(1)
            status = "GODKJENT"
            dekning = "FULL"
            lovvalgsland = "AT"
            lovvalg = "FOROVRIG"
            grunnlag = "ARBEID"
            medlem = true
            sporingsinformasjon = Sporingsinformasjon().apply {
                versjon = 0
                registrert = LocalDate.now()
                besluttet = LocalDate.now()
                kilde = SRVMELOSYS
                kildedokument = "DEFAULT_TEST_DOCUMENT"
                opprettet = LocalDateTime.now()
                opprettetAv = SRVMELOSYS
                sistEndret = LocalDateTime.now()
                sistEndretAv = SRVMELOSYS
            }
        })
    }
}

fun MedlRepository.lagre(medlemskapsunntakForGet: MedlemskapsunntakForGet) {
    this[medlemskapsunntakForGet.unntakId] = medlemskapsunntakForGet
}

fun MedlRepository.opprett(medlemskapsunntakForPost: MedlemskapsunntakForPost): MedlemskapsunntakForGet =
    medlemskapsunntakForPost.ident.let { fnr ->
        PersonRepo.repo[fnr]
            ?.let {
                medlemskapsunntakForPost.tilGet()
                    .apply { unntakId = lagRandomLongId() }
                    .also(::lagre)
            } ?: throw NoSuchElementException("Ingen person med fnr $fnr")
    }

fun MedlRepository.oppdater(medlemskapsunntakForPut: MedlemskapsunntakForPut): MedlemskapsunntakForGet =
    medlemskapsunntakForPut.unntakId
        .let { periodeId ->
            this[periodeId]
                ?.let { eksisterendeMedlemskapsunntak ->
                    medlemskapsunntakForPut.tilGet()
                        .apply {
                            sporingsinformasjon?.run {
                                registrert = eksisterendeMedlemskapsunntak.sporingsinformasjon!!.registrert
                                besluttet = eksisterendeMedlemskapsunntak.sporingsinformasjon!!.besluttet
                                opprettet = eksisterendeMedlemskapsunntak.sporingsinformasjon!!.opprettet
                            }
                        }
                        .also(::lagre)
                } ?: throw NoSuchElementException("Ingen periode med id $periodeId")
        }

fun MedlRepository.hent(periodeId: Long): MedlemskapsunntakForGet =
    this[periodeId] ?: throw NoSuchElementException("Ingen periode med id $periodeId")

fun MedlRepository.finn(fnr: String): Collection<MedlemskapsunntakForGet> =
    PersonRepo.repo[fnr]
        ?.let {
            this.map { it.value }
                .filter { it.ident == fnr }
        } ?: throw NoSuchElementException("Ingen person med fnr $fnr")

fun MedlRepository.finn(
    fnr: String,
    fom: LocalDate,
    tom: LocalDate
): Collection<MedlemskapsunntakForGet> = finn(fnr)
    .filter { it.fraOgMed!!.isEqual(fom) || it.fraOgMed!!.isAfter(fom) }
    .filter { it.tilOgMed!!.isEqual(tom) || it.tilOgMed!!.isBefore(tom) }

@JvmName("nullableFinn")
fun MedlRepository.finn(
    fnr: String,
    fom: LocalDate?,
    tom: LocalDate?
): Collection<MedlemskapsunntakForGet> =
    if (fom != null && tom != null) {
        finn(fnr, fom, tom)
    } else {
        finn(fnr)
    }

private const val SRVMELOSYS = "srvmelosys"

fun MedlemskapsunntakForPost.tilGet(): MedlemskapsunntakForGet =
    let {
        MedlemskapsunntakForGet().apply {
            ident = it.ident
            fraOgMed = it.fraOgMed
            tilOgMed = it.tilOgMed
            status = it.status
            dekning = it.dekning
            lovvalgsland = it.lovvalgsland
            lovvalg = it.lovvalg
            grunnlag = it.grunnlag
            medlem = true
            sporingsinformasjon = Sporingsinformasjon().apply {
                versjon = 0
                registrert = LocalDate.now()
                besluttet = LocalDate.now()
                kilde = SRVMELOSYS
                kildedokument = it.sporingsinformasjon!!.kildedokument
                opprettet = LocalDateTime.now()
                opprettetAv = SRVMELOSYS
                sistEndret = LocalDateTime.now()
                sistEndretAv = SRVMELOSYS
            }
        }
    }

fun MedlemskapsunntakForPut.tilGet(): MedlemskapsunntakForGet =
    let {
        MedlemskapsunntakForGet().apply {
            unntakId = it.unntakId
            fraOgMed = it.fraOgMed
            tilOgMed = it.tilOgMed
            status = it.status
            dekning = it.dekning
            lovvalgsland = it.lovvalgsland
            lovvalg = it.lovvalg
            grunnlag = it.grunnlag
            medlem = true
            Sporingsinformasjon().apply {
                versjon = it.sporingsinformasjon!!.versjon
                kilde = SRVMELOSYS
                kildedokument = it.sporingsinformasjon!!.kildedokument
                opprettetAv = SRVMELOSYS
                sistEndretAv = SRVMELOSYS
                sistEndret = LocalDateTime.now()
            }
        }
    }
