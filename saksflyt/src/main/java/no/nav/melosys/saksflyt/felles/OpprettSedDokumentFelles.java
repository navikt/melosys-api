package no.nav.melosys.saksflyt.felles;

import java.time.Instant;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningKilde;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.eessi.melding.Statsborgerskap;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.integrasjon.eessi.dto.Bestemmelse;
import no.nav.melosys.repository.SaksopplysningRepository;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OpprettSedDokumentFelles {
    private static final Logger log = LoggerFactory.getLogger(OpprettSedDokumentFelles.class);
    private static final String SED_DOKUMENT_VERSJON = "1.0";

    private final DokumentFactory dokumentFactory;
    private final SaksopplysningRepository saksopplysningRepository;

    @Autowired
    public OpprettSedDokumentFelles(DokumentFactory dokumentFactory, SaksopplysningRepository saksopplysningRepository) {
        this.dokumentFactory = dokumentFactory;
        this.saksopplysningRepository = saksopplysningRepository;
    }

    public Saksopplysning opprettSedSaksopplysning(MelosysEessiMelding melosysEessiMelding, Behandling behandling) {
        Instant nå = Instant.now();
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setDokument(opprettSedDokument(melosysEessiMelding));
        saksopplysning.setType(SaksopplysningType.SEDOPPL);
        saksopplysning.setBehandling(behandling);
        saksopplysning.setKilde(SaksopplysningKilde.EESSI);
        saksopplysning.setVersjon(SED_DOKUMENT_VERSJON);
        saksopplysning.setEndretDato(nå);
        saksopplysning.setRegistrertDato(nå);

        String xml = dokumentFactory.lagInternXml(saksopplysning);
        saksopplysning.setDokumentXml(xml);

        saksopplysningRepository.save(saksopplysning);
        log.info("Saksopplysning: SedDokument opprettet for behandling {}", behandling.getId());
        return saksopplysning;
    }

    private static SedDokument opprettSedDokument(MelosysEessiMelding melosysEessiMelding) {
        SedDokument sedDokument = new SedDokument();
        sedDokument.setLovvalgslandKode(Landkoder.valueOf(melosysEessiMelding.getLovvalgsland()));
        sedDokument.setLovvalgBestemmelse(
            Bestemmelse.fraBestemmelseString(melosysEessiMelding.getArtikkel()).tilMelosysBestemmelse()
        );
        if (melosysEessiMelding.getAnmodningUnntak() != null) {
            sedDokument.setUnntakFraLovvalgslandKode(hentUnntakFraLovvalgsland(melosysEessiMelding));
            sedDokument.setUnntakFraLovvalgBestemmelse(hentUnntakFraLovvalgBestemmelse(melosysEessiMelding));
        }
        sedDokument.setRinaSaksnummer(melosysEessiMelding.getRinaSaksnummer());
        sedDokument.setLovvalgsperiode(tilPeriode(melosysEessiMelding.getPeriode()));
        sedDokument.setRinaDokumentID(melosysEessiMelding.getSedId());
        sedDokument.setStatsborgerskapKoder(
            melosysEessiMelding.getStatsborgerskap().stream().map(Statsborgerskap::getLandkode).collect(Collectors.toList())
        );
        sedDokument.setArbeidssteder(melosysEessiMelding.getArbeidssteder());
        sedDokument.setErEndring(melosysEessiMelding.getErEndring());
        sedDokument.setSedType(SedType.valueOf(melosysEessiMelding.getSedType()));
        sedDokument.setBucType(BucType.valueOf(melosysEessiMelding.getBucType()));
        sedDokument.setErElektronisk(true);

        return sedDokument;
    }

    private static LovvalgBestemmelse hentUnntakFraLovvalgBestemmelse(MelosysEessiMelding melosysEessiMelding) {
        String unntakFraLovvalgsbestemmelse = melosysEessiMelding.getAnmodningUnntak().getUnntakFraLovvalgsbestemmelse();

        if (StringUtils.isEmpty(unntakFraLovvalgsbestemmelse)) {
            return null;
        }

        return Bestemmelse.fraBestemmelseString(unntakFraLovvalgsbestemmelse).tilMelosysBestemmelse();
    }

    private static Landkoder hentUnntakFraLovvalgsland(MelosysEessiMelding melosysEessiMelding) {
        String unntakFraLovvalgsland = melosysEessiMelding.getAnmodningUnntak().getUnntakFraLovvalgsland();

        if (StringUtils.isEmpty(unntakFraLovvalgsland)) {
            return null;
        }

        return Landkoder.valueOf(unntakFraLovvalgsland);
    }

    private static Periode tilPeriode(no.nav.melosys.domain.eessi.melding.Periode periode) {
        return new Periode(
            periode.getFom(),
            periode.getTom()
        );
    }
}
