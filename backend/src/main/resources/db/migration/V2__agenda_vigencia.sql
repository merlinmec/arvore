-- A grade semanal passa a ter histórico. Cada linha vale de vigente_de até o dia anterior a
-- vigente_ate (null = em vigor). Mudar a grade encerra ou abre linhas em vez de apagar, então o
-- "planejado" dos dias passados deixa de mudar junto.

alter table agenda_semanal
    add column vigente_de date,
    add column vigente_ate date;

-- Linhas existentes valem desde o dia (no fuso do app) em que foram agendadas.
update agenda_semanal
set vigente_de = (criado_em at time zone 'America/Sao_Paulo')::date;

alter table agenda_semanal
    alter column vigente_de set not null,
    add constraint ck_agenda_semanal_vigencia check (vigente_ate is null or vigente_ate > vigente_de),
    drop constraint agenda_semanal_atividade_id_dia_semana_key;

-- No máximo uma linha em vigor por atividade e dia; as encerradas podem se repetir.
create unique index uq_agenda_semanal_em_vigor
    on agenda_semanal (atividade_id, dia_semana)
    where vigente_ate is null;
