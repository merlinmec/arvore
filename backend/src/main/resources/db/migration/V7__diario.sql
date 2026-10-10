-- Diário do dia: humor, energia (1 a 5) e uma nota livre por data. Ao menos um dos três é
-- preenchido; apagar tudo apaga a linha.

create table diario (
    id uuid primary key default gen_random_uuid(),
    data date not null unique,
    humor smallint check (humor between 1 and 5),
    energia smallint check (energia between 1 and 5),
    nota text,
    criado_em timestamptz not null default now(),
    atualizado_em timestamptz not null default now(),
    constraint ck_diario_preenchido check (humor is not null or energia is not null or nota is not null)
);
