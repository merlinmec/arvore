create extension if not exists "pgcrypto";

create table area (
    id uuid primary key default gen_random_uuid(),
    area_pai_id uuid references area (id) on delete cascade,
    nome varchar(120) not null,
    descricao_geral text,
    cor_tema varchar(20),
    wallpaper_url varchar(500),
    ordem int not null default 0,
    criado_em timestamptz not null default now(),
    atualizado_em timestamptz not null default now()
);

create index idx_area_area_pai_id on area (area_pai_id);

create table tag (
    id uuid primary key default gen_random_uuid(),
    nome varchar(60) not null unique
);

create table nota (
    id uuid primary key default gen_random_uuid(),
    area_id uuid not null references area (id) on delete cascade,
    titulo varchar(200) not null,
    corpo text not null default '',
    criado_em timestamptz not null default now(),
    atualizado_em timestamptz not null default now()
);

create index idx_nota_area_id on nota (area_id);

create table nota_tag (
    nota_id uuid not null references nota (id) on delete cascade,
    tag_id uuid not null references tag (id) on delete cascade,
    primary key (nota_id, tag_id)
);

create table objetivo (
    id uuid primary key default gen_random_uuid(),
    area_id uuid not null references area (id) on delete cascade,
    descricao text not null,
    prazo date,
    status varchar(20) not null default 'ATIVO',
    criado_em timestamptz not null default now(),
    atualizado_em timestamptz not null default now()
);

create index idx_objetivo_area_id on objetivo (area_id);

create table atividade (
    id uuid primary key default gen_random_uuid(),
    objetivo_id uuid not null references objetivo (id) on delete cascade,
    nome varchar(150) not null,
    criado_em timestamptz not null default now(),
    atualizado_em timestamptz not null default now()
);

create index idx_atividade_objetivo_id on atividade (objetivo_id);

create table agenda_semanal (
    id uuid primary key default gen_random_uuid(),
    atividade_id uuid not null references atividade (id) on delete cascade,
    dia_semana varchar(10) not null,
    criado_em timestamptz not null default now(),
    unique (atividade_id, dia_semana)
);

create index idx_agenda_semanal_atividade_id on agenda_semanal (atividade_id);

create table conclusao (
    id uuid primary key default gen_random_uuid(),
    atividade_id uuid not null references atividade (id) on delete cascade,
    data date not null,
    criado_em timestamptz not null default now(),
    unique (atividade_id, data)
);

create index idx_conclusao_atividade_id on conclusao (atividade_id);
