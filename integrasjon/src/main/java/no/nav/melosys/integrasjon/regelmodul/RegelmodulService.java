package no.nav.melosys.integrasjon.regelmodul;

import java.util.List;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;

import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.soeknad.Periode;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.felles.RestClientLoggingFilter;
import no.nav.melosys.regler.api.lovvalg.LovvalgTjeneste;
import no.nav.melosys.regler.api.lovvalg.rep.VurderInngangsvilkaarReply;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.logging.LoggingFeature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class RegelmodulService implements RegelmodulFasade {
    private final String regelmodulUrl;

    @Autowired
    public RegelmodulService(@Value("${melosys.service.regelmodul.url}") String regelmodulUrl) {
        this.regelmodulUrl = regelmodulUrl;
    }

    /**
     * Kaller regelmodulen for å kjøre inngangsvilkårsvurdering
     *
     * @throws RuntimeException Hvis request- eller reply-prosessering feiler, hvis IO-feil ved kommunikasjon med regelmodulen, eller hvis regelmodulen returnerer noe annet enn HTTP 2xx
     */
    @Override
    public VurderInngangsvilkaarReply vurderInngangsvilkår(Land brukersStatsborgerskap, List<String> søknadsland, Periode søknadsperiode) throws TekniskException {
        Assert.notNull(brukersStatsborgerskap, "Tjenesten krever at brukersStatsborgerskap ikke er null");
        Assert.notEmpty(søknadsland, "Tjenesten krever at søknadsland ikke er null eller tom");
        Assert.notNull(søknadsperiode, "Tjenesten krever at søknadsperiode ikke er null");
        Assert.notNull(søknadsperiode.getFom(), "Tjenesten krever at søknadsperiode har fom dato");

        try {
            String req = lagXMLRequest(brukersStatsborgerskap.getKode(), søknadsperiode.getFom().toString(), søknadsperiode.getTom().toString(), søknadsland);

            ClientConfig clientConfig = new ClientConfig();
            clientConfig.property(LoggingFeature.LOGGING_FEATURE_VERBOSITY_CLIENT, LoggingFeature.Verbosity.PAYLOAD_ANY);
            clientConfig.register(new RestClientLoggingFilter());

            return ClientBuilder.newClient(clientConfig)
                .target(regelmodulUrl)
                .path("/inngangsvilkaar")
                .request(LovvalgTjeneste.MEDIA_TYPE_CONSUMED)
                .post(Entity.entity(req, LovvalgTjeneste.MEDIA_TYPE_CONSUMED), VurderInngangsvilkaarReply.class);
        } catch (ProcessingException | WebApplicationException e) {
            throw new TekniskException("Uventet feil ved vurdering av inngangsvilkår", e);
        }
    }

    private String lagXMLRequest(String statsborgerskap, String fom, String tom, List<String> søknadsland) {
        StringBuilder format = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>" +
            "<FastsettLovvalgRequest><personDokument xmlns:tps3=\"http://nav.no/tjeneste/virksomhet/person/v3\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" +
            "<statsborgerskap><kode>%s</kode></statsborgerskap></personDokument><soeknadDokument><oppholdUtland><oppholdsPeriode><fom>%s</fom><tom>%s</tom></oppholdsPeriode>");

        // Vi sender soknadsland som oppholdsland og søknadsperiode som oppholdsperiode
        //  til regelmodulen inntil den er oppdatert med en nyere versjon av melosys
        for (String land : søknadsland) {
            format.append("<oppholdslandKoder>").append(land).append("</oppholdslandKoder>");
        }

        format.append("</oppholdUtland></soeknadDokument></FastsettLovvalgRequest>");
        return String.format(format.toString(), statsborgerskap, fom, tom);
    }
}
