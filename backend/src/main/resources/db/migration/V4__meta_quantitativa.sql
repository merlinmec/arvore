-- Meta quantitativa no objetivo ("correr 100 km") e valor opcional em cada conclusão ("5 km").
-- O acumulado do objetivo é a soma dos valores das conclusões das suas atividades.

alter table objetivo
    add column meta_valor numeric(12, 2),
    add column meta_unidade varchar(20),
    add constraint ck_objetivo_meta check (
        (meta_valor is null and meta_unidade is null)
        or (meta_valor > 0 and meta_unidade is not null)
    );

alter table conclusao
    add column valor numeric(12, 2),
    add constraint ck_conclusao_valor check (valor is null or valor >= 0);
