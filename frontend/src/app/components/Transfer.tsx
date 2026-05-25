import {
    ArrowLeft,
    ArrowRight,
    ChevronDown,
    Landmark,
    Send,
  } from "lucide-react";
  import { FormEvent, useEffect, useMemo, useState } from "react";
  import { Link, useNavigate } from "react-router-dom";
  import {
    type Account,
    type Transaction,
    createTransfer,
    fetchAccountHistory,
    fetchDashboard,
    formatDate,
    formatMoney,
  } from "../lib/api";
  import { loadSession } from "../lib/session";
  
  export default function Transfer() {
    const navigate = useNavigate();
    const [accounts, setAccounts] = useState<Account[]>([]);
    const [history, setHistory] = useState<Transaction[]>([]);
    const [fromAccountNumber, setFromAccountNumber] = useState("");
    const [toAccountNumber, setToAccountNumber] = useState("");
    const [amount, setAmount] = useState("");
    const [description, setDescription] = useState("");
    const [error, setError] = useState("");
    const [success, setSuccess] = useState("");
    const [loading, setLoading] = useState(true);
    const [submitting, setSubmitting] = useState(false);
  
    async function loadTransferData(selectedAccount?: string) {
      const session = loadSession();
      if (!session) {
        navigate("/login");
        return;
      }
  
      setLoading(true);
      setError("");
      try {
        const dashboard = await fetchDashboard(session.customer.customerId);
        setAccounts(dashboard.accounts);
        const activeAccount = selectedAccount || dashboard.accounts[0]?.number || "";
        setFromAccountNumber(activeAccount);
  
        if (activeAccount) {
          const accountHistory = await fetchAccountHistory(activeAccount);
          setHistory(accountHistory.transactions.slice(0, 5));
        } else {
          setHistory([]);
        }
      } catch (loadError) {
        setError(
          loadError instanceof Error
            ? loadError.message
            : "Не удалось загрузить данные для перевода",
        );
      } finally {
        setLoading(false);
      }
    }
  
    useEffect(() => {
      void loadTransferData();
    }, []);
  
    useEffect(() => {
      if (!fromAccountNumber) {
        return;
      }
  
      void (async () => {
        try {
          const accountHistory = await fetchAccountHistory(fromAccountNumber);
          setHistory(accountHistory.transactions.slice(0, 5));
        } catch {
          setHistory([]);
        }
      })();
    }, [fromAccountNumber]);
  
    const selectedAccount = useMemo(
      () => accounts.find((account) => account.number === fromAccountNumber) ?? null,
      [accounts, fromAccountNumber],
    );
  
    async function handleSubmit(event: FormEvent<HTMLFormElement>) {
      event.preventDefault();
      setError("");
      setSuccess("");
      setSubmitting(true);
  
      try {
        await createTransfer({
          fromAccountNumber,
          toAccountNumber,
          amount,
          description,
        });
        setSuccess("Перевод выполнен успешно");
        setAmount("");
        setDescription("");
        setToAccountNumber("");
        await loadTransferData(fromAccountNumber);
      } catch (submitError) {
        setError(
          submitError instanceof Error
            ? submitError.message
            : "Не удалось выполнить перевод",
        );
      } finally {
        setSubmitting(false);
      }
    }
  
    return (
      <div className="min-h-screen bg-[linear-gradient(180deg,#eef3f8_0%,#f8fafc_100%)] px-6 py-8">
        <div className="mx-auto max-w-6xl">
          <div className="mb-6 flex items-center justify-between">
            <Link
              to="/dashboard"
              className="inline-flex items-center gap-2 rounded-full border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-slate-700 shadow-sm transition hover:bg-slate-50"
            >
              <ArrowLeft className="h-4 w-4" />
              Назад в кабинет
            </Link>
          </div>
  
          <div className="grid gap-6 lg:grid-cols-[1.05fr_0.95fr]">
            <section className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm">
              <div className="flex items-center gap-3">
                <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-slate-950 text-white">
                  <Send className="h-5 w-5" />
                </div>
                <div>
                  <div className="text-sm uppercase tracking-[0.2em] text-slate-500">
                    Transfer
                  </div>
                  <h1 className="text-3xl font-semibold text-slate-950">
                    Новый перевод
                  </h1>
                </div>
              </div>
  
              {loading ? (
                <div className="mt-8 text-slate-600">Загружаем счета...</div>
              ) : (
                <form className="mt-8 space-y-5" onSubmit={handleSubmit}>
                  <label className="block">
                    <span className="mb-2 block text-sm font-medium text-slate-700">
                      С какого счета
                    </span>
                    <div className="relative">
                      <select
                        value={fromAccountNumber}
                        onChange={(event) => setFromAccountNumber(event.target.value)}
                        className="w-full appearance-none rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4 pr-14 outline-none transition focus:border-blue-500 focus:bg-white focus:ring-4 focus:ring-blue-100"
                        required
                      >
                        {accounts.map((account) => (
                          <option key={account.accountId} value={account.number}>
                            {account.number} · {formatMoney(account.balance, account.currency)}
                          </option>
                        ))}
                      </select>
                      <ChevronDown className="pointer-events-none absolute right-6 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-500" />
                    </div>
                  </label>
  
                  <label className="block">
                    <span className="mb-2 block text-sm font-medium text-slate-700">
                      Счет получателя
                    </span>
                    <input
                      type="text"
                      value={toAccountNumber}
                      onChange={(event) => setToAccountNumber(event.target.value)}
                      placeholder="Например, ACC-001"
                      className="w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4 outline-none transition focus:border-blue-500 focus:bg-white focus:ring-4 focus:ring-blue-100"
                      required
                    />
                  </label>
  
                  <label className="block">
                    <span className="mb-2 block text-sm font-medium text-slate-700">
                      Сумма
                    </span>
                    <input
                      type="number"
                      min="0.01"
                      step="0.01"
                      value={amount}
                      onChange={(event) => setAmount(event.target.value)}
                      placeholder="1000.00"
                      className="w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4 outline-none transition focus:border-blue-500 focus:bg-white focus:ring-4 focus:ring-blue-100"
                      required
                    />
                  </label>
  
                  <label className="block">
                    <span className="mb-2 block text-sm font-medium text-slate-700">
                      Описание
                    </span>
                    <input
                      type="text"
                      value={description}
                      onChange={(event) => setDescription(event.target.value)}
                      placeholder="Перевод между счетами"
                      className="w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4 outline-none transition focus:border-blue-500 focus:bg-white focus:ring-4 focus:ring-blue-100"
                    />
                  </label>
  
                  {error ? (
                    <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
                      {error}
                    </div>
                  ) : null}
  
                  {success ? (
                    <div className="rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">
                      {success}
                    </div>
                  ) : null}
  
                  <button
                    type="submit"
                    disabled={submitting}
                    className="inline-flex w-full items-center justify-center gap-2 rounded-2xl bg-[linear-gradient(135deg,#0f3cc9_0%,#1d7cf2_100%)] px-5 py-4 font-medium text-white shadow-lg shadow-blue-900/20 transition hover:opacity-95 disabled:cursor-not-allowed disabled:opacity-60"
                  >
                    {submitting ? "Отправляем..." : "Отправить перевод"}
                    <ArrowRight className="h-4 w-4" />
                  </button>
                </form>
              )}
            </section>
  
            <section className="space-y-6">
              <div className="rounded-[2rem] border border-slate-200 bg-[linear-gradient(135deg,#0b1733_0%,#12306d_55%,#1d7cf2_100%)] p-8 text-white shadow-[0_40px_100px_-50px_rgba(2,6,23,0.75)]">
                <div className="flex items-center gap-3">
                  <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-white/15">
                    <Landmark className="h-5 w-5" />
                  </div>
                  <div>
                    <div className="text-sm uppercase tracking-[0.2em] text-blue-100/80">
                      Source account
                    </div>
                    <div className="text-2xl font-semibold">
                      {selectedAccount?.number ?? "Нет счета"}
                    </div>
                  </div>
                </div>
  
                <div className="mt-6 text-4xl font-semibold">
                  {selectedAccount
                    ? formatMoney(selectedAccount.balance, selectedAccount.currency)
                    : "—"}
                </div>
                <div className="mt-2 text-blue-100/80">
                  {selectedAccount?.currency ?? ""}
                </div>
              </div>
  
              <div className="rounded-[2rem] border border-slate-200 bg-white p-7 shadow-sm">
                <h2 className="text-2xl font-semibold text-slate-950">
                  Последние операции по счету
                </h2>
                <div className="mt-5 space-y-3">
                  {history.length ? (
                    history.map((transaction) => (
                      <div
                        key={transaction.transactionId}
                        className="rounded-[1.5rem] border border-slate-200 px-4 py-4"
                      >
                        <div className="flex items-center justify-between gap-4">
                          <div>
                            <div className="font-medium text-slate-950">
                              {transaction.description || transaction.type}
                            </div>
                            <div className="mt-1 text-sm text-slate-500">
                              {formatDate(transaction.timestamp)}
                            </div>
                          </div>
                          <div className="text-right font-semibold text-slate-950">
                            {formatMoney(transaction.amount, transaction.currency)}
                          </div>
                        </div>
                      </div>
                    ))
                  ) : (
                    <div className="rounded-[1.5rem] border border-dashed border-slate-200 px-5 py-8 text-center text-slate-500">
                      История для выбранного счета пока пустая.
                    </div>
                  )}
                </div>
              </div>
            </section>
          </div>
        </div>
      </div>
    );
  }
  