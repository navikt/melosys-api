package no.nav.melosys.service.registeropplysninger;

import java.util.HashSet;
import java.util.Set;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Primary
@Service
public class OrganisasjonOppslagService {
    private final EregFasade eregFasade;

    public OrganisasjonOppslagService(EregFasade eregFasade) {
        this.eregFasade = eregFasade;
    }

    public Set<OrganisasjonDokument> hentOrganisasjoner(Set<String> orgnumre) {
        Set<OrganisasjonDokument> organisasjoner = new HashSet<>();
        for (String orgnr : orgnumre) {
            OrganisasjonDokument saksopplysning = hentOrganisasjon(orgnr);
            if (saksopplysning != null) {
                organisasjoner.add(saksopplysning);
            }
        }
        return organisasjoner;
    }

    public OrganisasjonDokument hentOrganisasjon(String orgnummer) {
        Saksopplysning saksopplysning = eregFasade.hentOrganisasjon(validerOgVaskOrgnr(orgnummer));
        return (OrganisasjonDokument) saksopplysning.getDokument();
    }

    private String validerOgVaskOrgnr(String orgnr) {
        orgnr = orgnr.replace(" ", "");

        if (orgnr.length() != 9) {
            throw new FunksjonellException("Ugyldig orgnr " + orgnr);
        }

        return orgnr;
    }
}
