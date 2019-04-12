package no.nav.melosys.service.eessi;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

import no.nav.melosys.domain.ProsessDataKey;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.ProsessType;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.eessi.avro.MelosysEessiMelding;
import no.nav.melosys.eessi.avro.Statsborgerskap;
import no.nav.melosys.service.dokument.sed.mapper.LovvalgTilBestemmelseDtoMapper;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.springframework.stereotype.Service;

/**
 * Starter behandling av mottatte meldinger fra EESSI
 */
@Service
public class EessiMottakService {

    private final ProsessinstansService prosessinstansService;

    static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public EessiMottakService(ProsessinstansService prosessinstansService) {
        this.prosessinstansService = prosessinstansService;
    }

    public void behandleMottattMelding(MelosysEessiMelding melosysEessiMelding) {

        LocalDateTime nå = LocalDateTime.now();

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.REGISTRERING_UNNTAK);
        prosessinstans.setSteg(ProsessSteg.REG_UNNTAK_OPPRETT_SAK_OG_BEH);
        prosessinstans.setRegistrertDato(nå);
        prosessinstans.setEndretDato(nå);

        prosessinstans.setData(ProsessDataKey.AKTØR_ID, melosysEessiMelding.getAktoerId());
        prosessinstans.setData(ProsessDataKey.JOURNALPOST_ID, melosysEessiMelding.getJournalpostId());
        prosessinstans.setData(ProsessDataKey.DOKUMENT_ID, melosysEessiMelding.getDokumentId());
        prosessinstans.setData(ProsessDataKey.ER_ENDRING, melosysEessiMelding.getErEndring());
        prosessinstans.setData(ProsessDataKey.GSAK_SAK_ID, melosysEessiMelding.getGsakSaksnummer());
        prosessinstans.setData(ProsessDataKey.OPPHOLDSLAND, "NO"); //todo alltid norge?
        prosessinstans.setData(ProsessDataKey.SØKNADSPERIODE, tilPeriode(melosysEessiMelding.getPeriode()));
        prosessinstans.setData(ProsessDataKey.SED_DOKUMENT, opprettSedDokument(melosysEessiMelding));
        prosessinstansService.lagre(prosessinstans);
    }

    private SedDokument opprettSedDokument(MelosysEessiMelding melosysEessiMelding) {
        SedDokument sedDokument = new SedDokument();
        sedDokument.setLovvalgsland(Landkoder.valueOf(melosysEessiMelding.getLovvalgsland()));
        sedDokument.setLovvalgBestemmelse(
            LovvalgTilBestemmelseDtoMapper.mapBestemmelseVerdiTilMelosysLovvalgBestemmelse(melosysEessiMelding.getArtikkel())
        );
        sedDokument.setRinaSaksnummer(melosysEessiMelding.getRinaSaksnummer());
        sedDokument.setPeriode(tilPeriode(melosysEessiMelding.getPeriode()));
        sedDokument.setRinaDokumentId(melosysEessiMelding.getSedId());
        sedDokument.setStatsborgerskap(
            melosysEessiMelding.getStatsborgerskap().stream().map(Statsborgerskap::getLandkode).collect(Collectors.toList())
        );
        sedDokument.setErEndring(melosysEessiMelding.getErEndring());

        return sedDokument;
    }

    private Periode tilPeriode(no.nav.melosys.eessi.avro.Periode periode) {
        return new Periode(
            LocalDate.parse(periode.getFom(),dateTimeFormatter),
            LocalDate.parse(periode.getTom(),dateTimeFormatter)
        );
    }
}
