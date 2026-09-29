package dev.joaomanoel.arvore.dto;

import java.time.LocalDate;
import java.util.List;

/** O plano de um dia. {@code editavel} é falso pra datas futuras, que não aceitam conclusão. */
public record DiaHojeDTO(LocalDate data, LocalDate hoje, boolean editavel, List<ItemHojeDTO> itens) {}
