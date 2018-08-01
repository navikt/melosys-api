package no.nav.melosys.integrasjon.sakogbehandling.behandlingskjede;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.xml.datatype.DatatypeConfigurationException;

import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.integrasjon.KonverteringsUtils;
import no.nav.tjeneste.virksomhet.sakogbehandling.v1.binding.SakOgBehandlingV1;
import no.nav.tjeneste.virksomhet.sakogbehandling.v1.informasjon.finnsakogbehandlingskjedeliste.Sak;
import no.nav.tjeneste.virksomhet.sakogbehandling.v1.meldinger.FinnSakOgBehandlingskjedeListeRequest;
import no.nav.tjeneste.virksomhet.sakogbehandling.v1.meldinger.FinnSakOgBehandlingskjedeListeResponse;

public class BehandlingskjedeConsumerImpl implements BehandlingskjedeConsumer {

    private SakOgBehandlingV1 port;

    public BehandlingskjedeConsumerImpl(SakOgBehandlingV1 port) {
        this.port = port;
    }

    @Override
    public List<SakDto> finnSakOgBehandlingskjedeListeResponse(String aktørId, LocalDate tidspunkt) throws IntegrasjonException {
        FinnSakOgBehandlingskjedeListeRequest request = new FinnSakOgBehandlingskjedeListeRequest();

        request.setAktoerREF(aktørId);
        try {
            request.setTidspunkt(KonverteringsUtils.localDateToXMLGregorianCalendar(tidspunkt));
        } catch (DatatypeConfigurationException e) {
            throw new IntegrasjonException("Kunne ikke konvertere dato");
        }

        FinnSakOgBehandlingskjedeListeResponse response = port.finnSakOgBehandlingskjedeListe(request);

        List<SakDto> saker = new ArrayList<>();

        // TODO Filtrer på sakstema og behandlingstema
        for (Sak sak : response.getSak()) {
            SakDto sakDto = new SakDto();
            sakDto.setSaksId(sak.getSaksId());
            sakDto.setSakstema(sak.getSakstema().getKodeverksRef());
            sakDto.setOpprettet(KonverteringsUtils.xmlGregorianCalendarToLocalDateTime(sak.getOpprettet()));
            sakDto.setLukket(KonverteringsUtils.xmlGregorianCalendarToLocalDateTime(sak.getLukket()));
            sakDto.setBehandlingskjede(new ArrayList<>());

            for (no.nav.tjeneste.virksomhet.sakogbehandling.v1.informasjon.finnsakogbehandlingskjedeliste.Behandlingskjede behandlingskjede : sak.getBehandlingskjede()) {
                BehandlingskjedeDto behandlingskjedeDto = new BehandlingskjedeDto();
                behandlingskjedeDto.setBehandlingstema(behandlingskjede.getBehandlingstema().getKodeverksRef());
                sakDto.getBehandlingskjede().add(behandlingskjedeDto);
            }
            saker.add(sakDto);
        }

        return saker;
    }
}
