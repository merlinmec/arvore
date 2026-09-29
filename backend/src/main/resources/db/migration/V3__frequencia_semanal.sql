-- Modo "N vezes por semana, em qualquer dia", alternativo aos dias fixos de agenda_semanal.
-- Mesma vigência da grade: cada linha vale de vigente_de até o dia anterior a vigente_ate
-- (null = em vigor), e mudar a meta não reescreve o passado.

create table frequencia_semanal (
    id uuid primary key default gen_random_uuid(),
    atividade_id uuid not null references atividade (id) on delete cascade,
    vezes smallint not null check (vezes between 1 and 7),
    vigente_de date not null,
    vigente_ate date,
    criado_em timestamptz not null default now(),
    constraint ck_frequencia_semanal_vigencia check (vigente_ate is null or vigente_ate > vigente_de)
);

create index idx_frequencia_semanal_atividade_id on frequencia_semanal (atividade_id);

-- No máximo uma meta em vigor por atividade.
create unique index uq_frequencia_semanal_em_vigor
    on frequencia_semanal (atividade_id)
    where vigente_ate is null;
