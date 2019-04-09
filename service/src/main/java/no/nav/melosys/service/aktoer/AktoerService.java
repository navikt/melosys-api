package no.nav.melosys.service.aktoer;

import java.util.List;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Representerer;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.AktoerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AktoerService {
    private final AktoerRepository aktørRepository;

    @Autowired
    public AktoerService(AktoerRepository aktørRepository) {
        this.aktørRepository = aktørRepository;
    }

    public List<Aktoer> hentfagsakAktører(Fagsak fagsak, Aktoersroller aktoersrolle, Representerer representerer) {
        Aktoer aktør = new Aktoer();
        aktør.setFagsak(fagsak);
        aktør.setRolle(aktoersrolle);
        aktør.setRepresenterer(representerer);

        return aktørRepository.findAll(Example.of(aktør));
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void lagEllerOppdaterAktoer(Fagsak fagsak, AktoerDto aktoerDto) throws FunksjonellException {
        if (aktoerDto.getRolleKode() == null) {
            throw new FunksjonellException("Kan ikke lagre aktør uten rolle. Saksnummer: " + fagsak.getSaksnummer());
        }

        Aktoer aktørFraDto = new Aktoer();
        aktørFraDto.setAktørId(aktoerDto.getAktoerID());
        aktørFraDto.setInstitusjonId(aktoerDto.getInstitusjonsID());
        aktørFraDto.setUtenlandskPersonId(aktoerDto.getUtenlandskPersonID());
        aktørFraDto.setOrgnr(aktoerDto.getOrgnr());
        aktørFraDto.setRolle(Aktoersroller.valueOf(aktoerDto.getRolleKode()));

        if (aktoerDto.getRepresentererKode() != null) {
            aktørFraDto.setRepresenterer(Representerer.valueOf(aktoerDto.getRepresentererKode()));
        }

        aktørFraDto.setFagsak(fagsak);

        aktørRepository.findByFagsakAndRolleAndRepresenterer(fagsak, aktørFraDto.getRolle(), aktørFraDto.getRepresenterer())
            .ifPresent(aktørRepository::deleteById);

        Aktoer aktoer = aktørRepository.save(aktørFraDto);
        aktoerDto.setDatabaseID(aktoer.getId());
    }

    public void slettAktoer(long databaseID) throws TekniskException, FunksjonellException {
        Aktoer aktoer = aktørRepository.findById(databaseID).
            orElseThrow(() -> new TekniskException("Klarte ikke slette aktøren. Fant ingen aktør på id: " + databaseID));

        if (aktoer.getRolle().equals(Aktoersroller.BRUKER)) {
            throw new FunksjonellException("Aktøren er en bruker. Det er ikke lov til å slette denne");
        }
        aktørRepository.deleteById(aktoer);
    }

    @Transactional
    public void erstattEksisterendeArbeidsgiveraktører(Fagsak fagsak, List<String> orgnumre) {
        aktørRepository.deleteAllByFagsakAndRolle(fagsak, Aktoersroller.ARBEIDSGIVER);

        for (String orgnummer : orgnumre) {
            lagArbeidsgiveraktør(fagsak, orgnummer);
        }
    }

    private void lagArbeidsgiveraktør(Fagsak fagsak, String orgnummer) {
        Aktoer aktør = new Aktoer();
        aktør.setFagsak(fagsak);
        aktør.setRolle(Aktoersroller.ARBEIDSGIVER);
        aktør.setOrgnr(orgnummer);

        aktørRepository.save(aktør);
    }
}
