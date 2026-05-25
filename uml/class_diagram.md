@startuml
skinparam class {
    BackgroundColor<<domain>>      #FFFFCC
    BackgroundColor<<persistence>> #DDEEFF
    BackgroundColor<<service>>     #EEFFEE
    BorderColor Black
}

' --- домен (слева направо: пользователь — клиент — счета — транзакции) ---
enum AccountStatus <<domain>> {
    ACTIVE
    CLOSED
    FROZEN
}

enum TransactionStatus <<domain>> {
    PENDING
    COMPLETED
    FAILED
}

class Customer <<domain>> {
    customerId : String
    fullName   : String
    phone      : String
    accounts   : List<Account>
}

class User <<domain>> {
    login        : String
    passwordHash : String
    customerId   : String
}

abstract class Account <<domain>> {
    accountId : String
    number    : String
    balance   : BigDecimal
    currency  : String
    status    : AccountStatus
    owner     : Customer
}

class DebitAccount   <<domain>>
class SavingsAccount <<domain>>
class CreditAccount  <<domain>>

' --- Command pattern: Transaction знает execute()/rollback() ---
interface Command <<domain>> {
    execute()
    rollback()
}

abstract class Transaction <<domain>> {
    transactionId : String
    type          : String
    amount        : BigDecimal
    timestamp     : Instant
    status        : TransactionStatus
    description   : String
}

class DepositTransaction         <<domain>>
class WithdrawTransaction        <<domain>>
class TransferTransaction        <<domain>>
class InterestAccrualTransaction <<domain>>
class StoredTransaction          <<domain>>

' --- Factory Method: AccountFactory создаёт нужный подкласс Account ---
class AccountFactory <<domain>> {
    +createAccount(kind, spec) : Account
}

' --- 1:1 User и Customer (один логин — один клиент) ---
User "1" -- "1" Customer : по customerId

Customer "1" o-- "0..*" Account : содержит
AccountStatus -- Account
User ..> Customer : ссылается на

Account <|-- DebitAccount
Account <|-- SavingsAccount
Account <|-- CreditAccount

TransactionStatus -- Transaction
Account ..> Transaction : участвует в

Transaction ..|> Command
Transaction <|-- DepositTransaction
Transaction <|-- WithdrawTransaction
Transaction <|-- TransferTransaction
Transaction <|-- InterestAccrualTransaction
Transaction <|-- StoredTransaction

AccountFactory ..> Account : <<creates>>

' --- persistence: явная связь репозиторий → класс домена ---
interface AccountRepository     <<persistence>>
interface UserRepository        <<persistence>>
interface CustomerRepository    <<persistence>>
interface TransactionRepository <<persistence>>
interface TransactionBroker     <<persistence>>

class SqlAccountRepository      <<persistence>>
class SqlUserRepository         <<persistence>>
class SqlCustomerRepository     <<persistence>>
class SqlTransactionRepository  <<persistence>>
class SqliteTransactionBroker   <<persistence>>
class Database                  <<persistence>>

SqlAccountRepository     ..|> AccountRepository
SqlUserRepository        ..|> UserRepository
SqlCustomerRepository    ..|> CustomerRepository
SqlTransactionRepository ..|> TransactionRepository
SqliteTransactionBroker  ..|> TransactionBroker

SqlAccountRepository     --> Database
SqlUserRepository        --> Database
SqlCustomerRepository    --> Database
SqlTransactionRepository --> Database
SqliteTransactionBroker  --> Database

SqlUserRepository        ..> User        : хранит / загружает
SqlAccountRepository     ..> Account     : хранит / загружает
SqlAccountRepository     ..> AccountFactory : создаёт по account_kind
SqlCustomerRepository    ..> Customer    : хранит / загружает
SqlTransactionRepository ..> Transaction : хранит / загружает

' --- сервисы ---
' Facade pattern: BankFacade прячет работу с репозиториями и БД-транзакциями.
interface BankFacade <<service>>

class BankService <<service>>
class AuthService <<service>>

BankService ..|> BankFacade
BankService ..> AccountRepository
BankService ..> CustomerRepository
BankService ..> TransactionRepository
BankService ..> TransactionBroker

AuthService ..> UserRepository
@enduml
