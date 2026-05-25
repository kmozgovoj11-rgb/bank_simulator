import {
  ArrowDownToLine,
  ChevronDown,
  CreditCard,
  HandCoins,
  History,
  Landmark,
  LogOut,
  PiggyBank,
  PlusCircle,
  Target,
  Trash2,
} from "lucide-react";
import { FormEvent, useEffect, useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import {
  type Account,
  type DashboardResponse,
  type Transaction,
  accrueInterest,
  createAdditionalAccount,
  deleteAdditionalAccount,
  fetchDashboard,
  formatDate,
  formatMoney,
  fundSavingsAccount,
  topUpStandardAccount,
} from "../lib/api";
import { clearSession, loadSession } from "../lib/session";

type QuickAction = "SAVINGS" | "CREDIT" | "TOPUP" | "FUND_SAVINGS" | "DELETE";

export default function Dashboard() {
  const navigate = useNavigate();
  const [data, setData] = useState<DashboardResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [quickAction, setQuickAction] = useState<QuickAction>("SAVINGS");
  const [depositAmount, setDepositAmount] = useState("1000");
  const [savingsDepositAmount, setSavingsDepositAmount] = useState("1000");
  const [savingsAccountToFund, setSavingsAccountToFund] = useState("");
  const [accountToDelete, setAccountToDelete] = useState("");
  const [actionMessage, setActionMessage] = useState("");
  const [actionBusy, setActionBusy] = useState(false);

  async function loadData() {
    const session = loadSession();
    if (!session) {
      navigate("/login");
      return;
    }

    setLoading(true);
    setError("");
    try {
      const dashboard = await fetchDashboard(session.customer.customerId);
      setData(dashboard);
    } catch (loadError) {
      setError(
        loadError instanceof Error
          ? loadError.message
          : "Не удалось загрузить данные клиента",
      );
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadData();
  }, []);

  const totalBalance = useMemo(() => {
    if (!data) {
      return 0;
    }

    return data.accounts.reduce(
      (sum, account) => sum + Number.parseFloat(account.balance),
      0,
    );
  }, [data]);

  const debitAccounts = useMemo(
    () => data?.accounts.filter((account) => account.kind === "DEBIT") ?? [],
    [data],
  );

  const removableAccounts = useMemo(
    () => data?.accounts.filter((account) => account.kind !== "DEBIT") ?? [],
    [data],
  );

  const savingsAccounts = useMemo(
    () => data?.accounts.filter((account) => account.kind === "SAVINGS") ?? [],
    [data],
  );

  useEffect(() => {
    if (!removableAccounts.length) {
      setAccountToDelete("");
      return;
    }

    if (!removableAccounts.some((account) => account.number === accountToDelete)) {
      setAccountToDelete(removableAccounts[0]?.number ?? "");
    }
  }, [accountToDelete, removableAccounts]);

  useEffect(() => {
    if (!savingsAccounts.length) {
      setSavingsAccountToFund("");
      return;
    }

    if (!savingsAccounts.some((account) => account.number === savingsAccountToFund)) {
      setSavingsAccountToFund(savingsAccounts[0]?.number ?? "");
    }
  }, [savingsAccountToFund, savingsAccounts]);

  function handleLogout() {
    clearSession();
    navigate("/login");
  }

  async function handleQuickAction(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const session = loadSession();
    if (!session) {
      navigate("/login");
      return;
    }

    setActionBusy(true);
    setActionMessage("");
    setError("");

    try {
      switch (quickAction) {
        case "TOPUP":
          await topUpStandardAccount({
            customerId: session.customer.customerId,
            amount: depositAmount,
            description: "Пополнение стандартного дебетового счета",
          });
          setActionMessage("Стандартный дебетовый счет успешно пополнен");
          break;
        case "FUND_SAVINGS": {
          const transaction = await fundSavingsAccount({
            customerId: session.customer.customerId,
            accountNumber: savingsAccountToFund,
            amount: savingsDepositAmount,
            description: "Перевод на сберегательный счет",
          });
          setActionMessage(
            `Сберегательный счет ${transaction.toAccountNumber} пополнен на ${formatMoney(
              transaction.amount,
              transaction.currency,
            )}`,
          );
          break;
        }
        case "DELETE": {
          const removed = await deleteAdditionalAccount({
            customerId: session.customer.customerId,
            accountNumber: accountToDelete,
          });
          setActionMessage(`Счет ${removed.number} успешно удален`);
          break;
        }
        default: {
          const created = await createAdditionalAccount({
            customerId: session.customer.customerId,
            accountKind: quickAction,
          });

          setActionMessage(
            quickAction === "SAVINGS"
              ? `Открыт сберегательный счет ${created.number}`
              : `Открыт кредитный счет ${created.number}`,
          );
        }
      }

      await loadData();
    } catch (actionError) {
      setError(
        actionError instanceof Error
          ? actionError.message
          : "Не удалось выполнить действие",
      );
    } finally {
      setActionBusy(false);
    }
  }

  async function handleAccrueInterest(accountNumber: string) {
    const session = loadSession();
    if (!session) {
      navigate("/login");
      return;
    }

    setActionBusy(true);
    setActionMessage("");
    setError("");

    try {
      const result = await accrueInterest({
        customerId: session.customer.customerId,
        accountNumber,
      });
      setActionMessage(
        `Начислены проценты ${formatMoney(result.amount, result.transaction.currency)}`,
      );
      await loadData();
    } catch (actionError) {
      setError(
        actionError instanceof Error
          ? actionError.message
          : "Не удалось начислить проценты",
      );
    } finally {
      setActionBusy(false);
    }
  }

  return (
    <div className="min-h-screen bg-[linear-gradient(180deg,#eef3f8_0%,#f8fafc_100%)]">
      <div className="mx-auto max-w-7xl px-6 py-8 lg:px-10">
        <div className="mb-8 flex flex-col gap-4 rounded-[2rem] bg-[linear-gradient(135deg,#0b1733_0%,#12306d_52%,#1d7cf2_100%)] p-8 text-white shadow-[0_40px_100px_-50px_rgba(2,6,23,0.75)] lg:flex-row lg:items-end lg:justify-between">
          <div>
            <div className="inline-flex items-center gap-2 rounded-full bg-white/10 px-4 py-2 text-sm backdrop-blur">
              <Landmark className="h-4 w-4" />
              Личный кабинет клиента
            </div>
            <h1 className="mt-5 text-4xl font-semibold tracking-tight">
              {data?.customer.fullName ?? "Банковский кабинет"}
            </h1>
            <p className="mt-3 max-w-2xl text-blue-100/90">
              Счета, баланс и последние операции в одном месте.
            </p>
          </div>

          <button
            onClick={handleLogout}
            className="inline-flex items-center gap-2 self-start rounded-full border border-white/15 bg-white/10 px-5 py-3 text-sm font-medium backdrop-blur transition hover:bg-white/15"
          >
            <LogOut className="h-4 w-4" />
            Выйти
          </button>
        </div>

        {error ? (
          <div className="mb-6 rounded-2xl border border-red-200 bg-red-50 px-5 py-4 text-red-700">
            {error}
          </div>
        ) : null}

        {loading ? (
          <div className="rounded-[2rem] border border-slate-200 bg-white p-10 text-slate-600 shadow-sm">
            Загружаем данные клиента...
          </div>
        ) : data ? (
          <div className="space-y-8">
            <div className="grid gap-5 lg:grid-cols-[1.1fr_0.9fr]">
              <section className="rounded-[2rem] border border-slate-200 bg-white p-7 shadow-sm">
                <div className="flex items-start justify-between gap-4">
                  <div>
                    <div className="text-sm uppercase tracking-[0.2em] text-slate-500">
                      Total balance
                    </div>
                    <div className="mt-3 text-4xl font-semibold text-slate-950">
                      {formatMoney(
                        String(totalBalance),
                        data.accounts[0]?.currency ?? "RUB",
                      )}
                    </div>
                    <div className="mt-3 text-slate-600">
                      Телефон: {data.customer.phone}
                    </div>
                  </div>
                  <div className="rounded-3xl bg-blue-50 px-4 py-3 text-sm font-medium text-blue-700">
                    {data.accounts.length} счет(а)
                  </div>
                </div>

                <div className="mt-6 grid gap-4 md:grid-cols-2">
                  {data.accounts.map((account) => (
                    <AccountCard
                      key={account.accountId}
                      account={account}
                      isBusy={actionBusy}
                      onAccrueInterest={handleAccrueInterest}
                    />
                  ))}
                </div>
              </section>

              <section className="rounded-[2rem] border border-slate-200 bg-white p-7 shadow-sm">
                <div className="text-sm uppercase tracking-[0.2em] text-slate-500">
                  Quick actions
                </div>

                <form className="mt-5 space-y-4" onSubmit={handleQuickAction}>
                  <label className="block">
                    <span className="mb-2 block text-sm font-medium text-slate-700">
                      Действие
                    </span>
                    <div className="relative">
                      <select
                        value={quickAction}
                        onChange={(event) =>
                          setQuickAction(event.target.value as QuickAction)
                        }
                        className="w-full appearance-none rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4 pr-14 outline-none transition focus:border-blue-500 focus:bg-white focus:ring-4 focus:ring-blue-100"
                      >
                        <option value="SAVINGS">Открыть сберегательный счет</option>
                        <option value="CREDIT">Открыть кредитный счет</option>
                        <option value="TOPUP">
                          Пополнить стандартный дебетовый счет
                        </option>
                        <option value="FUND_SAVINGS">
                          Перевести на сберегательный счет
                        </option>
                        <option value="DELETE">
                          Удалить дополнительный счет
                        </option>
                      </select>
                      <ChevronDown className="pointer-events-none absolute right-4 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-500" />
                    </div>
                  </label>

                  {quickAction === "TOPUP" ? (
                    <label className="block">
                      <span className="mb-2 block text-sm font-medium text-slate-700">
                        Сумма пополнения
                      </span>
                      <input
                        type="number"
                        min="0.01"
                        step="0.01"
                        value={depositAmount}
                        onChange={(event) => setDepositAmount(event.target.value)}
                        className="w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4 outline-none transition focus:border-blue-500 focus:bg-white focus:ring-4 focus:ring-blue-100"
                        required
                      />
                    </label>
                  ) : null}

                  {quickAction === "FUND_SAVINGS" ? (
                    <div className="space-y-4">
                      <label className="block">
                        <span className="mb-2 block text-sm font-medium text-slate-700">
                          Сберегательный счет
                        </span>
                        <div className="relative">
                          <select
                            value={savingsAccountToFund}
                            onChange={(event) => setSavingsAccountToFund(event.target.value)}
                            className="w-full appearance-none rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4 pr-14 outline-none transition focus:border-blue-500 focus:bg-white focus:ring-4 focus:ring-blue-100"
                            disabled={!savingsAccounts.length}
                            required
                          >
                            {savingsAccounts.length ? (
                              savingsAccounts.map((account) => (
                                <option key={account.accountId} value={account.number}>
                                  {account.number} · {formatMoney(account.balance, account.currency)}
                                </option>
                              ))
                            ) : (
                              <option value="">
                                Сначала откройте сберегательный счет
                              </option>
                            )}
                          </select>
                          <ChevronDown className="pointer-events-none absolute right-4 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-500" />
                        </div>
                      </label>

                      <label className="block">
                        <span className="mb-2 block text-sm font-medium text-slate-700">
                          Сумма перевода
                        </span>
                        <input
                          type="number"
                          min="0.01"
                          step="0.01"
                          value={savingsDepositAmount}
                          onChange={(event) => setSavingsDepositAmount(event.target.value)}
                          className="w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4 outline-none transition focus:border-blue-500 focus:bg-white focus:ring-4 focus:ring-blue-100"
                          required
                        />
                      </label>

                      <p className="text-sm text-slate-500">
                        Деньги спишутся со стандартного дебетового счета.
                      </p>
                    </div>
                  ) : null}

                  {quickAction === "DELETE" ? (
                    <label className="block">
                      <span className="mb-2 block text-sm font-medium text-slate-700">
                        Какой счет удалить
                      </span>
                      <div className="relative">
                        <select
                          value={accountToDelete}
                          onChange={(event) => setAccountToDelete(event.target.value)}
                          className="w-full appearance-none rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4 pr-14 outline-none transition focus:border-blue-500 focus:bg-white focus:ring-4 focus:ring-blue-100"
                          disabled={!removableAccounts.length}
                          required
                        >
                          {removableAccounts.length ? (
                            removableAccounts.map((account) => (
                              <option key={account.accountId} value={account.number}>
                                {account.number} · {formatMoney(account.balance, account.currency)}
                              </option>
                            ))
                          ) : (
                            <option value="">
                              Нет дополнительных счетов для удаления
                            </option>
                          )}
                        </select>
                        <ChevronDown className="pointer-events-none absolute right-4 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-500" />
                      </div>
                      <p className="mt-2 text-sm text-slate-500">
                        Удалить можно только сберегательный или кредитный счет с нулевым балансом.
                      </p>
                    </label>
                  ) : null}

                  <button
                    type="submit"
                    disabled={
                      actionBusy ||
                      (quickAction === "FUND_SAVINGS" && savingsAccounts.length === 0) ||
                      (quickAction === "DELETE" && removableAccounts.length === 0)
                    }
                    className="inline-flex w-full items-center justify-center gap-2 rounded-2xl bg-[linear-gradient(135deg,#0f3cc9_0%,#1d7cf2_100%)] px-5 py-4 font-medium text-white shadow-lg shadow-blue-900/20 transition hover:opacity-95 disabled:cursor-not-allowed disabled:opacity-60"
                  >
                    {quickAction === "DELETE" ? (
                      <Trash2 className="h-4 w-4" />
                    ) : (
                      <PlusCircle className="h-4 w-4" />
                    )}
                    {actionBusy ? "Выполняем..." : "Подтвердить действие"}
                  </button>
                </form>

                {actionMessage ? (
                  <div className="mt-4 rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">
                    {actionMessage}
                  </div>
                ) : null}

                <div className="mt-5 space-y-4">
                  <Link
                    to="/transfer"
                    className="flex items-center justify-between rounded-[1.5rem] border border-slate-200 bg-slate-50 px-5 py-4 transition hover:border-blue-300 hover:bg-blue-50"
                  >
                    <div className="flex items-center gap-3">
                      <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-blue-600 text-white">
                        <HandCoins className="h-5 w-5" />
                      </div>
                      <div>
                        <div className="font-medium text-slate-950">
                          Новый перевод
                        </div>
                        <div className="text-sm text-slate-600">
                          По телефону или на стандартный дебетовый счет
                        </div>
                      </div>
                    </div>
                  </Link>

                  <Link
                    to="/goals"
                    className="flex items-center justify-between rounded-[1.5rem] border border-slate-200 bg-slate-50 px-5 py-4 transition hover:border-blue-300 hover:bg-blue-50"
                  >
                    <div className="flex items-center gap-3">
                      <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-slate-950 text-white">
                        <Target className="h-5 w-5" />
                      </div>
                      <div>
                        <div className="font-medium text-slate-950">
                          Финансовые цели
                        </div>
                        <div className="text-sm text-slate-600">
                          Перейти к экрану накоплений
                        </div>
                      </div>
                    </div>
                  </Link>
                </div>
              </section>
            </div>

            <section className="rounded-[2rem] border border-slate-200 bg-white p-7 shadow-sm">
              <div className="mb-5 flex items-center gap-3">
                <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-slate-950 text-white">
                  <History className="h-5 w-5" />
                </div>
                <div>
                  <h2 className="text-2xl font-semibold text-slate-950">
                    Последние транзакции
                  </h2>
                  <p className="text-slate-600">
                    Пополнения, переводы и история по всем счетам клиента
                  </p>
                </div>
              </div>

              <div className="space-y-3">
                {data.recentTransactions.length ? (
                  data.recentTransactions.slice(0, 8).map((transaction) => (
                    <TransactionRow
                      key={transaction.transactionId}
                      transaction={transaction}
                    />
                  ))
                ) : (
                  <div className="rounded-[1.5rem] border border-dashed border-slate-200 px-5 py-8 text-center text-slate-500">
                    У клиента пока нет транзакций.
                  </div>
                )}
              </div>
            </section>

            {debitAccounts.length > 1 ? (
              <div className="rounded-2xl border border-amber-200 bg-amber-50 px-5 py-4 text-amber-800">
                У клиента найдено несколько дебетовых счетов. Для переводов и пополнений используется первый стандартный дебетовый счет.
              </div>
            ) : null}
          </div>
        ) : null}
      </div>
    </div>
  );
}

function AccountCard({
  account,
  isBusy,
  onAccrueInterest,
}: {
  account: Account;
  isBusy: boolean;
  onAccrueInterest: (accountNumber: string) => void;
}) {
  const titleByKind: Record<string, string> = {
    DEBIT: "Стандартный дебетовый счет",
    SAVINGS: "Сберегательный счет",
    CREDIT: "Кредитный счет",
  };

  const title = titleByKind[account.kind] ?? account.kind;
  const interestRatePercent =
    account.interestRate == null
      ? null
      : (Number.parseFloat(account.interestRate) * 100).toFixed(2);

  return (
    <div className="rounded-[1.75rem] bg-[linear-gradient(135deg,#0f3cc9_0%,#1d7cf2_100%)] p-6 text-white shadow-lg shadow-blue-900/20">
      <div className="flex items-start justify-between">
        <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-white/15">
          {account.kind === "SAVINGS" ? (
            <PiggyBank className="h-5 w-5" />
          ) : (
            <CreditCard className="h-5 w-5" />
          )}
        </div>
        <div className="rounded-full bg-white/15 px-3 py-1 text-xs uppercase tracking-[0.2em]">
          {account.status}
        </div>
      </div>
      <div className="mt-8 text-sm text-blue-100/80">{title}</div>
      <div className="mt-2 text-sm text-blue-100/80">{account.number}</div>
      <div className="mt-2 text-3xl font-semibold">
        {formatMoney(account.balance, account.currency)}
      </div>
      <div className="mt-2 text-sm text-blue-100/80">{account.currency}</div>
      {account.kind === "SAVINGS" ? (
        <div className="mt-5 rounded-2xl bg-white/10 p-4 text-sm text-blue-50">
          <div>
            Ставка: {interestRatePercent == null ? "не указана" : `${interestRatePercent}% годовых`}
          </div>
          <div className="mt-1">
            Последнее начисление:{" "}
            {account.lastInterestAccrualAt
              ? formatDate(account.lastInterestAccrualAt)
              : "ещё не было"}
          </div>
          <button
            type="button"
            disabled={isBusy || account.canAccrueInterest !== true}
            onClick={() => onAccrueInterest(account.number)}
            className="mt-4 inline-flex w-full items-center justify-center rounded-xl bg-white px-4 py-3 font-medium text-blue-700 transition hover:bg-blue-50 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {isBusy ? "Начисляем..." : "Начислить проценты за месяц"}
          </button>
          {account.canAccrueInterest !== true ? (
            <div className="mt-2 text-xs text-blue-100/80">
              Начисление доступно раз в 30 дней для активного счёта.
            </div>
          ) : null}
        </div>
      ) : null}
    </div>
  );
}

function TransactionRow({ transaction }: { transaction: Transaction }) {
  const meta = getTransactionMeta(transaction);

  return (
    <div className="grid gap-4 rounded-[1.5rem] border border-slate-200 px-5 py-4 lg:grid-cols-[1.25fr_0.9fr_0.7fr] lg:items-center">
      <div className="flex items-center gap-4">
        <div className={`flex h-11 w-11 items-center justify-center rounded-2xl ${meta.iconBg}`}>
          <meta.icon className={`h-5 w-5 ${meta.iconColor}`} />
        </div>
        <div>
          <div className="font-medium text-slate-950">
            {transaction.description || meta.title}
          </div>
          <div className="mt-1 text-sm text-slate-500">
            {meta.subtitle(transaction)}
          </div>
        </div>
      </div>
      <div className="text-sm text-slate-600">{formatDate(transaction.timestamp)}</div>
      <div className={`text-right font-semibold ${meta.amountColor}`}>
        {meta.prefix}
        {formatMoney(transaction.amount, transaction.currency)}
      </div>
    </div>
  );
}

function getTransactionMeta(transaction: Transaction) {
  switch (transaction.type) {
    case "DEPOSIT":
      return {
        title: "Пополнение счета",
        subtitle: (item: Transaction) =>
          `На счет ${item.toAccountNumber ?? "стандартный дебетовый счет"}`,
        prefix: "+",
        amountColor: "text-emerald-700",
        icon: ArrowDownToLine,
        iconBg: "bg-emerald-100",
        iconColor: "text-emerald-700",
      };
    case "INTEREST":
      return {
        title: "Начисление процентов",
        subtitle: (item: Transaction) =>
          `На счет ${item.toAccountNumber ?? "сберегательный счет"}`,
        prefix: "+",
        amountColor: "text-emerald-700",
        icon: PiggyBank,
        iconBg: "bg-emerald-100",
        iconColor: "text-emerald-700",
      };
    case "TRANSFER":
      return {
        title: "Перевод",
        subtitle: (item: Transaction) =>
          `${item.fromAccountNumber ?? "external"} -> ${item.toAccountNumber ?? "external"}`,
        prefix: "-",
        amountColor: "text-slate-950",
        icon: HandCoins,
        iconBg: "bg-blue-100",
        iconColor: "text-blue-700",
      };
    default:
      return {
        title: transaction.type,
        subtitle: () => "Операция по счету",
        prefix: "",
        amountColor: "text-slate-950",
        icon: History,
        iconBg: "bg-slate-100",
        iconColor: "text-slate-700",
      };
  }
}
