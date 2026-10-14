package dev.joaomanoel.arvore.demo;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DelegatingDataSource;

/**
 * Aponta cada conexão emprestada do pool pro schema do visitante da requisição (o search_path do
 * Postgres), ou pro {@code public} quando não há visitante. Define sempre, nos dois casos: o pool
 * devolve conexões usadas, e uma que serviu a um visitante não pode chegar com o schema dele a outro.
 */
class DataSourceDoVisitante extends DelegatingDataSource {

    static final String ESQUEMA_PADRAO = "public";

    DataSourceDoVisitante(DataSource original) {
        super(original);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return apontar(super.getConnection());
    }

    @Override
    public Connection getConnection(String usuario, String senha) throws SQLException {
        return apontar(super.getConnection(usuario, senha));
    }

    private static Connection apontar(Connection conexao) throws SQLException {
        try {
            conexao.setSchema(EsquemaDoVisitante.atual().orElse(ESQUEMA_PADRAO));
            return conexao;
        } catch (SQLException erro) {
            conexao.close();
            throw erro;
        }
    }
}
