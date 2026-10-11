package br.com.fiap.esg;
import jakarta.persistence.*;
@Entity
public class Iniciativa {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @Column(nullable=false, length=120) public String nome;
    @Column(nullable=false, length=120) public String cidade;
    @Column(nullable=false, length=2000) public String descricao;
    @Enumerated(EnumType.STRING) @Column(nullable=false) public Categoria categoria;
    @Enumerated(EnumType.STRING) @Column(nullable=false) public Status status;
    public enum Categoria { AMBIENTAL, SOCIAL, GOVERNANCA }
    public enum Status { PLANEJADA, EM_ANDAMENTO, CONCLUIDA }
}
