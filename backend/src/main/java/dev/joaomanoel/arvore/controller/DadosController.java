package dev.joaomanoel.arvore.controller;

import dev.joaomanoel.arvore.dto.BackupDTO;
import dev.joaomanoel.arvore.dto.ResumoBackupDTO;
import dev.joaomanoel.arvore.service.BackupService;
import dev.joaomanoel.arvore.util.Datas;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Os dados saindo e voltando: CSV das conclusões, backup completo e restauração. */
@RestController
@RequestMapping("/api/dados")
public class DadosController {

    private static final MediaType CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

    private final BackupService backupService;

    public DadosController(BackupService backupService) {
        this.backupService = backupService;
    }

    /** Sem datas, exporta tudo até hoje. */
    @GetMapping("/conclusoes.csv")
    public ResponseEntity<byte[]> conclusoesCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        byte[] csv = backupService.conclusoesCsv(inicio, fim).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(CSV)
                .header(HttpHeaders.CONTENT_DISPOSITION, anexo("arvore-conclusoes-" + Datas.hoje() + ".csv"))
                .body(csv);
    }

    @GetMapping("/backup")
    public ResponseEntity<BackupDTO> backup() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, anexo("arvore-backup-" + Datas.hoje() + ".json"))
                .body(backupService.exportar());
    }

    /** Substitui todos os dados pelos do backup. Backup inconsistente dá 400 e nada muda. */
    @PutMapping("/backup")
    public ResumoBackupDTO restaurar(@RequestBody BackupDTO backup) {
        return backupService.restaurar(backup);
    }

    private static String anexo(String nome) {
        return ContentDisposition.attachment().filename(nome).build().toString();
    }
}
