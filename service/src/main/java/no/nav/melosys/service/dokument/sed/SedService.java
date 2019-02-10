package no.nav.melosys.service.dokument.sed;

import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Landkoder;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.bestemmelse.LovvalgBestemmelse;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.service.dokument.sed.bygger.SedDataBygger;
import no.nav.melosys.service.dokument.sed.dto.SedDataDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class SedService {

    private static final Logger log = LoggerFactory.getLogger(SedService.class);

    private final SedDataByggerVelger sedDataByggerVelger;
    private final FagsakRepository fagsakRepository;

    public SedService(SedDataByggerVelger sedDataByggerVelger, FagsakRepository fagsakRepository) {
        this.sedDataByggerVelger = sedDataByggerVelger;
        this.fagsakRepository = fagsakRepository;
    }

    // SED-er sendes ikke i Lev. 1
    public void opprettOgSendSed() throws MelosysException {
    }

    public void opprettOgSendSed(Behandling behandling, Behandlingsresultat behandlingsresultat) throws MelosysException {

        Lovvalgsperiode lovvalgsperiode = behandlingsresultat.getLovvalgsperioder().stream()
            .findFirst().orElseThrow(() -> new TekniskException("Finner ingen lovvalgsperiode!")); //TODO: flere lovvalgsperioder
        Landkoder lovvalgsLand = lovvalgsperiode.getLovvalgsland();

        LovvalgBestemmelse lovvalgBestemmelse = lovvalgsperiode.getBestemmelse();

        SedDataBygger sedDataBygger = sedDataByggerVelger.hent(lovvalgsperiode.getBestemmelse());
        SedDataDto sedData = sedDataBygger.lag(behandling);

        //SedMapper sedMapper = SedDataMapperRuter.sedMapper(sedType);

        /*SED sed = sedMapper.mapTilSed(sedData);
        BucType bucType = SedUtils.hentBucFraLovvalgsBestemmelse(lovvalgBestemmelse);
        String fagsaknummer = behandling.getFagsak().getSaksnummer();
        String mottakerId = hentFørsteInstitusjonId(euxConsumer.hentInstitusjoner(bucType.name(), lovvalgsLand.getKode()));

        log.info("Oppretter buc {} og sed {} for behandling {}, fagsak {}", bucType, sedType, behandling.getId(), fagsaknummer);
        Map<String,String> rinaSakInfo = euxConsumer.opprettBucOgSed(bucType.name(), mottakerId, sed);
        String rinaSaksnummer = rinaSakInfo.get("caseId");
        String dokumentId = rinaSakInfo.get("documentId"); //brukes til journalføring

        Fagsak fagsak = behandling.getFagsak();
        fagsak.setRinasaksnummer(rinaSaksnummer);
        fagsakRepository.save(fagsak);

        euxConsumer.sendSed(rinaSaksnummer, null, dokumentId);*/ //Flyttet til Melosys-eessi

        //TODO: journalfør
    }

    //Lovvalg skal kun ha en institusjon i hvert medlemsland.
    private String hentFørsteInstitusjonId(List<String> institusjoner) throws FunksjonellException, TekniskException {

        if (CollectionUtils.isEmpty(institusjoner)){
            throw new FunksjonellException("Liste av institusjoner er tom!");
        }

        String institusjon = institusjoner.get(0);
        String[] splittetInstitusjon =  institusjon.split(":");

        if (splittetInstitusjon.length == 2){
            return splittetInstitusjon[1];
        }

         throw new TekniskException("Kan ikke hente ut instutisjonnavn fra string: \""+ institusjon + "\"");
    }
}
