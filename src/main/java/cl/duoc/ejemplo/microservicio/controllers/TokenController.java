package cl.duoc.ejemplo.microservicio.controllerss;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class TokenController {

    @GetMapping("/api/token")
    public Map<String, String> getToken(
            @RegisteredOAuth2AuthorizedClient("azure-b2c") OAuth2AuthorizedClient authorizedClient,
            @AuthenticationPrincipal OidcUser oidcUser) {
        
        Map<String, String> response = new HashMap<>();
        // Extraemos el ID Token gestionado por Spring Boot (JWT válido para AWS)
        String token = oidcUser.getIdToken().getTokenValue();
        System.out.println("=== DEBUG TOKEN PARA AWS ===");
        System.out.println("Issuer (iss): " + oidcUser.getIdToken().getIssuer());
        System.out.println("Audience (aud): " + oidcUser.getIdToken().getAudience());
        System.out.println("============================");
        response.put("token", token);
        // Nombre del usuario logueado
        response.put("username", oidcUser != null ? oidcUser.getFullName() : "Usuario");
        return response;
    }
}
