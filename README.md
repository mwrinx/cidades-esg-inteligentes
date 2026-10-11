# Projeto — Cidades ESG Inteligentes

Autora: Marina Espirito Santo Lopes — RM 558200.

Aplicação acadêmica criada para o desafio DevOps. Cadastra, lista, edita e exclui iniciativas municipais dos eixos ambiental, social e governança. Campos: nome, cidade, descrição, categoria e status. Dados são persistidos em PostgreSQL. Não há autenticação; os ambientes são demonstrações locais, acessíveis somente pelo próprio computador.

## Como executar localmente com Docker

Pré-requisitos: Docker Desktop iniciado com contêineres Linux. No PowerShell, abra a pasta do projeto:

```powershell
Copy-Item .env.example .env
# Edite DB_PASSWORD no arquivo .env antes de continuar.
docker compose up -d --build --wait --wait-timeout 180
docker compose ps
```

Abra http://localhost:8080. Para encerrar sem apagar os dados: `docker compose down`. Não utilize `down -v` se quiser preservar os registros.

### Testes automatizados

```powershell
docker run --rm -v "${PWD}:/project" -w /project maven:3.9.9-eclipse-temurin-21 mvn -B verify
```

São oito testes de integração com Spring Boot, MockMvc e H2: CRUD completo e persistência, nome vazio, categoria inválida, campos ausentes, atualização e exclusão inexistentes, identificação do ambiente e saúde. H2 é usado somente nos testes; os ambientes Docker usam PostgreSQL.

## Pipeline CI/CD

Ferramenta: GitHub Actions, arquivo `.github/workflows/ci-cd.yml`. Push em `main` ou execução manual dispara:

1. **Build**: Java 21 e `mvn compile` em runner Ubuntu do GitHub.
2. **Testes**: `mvn verify` e publicação dos relatórios JUnit.
3. **Imagem**: Docker build em um único runner Windows local; a tag é o SHA do commit.
4. **Staging**: Compose com projeto `esg-staging`, porta 8081 e verificação de saúde, versão e API.
5. **Produção**: após staging aprovado automaticamente, usa a mesma imagem, projeto `esg-production` e porta 8082.

Falhas interrompem os jobs dependentes. Concurrency impede pipelines simultâneos. O deploy é persistente no computador do runner, não em máquinas temporárias do GitHub. Ambos os ambientes simulam produção local; não são hospedagem pública nem uma arquitetura de alta disponibilidade. Não existe rollback automático.

### Configuração do GitHub

Repositório: https://github.com/mwrinx/cidades-esg-inteligentes

- Em Settings → Environments, crie `staging` e `production`.
- Em cada ambiente, configure o secret `DB_PASSWORD` com uma senha própria. Não publique as senhas no código.
- Em Settings → Actions → Runners → New self-hosted runner, selecione Windows x64. Siga os comandos mostrados pelo GitHub em uma pasta separada do projeto.
- Cadastre **um único runner**, execute `run.cmd` na sessão do usuário que abriu o Docker Desktop e mantenha computador, Docker e runner ligados durante o pipeline.
- O token temporário de registro é usado apenas no computador, não deve ser compartilhado.
- O runner deve encontrar `docker` e `git` no PATH. Reinicie o terminal após instalações.
- Envie os arquivos incluindo `.github/workflows/ci-cd.yml` e execute o workflow.

As URLs localhost só funcionam no computador do runner. O professor pode executar o ZIP usando as instruções locais; acesso ao repositório privado exige permissão concedida pela autora.

### Deploy manual para diagnóstico

```powershell
docker build -t cidades-esg:manual .
$env:DB_PASSWORD='sua_senha_local'
./scripts/deploy.ps1 -Environment staging -Image cidades-esg:manual -Version manual
./scripts/deploy.ps1 -Environment production -Image cidades-esg:manual -Version manual
```

Abra http://localhost:8081 e http://localhost:8082. Deploy manual não substitui a evidência de execução automática no GitHub Actions.

## Containerização

Dockerfile em duas etapas: Maven/JDK 21 compila o JAR; JRE 21 executa com usuário sem privilégios. O build da imagem pula testes porque eles já são exigidos no job anterior; a validação local deve executar `mvn verify` antes de construir a imagem.

```dockerfile
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B package -DskipTests

FROM eclipse-temurin:21-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/* && useradd --system --uid 10001 app
WORKDIR /app
COPY --from=build --chown=app:app /build/target/app.jar app.jar
USER app
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=6 CMD curl -fsS http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java","-XX:MaxRAMPercentage=70","-jar","app.jar"]
```

Compose orquestra `app` e `db`. Rede bridge e volume nomeado são isolados por projeto Compose. O banco não publica portas no host. Variáveis de ambiente configuram conexão, senha, ambiente e versão. O healthcheck do PostgreSQL condiciona a inicialização da aplicação. Hibernate `update` simplifica o esquema deste exercício; uma implantação real precisaria de migrações versionadas, autenticação, TLS, backups e gestão de segredos apropriada.

## Prints do funcionamento

**Pendente de execução e captura real.** Não considerar este projeto pronto para entrega até incluir:

- Print dos jobs build, testes, imagem, staging e produção concluídos no GitHub Actions e link da execução.
- Print da tela http://localhost:8081 com indicador STAGING e uma iniciativa cadastrada.
- Print da tela http://localhost:8082 com indicador PRODUCTION e uma iniciativa cadastrada.
- Print de `docker compose -p esg-staging ps` e `docker compose -p esg-production ps` com serviços saudáveis.
- Evidência de persistência após reiniciar a aplicação e de isolamento entre os bancos.

Guardar prints na pasta `docs/evidencias` e referenciá-los aqui após validação. Não incluir segredos nos prints.

## Tecnologias utilizadas

Java 21, Spring Boot 3.5.16, Spring Web, Spring Data JPA, Bean Validation, Actuator, PostgreSQL 16, H2, JUnit 5, MockMvc, Maven, HTML/CSS/JavaScript, Docker, Docker Compose, GitHub Actions e PowerShell.

## Desafios encontrados e soluções

- Virtual Machine Platform desativada: habilitado o recurso Windows e reiniciado o computador.
- WSL ausente: instalado com `wsl --install`, configurado usuário Ubuntu e reiniciado; Docker passou a indicar Engine running.
- Deploy no computador local: runner Windows próprio para que os serviços persistam após os jobs.
- Separação de ambientes: nomes Compose distintos, volumes e redes próprios e portas 8081/8082.

## Checklist de entrega

Atualizar somente depois de validar cada item.

- [ ] Projeto compactado em ZIP com estrutura organizada
- [ ] Dockerfile funcional (build e execução verificados)
- [ ] Docker Compose funcional (serviços verificados)
- [ ] Pipeline executado com build, teste e deploy
- [ ] README com instruções e prints reais
- [ ] Documentação técnica PDF ou PPT com evidências
- [ ] Deploy automático realizado em staging e produção
