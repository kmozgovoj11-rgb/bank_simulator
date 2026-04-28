import { CreditCard, History, Landmark, LogOut, Target } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import {
  type Account,
  type DashboardResponse,
  type Transaction,
  fetchDashboard,
  formatDate,
  formatMoney,
} from "../lib/api";
import { clearSession, loadSession } from "../lib/session";

export default function Dashboard() {
  const navigate = useNavigate();
  const [data, setData] = useState<DashboardResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

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
          : "Не удалось загрузить дашборд",
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

  function handleLogout() {
    clearSession();
    navigate("/login");
  }

  return (
    <div className="min-h-screen bg-[linear-gradient(180deg,#eef3f8_0%,#f8fafc_100%)]">
      <div className="mx-auto max-w-7xl px-6 py-8 lg:px-10">
        <div className="mb-8 flex flex-col gap-4 rounded-[2rem] bg-[linear-gradient(135deg,#0b1733_0%,#12306d_52%,#1d7cf2_100%)] p-8 text-white shadow-[0_40px_100px_-50px_rgba(2,6,23,0.75)] lg:flex-row lg:items-end lg:justify-between">
          <div>
            <div className="inline-flex items-center gap-2 rounded-full bg-white/10 px-4 py-2 text-sm backdrop-blur">
              <Landmark className="h-4 w-4" />
              Client dashboard
            </div>
            <h1 className="mt-5 text-4xl font-semibold tracking-tight">
              {data?.customer.fullName ?? "Банковый кабинет"}
            </h1>
            <p className="mt-3 max-w-2xl text-blue-100/90">
              Счета, баланс и последние операции в одном месте.
            </p>
          </div>

          <div className="flex flex-wrap gap-3">
            <button
              onClick={handleLogout}
              className="inline-flex items-center gap-2 rounded-full border border-white/15 bg-white/10 px-5 py-3 text-sm font-medium backdrop-blur transition hover:bg-white/15"
            >
              <LogOut className="h-4 w-4" />
              Выйти
            </button>
          </div>
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
                    {data.accounts.length} счета
                  </div>
                </div>

                <div className="mt-6 grid gap-4 md:grid-cols-2">
                  {data.accounts.map((account) => (
                    <AccountCard key={account.accountId} account={account} />
                  ))}
                </div>
              </section>

              <section className="rounded-[2rem] border border-slate-200 bg-white p-7 shadow-sm">
                <div className="text-sm uppercase tracking-[0.2em] text-slate-500">
                  Quick actions
                </div>
                <div className="mt-5 space-y-4">
                  <Link
                    to="/transfer"
                    className="flex items-center justify-between rounded-[1.5rem] border border-slate-200 bg-slate-50 px-5 py-4 transition hover:border-blue-300 hover:bg-blue-50"
                  >
                    <div className="flex items-center gap-3">
                      <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-blue-600 text-white">
                        <Landmark className="h-5 w-5" />
                      </div>
                      <div>
                        <div className="font-medium text-slate-950">
                          Новый перевод
                        </div>
                        <div className="text-sm text-slate-600">
                          Перейти к переводу между счетами
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
                    Агрегированная история по счетам клиента
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
          </div>
        ) : null}
      </div>
    </div>
  );
}

function AccountCard({ account }: { account: Account }) {
  return (
    <div className="rounded-[1.75rem] bg-[linear-gradient(135deg,#0f3cc9_0%,#1d7cf2_100%)] p-6 text-white shadow-lg shadow-blue-900/20">
      <div className="flex items-start justify-between">
        <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-white/15">
          <CreditCard className="h-5 w-5" />
        </div>
        <div className="rounded-full bg-white/15 px-3 py-1 text-xs uppercase tracking-[0.2em]">
          {account.status}
        </div>
      </div>
      <div className="mt-8 text-sm text-blue-100/80">{account.number}</div>
      <div className="mt-2 text-3xl font-semibold">
        {formatMoney(account.balance, account.currency)}
      </div>
      <div className="mt-2 text-sm text-blue-100/80">{account.currency}</div>
    </div>
  );
}

function TransactionRow({ transaction }: { transaction: Transaction }) {
  const isOutgoing =
    transaction.type === "TRANSFER" && transaction.fromAccountNumber;
  const amountPrefix = isOutgoing ? "-" : "+";

  return (
    <div className="grid gap-4 rounded-[1.5rem] border border-slate-200 px-5 py-4 lg:grid-cols-[1.2fr_0.9fr_0.7fr] lg:items-center">
      <div>
        <div className="font-medium text-slate-950">
          {transaction.description || transaction.type}
        </div>
        <div className="mt-1 text-sm text-slate-500">
          {transaction.fromAccountNumber ?? "external"} →{" "}
          {transaction.toAccountNumber ?? "external"}
        </div>
      </div>
      <div className="text-sm text-slate-600">
        {formatDate(transaction.timestamp)}
      </div>
      <div className="text-right font-semibold text-slate-950">
        {amountPrefix}
        {formatMoney(transaction.amount, transaction.currency)}
      </div>
    </div>
  );
}
