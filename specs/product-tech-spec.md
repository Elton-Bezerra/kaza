# Specification & Architecture Document

## SaaS de Gestão Financeira e Operacional para Microcondomínios

  

---

  

# SEÇÃO I: VISÃO DO NEGÓCIO & MODELO FINANCEIRO

  

## 1. Tese de Mercado & Posicionamento

Em grandes centros urbanos como São Paulo, existe uma massa negligenciada de **microcondomínios (4 a 20 unidades)**, vilas e predinhos baixos.

  

* **O Problema:** As administradoras tradicionais cobram taxas mínimas elevadas (R$ 800 a R$ 1.500/mês), inviabilizando a contratação. A gestão é feita de forma amadora por "síndicos moradores", gerando inadimplência, falta de transparência e atritos.

* **A Solução:** Um SaaS *mobile-first* enxuto que automatiza a cobrança por fração ideal, o pagamento de despesas e a prestação de contas, sem encarecer a taxa condominial.

  

---

  

## 2. Modelo Financeiro & Unit Economics

  

### 2.1 Estratégia de Precificação (Pricing Strategy)

Para não inviabilizar a operação dos condôminos, o modelo **elimina a cobrança de taxas/markups por boleto individual**.

  

A receita é baseada **exclusivamente na Assinatura Recorrente por Unidade/Mês (SaaS Subscription)**:

* **Preço por Unidade:** **R$ 6,00 a R$ 8,00 / unidade / mês**.

* *Exemplo (Condomínio de 10 unidades a R$ 7,00/unidade):* **R$ 70,00/mês total para o condomínio**.

* O custo transacional do parceiro de Banking as a Service (Asaas) para liquidação de PIX/Boleto (~R$ 1,99) é absorvido como custo variável da operação do SaaS.

  

### 2.2 Estrutura de Custos Estimada (CNPJ Exclusivo no Simples Nacional)

  

#### Custos Fixos Mensais (Fase Inicial / MVP)

| Categoria | Descrição | Valor Estimado (R$) |

| :--- | :--- | :--- |

| **Contabilidade** | Contabilidade online para CNPJ de TI (Simples Nacional) | R$ 150,00 |

| **Infraestrutura Cloud** | Servidor Backend, Banco de Dados, DNS, SSL | R$ 120,00 |

| **Messaging / API** | API de mensagens do WhatsApp/E-mail (Lembretes) | R$ 100,00 |

| **SaaS & Licenças** | E-mail corporativo e ferramentas de dev | R$ 80,00 |

| **Custos Bancários** | Conta PJ + Amortização de Certificado Digital | R$ 50,00 |

| **Pró-Labore Mínimo** | Base de recolhimento de INSS/Fator R | R$ 1.412,00 |

| **Total Custo Fixo Mensal** | | **R$ 1.912,00 / mês** |

  

#### Custos Variáveis por Condomínio

* **Taxas de BaaS (Asaas):** ~R$ 1,99 por PIX/Boleto pago.

* **Impostos (Simples Nacional - Anexo III):** ~6% sobre o faturamento bruto.

  

### 2.3 Cálculo do Ponto de Equilíbrio (Break-Even)

Considerando um condomínio médio de **10 unidades** pagando R$ 70,00/mês:

* **Receita Bruta por Condomínio (ARPU):** R$ 70,00

* **(-) Impostos (6%):** R$ 4,20

* **(-) Custos de Transação BaaS (10 x R$ 1,99):** R$ 19,90

* **(=) Margem de Contribuição Líquida por Condomínio:** **R$ 45,90 / mês**

  

#### Ponto de Equilíbrio Operacional (Cobrir R$ 500,00 de infraestrutura e contabilidade)

$$\text{Break-Even Operacional} = \frac{\text{R\$ 500,00}}{\text{R\$ 45,90}} \approx \mathbf{11 \text{ condomínios (~110 unidades)}}$$

  

#### Ponto de Equilíbrio Completo (Cobrir Custo Fixo Total + Pró-Labore de R$ 1.912,00)

$$\text{Break-Even Completo} = \frac{\text{R\$ 1.912,00}}{\text{R\$ 45,90}} \approx \mathbf{42 \text{ condomínios (~420 unidades)}}$$

  

---

  

# SEÇÃO II: ESPECIFICAÇÃO DE PRODUTO

  

## 1. Mapeamento de Personas

  
  

```

  

+---------------------------------------------------------------------------------------+

| 1. SÍNDICO MORADOR (ADMIN) |

| - Perfil: Morador eleito ou voluntário para gerir a vila/prédio de 4 a 20 unidades. |

| - Dores: Falta de tempo, cobrança chata de vizinhos, uso indesejado de conta pessoal. |

| - Objetivos: Emitir cobranças rápido, pagar contas operacionais e prestar contas. |

+---------------------------------------------------------------------------------------+

| 2. CONDÔMINO / MORADOR (USER) |

| - Perfil: Proprietário ou inquilino da unidade. |

| - Dores: Falta de transparência nas despesas, boletos perdidos. |

| - Objetivos: Pagar o condomínio via PIX em segundos e acompanhar os gastos. |

+---------------------------------------------------------------------------------------+

| 3. ADMIN DA PLATAFORMA (SUPERADMIN) |

| - Perfil: Operador do SaaS (Você). |

| - Objetivos: Acompanhar subcontas BaaS, métricas de churn e saúde da infraestrutura. |

+---------------------------------------------------------------------------------------+

  

```

  

---

  

## 2. Matriz de Funcionalidades: MVP (Fase 1) vs. Long-Term (Fase 2+)

  

| Módulo / Recurso | MVP (Fase 1 - Tração & Validação) | Long-Term (Fase 2+ - Automação & Escala) |

| :--- | :--- | :--- |

| **Onboarding do Condomínio** | Cadastro manual de CPF/CNPJ com abertura de subconta no BaaS via API. | Onboarding 100% self-service com envio automático de documentos para KYC. |

| **Experiência do Morador** | **Sem app obrigatório.** Recebe cobrança com PIX Copia-e-Cola no WhatsApp/E-mail. | **Área Logada PWA/Mobile.** Histórico financeiro, 2ª via e extrato transparente. |

| **Emissão de Cobranças** | Rateio simples (Fração Ideal ou Igualitário). Disparo mensal via painel. | Régua de cobrança inteligente com inteligência de renegociação automática. |

| **Entrada de Despesas** | **Entrada Manual:** Upload de fotos de recibos ou leitura de código de barras pela câmera. | **Entrada Passiva/Autônoma:** Captura via DDA bancário automático, parsing de NF por e-mail e OCR/IA. |

| **Aprovação e Pagamento** | Inserção de PIN de segurança pelo síndico no app para autorizar pagamento de boletos. | **Smart Approvals:** Aprovação em 1 clique via Push/WhatsApp e regras automáticas para contas recorrentes. |

| **Comunicação e Governança** | Lembretes automatizados no WhatsApp (Twilio / Z-API) e enquetes simples. | Mural de avisos, chamados/ocorrências e Assembleia Virtual com validade jurídica (Lei 14.309/22). |

  

---

  

## 3. Fluxos de Processo

  

### 3.1 Arquitetura do MVP (Monólito Modular Java + Frontend/Mobile Desacoplados)

  

+--------------------------------------------------+

| DISPOSITIVOS / CLIENTS |

| [ App Flutter: Android/iOS/macOS ] [ Web ] |

+------------------------+-------------------------+

|

HTTPS / TLS 1.3 (REST API)

|

+------------------------v-------------------------+

| BACKEND JAVA (Spring Boot) |

| - Spring Security (JWT / RBAC / WebAuthn) |

| - Spring Data JPA (Multi-Tenancy RLS) |

| - RestClient / Feign (Integração Asaas) |

+----+-------------------+--------------------+----+

| | |

SQL Query | | Event / Queue | Webhook

v v v

+------------------------+ +-------------------+ +--------------------+

| BANCO DE DADOS RELAC. | | API ASAAS (BaaS) | | EVENT BROKER |

| (PostgreSQL) | | Subcontas / PIX / | | (RabbitMQ / Kafka) |

| - Tenant Isolation | | Pagamentos / DDA | | Processamento Webhk|

+------------------------+ +-------------------+ +--------------------+

  

### 3.2 Fluxo Autônomo de Entrada e Pagamento de Despesas (Long-Term)

  
  

```

  

[Concessionária / Fornecedor]

│ (Emite Boleto no CNPJ/CPF do Condomínio ou envia E-mail com NF)

▼

[SaaS: Ingestion Engine] ────> [Captura DDA no Asaas / Reading OCR via IA]

│

▼

[Pré-lançamento no Extrato do SaaS]

│

▼

[Notificação Push/WhatsApp para o Síndico]

"Nova conta Sabesp (R$ 142,50). [Aprovar com 1-Tap]"

│

▼

[Síndico aprova via Biometria / PIN]

│

▼

[API Asaas efetua a liquidação e salva PDF no Extrato]

  

```

  

---

  

# SEÇÃO III: ARQUITETURA TÉCNICA & SEGURANÇA

  

## 1. Modelo de Responsabilidade Compartilhada (Segurança & BaaS)

  
  

```

  

+-----------------------------------------------------------------------+

| CAMADA DE APLICAÇÃO |

| - Autenticação de Usuários, MFA e Sessões |

| - Parsing de Documentos (OCR/IA) e Validação de Uploads | ==> RESPONSABILIDADE

| - Gestão Segura de API Keys (Server-to-Server) | DO SEU SAAS

| - Regras de Negócio e Permissões de Acesso (RBAC) |

+-----------------------------------------------------------------------+

| CAMADA DE INFRAESTRUTURA |

| - Criptografia em Trânsito (TLS 1.3) e em Repouso (AES-256) | ==> RESPONSABILIDADE

| - Conformidade com a LGPD (Gestão de Dados Pessoais) | COMPARTILHADA

+-----------------------------------------------------------------------+

| CAMADA FINANCEIRA E BAAS |

| - Regulação perante o Banco Central do Brasil |

| - Custódia de Valores e Liquidação do PIX/Boleto/DDA | ==> RESPONSABILIDADE

| - Processamento Bancário e Onboarding KYC | DO ASAAS

+-----------------------------------------------------------------------+

  

```

  

---

  

## 2. Autenticação, Autorização e Segurança

  

* **Autenticação:** JSON Web Tokens (JWT) com *Refresh Tokens* em cookies HTTP-Only e `SameSite=Strict`.

* **Segurança de Ações Críticas (Aprovação de Pagamentos):** Exigência de PIN numérico ou Biometria (WebAuthn).

* **Controle de Acesso (RBAC):**

* `SUPER_ADMIN`: Acesso global de suporte e métricas.

* `SINDICO`: Gestão do condomínio, emissão de cobranças e liquidação de contas.

* `MORADOR`: Leitura de extratos, avisos e cobranças da sua unidade.

  

---

  

## 3. Arquitetura de Solução

  

### 3.1 Arquitetura do MVP (Monólito Modular Enxuto)

  
  

```

  

```

+--------------------------------------------------+

| DISPOSITIVOS / CLIENTS |

| [ App Mobile / PWA Síndico ] [ Web Morador ] |

+------------------------+-------------------------+

|

HTTPS / TLS 1.3

|

+------------------------v-------------------------+

| API GATEWAY / BACKEND |

| - Auth Middleware (JWT / RBAC) |

| - Rate Limiting |

| - Controllers & Business Logic |

+----+-------------------+--------------------+----+

| | |

SQL Query | | REST / API | Webhook

v v v

  

```

  

+------------------------+ +-------------------+ +--------------------+

| BANCO DE DADOS RELAC. | | API ASAAS (BaaS) | | WORKER DE FILAS |

| (PostgreSQL / Supabase)| | Subcontas / PIX / | | (Redis + BullMQ) |

| - Isolation por Tenant | | Pagamento / DDA | | Disparo WhatsApp |

+------------------------+ +-------------------+ +--------------------+

  

```

  

---

  

## 4. Diagrama de Componentes (MVP ao Long-Term Pipeline)

  
  

```

  

+-----------------------------------------------------------------------------------+

| CONTROLLER LAYER |

| - AuthController: /api/v1/auth (Login, Refresh, PIN) |

| - BillingController: /api/v1/condos/:id/charge (Geração de cobranças) |

| - PayableController: /api/v1/condos/:id/pay (Leitura de código de barras e pgto) |

| - WebhookController: /api/v1/webhooks/asaas (Recepção de notificações) |

+-----------------------------------------+-----------------------------------------+

|

v

+-----------------------------------------------------------------------------------+

| SERVICE LAYER (Regras de Negócio) |

| - LedgerService: Controle de caixa e cálculo por fração ideal. |

| - BaaSService: Abstração das chamadas HTTP para a API do Asaas. |

| - ExpenseIngestionWorker (Long-Term): DDA e Parser OCR de notas/boletos por IA. |

| - NotificationService: Fila de mensagens para envio via WhatsApp. |

+-----------------------------------------+-----------------------------------------+

|

v

+-----------------------------------------------------------------------------------+

| DATA ACCESS LAYER (ORM / Database) |

| - Multi-tenant Strategy: Schema-based ou Row-Level Security (RLS) por tenant. |

| - Entities: Condominium, Unit, User, Invoice, Expense, AuditLog. |

+-----------------------------------------------------------------------------------+

  

```

  

---

  

# SEÇÃO IV: ALERTAS DE ARQUITETURA, JURÍDICO E FINANCEIRO

  

> [!NOTE]

> **AI ALERT — Considerações Estratégicas:**

> As recomendações abaixo foram estruturadas com base em boas práticas de mercado para validação prévia do fundador.

  

1. **Isolamento por Tenant (Multi-tenancy):**

Garantir a utilização de **Row-Level Security (RLS)** nativo no PostgreSQL ou assegurar que todas as queries de banco contenham obrigatoriamente o filtro `WHERE condominium_id = :tenantId`, impedindo o vazamento de dados ou saldos entre condomínios diferentes.

  

2. **Garantia de Isenção da Plataforma nos Termos de Uso:**

Os Termos de Uso do SaaS devem explicitar que a verificação do favorecido e dos dados do boleto é de responsabilidade exclusiva do usuário aprovador (síndico). O SaaS provê a infraestrutura de tecnologia e integração, sem realizar auditoria de mérito fiscal/financeiro sobre as contas aprovadas.

  

3. **Arquitetura Resiliente para Webhooks:**

Notificações bancárias do Asaas devem ser recebidas por um endpoint leve, enfileiradas imediatamente em um broker de mensagens (ex: Redis com BullMQ) e processadas de forma assíncrona, evitando perda de dados por oscilação de rede.

  

```