import { Lock, Phone } from "lucide-react";
import { FormEvent, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { loginCustomer } from "../lib/api";
import { saveSession } from "../lib/session";

export default function Login() {
  const navigate = useNavigate();
  const [phone, setPhone] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setLoading(true);

    try {
      const session = await loginCustomer({ phone, password });
      saveSession(session);
      navigate("/dashboard");
    } catch (submitError) {
      setError(
        submitError instanceof Error
          ? submitError.message
          : "Не удалось выполнить вход",
      );
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="min-h-screen bg-[linear-gradient(180deg,#e9eef5_0%,#f7f9fc_100%)] px-6 py-10">
      <div className="mx-auto grid max-w-6xl overflow-hidden rounded-[2rem] border border-white/70 bg-white shadow-[0_40px_120px_-50px_rgba(15,23,42,0.45)] lg:grid-cols-[0.9fr_1.1fr]">
        <div className="relative overflow-hidden bg-[linear-gradient(160deg,#10295b_0%,#1546a0_55%,#2a7cf6_100%)] p-10 text-white lg:p-14">
          <div className="absolute -left-16 top-10 h-52 w-52 rounded-full bg-white/10 blur-3xl" />
          <div className="absolute bottom-0 right-0 h-72 w-72 translate-x-1/3 translate-y-1/4 rounded-full bg-cyan-300/20 blur-3xl" />
          <div className="relative flex min-h-full items-center">
            <h1 className="max-w-md text-4xl font-semibold leading-tight lg:text-5xl">
              Вход в банковскую систему
            </h1>
          </div>
        </div>

        <div className="p-8 lg:p-14">
          <div className="mx-auto max-w-md">
            <div className="mb-10">
              <div className="text-sm uppercase tracking-[0.2em] text-slate-500">
                Login
              </div>
              <h2 className="mt-3 text-3xl font-semibold text-slate-950">
                Добро пожаловать обратно
              </h2>
              <p className="mt-3 text-slate-600">
                Введите номер телефона и пароль.
              </p>
            </div>

            <form className="space-y-5" onSubmit={handleSubmit}>
              <label className="block">
                <span className="mb-2 block text-sm font-medium text-slate-700">
                  Телефон
                </span>
                <div className="relative">
                  <Phone className="pointer-events-none absolute left-4 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-400" />
                  <input
                    type="tel"
                    value={phone}
                    onChange={(event) => setPhone(event.target.value)}
                    placeholder="+79991234567"
                    className="w-full rounded-2xl border border-slate-200 bg-slate-50 py-4 pl-12 pr-4 outline-none transition focus:border-blue-500 focus:bg-white focus:ring-4 focus:ring-blue-100"
                    required
                  />
                </div>
              </label>

              <label className="block">
                <span className="mb-2 block text-sm font-medium text-slate-700">
                  Пароль
                </span>
                <div className="relative">
                  <Lock className="pointer-events-none absolute left-4 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-400" />
                  <input
                    type="password"
                    value={password}
                    onChange={(event) => setPassword(event.target.value)}
                    placeholder="Введите пароль"
                    className="w-full rounded-2xl border border-slate-200 bg-slate-50 py-4 pl-12 pr-4 outline-none transition focus:border-blue-500 focus:bg-white focus:ring-4 focus:ring-blue-100"
                    required
                  />
                </div>
              </label>

              {error ? (
                <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
                  {error}
                </div>
              ) : null}

              <button
                type="submit"
                disabled={loading}
                className="w-full rounded-2xl bg-[linear-gradient(135deg,#0f3cc9_0%,#1d7cf2_100%)] px-5 py-4 font-medium text-white shadow-lg shadow-blue-900/20 transition hover:opacity-95 disabled:cursor-not-allowed disabled:opacity-60"
              >
                {loading ? "Входим..." : "Войти"}
              </button>
            </form>

            <div className="mt-8 rounded-2xl border border-slate-200 bg-slate-50 px-5 py-4 text-sm text-slate-600">
              Нет аккаунта?{" "}
              <Link to="/registration" className="font-medium text-blue-700">
                Зарегистрироваться
              </Link>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
