package no.nav.melosys.service;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.inntekt.InntektDokument;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.regler.api.lovvalg.rep.FastsettLovvalgReply;
import no.nav.melosys.regler.api.lovvalg.req.FastsettLovvalgRequest;
import no.nav.melosys.repository.BehandlingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.ws.rs.client.ClientBuilder;
import java.util.ArrayList;

/**
 * Service som kaller regelmodulen.
 */
@Service
public class RegelmodulService {

    private String regelmodulUrl;

    private BehandlingRepository behandlingRepo;

    @Autowired
    public RegelmodulService(@Value("${melosys.service.regelmodul.url}") String regelmodulUrl, BehandlingRepository repository) {
        this.regelmodulUrl = regelmodulUrl;
        this.behandlingRepo = repository;
    }

    /**
     * Kall til regelmodulen med opplysninger knyttet til behandlingen med ID {@code behandlingID}.
     * @param behandlingID Database ID til den behandlingen som brukes for å konstruere requesten til regelmodulen.
     */
    public FastsettLovvalgReply fastsettLovvalg(long behandlingID) {
        Behandling behandling = behandlingRepo.findOne(behandlingID);
        if (behandling == null) {
            // Ikke funnet
            return null;
        }

        FastsettLovvalgRequest fastsettLovvalgRequest = lagRequest(behandling);

        FastsettLovvalgReply fastsettLovvalgReply = ClientBuilder.newClient().target(regelmodulUrl)
                .queryParam("req", fastsettLovvalgRequest)
                .request()
                .get(FastsettLovvalgReply.class);

        return fastsettLovvalgReply;
    }

    /**
     * Lager en request til regelmodulen for en gitt behandling.
     */
    FastsettLovvalgRequest lagRequest(Behandling behandling) {
        FastsettLovvalgRequest fastsettLovvalgRequest = new FastsettLovvalgRequest();
        fastsettLovvalgRequest.arbeidsforholdDokumenter = new ArrayList<>();
        fastsettLovvalgRequest.inntektDokumenter = new ArrayList<>();
        fastsettLovvalgRequest.medlemskapDokumenter = new ArrayList<>();
        fastsettLovvalgRequest.organisasjonDokumenter = new ArrayList<>();

        for (Saksopplysning saksopplysning : behandling.getSaksopplysninger()) {
            SaksopplysningType type = saksopplysning.getType();
            SaksopplysningDokument dokument = saksopplysning.getDokument();

            switch (type) {
                case ARBEIDSFORHOLD:
                    fastsettLovvalgRequest.arbeidsforholdDokumenter.add((ArbeidsforholdDokument)dokument);
                    break;
                case INNTEKT:
                    fastsettLovvalgRequest.inntektDokumenter.add((InntektDokument)dokument);
                    break;
                case MEDLEMSKAP:
                    fastsettLovvalgRequest.medlemskapDokumenter.add((MedlemskapDokument)dokument);
                    break;
                case ORGANISASJON:
                    fastsettLovvalgRequest.organisasjonDokumenter.add((OrganisasjonDokument)dokument);
                    break;
                case PERSONOPPLYSNING:
                    fastsettLovvalgRequest.personopplysningDokument = (PersonDokument) dokument;
                    break;
                case SØKNAD:
                    fastsettLovvalgRequest.søknadDokument = (SoeknadDokument) dokument;
                    break;
                default:
                    throw new IllegalArgumentException("Type " + type.getKode() + " ikke støttet.");
            }
        }

        return fastsettLovvalgRequest;
    }

}
