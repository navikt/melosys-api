package no.nav.melosys.controller;

import javax.ws.rs.Path;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

import io.swagger.annotations.Api;

@Api(tags = {"person"})
@Path("/person")
@Service
@Scope(value= WebApplicationContext.SCOPE_REQUEST)
public class PersonRestTjeneste {
}
