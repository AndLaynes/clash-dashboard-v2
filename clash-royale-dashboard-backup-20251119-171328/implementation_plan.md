# Plano de Implementação - Dashboard Clash Royale (Python + Static Site)

## Objetivo
Criar um script Python que busca dados da API do Clash Royale e gera um dashboard HTML estático e moderno. O usuário rodará o script localmente e subirá o HTML gerado para um repositório online.

## Arquitetura
1.  **Script Python (`main.py`)**:
    *   Lê a API Key das variáveis de ambiente (`CR_API_KEY`).
    *   Define o Clan Tag (configurável).
    *   Faz requisições para a API do Clash Royale:
        *   `/clans/{clanTag}` (Info do Clã e Membros)
        *   `/clans/{clanTag}/currentriverrace` (Guerra Atual)
        *   `/clans/{clanTag}/riverracelog` (Histórico de Guerras)
    *   Processa os dados.
    *   Renderiza um template HTML usando `jinja2`.
    *   Salva o resultado como `index.html`.

2.  **Template (`templates/index.html`)**:
    *   HTML5 + CSS3 (Vanilla).
    *   Design responsivo e temático (Clash Royale).
    *   Uso de Jinja2 para iterar sobre membros e guerras.

3.  **Estilos (`static/style.css`)**:
    *   Variáveis CSS para cores e fontes.
    *   Layout Grid/Flexbox.
    *   Animações sutis.

## Estrutura de Arquivos
- `main.py`: Script principal.
- `requirements.txt`: Dependências (`requests`, `jinja2`).
- `templates/index.html`: Template Jinja2.
- `static/style.css`: Folha de estilos (será embutida ou linkada, preferencialmente embutida para arquivo único se desejado, mas linkada é melhor para organização. O script pode ler o CSS e injetar no HTML para um único arquivo final se o usuário preferir, mas padrão web é melhor). *Decisão: Vamos gerar um `index.html` que referencia `style.css` e `assets` se necessário, ou tentar fazer tudo inline se o usuário quiser portabilidade total. Para "repositório online", arquivos separados são ok.*

## Verificação
- Configurar variável de ambiente `CR_API_KEY` com uma chave de teste (ou mock se não tivermos uma real).
- Rodar `python main.py`.
- Abrir `index.html` no navegador e verificar se os dados aparecem e o layout está correto.

## Funcionalidade: Histórico de Guerra
### Nova Página: `war_history.html`
- Tabela detalhada da última guerra concluída.
- Colunas:
    1. **#** (Índice)
    2. **Status** (Classificação baseada em decks usados)
    3. **Jogador** (Nome)
    4. **Decks Usados** (x/16)
    5. **Fama** (Pontos)
- **Lógica de Classificação**:
    - 16 Decks: **Campeão** (Verde)
    - 12-15 Decks: **Atenção** (Amarelo)
    - < 12 Decks: **Perigo** (Vermelho)
- **Interatividade**:
    - Script JS simples para ordenar colunas (clique no cabeçalho).
- **Navegação**:
    - Menu no topo para alternar entre "Visão Geral" e "Histórico de Guerra".
