-- Temporizador de foco: cada sessão é um intervalo de trabalho numa atividade. Fim nulo é a sessão
-- em andamento, e só pode existir uma por vez.

create table sessao_foco (
    id uuid primary key default gen_random_uuid(),
    atividade_id uuid not null references atividade (id) on delete cascade,
    inicio timestamptz not null,
    fim timestamptz,
    constraint ck_sessao_foco_intervalo check (fim is null or fim >= inicio)
);

create index idx_sessao_foco_atividade_id on sessao_foco (atividade_id);
create index idx_sessao_foco_inicio on sessao_foco (inicio);

create unique index uq_sessao_foco_em_andamento on sessao_foco ((fim is null)) where fim is null;
