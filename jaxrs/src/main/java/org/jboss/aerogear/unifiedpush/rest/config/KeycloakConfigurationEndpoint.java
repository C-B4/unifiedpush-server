package org.jboss.aerogear.unifiedpush.rest.config;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.aerogear.unifiedpush.service.impl.spring.IConfigurationService;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.keycloak.util.SystemPropertiesJsonParserFactory;
import org.springframework.stereotype.Controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
@Path("/keycloak/config")
public class KeycloakConfigurationEndpoint {

	@Inject
	private IConfigurationService configurationService;

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response configurationFile() {

		ObjectMapper mapper = new ObjectMapper(new SystemPropertiesJsonParserFactory());
		mapper.setSerializationInclusion(JsonInclude.Include.NON_DEFAULT);
		AdapterConfig adapterConfig = new AdapterConfig();
		adapterConfig.setRealm(configurationService.getUpsRealm());
		adapterConfig.setAuthServerUrl(configurationService.getOAuth2Url());
		adapterConfig.setSslRequired("external");
		adapterConfig.setPublicClient(true);
		adapterConfig.setResource("unified-push-server-js");

		try {
			return Response.ok(mapper.writeValueAsString(adapterConfig)).build();
		} catch (JsonProcessingException e) {
			return Response.serverError().build();
		}

	}
}
