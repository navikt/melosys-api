package no.nav.melosys.tjenester.gui;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import no.nav.melosys.domain.ProsessDataKey;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.ProsessType;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.saksflyt.api.Binge;
import no.nav.melosys.tjenester.gui.dto.ProsessinstansDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

import static no.nav.melosys.domain.ProsessDataKey.*;

@Profile("local")
@Path("/prosesser")
@Service
@Scope(value = WebApplicationContext.SCOPE_APPLICATION)
public class ProsessTjeneste extends RestTjeneste {

    private Binge binge;

    private ProsessinstansRepository prosessinstansRepo;

    @Autowired
    public ProsessTjeneste(Binge binge, ProsessinstansRepository prosessinstansRepository) {
        this.binge = binge;
        this.prosessinstansRepo = prosessinstansRepository;
    }

    @GET
    public List<ProsessinstansDto> finnAlle() {
        Iterable<Prosessinstans> alle = prosessinstansRepo.findAll();
        List<ProsessinstansDto> liste = new ArrayList<>();
        for (Prosessinstans p : alle) {
            ProsessinstansDto dto = new ProsessinstansDto();
            dto.setId(p.getId());
            dto.setData(p.getData());
            if (p.getBehandling() != null) {
                dto.setBehandlingID(p.getBehandling().getId());
            }
            dto.setEndretDato(p.getEndretDato());
            dto.setRegistrertDato(p.getRegistrertDato());
            dto.setSteg(p.getSteg());
            dto.setType(p.getType());
            liste.add(dto);
        }
        return liste;
    }

    @GET
    @Path("test")
    public Response test() {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.JFR_NY_SAK);
        prosessinstans.setSteg(ProsessSteg.JFR_OPPDATER_JOURNALPOST);
        Properties properties = new Properties();
        prosessinstans.setData(JOURNALPOST_ID, "415782364");
        prosessinstans.setData(DOKUMENT_ID, "425225989");
        prosessinstans.setData(GSAK_SAK_ID, "gsak_ID");
        prosessinstans.setData(BRUKER_ID, "FJERNET");
        prosessinstans.setData(AVSENDER_ID, "Avsender");
        prosessinstans.setData(AVSENDER_NAVN, "Avsernder navn");
        prosessinstans.setData(HOVEDDOKUMENT_TITTEL, "tittel");
        prosessinstans.addData(properties);
        LocalDateTime nå = LocalDateTime.now();
        prosessinstans.setEndretDato(nå);
        prosessinstans.setRegistrertDato(nå);
        prosessinstansRepo.save(prosessinstans);
        binge.leggTil(prosessinstans);

        return Response.ok().build();
    }

    private void testOpprett() {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.JFR_NY_SAK);
        //prosessinstans.setSteg(ProsessSteg.JFR_AKTOER_ID);
        prosessinstans.setSteg(ProsessSteg.JFR_OPPRETT_SAK_OG_BEH);
        prosessinstans.setData(ProsessDataKey.AKTØR_ID, "FJERNET93");
        LocalDateTime nå = LocalDateTime.now();
        prosessinstans.setEndretDato(nå);
        prosessinstans.setRegistrertDato(nå);
        prosessinstansRepo.save(prosessinstans);
        binge.leggTil(prosessinstans);
    }
}
