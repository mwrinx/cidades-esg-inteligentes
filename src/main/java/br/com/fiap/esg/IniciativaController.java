package br.com.fiap.esg;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.beans.factory.annotation.Value;
import java.util.*;
@RestController
@RequestMapping("/api")
public class IniciativaController {
    private final IniciativaRepository repository;
    @Value("${app.environment}") private String environment;
    @Value("${app.version}") private String version;
    public IniciativaController(IniciativaRepository repository) { this.repository=repository; }
    public record Dados(@NotBlank @Size(max=120) String nome, @NotBlank @Size(max=120) String cidade,
        @NotBlank @Size(max=2000) String descricao, @NotNull Iniciativa.Categoria categoria, @NotNull Iniciativa.Status status) {}
    @GetMapping("/ambiente") public Map<String,String> ambiente() { return Map.of("ambiente",environment,"versao",version); }
    @GetMapping("/iniciativas") public List<Iniciativa> listar() { return repository.findAll(org.springframework.data.domain.Sort.by("id").descending()); }
    @GetMapping("/iniciativas/{id}") public Iniciativa buscar(@PathVariable Long id) {
        return repository.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Iniciativa não encontrada"));
    }
    @PostMapping("/iniciativas") @ResponseStatus(HttpStatus.CREATED)
    public Iniciativa criar(@Valid @RequestBody Dados dados) { return salvar(new Iniciativa(),dados); }
    @PutMapping("/iniciativas/{id}") public Iniciativa atualizar(@PathVariable Long id,@Valid @RequestBody Dados dados) { return salvar(buscar(id),dados); }
    @DeleteMapping("/iniciativas/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) { repository.delete(buscar(id)); }
    private Iniciativa salvar(Iniciativa i, Dados d) {
        i.nome=d.nome().trim(); i.cidade=d.cidade().trim(); i.descricao=d.descricao().trim(); i.categoria=d.categoria(); i.status=d.status();
        return repository.save(i);
    }
}
