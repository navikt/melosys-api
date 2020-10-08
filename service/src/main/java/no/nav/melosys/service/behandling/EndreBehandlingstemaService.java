package no.nav.melosys.service.behandling;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.repository.BehandlingRepository;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.*;

@Service
public class EndreBehandlingstemaService {
    private final BehandlingRepository behandlingRepository;

    private static final List<Behandlingstema> muligeBehandlingstemaForSoknad = Arrays.asList(UTSENDT_ARBEIDSTAKER, UTSENDT_SELVSTENDIG, ARBEID_ETT_LAND_ØVRIG, IKKE_YRKESAKTIV, ARBEID_FLERE_LAND,
        ARBEID_NORGE_BOSATT_ANNET_LAND);
    private static final List<Behandlingstema> muligeBehandlingstemaForSED = Arrays.asList(ØVRIGE_SED_MED, ØVRIGE_SED_UFM, TRYGDETID);

    public EndreBehandlingstemaService(BehandlingRepository behandlingRepository) {
        this.behandlingRepository = behandlingRepository;
    }


    public List<Behandlingstema> hentMuligeBehandlingstema(Behandling behandling) {
        if (muligeBehandlingstemaForSoknad.contains(behandling.getTema())) {
            return muligeBehandlingstemaForSoknad;
        } else if (muligeBehandlingstemaForSED.contains(behandling.getTema())) {
            return muligeBehandlingstemaForSED;
        } else {
            return Collections.emptyList();
        }
    }

    public Behandling endreBehandlingstemaTilBehandling(Behandling behandling, Behandlingstema nyttTema) {
        if (muligeBehandlingstemaForSoknad.contains(behandling.getTema()) && muligeBehandlingstemaForSoknad.contains(nyttTema)) {
            behandling.setTema(nyttTema);
            return behandlingRepository.save(behandling);

        } else if (muligeBehandlingstemaForSED.contains(behandling.getTema()) && muligeBehandlingstemaForSED.contains(nyttTema)) {
            behandling.setTema(nyttTema);
            return behandlingRepository.save(behandling);

        } else {
            return null;
        }
    }

}
