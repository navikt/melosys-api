package no.nav.melosys.service;

import java.util.Optional;

import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.inntekt.InntektDokument;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.person.PersonhistorikkDokument;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.repository.SaksopplysningRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SaksopplysningerService {
    private final SaksopplysningRepository saksopplysningRepo;

    @Autowired
    public SaksopplysningerService(SaksopplysningRepository saksopplysningRepo) {
        this.saksopplysningRepo = saksopplysningRepo;
    }

    public Optional<PersonDokument> finnPersonOpplysninger(long behandlingID) {
        return saksopplysningRepo.findByBehandling_IdAndType(behandlingID, SaksopplysningType.PERSOPL)
            .map(s -> (PersonDokument) s.getDokument());
    }

    public PersonDokument hentPersonOpplysninger(long behandlingID) {
        return finnPersonOpplysninger(behandlingID)
            .orElseThrow(() -> new IkkeFunnetException("Finner ikke persondokument for behandling " + behandlingID));
    }

    public Optional<SedDokument> finnSedOpplysninger(long behandlingID) {
        return saksopplysningRepo.findByBehandling_IdAndType(behandlingID, SaksopplysningType.SEDOPPL)
            .map(s -> (SedDokument) s.getDokument());
    }

    public SedDokument hentSedOpplysninger(long behandlingID) {
        return finnSedOpplysninger(behandlingID)
            .orElseThrow(() -> new IkkeFunnetException("Finner ikke seddokument for behandling " + behandlingID));
    }

    public Optional<ArbeidsforholdDokument> finnArbeidsforholdsopplysninger(long behandlingID) {
        return saksopplysningRepo.findByBehandling_IdAndType(behandlingID, SaksopplysningType.ARBFORH)
            .map(s -> (ArbeidsforholdDokument) s.getDokument());
    }

    public Optional<InntektDokument> finnInntektsopplysninger(long behandlingID) {
        return saksopplysningRepo.findByBehandling_IdAndType(behandlingID, SaksopplysningType.INNTK)
            .map(s -> (InntektDokument) s.getDokument());
    }

    public PersonhistorikkDokument hentPersonhistorikk(long behandlingID) {
        return saksopplysningRepo.findByBehandling_IdAndType(behandlingID, SaksopplysningType.PERSHIST)
            .map(s -> (PersonhistorikkDokument) s.getDokument())
            .orElseThrow(() -> new IkkeFunnetException("Finner ikke personhistorikkDokument for behandling " + behandlingID));
    }
}
