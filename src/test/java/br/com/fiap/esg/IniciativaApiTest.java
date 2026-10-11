package br.com.fiap.esg;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @AutoConfigureMockMvc
class IniciativaApiTest {
    @Autowired MockMvc mvc;
    @Autowired IniciativaRepository repository;
    @Autowired ObjectMapper mapper;
    private final String valid="""
      {"nome":"Coleta seletiva","cidade":"São Paulo","descricao":"Reciclagem no bairro","categoria":"AMBIENTAL","status":"PLANEJADA"}
      """;
    @BeforeEach void limpar(){ repository.deleteAll(); }
    @Test void cicloCrudEPersistencia() throws Exception {
        String body=mvc.perform(post("/api/iniciativas").contentType("application/json").content(valid))
          .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id=mapper.readTree(body).get("id").asLong();
        mvc.perform(get("/api/iniciativas")).andExpect(jsonPath("$[0].id").value(id));
        mvc.perform(get("/api/iniciativas/"+id)).andExpect(jsonPath("$.cidade").value("São Paulo"));
        mvc.perform(put("/api/iniciativas/"+id).contentType("application/json").content(valid.replace("PLANEJADA","CONCLUIDA")))
          .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CONCLUIDA"));
        Assertions.assertEquals(Iniciativa.Status.CONCLUIDA, repository.findById(id).orElseThrow().status);
        mvc.perform(delete("/api/iniciativas/"+id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/iniciativas/"+id)).andExpect(status().isNotFound());
    }
    @Test void rejeitaNomeVazio() throws Exception { mvc.perform(post("/api/iniciativas").contentType("application/json").content(valid.replace("Coleta seletiva"," "))).andExpect(status().isBadRequest()); }
    @Test void rejeitaCategoriaInvalida() throws Exception { mvc.perform(post("/api/iniciativas").contentType("application/json").content(valid.replace("AMBIENTAL","INVALIDA"))).andExpect(status().isBadRequest()); }
    @Test void rejeitaCamposAusentes() throws Exception { mvc.perform(post("/api/iniciativas").contentType("application/json").content("{}" )).andExpect(status().isBadRequest()); }
    @Test void naoAtualizaRegistroInexistente() throws Exception { mvc.perform(put("/api/iniciativas/999").contentType("application/json").content(valid)).andExpect(status().isNotFound()); }
    @Test void naoExcluiRegistroInexistente() throws Exception { mvc.perform(delete("/api/iniciativas/999")).andExpect(status().isNotFound()); }
    @Test void informaAmbiente() throws Exception { mvc.perform(get("/api/ambiente")).andExpect(status().isOk()).andExpect(jsonPath("$.ambiente").value("test")); }
    @Test void healthCheck() throws Exception { mvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP")); }
}
