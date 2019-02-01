package no.nav.melosys.service.dokument.sed;

import java.util.List;
import java.util.Map;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.bestemmelse.LovvalgBestemmelse;
import no.nav.melosys.eux.BucType;
import no.nav.melosys.eux.model.SedType;
import no.nav.melosys.eux.model.nav.SED;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.eux.consumer.EuxConsumer;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.service.dokument.DokumentDataMapperRuter;
import no.nav.melosys.service.dokument.sed.bygger.AbstraktSedDataBygger;
import no.nav.melosys.service.dokument.sed.mapper.AbstraktSedMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class SedService {

    private static final Logger log = LoggerFactory.getLogger(SedService.class);

    private final EuxConsumer euxConsumer;
    private final SedDataByggerVelger sedDataByggerVelger;
    private final FagsakRepository fagsakRepository;

    public SedService(@Qualifier("system") EuxConsumer euxConsumer, SedDataByggerVelger sedDataByggerVelger, FagsakRepository fagsakRepository) {
        this.euxConsumer = euxConsumer;
        this.sedDataByggerVelger = sedDataByggerVelger;
        this.fagsakRepository = fagsakRepository;
    }

    public void opprettOgSendSed(Behandling behandling, Behandlingsresultat behandlingsresultat) throws MelosysException {

        Lovvalgsperiode lovvalgsperiode = behandlingsresultat.getLovvalgsperioder().stream()
            .findFirst().orElseThrow(() -> new TekniskException("Finner ingen lovvalgsperiode!")); //TODO: flere lovvalgsperioder
        Landkoder lovvalgsLand = lovvalgsperiode.getLovvalgsland();

        LovvalgBestemmelse lovvalgBestemmelse = lovvalgsperiode.getBestemmelse();
        SedType sedType = SedUtils.hentSedTypeFraLovvalgsBestemmelse(lovvalgBestemmelse);

        AbstraktSedDataBygger sedDataBygger = sedDataByggerVelger.hent(sedType);
        AbstraktSedData sedData = sedDataBygger.lag(behandling);

        AbstraktSedMapper abstraktSedMapper = DokumentDataMapperRuter.sedMapper(sedType);

        SED sed = abstraktSedMapper.mapTilSed(sedData);
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

        euxConsumer.sendSed(rinaSaksnummer, null, dokumentId);

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
