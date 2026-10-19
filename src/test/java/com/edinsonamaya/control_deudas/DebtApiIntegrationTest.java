package com.edinsonamaya.control_deudas;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DebtApiIntegrationTest {

    private static final String OWNER_EMAIL = "flujocompleto@test.com";
    private static final String PASSWORD = "secreta123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void flujoCompletoDeudasYAbonos() throws Exception {
        String token = register(OWNER_EMAIL);

        mockMvc.perform(get("/api/users/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(OWNER_EMAIL));

        long folderId = createFolder(token, "Viaje familiar");

        mockMvc.perform(get("/api/users/me/folders").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Viaje familiar"))
                .andExpect(jsonPath("$[0].totalDebt").value(0));

        long debtId = createDebt(token, folderId, "Pasajes", "500.00");

        mockMvc.perform(get("/api/folders/" + folderId + "/debts").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(debtId))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].paid").value(0))
                .andExpect(jsonPath("$[0].balance").value(500.00));

        MvcResult partial = mockMvc.perform(post("/api/debts/" + debtId + "/payments")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentBody("100.00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(100.00))
                .andReturn();
        assertThat(partial.getResponse().getContentAsString()).contains("100.00");

        mockMvc.perform(get("/api/debts/" + debtId + "/payments").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paid").value(100.00))
                .andExpect(jsonPath("$.balance").value(400.00))
                .andExpect(jsonPath("$.status").value("PARTIAL"))
                .andExpect(jsonPath("$.payments.length()").value(1));

        MvcResult overpay = mockMvc.perform(post("/api/debts/" + debtId + "/payments")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentBody("600.00")))
                .andExpect(status().isBadRequest())
                .andReturn();
        JsonNode overpayBody = objectMapper.readTree(overpay.getResponse().getContentAsString());
        assertThat(overpayBody.get("message").asText())
                .contains("no puede ser mayor al saldo pendiente");
        assertThat(overpayBody.get("path").asText()).isEqualTo("/api/debts/" + debtId + "/payments");

        mockMvc.perform(post("/api/debts/" + debtId + "/payments")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentBody("400.00")))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/debts/" + debtId + "/payments").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paid").value(500.00))
                .andExpect(jsonPath("$.balance").value(0))
                .andExpect(jsonPath("$.status").value("PAID"));

        MvcResult paidDebt = mockMvc.perform(post("/api/debts/" + debtId + "/payments")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentBody("10.00")))
                .andExpect(status().isBadRequest())
                .andReturn();
        assertThat(objectMapper.readTree(paidDebt.getResponse().getContentAsString())
                .get("message").asText()).contains("ya está completamente pagada");

        mockMvc.perform(get("/api/folders/" + folderId).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.debts[0].status").value("PAID"))
                .andExpect(jsonPath("$.debts[0].paid").value(500.00))
                .andExpect(jsonPath("$.debts[0].balance").value(0));

        mockMvc.perform(get("/api/folders/" + folderId + "/debts").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PAID"));
    }

    @Test
    void registroDuplicadoDevuelve409() throws Exception {
        register("duplicado@test.com");

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(authBody("duplicado@test.com")))
                .andExpect(status().isConflict())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("status").asInt()).isEqualTo(409);
        assertThat(body.get("message").asText()).contains("Ya existe");
    }

    @Test
    void loginConPasswordIncorrectoDevuelve401() throws Exception {
        register("login@test.com");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"login@test.com\",\"password\":\"otra-password\"}"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        assertThat(objectMapper.readTree(result.getResponse().getContentAsString())
                .get("message").asText()).isNotBlank();
    }

    @Test
    void peticionSinTokenDevuelve401() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andReturn();

        assertThat(objectMapper.readTree(result.getResponse().getContentAsString())
                .get("message").asText()).contains("Token JWT");
    }

    @Test
    void accesoACarpetaDeOtroUsuarioDevuelve403() throws Exception {
        String ownerToken = register("duenio@test.com");
        long folderId = createFolder(ownerToken, "Privada");

        String otherToken = register("intruso@test.com");

        mockMvc.perform(get("/api/folders/" + folderId).header("Authorization", bearer(otherToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message")
                        .value("No tiene permiso para acceder a este recurso."));

        mockMvc.perform(get("/api/folders/" + folderId + "/debts")
                        .header("Authorization", bearer(otherToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void carpetaInexistenteDevuelve404() throws Exception {
        String token = register("noexiste@test.com");

        MvcResult result = mockMvc.perform(get("/api/folders/999999")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound())
                .andReturn();

        assertThat(objectMapper.readTree(result.getResponse().getContentAsString())
                .get("message").asText()).contains("Carpeta");
    }

    @Test
    void validacionDeCuerpoDevuelve400() throws Exception {
        String token = register("validacion@test.com");

        mockMvc.perform(post("/api/users/me/folders")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(
                        "name: El nombre de la carpeta es obligatorio"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"corta@test.com\",\"password\":\"1234567\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void documentacionOpenApiDisponibleSinToken() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").isNotEmpty())
                .andExpect(jsonPath("$.paths").isNotEmpty());
    }

    private String register(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(authBody(email)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token").asText();
    }

    private long createFolder(String token, String name) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        MvcResult result = mockMvc.perform(post("/api/users/me/folders")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asLong();
    }

    private long createDebt(String token, long folderId, String description, String amount)
            throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("description", description);
        body.put("amount", new java.math.BigDecimal(amount));
        body.put("date", LocalDate.now().plusMonths(1).toString());
        MvcResult result = mockMvc.perform(post("/api/folders/" + folderId + "/debts")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asLong();
    }

    private String authBody(String email) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("email", email);
        body.put("password", PASSWORD);
        body.put("fullName", "Usuario de Prueba");
        return objectMapper.writeValueAsString(body);
    }

    private String paymentBody(String amount) {
        return "{\"amount\":" + amount + "}";
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
