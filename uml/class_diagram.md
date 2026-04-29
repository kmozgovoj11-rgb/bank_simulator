@startuml BankSim_UML

skinparam class {
  BackgroundColor<<Enumeration>> #FFFFCC
  BackgroundColor<<Abstract>> #DDEEFF
  BackgroundColor<<Interface>> #EEFFEE
  BorderColor Black
}

' --- домен (слева направо: пользователь — клиент — счета — транзакции) ---

enum AccountStatus <<Enumeration>> {
  ACTIVE
  CLOSED
  FROZEN
}

enum TransactionStatus <<Enumeration>> {
  PENDING
  COMPLETED
  FAILED
}

class Customer {
  customerId : String
  fullName : String
  phone : String
  accounts : List<Account>
}

class User {
  login : String
  passwordHash : String
  customerId : String
}

abstract class Account <<Abstract>> {
  accountId : String
  number : String
  balance : BigDecimal
  currency : String
  status : AccountStatus
  owner : Customer
}

class DebitAccount
class SavingsAccount
class CreditAccount

abstract class Transaction <<Abstract>> {
  transactionId : String
  type : String
  amount : BigDecimal
  timestamp : Instant
  status : TransactionStatus
  description : String
}

class DepositTransaction
class WithdrawTransaction
class TransferTransaction
class StoredTransaction

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

Transaction <|-- DepositTransaction
Transaction <|-- WithdrawTransaction
Transaction <|-- TransferTransaction
Transaction <|-- StoredTransaction

' --- persistence: явная связь репозиторий → класс домена ---

interface AccountRepository <<Interface>>
interface UserRepository <<Interface>>
interface CustomerRepository <<Interface>>
interface TransactionRepository <<Interface>>
interface TransactionBroker <<Interface>>

class SqlAccountRepository
class SqlUserRepository
class SqlCustomerRepository
class SqlTransactionRepository
class SqliteTransactionBroker
class Database

SqlAccountRepository ..|> AccountRepository
SqlUserRepository ..|> UserRepository
SqlCustomerRepository ..|> CustomerRepository
SqlTransactionRepository ..|> TransactionRepository
SqliteTransactionBroker ..|> TransactionBroker

SqlAccountRepository --> Database
SqlUserRepository --> Database
SqlCustomerRepository --> Database
SqlTransactionRepository --> Database
SqliteTransactionBroker --> Database

SqlUserRepository ..> User : хранит / загружает
SqlAccountRepository ..> Account : хранит / загружает
SqlCustomerRepository ..> Customer : хранит / загружает
SqlTransactionRepository ..> Transaction : хранит / загружает

' --- сервисы ---

class BankService
class AuthService

BankService ..> AccountRepository
BankService ..> CustomerRepository
BankService ..> TransactionRepository
BankService ..> TransactionBroker

AuthService ..> UserRepository

@enduml
