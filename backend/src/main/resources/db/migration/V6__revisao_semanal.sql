-- Revisão semanal: uma nota por semana, identificada pela segunda-feira. O retrato da semana
-- (cumprido, o que escapou) é calculado na hora a partir das conclusões, então não é guardado.

create table revisao_semanal (
    id uuid primary key default gen_random_uuid(),
    semana date not null unique,
    nota text not null,
    criado_em timestamptz not null default now(),
    atualizado_em timestamptz not null default now(),
    constraint ck_revisao_semanal_segunda check (extract(isodow from semana) = 1)
);
