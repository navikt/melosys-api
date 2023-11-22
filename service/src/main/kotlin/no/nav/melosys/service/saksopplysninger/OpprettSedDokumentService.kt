package no.nav.melosys.service.saksopplysninger

import mu.KotlinLogging
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Saksopplysning
import no.nav.melosys.domain.SaksopplysningKildesystem
import no.nav.melosys.domain.SaksopplysningType
import no.nav.melosys.domain.dokument.DokumentFactory
import no.nav.melosys.domain.dokument.sed.SedDokument
import no.nav.melosys.domain.eessi.BucType
import no.nav.melosys.domain.eessi.Periode
import no.nav.melosys.domain.eessi.SedType
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding
import no.nav.melosys.domain.eessi.sed.Bestemmelse
import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse
import no.nav.melosys.repository.SaksopplysningRepository
import org.springframework.stereotype.Component
import java.time.Instant

private val log = KotlinLogging.logger { }

@Component
class OpprettSedDokumentService(
    private val dokumentFactory: DokumentFactory,
    private val saksopplysningRepository: SaksopplysningRepository
) {
    fun opprettSedSaksopplysning(melosysEessiMelding: MelosysEessiMelding, behandling: Behandling): Saksopplysning {
        val now = Instant.now()
        val saksopplysning = Saksopplysning().apply {
            dokument = opprettSedDokument(melosysEessiMelding)
            type = SaksopplysningType.SEDOPPL
            this.behandling = behandling
            versjon = SED_DOKUMENT_VERSJON
            endretDato = now
            registrertDato = now
        }

        val xml = dokumentFactory.lagForenkletXml(saksopplysning)
        saksopplysning.leggTilKildesystemOgMottattDokument(SaksopplysningKildesystem.EESSI, xml)
        saksopplysningRepository.save(saksopplysning)
        log.info("Saksopplysning: SedDokument opprettet for behandling {}", behandling.id)
        return saksopplysning
    }

    private fun opprettSedDokument(melosysEessiMelding: MelosysEessiMelding): SedDokument {
        return SedDokument().apply {
            avsenderLandkode = Landkoder.valueOf(melosysEessiMelding.avsender.landkode)
            lovvalgslandKode = Landkoder.valueOf(melosysEessiMelding.lovvalgsland)
            lovvalgBestemmelse = Bestemmelse.fraBestemmelseString(melosysEessiMelding.artikkel).tilMelosysBestemmelse()
            unntakFraLovvalgslandKode = hentUnntakFraLovvalgsland(melosysEessiMelding)
            unntakFraLovvalgBestemmelse = hentUnntakFraLovvalgBestemmelse(melosysEessiMelding)
            rinaSaksnummer = melosysEessiMelding.rinaSaksnummer
            lovvalgsperiode = tilMedlemskapPeriode(melosysEessiMelding.periode)
            rinaDokumentID = melosysEessiMelding.sedId
            statsborgerskapKoder = melosysEessiMelding.statsborgerskap.map { it.landkode }
            arbeidssteder = melosysEessiMelding.arbeidssteder
            erEndring = melosysEessiMelding.erEndring
            sedType = SedType.valueOf(melosysEessiMelding.sedType)
            bucType = BucType.valueOf(melosysEessiMelding.bucType)
        }
    }

    private fun hentUnntakFraLovvalgBestemmelse(melosysEessiMelding: MelosysEessiMelding): LovvalgBestemmelse? =
        melosysEessiMelding.anmodningUnntak?.unntakFraLovvalgsbestemmelse
            ?.takeIf { it.isNotBlank() }
            ?.let { Bestemmelse.fraBestemmelseString(it).tilMelosysBestemmelse() }

    private fun hentUnntakFraLovvalgsland(melosysEessiMelding: MelosysEessiMelding): Landkoder? =
        melosysEessiMelding.anmodningUnntak?.unntakFraLovvalgsland
            ?.takeIf { it.isNotBlank() }
            ?.let { Landkoder.valueOf(it) }

    private fun tilMedlemskapPeriode(periode: Periode): no.nav.melosys.domain.dokument.medlemskap.Periode =
        no.nav.melosys.domain.dokument.medlemskap.Periode(periode.fom, periode.tom)

    companion object {
        private const val SED_DOKUMENT_VERSJON = "1.0"
    }
}
