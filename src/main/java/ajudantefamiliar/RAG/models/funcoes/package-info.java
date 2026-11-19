/**
 * Tabelas da API de funções, mapeadas SOMENTE PARA LEITURA ({@code @Immutable}): o RAG nunca grava nelas.
 * São a fonte da linha do tempo do idoso usada no chat e nos relatórios (busca por idoso e período).
 * <p>
 * Mudou uma tabela na API de funções (nova migration M_AJ_V)? Atualize a entidade correspondente aqui.
 * Colunas com valores de banco (tipo de refeição, status da consulta) ficam como texto, do jeito que estão
 * gravadas, porque vão direto para o contexto da IA.
 */
package ajudantefamiliar.RAG.models.funcoes;
