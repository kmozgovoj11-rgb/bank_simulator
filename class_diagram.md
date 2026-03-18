@startuml BankSim_UML

skinparam class {
  BackgroundColor<<abstract>> LightBlue
  BackgroundColor<<interface>> LightGreen
  BorderColor Black
}

enum AccountStatus {
  ACTIVE
  CLOSED
  FROZEN
}

class Customer {
  - customer_id: String
  - fullName: String
  - phone: String
  - status: AccountStatus
  - accounts: List<Account>
  + add_account()
  + remove_account()
  + get_accounts()
}

class User {
  - login: String
  - password_hash: String
  - customer_id: String
  + check_password()
  + get_customer()
}

abstract class Account <<abstract>> {
  - account_id: String
  - number: String
  - balance: Decimal
  - currency: String
  - status: AccountStatus
  - owner: Customer
  + deposit()
  + withdraw()
  + get_balance()
  + close()
}

class DebitAccount {
  - daily_limit: Money
  + check_limit()
}

class SavingsAccount {
  - interest_rate: float
  + calculate_interest()
}

class CreditAccount {
  - credit_limit: Money
  - current_debt: Money
  + repay_debt()
}

abstract class Transaction <<abstract>> {
  - transaction_id: String
  - type: String
  - amount: Decimal
  - timestamp: Date
  - status: String
  - description: String
  + validate()
  + execute()
  + rollback()
}

class DepositTransaction {
  - source: String
}

class WithdrawTransaction {
  - destination: String
}

class TransferTransaction {
  - from_account: Account
  - to_account: Account
}

class BankService {
  - account_repository: AccountRepository
  - user_repository: UserRepository
  + create_customer()
  + transfer_money()
  + get_account_history()
  + find_account()
}

class AuthService {
  - user_repository: UserRepository
  + register_user()
  + authenticate()
}

interface AccountRepository <<interface>> {
  + find_by_number()
  + save()
  + find_by_customer()
  + delete()
}

interface UserRepository <<interface>> {
  + find_by_login()
  + save()
  + delete()
}

class Database {
  - connection_string: String
  + connect()
  + disconnect()
  + executeQuery()
  + executeUpdate()
}

class SqlAccountRepository {
  - db: Database
}

class SqlUserRepository {
  - db: Database
}

SqlAccountRepository ..|> AccountRepository
SqlUserRepository ..|> UserRepository

SqlAccountRepository o--> Database : использует
SqlUserRepository o--> Database : использует


Customer "1" o-- "0..*" Account : содержит

Account <|-- DebitAccount : наследование
Account <|-- SavingsAccount : наследование
Account <|-- CreditAccount : наследование

Account ..> Transaction : использует
Transaction <|-- DepositTransaction : наследование
Transaction <|-- WithdrawTransaction : наследование
Transaction <|-- TransferTransaction : наследование
TransferTransaction }o-- Account : from/to

BankService o--> AccountRepository : использует
BankService o--> UserRepository : использует
AuthService o--> UserRepository : использует

User --> Customer : customer_id

AccountStatus -- Customer : статус
AccountStatus -- Account : статус

@enduml

