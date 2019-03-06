package no.nav.melosys.saksflyt.agent.unntakmed;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import no.nav.melosys.domain.ProsessDataKey;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.eessi.avro.MelosysEessiMelding;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.repository.SaksopplysningRepository;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OpprettSedDokument extends AbstraktStegBehandler {
//TODO: _lagre_ sedDokument
    private static final Logger log = LoggerFactory.getLogger(OpprettSedDokument.class);

    private static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd-mm-yyyy");

    private final SaksopplysningRepository saksopplysningRepository;

    @Autowired
    public OpprettSedDokument(SaksopplysningRepository saksopplysningRepository) {
        this.saksopplysningRepository = saksopplysningRepository;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.OPPRETT_SEDINFO;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {

        MelosysEessiMelding melosysEessiMelding = prosessinstans.getData(ProsessDataKey.SEDDOKUMENT, MelosysEessiMelding.class);

//        Instant nå = Instant.now();
//        Saksopplysning saksopplysning = new Saksopplysning();
//        saksopplysning.setDokument(opprettSedDokument(melosysEessiMelding));
//        saksopplysning.setType(SaksopplysningType.SEDOPPLYSNINGER);
//        saksopplysning.setBehandling(prosessinstans.getBehandling());
//        saksopplysning.setKilde(SaksopplysningKilde.EESSI);
//        saksopplysning.setVersjon("0.1-SNAPSHOT");
//        saksopplysning.setEndretDato(nå);
//        saksopplysning.setRegistrertDato(nå);

//        saksopplysningRepository.save(saksopplysning);

        prosessinstans.setSteg(ProsessSteg.VALIDER_UNNTAK);
    }

    private Periode tilPeriode(no.nav.melosys.eessi.avro.Periode periode) {
        return new Periode(
            LocalDate.parse(periode.getFom(),dateTimeFormatter),
            LocalDate.parse(periode.getTom(),dateTimeFormatter)
        );
    }
}
