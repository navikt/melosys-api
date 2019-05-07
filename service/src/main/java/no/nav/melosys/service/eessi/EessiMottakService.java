package no.nav.melosys.service.eessi;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.dokument.sed.mapper.LovvalgTilBestemmelseDtoMapper;
import no.nav.melosys.service.kafka.model.MelosysEessiMelding;
import no.nav.melosys.service.kafka.model.Statsborgerskap;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Starter behandling av mottatte meldinger fra EESSI
 */
@Service
public class EessiMottakService {

    private static final Logger log = LoggerFactory.getLogger(EessiMottakService.class);

    private final ProsessinstansService prosessinstansService;
    private final FagsakService fagsakService;
    private final LovvalgsperiodeService lovvalgsperiodeService;

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public EessiMottakService(ProsessinstansService prosessinstansService, FagsakService fagsakService, LovvalgsperiodeService lovvalgsperiodeService) {
        this.prosessinstansService = prosessinstansService;
        this.fagsakService = fagsakService;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
    }

    public void behandleMottattMelding(MelosysEessiMelding melosysEessiMelding) {
        if (skalBehandles(melosysEessiMelding)) {
            log.info("Behandler mottatt EESSI-medling. Buc: {}, SED: {}", melosysEessiMelding.getRinaSaksnummer(), melosysEessiMelding.getSedId());
            opprettProsessinstans(melosysEessiMelding);
        }
    }

    private boolean skalBehandles(MelosysEessiMelding melosysEessiMelding) {
        return !melosysEessiMelding.getErEndring() || periodeErEndret(melosysEessiMelding);
    }

    private boolean periodeErEndret(MelosysEessiMelding melosysEessiMelding) {
        Periode periode = tilPeriode(melosysEessiMelding.getPeriode());
        Lovvalgsperiode lovvalgsperiode;

        try {
            Optional<Fagsak> eksisterendeFagsak = fagsakService.hentFagsakFraGsakSaksnummer(melosysEessiMelding.getGsakSaksnummer());
            if (eksisterendeFagsak.isPresent()) {
                Fagsak fagsak = eksisterendeFagsak.get();
                Behandling behandling = fagsak.getTidligsteInaktiveBehandling();
                lovvalgsperiode = lovvalgsperiodeService.hentOpprinneligLovvalgsperiode(behandling.getId());
                return !lovvalgsperiode.getFom().equals(periode.getFom()) &&
                    (periode.getTom() == null || !lovvalgsperiode.getTom().equals(periode.getTom()));
            }
        } catch (IkkeFunnetException ex) {
            // Om ikke finner fagsak -> behandle på nytt
            return true;
        }

        return true;
    }

    private void opprettProsessinstans(MelosysEessiMelding melosysEessiMelding) {
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

    private Periode tilPeriode(no.nav.melosys.service.kafka.model.Periode periode) {
        return new Periode(
            LocalDate.parse(periode.getFom(), dateTimeFormatter),
            periode.getTom() != null ? LocalDate.parse(periode.getTom(), dateTimeFormatter) : null
        );
    }
}
