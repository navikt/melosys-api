package no.nav.melosys.saksflyt.steg.ufm;

import java.time.Instant;
import java.util.stream.Collectors;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.domain.dokument.sed.BucType;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.dokument.sed.SedType;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.eessi.melding.Statsborgerskap;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.SaksopplysningRepository;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.dokument.sed.mapper.LovvalgTilBestemmelseDtoMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OpprettSedDokument extends AbstraktStegBehandler {
    private static final Logger log = LoggerFactory.getLogger(OpprettSedDokument.class);

    private final SaksopplysningRepository saksopplysningRepository;
    private final DokumentFactory dokumentFactory;

    @Autowired
    public OpprettSedDokument(SaksopplysningRepository saksopplysningRepository, DokumentFactory dokumentFactory) {
        this.saksopplysningRepository = saksopplysningRepository;
        this.dokumentFactory = dokumentFactory;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.REG_UNNTAK_OPPRETT_SEDDOKUMENT;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        Instant nå = Instant.now();
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setDokument(opprettSedDokument(prosessinstans.getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding.class)));
        saksopplysning.setType(SaksopplysningType.SEDOPPL);
        saksopplysning.setBehandling(prosessinstans.getBehandling());
        saksopplysning.setKilde(SaksopplysningKilde.EESSI);
        saksopplysning.setVersjon("0.2-SNAPSHOT");
        saksopplysning.setEndretDato(nå);
        saksopplysning.setRegistrertDato(nå);

        String xml = dokumentFactory.lagInternXml(saksopplysning);
        saksopplysning.setDokumentXml(xml);

        saksopplysningRepository.save(saksopplysning);
        log.info("Saksopplysning: SedDokument opprettet for behandling {}", prosessinstans.getBehandling().getId());

        prosessinstans.setSteg(ProsessSteg.REG_UNNTAK_HENT_PERSON);
    }

    private SedDokument opprettSedDokument(MelosysEessiMelding melosysEessiMelding) {
        SedDokument sedDokument = new SedDokument();
        sedDokument.setLovvalgslandKode(Landkoder.valueOf(melosysEessiMelding.getLovvalgsland()));
        sedDokument.setLovvalgBestemmelse(
            LovvalgTilBestemmelseDtoMapper.mapBestemmelseVerdiTilMelosysLovvalgBestemmelse(melosysEessiMelding.getArtikkel())
        );
        sedDokument.setRinaSaksnummer(melosysEessiMelding.getRinaSaksnummer());
        sedDokument.setLovvalgsperiode(tilPeriode(melosysEessiMelding.getPeriode()));
        sedDokument.setRinaDokumentID(melosysEessiMelding.getSedId());
        sedDokument.setStatsborgerskapKoder(
            melosysEessiMelding.getStatsborgerskap().stream().map(Statsborgerskap::getLandkode).collect(Collectors.toList())
        );
        sedDokument.setErEndring(melosysEessiMelding.getErEndring());
        sedDokument.setSedType(SedType.valueOf(melosysEessiMelding.getSedType()));
        sedDokument.setBucType(BucType.valueOf(melosysEessiMelding.getBucType()));

        return sedDokument;
    }

    private Periode tilPeriode(no.nav.melosys.domain.eessi.melding.Periode periode) {
        return new Periode(
                periode.getFom(),
                periode.getTom()
        );
    }
}
