-- Hábito negativo ("não fumar"): vale todo dia, sem grade, e cada registro em conclusao é uma
-- recaída. O tipo é escolhido na criação e não muda, pra não reinterpretar o histórico.

alter table atividade
    add column negativa boolean not null default false;
