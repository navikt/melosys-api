package no.nav.melosys.service.saksopplysninger

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
import no.nav.melosys.domain.eessi.melding.Statsborgerskap
import no.nav.melosys.domain.eessi.sed.Bestemmelse
import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse
import no.nav.melosys.repository.SaksopplysningRepository
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.stream.Collectors

@Component
class OpprettSedDokumentService(
    private val dokumentFactory: DokumentFactory,
    private val saksopplysningRepository: SaksopplysningRepository
) {
    fun opprettSedSaksopplysning(melosysEessiMelding: MelosysEessiMelding, behandling: Behandling): Saksopplysning {
        val nå = Instant.now()
        val saksopplysning = Saksopplysning()
        saksopplysning.dokument = opprettSedDokument(melosysEessiMelding)
        saksopplysning.type = SaksopplysningType.SEDOPPL
        saksopplysning.behandling = behandling
        saksopplysning.versjon = SED_DOKUMENT_VERSJON
        saksopplysning.endretDato = nå
        saksopplysning.registrertDato = nå
        val xml = dokumentFactory.lagForenkletXml(saksopplysning)
        saksopplysning.leggTilKildesystemOgMottattDokument(
            SaksopplysningKildesystem.EESSI, xml
        )
        saksopplysningRepository.save(saksopplysning)
        log.info("Saksopplysning: SedDokument opprettet for behandling {}", behandling.id)
        return saksopplysning
    }

    companion object {
        private val log = LoggerFactory.getLogger(OpprettSedDokumentService::class.java)
        private const val SED_DOKUMENT_VERSJON = "1.0"
        private fun opprettSedDokument(melosysEessiMelding: MelosysEessiMelding): SedDokument {
            val sedDokument = SedDokument()
            sedDokument.avsenderLandkode = Landkoder.valueOf(melosysEessiMelding.avsender.landkode)
            sedDokument.lovvalgslandKode = Landkoder.valueOf(melosysEessiMelding.lovvalgsland)
            sedDokument.lovvalgBestemmelse =
                Bestemmelse.fraBestemmelseString(melosysEessiMelding.artikkel).tilMelosysBestemmelse()
            if (melosysEessiMelding.anmodningUnntak != null) {
                sedDokument.unntakFraLovvalgslandKode = hentUnntakFraLovvalgsland(melosysEessiMelding)
                sedDokument.unntakFraLovvalgBestemmelse = hentUnntakFraLovvalgBestemmelse(melosysEessiMelding)
            }
            sedDokument.rinaSaksnummer = melosysEessiMelding.rinaSaksnummer
            sedDokument.lovvalgsperiode = tilPeriode(melosysEessiMelding.periode)
            sedDokument.rinaDokumentID = melosysEessiMelding.sedId
            sedDokument.statsborgerskapKoder =
                melosysEessiMelding.statsborgerskap.stream().map { obj: Statsborgerskap -> obj.landkode }
                    .collect(Collectors.toList())
            sedDokument.arbeidssteder = melosysEessiMelding.arbeidssteder
            sedDokument.erEndring = melosysEessiMelding.erEndring
            sedDokument.sedType = SedType.valueOf(melosysEessiMelding.sedType)
            sedDokument.bucType = BucType.valueOf(melosysEessiMelding.bucType)
            return sedDokument
        }

        private fun hentUnntakFraLovvalgBestemmelse(melosysEessiMelding: MelosysEessiMelding): LovvalgBestemmelse? {
            val unntakFraLovvalgsbestemmelse = melosysEessiMelding.anmodningUnntak.unntakFraLovvalgsbestemmelse
            return if (StringUtils.isEmpty(unntakFraLovvalgsbestemmelse)) {
                null
            } else Bestemmelse.fraBestemmelseString(unntakFraLovvalgsbestemmelse)
                .tilMelosysBestemmelse()
        }

        private fun hentUnntakFraLovvalgsland(melosysEessiMelding: MelosysEessiMelding): Landkoder? {
            val unntakFraLovvalgsland = melosysEessiMelding.anmodningUnntak.unntakFraLovvalgsland
            return if (StringUtils.isEmpty(unntakFraLovvalgsland)) {
                null
            } else Landkoder.valueOf(
                unntakFraLovvalgsland
            )
        }

        private fun tilPeriode(periode: Periode): no.nav.melosys.domain.dokument.medlemskap.Periode {
            return no.nav.melosys.domain.dokument.medlemskap.Periode(
                periode.fom,
                periode.tom
            )
        }
    }
}
