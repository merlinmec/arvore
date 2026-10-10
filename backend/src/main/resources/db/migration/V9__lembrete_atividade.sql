-- Lembrete opcional por atividade: um horário do dia em que o app avisa, se a atividade estiver
-- planejada pra hoje e ainda não tiver sido feita.

alter table atividade add column lembrete time;
