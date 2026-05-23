# Corretora Back-end

API REST de uma corretora de investimentos simulada, construída com **Spring Boot 4** e **Java 17**. O sistema permite que usuários se cadastrem, depositem saldo, consultem cotações do mercado de ações brasileiro em tempo real, comprem e vendam ativos, e acompanhem a evolução de sua carteira.

## Funcionalidades

- **Autenticação e usuários** — cadastro, login e gerenciamento de perfil com autenticação via JWT.
- **Carteira (Wallet)** — depósito, saque e confirmação de depósitos, com histórico de transações.
- **Mercado** — cotações de ações, dados históricos, listagem de ativos, índices de inflação e taxa de juros, consumindo a [API da Brapi](https://brapi.dev).
- **Portfólio** — compra e venda de ativos, posições consolidadas e histórico de evolução da carteira.
- **Pagamentos** — integração com **Stripe** para depósitos via cartão (Payment Intents e webhooks).
- **Administração** — endpoints administrativos para listagem de usuários, consulta de carteiras e gerenciamento de papéis (roles).
- **Atualização automática de preços** — scheduler que mantém os preços das posições atualizados.

## Tecnologias

- Java 17
- Spring Boot 4.0.3 (Web MVC, Data JPA, Security, Validation)
- MySQL
- JWT (`io.jsonwebtoken` / jjwt 0.12.6)
- Stripe Java SDK 25.3.0
- Lombok
- Maven (com Maven Wrapper)

## Estrutura do projeto

```
src/main/java/br/com/meuapp/corretorabackend/
├── config        # SecurityConfig, StripeConfig, BrapiConfig, GlobalExceptionHandler
├── controller    # Endpoints REST (Auth, Market, Portfolio, Wallet, Stripe, Webhook, Admin)
├── dto           # Objetos de requisição e resposta
├── model         # Entidades JPA (User, Wallet, Asset, Position, Trade, etc.)
├── repository    # Repositórios Spring Data JPA
├── security      # JwtService, JwtAuthFilter, UserDetailsServiceImpl
└── service       # Regras de negócio (Auth, Portfolio, Wallet, Brapi, Stripe, Admin)
```

## Pré-requisitos

- JDK 17 ou superior
- MySQL em execução
- Conta na [Brapi](https://brapi.dev) (token de API)
- Conta na [Stripe](https://stripe.com) (chaves de API)

## Configuração

O arquivo `src/main/resources/application.properties` não é versionado (está no `.gitignore`). Crie-o localmente com as configurações abaixo:

```properties
# Banco de dados
spring.datasource.url=jdbc:mysql://localhost:3306/corretora
spring.datasource.username=seu_usuario
spring.datasource.password=sua_senha
spring.jpa.hibernate.ddl-auto=update

# JWT
jwt.secret=sua_chave_secreta
jwt.expiration=86400000

# Brapi
brapi.api.base-url=https://brapi.dev/api
brapi.api.token=seu_token_brapi

# Stripe
stripe.public-key=sua_chave_publica
stripe.secret-key=sua_chave_secreta
stripe.webhook-secret=seu_webhook_secret
```

## Como executar

```bash
# Clonar o repositório
git clone https://github.com/guilhermefrauches/Corretora-back-end.git
cd Corretora-back-end

# Executar a aplicação
./mvnw spring-boot:run
```

A API ficará disponível em `http://localhost:8080`.

## Endpoints principais

### Autenticação — `/api/auth`
| Método | Rota | Descrição |
|--------|------|-----------|
| POST | `/register` | Cadastra um novo usuário |
| POST | `/login` | Autentica e retorna um token JWT |
| GET | `/me` | Retorna os dados do usuário autenticado |
| PUT | `/me` | Atualiza o perfil do usuário |

### Carteira — `/api/wallet`
| Método | Rota | Descrição |
|--------|------|-----------|
| GET | `/` | Consulta o saldo da carteira |
| POST | `/deposit` | Solicita um depósito |
| POST | `/confirm-deposit` | Confirma um depósito |
| POST | `/withdraw` | Realiza um saque |

### Mercado — `/api/market`
| Método | Rota | Descrição |
|--------|------|-----------|
| GET | `/quote/{tickers}` | Cotação de um ou mais ativos |
| GET | `/quote/{tickers}/history` | Histórico de preços de um ativo |
| GET | `/stocks` | Lista de ações disponíveis |
| GET | `/inflation` | Índice de inflação |
| GET | `/prime-rate` | Taxa de juros |

### Portfólio — `/api/portfolio`
| Método | Rota | Descrição |
|--------|------|-----------|
| GET | `/` | Posições consolidadas da carteira |
| POST | `/buy` | Compra de ativos |
| POST | `/sell` | Venda de ativos |
| GET | `/history` | Histórico de evolução da carteira |

### Pagamentos — `/api/stripe`
| Método | Rota | Descrição |
|--------|------|-----------|
| POST | `/create-payment-intent` | Cria uma intenção de pagamento |
| POST | `/webhook` | Recebe eventos do Stripe |

### Administração — `/api/admin`
| Método | Rota | Descrição |
|--------|------|-----------|
| GET | `/users` | Lista todos os usuários |
| GET | `/users/{id}/wallet` | Consulta a carteira de um usuário |
| PUT | `/users/{id}/role` | Altera o papel (role) de um usuário |

> As rotas, exceto `/api/auth/register` e `/api/auth/login`, exigem o header `Authorization: Bearer <token>`.

## Autor

Desenvolvido por [Guilherme Frauches](https://github.com/guilhermefrauches).
