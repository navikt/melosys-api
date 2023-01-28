package no.nav.melosys.service.brev.feature;

import java.util.List;

import no.nav.melosys.domain.brev.muligemottakere.Brevmottaker;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.domain.kodeverk.Aktoersroller.ETAT;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.FRITEKSTBREV;

@Component
public class HentMuligeBrevmottakereEtater {

    private final EregFasade eregFasade;

    public HentMuligeBrevmottakereEtater(EregFasade eregFasade) {
        this.eregFasade = eregFasade;
    }

    @Transactional
    public List<Brevmottaker> hentMuligeMottakereEtater(List<String> orgnrEtater) {
        return orgnrEtater.stream().map(this::mapTilBrevmottaker).toList();
    }

    @NotNull
    private Brevmottaker mapTilBrevmottaker(String orgnr) {
        return new Brevmottaker.Builder()
            .medRolle(ETAT)
            .medDokumentNavn(FRITEKSTBREV.getBeskrivelse())
            .medOrgnr(orgnr)
            .medMottakerNavn(hentMottakerNavn(orgnr))
            .build();
    }

    private String hentMottakerNavn(String orgnr) {
        var saksopplysning = eregFasade.finnOrganisasjon(orgnr);
        if (saksopplysning.isPresent()) {
            var orgDokument = (OrganisasjonDokument) saksopplysning.get().getDokument();
            return orgDokument.getNavn();
        } else {
            throw new IkkeFunnetException("Kan ikke hente mottakernavn til etat, fant ikke orgnr %s fra ereg".formatted(orgnr));
        }
    }
}
