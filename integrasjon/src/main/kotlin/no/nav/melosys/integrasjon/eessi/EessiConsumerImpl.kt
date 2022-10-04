package no.nav.melosys.integrasjon.eessi

import no.nav.melosys.domain.arkiv.Vedlegg
import no.nav.melosys.domain.eessi.BucType
import no.nav.melosys.domain.eessi.Institusjon
import no.nav.melosys.domain.eessi.SedType
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding
import no.nav.melosys.domain.eessi.sed.SedDataDto
import no.nav.melosys.domain.eessi.sed.SedGrunnlagDto
import no.nav.melosys.integrasjon.eessi.dto.*
import no.nav.melosys.integrasjon.felles.JsonRestIntegrasjon
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

open class EessiConsumerImpl(private val webClient: WebClient) : EessiConsumer, JsonRestIntegrasjon {

    override fun opprettBucOgSed(
        sedDataDto: SedDataDto,
        vedlegg: Collection<Vedlegg>,
        bucType: BucType,
        sendAutomatisk: Boolean,
        oppdaterEksisterendeOmFinnes: Boolean
    ) =
        webClient.post()
            .uri("/buc/$bucType?sendAutomatisk=$sendAutomatisk&oppdaterEksisterende=$oppdaterEksisterendeOmFinnes")
            .bodyValue(OpprettBucOgSedDto(sedDataDto, vedlegg))
            .retrieve()
            .bodyToMono<OpprettSedDto>()
            .block()!!

    override fun sendSedPåEksisterendeBuc(sedDataDto: SedDataDto, rinaSaksnummer: String, sedType: SedType) {
        webClient.post()
            .uri("/buc/$rinaSaksnummer/sed/$sedType")
            .bodyValue(sedDataDto)
            .retrieve()
            .bodyToMono<Void>()
            .block()
    }

    override fun hentMottakerinstitusjoner(bucType: String, landkoder: Collection<String>) =
        webClient.get()
            .uri("/buc/$bucType/institusjoner?land=${landkoder.joinToString(",")}")
            .retrieve()
            .bodyToMono<List<InstitusjonDto>>()
            .block()!!.map { Institusjon(it.id, it.navn, it.landkode) }.toList()

    override fun hentMelosysEessiMeldingFraJournalpostID(journalpostID: String) =
        webClient.get()
            .uri("/journalpost/$journalpostID/eessimelding")
            .retrieve()
            .bodyToMono<MelosysEessiMelding>()
            .block()!!

    override fun lagreSaksrelasjon(saksrelasjonDto: SaksrelasjonDto) {
        webClient.post()
            .uri("/sak")
            .bodyValue(saksrelasjonDto)
            .retrieve()
            .bodyToMono<Void>()
            .block()
    }

    override fun hentSakForRinasaksnummer(rinaSaksnummer: String) =
        webClient.get()
            .uri("/sak?rinaSaksnummer=$rinaSaksnummer")
            .retrieve()
            .bodyToMono<List<SaksrelasjonDto>>()
            .block()!!

    override fun genererSedPdf(sedDataDto: SedDataDto, sedType: SedType) =
        webClient.post()
            .uri("/sed/$sedType/pdf")
            .bodyValue(sedDataDto)
            .retrieve()
            .bodyToMono<ByteArray>()
            .block()!!

    override fun hentTilknyttedeBucer(gsakSaksnummer: Long, statuser: List<String>) =
        webClient.get()
            .uri("/sak/$gsakSaksnummer/bucer?statuser=${statuser.joinToString(",")}")
            .retrieve()
            .bodyToMono<List<BucinfoDto>>()
            .block()!!.map { it.tilDomene() }.toList()

    override fun hentSedGrunnlag(rinaSaksnummer: String, rinaDokumentID: String) =
        webClient.get()
            .uri("/buc/$rinaSaksnummer/sed/$rinaDokumentID/grunnlag")
            .retrieve()
            .bodyToMono<SedGrunnlagDto>()
            .block()!!

    override fun lukkBuc(rinaSaksnummer: String) {
        webClient.post()
            .uri("/buc/$rinaSaksnummer/lukk")
            .retrieve()
            .bodyToMono<Void>()
            .block()
    }

    override fun hentMuligeAksjoner(rinaSaksnummer: String) =
        webClient.get()
            .uri("/buc/$rinaSaksnummer/aksjoner")
            .retrieve()
            .bodyToMono<List<String>>()
            .block()!!
}
