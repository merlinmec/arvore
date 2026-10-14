package dev.joaomanoel.arvore.demo;

import dev.joaomanoel.arvore.dto.BackupDTO;
import dev.joaomanoel.arvore.entity.DiaSemana;
import dev.joaomanoel.arvore.util.Datas;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Os dados fictícios de cada visitante: uma árvore com dez semanas de história, com as datas
 * relativas a hoje. Sai como um {@link BackupDTO}, que a restauração grava com as datas de criação
 * no passado (pela API, tudo nasceria hoje, e o passado sem plano não conta no Painel).
 *
 * <p>Mostra de propósito cada recurso: grade que mudou no meio do caminho, meta em N vezes por
 * semana, meta quantitativa, hábito negativo, objetivo concluído, lembretes, diário, revisões,
 * sessões de foco e notas com tags. Hoje fica quase todo por fazer, pra quem chega poder marcar.
 */
final class DemoDados {

    private static final int SEMANAS = 10;
    private static final Set<DiaSemana> TODOS_OS_DIAS = EnumSet.allOf(DiaSemana.class);

    private final LocalDate hoje;
    private final LocalDate inicio;
    private final Instant criadoEm;
    // semente fixa: todo visitante vê a mesma história (e o teste, também)
    private final Random sorte = new Random(42);

    private final List<BackupDTO.Area> areas = new ArrayList<>();
    private final List<BackupDTO.Nota> notas = new ArrayList<>();
    private final List<BackupDTO.Objetivo> objetivos = new ArrayList<>();
    private final List<BackupDTO.Atividade> atividades = new ArrayList<>();
    private final List<BackupDTO.Agenda> agendas = new ArrayList<>();
    private final List<BackupDTO.Frequencia> frequencias = new ArrayList<>();
    private final List<BackupDTO.Conclusao> conclusoes = new ArrayList<>();
    private final List<BackupDTO.Revisao> revisoes = new ArrayList<>();
    private final List<BackupDTO.Diario> diario = new ArrayList<>();
    private final List<BackupDTO.SessaoFoco> sessoesFoco = new ArrayList<>();

    private DemoDados(LocalDate hoje) {
        this.hoje = hoje;
        this.inicio = Datas.inicioDaSemana(hoje).minusWeeks(SEMANAS - 1);
        this.criadoEm = noDia(inicio, 9, 0);
    }

    static BackupDTO gerar(LocalDate hoje) {
        return new DemoDados(hoje).montar();
    }

    private BackupDTO montar() {
        UUID saude = area(null, "Saúde", "Corpo em dia: treino, corrida e sono.", "#5fc489", 0);
        UUID corrida = area(saude, "Corrida", "Base aeróbica pra primeira prova de 10 km.", "#46a873", 0);
        UUID sono = area(saude, "Sono", "Dormir antes da meia-noite nos dias úteis.", "#8fd9ae", 1);
        UUID carreira = area(null, "Carreira", "Crescer como dev backend.", "#6aa6f2", 1);
        UUID estudos = area(carreira, "Estudos", "Spring, arquitetura e inglês.", "#93bdf6", 0);
        UUID mente = area(null, "Mente", "Leitura e meditação.", "#b48cf2", 2);
        UUID leitura = area(mente, "Leitura", null, "#cdb0f7", 0);
        UUID financas = area(null, "Finanças", "Reserva de emergência e menos gasto por impulso.", "#e6b450", 3);

        // Energia de cada dia passado: puxa a conclusão de tudo, e o humor vem do que foi cumprido.
        int dias = (int) ChronoUnit.DAYS.between(inicio, hoje);
        int[] energia = new int[dias];
        for (int i = 0; i < dias; i++) {
            energia[i] = 1 + Math.min(4, Math.max(0, (int) Math.round(2.4 + sorte.nextGaussian() * 0.9)));
        }
        Placar placar = new Placar(dias);

        // Corrida: começou segunda e quarta; na 5ª semana a grade passou pra terça, quinta e sábado.
        UUID correr = atividade(objetivo(corrida, "Correr 120 km até a prova", hoje.plusDays(40), "ATIVO", 120, "km"), "Correr", false, null);
        LocalDate trocaDaGrade = inicio.plusWeeks(4);
        agenda(correr, EnumSet.of(DiaSemana.SEGUNDA, DiaSemana.QUARTA), inicio, trocaDaGrade);
        agenda(correr, EnumSet.of(DiaSemana.TERCA, DiaSemana.QUINTA, DiaSemana.SABADO), trocaDaGrade, null);
        for (LocalDate dia = inicio; dia.isBefore(hoje); dia = dia.plusDays(1)) {
            Set<DiaSemana> grade = dia.isBefore(trocaDaGrade)
                    ? EnumSet.of(DiaSemana.SEGUNDA, DiaSemana.QUARTA)
                    : EnumSet.of(DiaSemana.TERCA, DiaSemana.QUINTA, DiaSemana.SABADO);
            if (grade.contains(Datas.diaSemanaDe(dia))) {
                double km = 4 + 3.5 * progresso(dia) + sorte.nextDouble() * 1.5;
                planejadoNoDia(placar, energia, dia, correr, 0.72, decimal(km, 1));
            }
        }

        UUID academia = atividade(objetivo(saude, "Treinar força 3x por semana", null, "ATIVO", 0, null), "Academia", false, null);
        frequencia(academia, 3);
        vezesPorSemana(energia, academia, 3, 0.55, () -> null);

        UUID telas = atividade(objetivo(sono, "Dormir antes da meia-noite", null, "ATIVO", 0, null), "Desligar as telas às 23h", false, LocalTime.of(22, 45));
        Set<DiaSemana> noitesDeSemana = EnumSet.of(DiaSemana.DOMINGO, DiaSemana.SEGUNDA, DiaSemana.TERCA, DiaSemana.QUARTA, DiaSemana.QUINTA);
        agenda(telas, noitesDeSemana, inicio, null);
        diasFixos(placar, energia, telas, noitesDeSemana, 0.6);

        UUID pomodoro = atividade(objetivo(estudos, "Terminar o curso de Spring Boot", hoje.plusDays(25), "ATIVO", 0, null), "Estudar 1 pomodoro", false, LocalTime.of(20, 0));
        Set<DiaSemana> diasDeEstudo = EnumSet.of(DiaSemana.SEGUNDA, DiaSemana.QUARTA, DiaSemana.SEXTA);
        agenda(pomodoro, diasDeEstudo, inicio, null);
        for (LocalDate dia : diasFixos(placar, energia, pomodoro, diasDeEstudo, 0.75)) {
            foco(pomodoro, dia, 20, 25 + sorte.nextInt(35));
        }

        UUID ingles = atividade(objetivo(carreira, "Conversar em inglês sem travar", hoje.plusDays(90), "ATIVO", 0, null), "Praticar inglês", false, null);
        frequencia(ingles, 2);
        vezesPorSemana(energia, ingles, 2, 0.4, () -> null);

        objetivo(carreira, "Publicar o Árvore no portfólio", hoje.minusDays(5), "CONCLUIDO", 0, null);

        UUID ler = atividade(objetivo(leitura, "Ler 600 páginas no trimestre", hoje.plusDays(50), "ATIVO", 600, "páginas"), "Ler", false, null);
        frequencia(ler, 5);
        vezesPorSemana(energia, ler, 5, 0.8, () -> BigDecimal.valueOf(12 + sorte.nextInt(24)));

        UUID meditar = atividade(objetivo(mente, "Meditar todos os dias", null, "ATIVO", 0, null), "Meditar 10 min", false, LocalTime.of(7, 30));
        agenda(meditar, TODOS_OS_DIAS, inicio, null);
        for (LocalDate dia : diasFixos(placar, energia, meditar, TODOS_OS_DIAS, 0.68)) {
            foco(meditar, dia, 7, 10 + sorte.nextInt(4));
        }
        // a de hoje já foi, de manhã: o resto do dia fica pra quem visita
        conclusao(meditar, hoje, null);

        UUID reserva = atividade(objetivo(financas, "Montar a reserva de emergência", hoje.plusDays(120), "ATIVO", 6000, "R$"), "Guardar dinheiro", false, null);
        frequencia(reserva, 1);
        vezesPorSemana(energia, reserva, 1, 0.5, () -> BigDecimal.valueOf(150 + 50L * sorte.nextInt(6)));

        // Hábito negativo: cada registro é uma recaída, mais comum no fim de semana e cada vez mais rara.
        UUID delivery = atividade(objetivo(financas, "Gastar menos com delivery", null, "ATIVO", 0, null), "Não pedir delivery", true, null);
        for (LocalDate dia = inicio; dia.isBefore(hoje); dia = dia.plusDays(1)) {
            boolean fimDeSemana = dia.getDayOfWeek().getValue() >= 5;
            double chance = (fimDeSemana ? 0.45 : 0.15) * (1 - 0.7 * progresso(dia));
            if (sorte.nextDouble() < chance) conclusao(delivery, dia, null);
        }

        diario(placar, energia);
        revisoes();
        notas(corrida, leitura, estudos, financas, mente);

        return new BackupDTO(
                BackupDTO.VERSAO_ATUAL,
                Instant.now(),
                areas,
                notas,
                objetivos,
                atividades,
                agendas,
                frequencias,
                conclusoes,
                revisoes,
                diario,
                sessoesFoco);
    }

    /** Planejado e cumprido por dia, pra o humor do diário acompanhar o dia que a pessoa teve. */
    private final class Placar {
        final int[] planejado;
        final int[] cumprido;

        Placar(int dias) {
            planejado = new int[dias];
            cumprido = new int[dias];
        }

        void contar(LocalDate dia, boolean feito) {
            int i = indice(dia);
            planejado[i]++;
            if (feito) cumprido[i]++;
        }
    }

    private List<LocalDate> diasFixos(Placar placar, int[] energia, UUID atividade, Set<DiaSemana> grade, double chance) {
        List<LocalDate> feitos = new ArrayList<>();
        for (LocalDate dia = inicio; dia.isBefore(hoje); dia = dia.plusDays(1)) {
            if (grade.contains(Datas.diaSemanaDe(dia)) && planejadoNoDia(placar, energia, dia, atividade, chance, null)) {
                feitos.add(dia);
            }
        }
        return feitos;
    }

    private boolean planejadoNoDia(Placar placar, int[] energia, LocalDate dia, UUID atividade, double chance, BigDecimal valor) {
        boolean feito = sorte.nextDouble() < chanceDoDia(chance, dia, energia);
        placar.contar(dia, feito);
        if (feito) conclusao(atividade, dia, valor);
        return feito;
    }

    /** Meta de N vezes por semana: o dia que der, até bater a meta da semana. */
    private void vezesPorSemana(int[] energia, UUID atividade, int vezes, double chance, Supplier<BigDecimal> valor) {
        int naSemana = 0;
        for (LocalDate dia = inicio; dia.isBefore(hoje); dia = dia.plusDays(1)) {
            if (dia.getDayOfWeek().getValue() == 1) naSemana = 0;
            if (naSemana >= vezes) continue;
            if (sorte.nextDouble() < chanceDoDia(chance, dia, energia)) {
                naSemana++;
                conclusao(atividade, dia, valor.get());
            }
        }
    }

    /** Melhora com o tempo e com a energia do dia. */
    private double chanceDoDia(double base, LocalDate dia, int[] energia) {
        double chance = base * (0.8 + 0.35 * progresso(dia)) + (energia[indice(dia)] - 3) * 0.08;
        return Math.max(0.05, Math.min(0.97, chance));
    }

    private void diario(Placar placar, int[] energia) {
        String[] notasDoDia = {
            "Dormi mal, mas o treino salvou o dia.",
            "Semana puxada no trabalho; fiz o básico.",
            "Corrida boa, sem dor no joelho.",
            "Muita reunião hoje, pouca cabeça pra estudar.",
            "Li no ônibus e rendeu.",
            "Dia leve. Deu tempo de tudo.",
            "Cansado. Amanhã recomeço.",
        };
        for (LocalDate dia = inicio; dia.isBefore(hoje); dia = dia.plusDays(1)) {
            if (sorte.nextDouble() > 0.85) continue;
            int i = indice(dia);
            double taxa = placar.planejado[i] == 0 ? 0.6 : (double) placar.cumprido[i] / placar.planejado[i];
            int humor = limitar((int) Math.round(1.6 + 3 * taxa + sorte.nextGaussian() * 0.5));
            String nota = sorte.nextDouble() < 0.22 ? notasDoDia[sorte.nextInt(notasDoDia.length)] : null;
            Instant registro = noDia(dia, 22, 0);
            diario.add(new BackupDTO.Diario(UUID.randomUUID(), dia, humor, energia[i], nota, registro, registro));
        }
    }

    private void revisoes() {
        String[] textos = {
            "Comecei bem, mas a corrida de quarta ficou pra trás. Vou correr de manhã.",
            "Semana boa! A meditação virou hábito; falta acertar o sono.",
            "Viajei no fim de semana e pedi delivery duas vezes. Faz parte.",
            "Troquei a corrida pra terça, quinta e sábado: segunda e quarta não estavam dando.",
            "Melhor semana até agora no estudo. O pomodoro às 20h funciona.",
            "Pouca energia, cumpri o essencial. Próxima semana: dormir mais cedo.",
            "Bati as 5 sessões de leitura pela primeira vez.",
            "Reserva de emergência passou de mil reais. Seguindo.",
            "Semana corrida no trabalho, mas não larguei a academia.",
        };
        for (int semana = 0; semana < SEMANAS - 1; semana++) {
            if (semana != 3 && sorte.nextDouble() > 0.8) continue;
            LocalDate segunda = inicio.plusWeeks(semana);
            Instant registro = noDia(segunda.plusDays(6), 18, 30);
            revisoes.add(new BackupDTO.Revisao(UUID.randomUUID(), segunda, textos[semana % textos.length], registro, registro));
        }
    }

    private void notas(UUID corrida, UUID leitura, UUID estudos, UUID financas, UUID mente) {
        nota(corrida, "Plano até a prova de 10 km", """
                Três corridas por semana, aumentando **10% de distância** por semana.

                - Terça: rodagem leve
                - Quinta: tiros (6 × 400 m)
                - Sábado: longão

                Semana da prova: só rodagem leve.""", List.of("treino", "metas"));
        nota(leitura, "Livros do trimestre", """
                - [x] *O Programador Pragmático*
                - [x] *Hábitos Atômicos*
                - [ ] *Arquitetura Limpa*
                - [ ] *Designing Data-Intensive Applications*""", List.of("leitura"));
        nota(estudos, "Resumo: transações no Spring", """
                `@Transactional` só funciona em chamada **vinda de fora** do bean: o proxy intercepta a chamada.

                ```java
                @Transactional
                public void transferir(Conta origem, Conta destino, BigDecimal valor) { ... }
                ```

                Exceção *checked* não faz rollback por padrão: usar `rollbackFor`.""", List.of("estudos", "spring"));
        nota(financas, "Regras da reserva", """
                1. Seis meses de custo fixo.
                2. Guardar **no dia do salário**, antes de gastar.
                3. Só mexer em emergência de verdade.""", List.of("metas", "financas"));
        nota(mente, "Por que meditar de manhã", """
                À noite eu sempre esquecia. Logo depois do café virou automático:
                o lembrete das 7h30 ajudou nas primeiras semanas.""", List.of("habitos"));
    }

    private UUID area(UUID pai, String nome, String descricao, String cor, int ordem) {
        UUID id = UUID.randomUUID();
        areas.add(new BackupDTO.Area(id, pai, nome, descricao, cor, null, ordem, criadoEm, criadoEm));
        return id;
    }

    private UUID objetivo(UUID area, String descricao, LocalDate prazo, String status, int meta, String unidade) {
        UUID id = UUID.randomUUID();
        BigDecimal metaValor = unidade == null ? null : BigDecimal.valueOf(meta);
        objetivos.add(new BackupDTO.Objetivo(id, area, descricao, prazo, status, metaValor, unidade, criadoEm, criadoEm));
        return id;
    }

    private UUID atividade(UUID objetivo, String nome, boolean negativa, LocalTime lembrete) {
        UUID id = UUID.randomUUID();
        atividades.add(new BackupDTO.Atividade(id, objetivo, nome, negativa, lembrete, criadoEm, criadoEm));
        return id;
    }

    private void agenda(UUID atividade, Set<DiaSemana> dias, LocalDate de, LocalDate ate) {
        for (DiaSemana dia : dias) {
            agendas.add(new BackupDTO.Agenda(UUID.randomUUID(), atividade, dia.name(), de, ate, noDia(de, 9, 0)));
        }
    }

    private void frequencia(UUID atividade, int vezes) {
        frequencias.add(new BackupDTO.Frequencia(UUID.randomUUID(), atividade, vezes, inicio, null, criadoEm));
    }

    private void conclusao(UUID atividade, LocalDate dia, BigDecimal valor) {
        Instant quando = dia.equals(hoje) ? Instant.now() : noDia(dia, 19, sorte.nextInt(60));
        conclusoes.add(new BackupDTO.Conclusao(UUID.randomUUID(), atividade, dia, valor, quando));
    }

    private void foco(UUID atividade, LocalDate dia, int hora, int minutos) {
        Instant comeco = noDia(dia, hora, sorte.nextInt(15));
        sessoesFoco.add(new BackupDTO.SessaoFoco(UUID.randomUUID(), atividade, comeco, comeco.plus(Duration.ofMinutes(minutos))));
    }

    private void nota(UUID area, String titulo, String corpo, List<String> tags) {
        Instant quando = criadoEm.plus(Duration.ofDays(3 + sorte.nextInt(30)));
        notas.add(new BackupDTO.Nota(UUID.randomUUID(), area, titulo, corpo, tags, quando, quando));
    }

    private double progresso(LocalDate dia) {
        return (double) indice(dia) / (SEMANAS * 7);
    }

    private int indice(LocalDate dia) {
        return (int) ChronoUnit.DAYS.between(inicio, dia);
    }

    private static Instant noDia(LocalDate dia, int hora, int minuto) {
        return Datas.inicioDoDia(dia).plus(Duration.ofHours(hora).plusMinutes(minuto));
    }

    private static int limitar(int nivel) {
        return Math.max(1, Math.min(5, nivel));
    }

    private static BigDecimal decimal(double valor, int casas) {
        return BigDecimal.valueOf(valor).setScale(casas, RoundingMode.HALF_UP);
    }
}
