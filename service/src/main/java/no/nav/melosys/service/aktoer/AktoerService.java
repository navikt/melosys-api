package no.nav.melosys.service.aktoer;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Representerer;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.repository.AktoerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AktoerService {
    private final AktoerRepository aktoerRepository;

    @Autowired
    public AktoerService(AktoerRepository aktoerRepository) {
        this.aktoerRepository = aktoerRepository;
    }

    public Aktoer hentfagsakAktoerer(Fagsak fagsak, String aktoersrolle, String representerer) throws IkkeFunnetException {
        return aktoerRepository.findByFagsakAndRolleAndRepresenterer(fagsak,
            Aktoersroller.valueOf(aktoersrolle),
            representerer != null ? Representerer.valueOf(representerer) : null)
            .orElseThrow(() -> new IkkeFunnetException("Det finnes ingen aktoer for gitt rolle, representerer og saksnummer" +
                aktoersrolle + ":" + representerer + ":" + fagsak.getSaksnummer()));
    }

    @Transactional
    public void lagEllerOppdaterAktoer(Fagsak fagsak, AktoerDto aktoerDto) throws FunksjonellException {

        Aktoer aktoerFraApi = new Aktoer();
        aktoerFraApi.setAktørId(aktoerDto.getAktoerID());
        aktoerFraApi.setInstitusjonId(aktoerDto.getInstitusjonsID());
        aktoerFraApi.setUtenlandskPersonId(aktoerDto.getUtenlandskPersonID());
        aktoerFraApi.setOrgnr(aktoerDto.getOrgnr());

        if (aktoerDto.getRolleKode() == null ) {
            throw new FunksjonellException("Kan ikke lagre aktør informasjon uten rolle for saksnummer : " + fagsak.getSaksnummer());
        }
        aktoerFraApi.setRolle(Aktoersroller.valueOf(aktoerDto.getRolleKode()));

        if (aktoerDto.getRepresentererKode() != null ) {
            aktoerFraApi.setRepresenterer(Representerer.valueOf(aktoerDto.getRepresentererKode()));
        }

        aktoerFraApi.setFagsak(fagsak);

        aktoerRepository.findByFagsakAndRolleAndRepresenterer(fagsak,
            aktoerFraApi.getRolle(), aktoerFraApi.getRepresenterer())
            .ifPresent(aktoer -> {
                aktoerRepository.deleteById(aktoer);
            });

        aktoerRepository.save(aktoerFraApi);
    }
}
