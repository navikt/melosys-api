package no.nav.melosys.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.inntekt.InntektDokument;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.person.PersonMedHistorikk;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.repository.SaksopplysningRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.service.persondata.PersondataService.PDL_PERSOPL_VERSJON;
import static no.nav.melosys.service.persondata.PersondataService.PDL_PERS_SAKS_VERSJON;

@Service
public class SaksopplysningerService {
    private final SaksopplysningRepository saksopplysningRepo;

    public SaksopplysningerService(SaksopplysningRepository saksopplysningRepo) {
        this.saksopplysningRepo = saksopplysningRepo;
    }

    public boolean harTpsPersonopplysninger(long behandlingID) {
        return saksopplysningRepo.existsByBehandling_IdAndType(behandlingID, SaksopplysningType.PERSOPL);
    }

    public PersonDokument hentTpsPersonopplysninger(long behandlingID) {
        return saksopplysningRepo.findByBehandling_IdAndType(behandlingID, SaksopplysningType.PERSOPL)
            .map(s -> (PersonDokument) s.getDokument())
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

    public Persondata hentPdlPersonopplysninger(long behandlingID) {
        return finnPdlPersonopplysninger(behandlingID)
            .orElseThrow(() -> new IkkeFunnetException("Fant ikke persondata for behandling " + behandlingID));
    }

    private Optional<Persondata> finnPdlPersonopplysninger(long behandlingID) {
        return saksopplysningRepo.findByBehandling_IdAndType(behandlingID, SaksopplysningType.PDL_PERSOPL)
            .map(s -> (Persondata) s.getDokument());
    }

    public Optional<PersonMedHistorikk> finnPdlPersonhistorikkTilSaksbehandler(long behandlingID) {
        return saksopplysningRepo.findByBehandling_IdAndType(behandlingID, SaksopplysningType.PDL_PERS_SAKS)
            .map(s -> (PersonMedHistorikk) s.getDokument());
    }

    @Transactional
    public void lagrePersonopplysninger(Behandling behandling, Persondata persondata) {
        if (behandling.manglerSaksopplysningerAvType(List.of(SaksopplysningType.PDL_PERSOPL))) {
            Instant nå = Instant.now();
            Saksopplysning saksopplysning = new Saksopplysning();
            saksopplysning.setDokument(persondata);
            saksopplysning.setType(SaksopplysningType.PDL_PERSOPL);
            saksopplysning.setBehandling(behandling);
            saksopplysning.setVersjon(PDL_PERSOPL_VERSJON);
            saksopplysning.setEndretDato(nå);
            saksopplysning.setRegistrertDato(nå);

            behandling.getSaksopplysninger().removeIf(s -> s.getType().equals(SaksopplysningType.PERSOPL));
            saksopplysningRepo.save(saksopplysning);
        }
    }

    @Transactional
    public void lagrePersonMedHistorikk(Behandling behandling, PersonMedHistorikk personMedHistorikk) {
        if (behandling.manglerSaksopplysningerAvType(List.of(SaksopplysningType.PDL_PERS_SAKS))) {
            Instant nå = Instant.now();
            Saksopplysning saksopplysning = new Saksopplysning();
            saksopplysning.setDokument(personMedHistorikk);
            saksopplysning.setType(SaksopplysningType.PDL_PERS_SAKS);
            saksopplysning.setBehandling(behandling);
            saksopplysning.setVersjon(PDL_PERS_SAKS_VERSJON);
            saksopplysning.setEndretDato(nå);
            saksopplysning.setRegistrertDato(nå);

            behandling.getSaksopplysninger().removeIf(s -> s.getType().equals(SaksopplysningType.PERSHIST));
            saksopplysningRepo.save(saksopplysning);
        }
    }
}
