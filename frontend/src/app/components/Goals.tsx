import { FormEvent, useEffect, useState } from "react";
import { Plus, Target, Trash2 } from "lucide-react";

type Goal = {
  id: string;
  title: string;
  current: string;
  target: string;
};

const STORAGE_KEY = "bank-simulator-goals";

export default function Goals() {
  const [goals, setGoals] = useState<Goal[]>([]);
  const [showForm, setShowForm] = useState(false);
  const [title, setTitle] = useState("");
  const [current, setCurrent] = useState("");
  const [target, setTarget] = useState("");

  useEffect(() => {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      return;
    }

    try {
      const parsed = JSON.parse(raw) as Goal[];
      setGoals(Array.isArray(parsed) ? parsed : []);
    } catch {
      localStorage.removeItem(STORAGE_KEY);
    }
  }, []);

  useEffect(() => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(goals));
  }, [goals]);

  function resetForm() {
    setTitle("");
    setCurrent("");
    setTarget("");
    setShowForm(false);
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    const nextGoal: Goal = {
      id: crypto.randomUUID(),
      title: title.trim(),
      current: current.trim() || "0",
      target: target.trim(),
    };

    setGoals((existing) => [...existing, nextGoal]);
    resetForm();
  }

  function removeGoal(goalId: string) {
    setGoals((existing) => existing.filter((goal) => goal.id !== goalId));
  }

  return (
    <div className="min-h-screen bg-[linear-gradient(180deg,#eef3f8_0%,#f8fafc_100%)] px-6 py-8">
      <div className="mx-auto max-w-4xl">
        <div className="rounded-[2rem] border border-slate-200 bg-white shadow-sm">
          <div className="flex items-center justify-between border-b border-slate-100 px-8 py-8">
            <div>
              <div className="text-sm uppercase tracking-[0.2em] text-slate-500">
                Goals
              </div>
              <h1 className="mt-2 text-3xl font-semibold text-slate-950">
                Финансовые цели
              </h1>
            </div>

            <button
              onClick={() => setShowForm((value) => !value)}
              className="inline-flex h-12 items-center gap-2 rounded-full bg-[linear-gradient(135deg,#0f3cc9_0%,#1d7cf2_100%)] px-5 text-sm font-medium text-white shadow-lg shadow-blue-900/20"
            >
              <Plus className="h-5 w-5" />
              {showForm ? "Скрыть форму" : "Добавить цель"}
            </button>
          </div>

          <div className="p-8">
            {showForm ? (
              <form
                onSubmit={handleSubmit}
                className="mb-8 grid gap-4 rounded-[1.75rem] border border-slate-200 bg-slate-50 p-6 md:grid-cols-2"
              >
                <label className="block md:col-span-2">
                  <span className="mb-2 block text-sm font-medium text-slate-700">
                    Название цели
                  </span>
                  <input
                    type="text"
                    value={title}
                    onChange={(event) => setTitle(event.target.value)}
                    placeholder="Например, поездка или новый ноутбук"
                    className="w-full rounded-2xl border border-slate-200 bg-white px-4 py-4 outline-none transition focus:border-blue-500 focus:ring-4 focus:ring-blue-100"
                    required
                  />
                </label>

                <label className="block">
                  <span className="mb-2 block text-sm font-medium text-slate-700">
                    Уже накоплено
                  </span>
                  <input
                    type="number"
                    min="0"
                    step="0.01"
                    value={current}
                    onChange={(event) => setCurrent(event.target.value)}
                    placeholder="0"
                    className="w-full rounded-2xl border border-slate-200 bg-white px-4 py-4 outline-none transition focus:border-blue-500 focus:ring-4 focus:ring-blue-100"
                  />
                </label>

                <label className="block">
                  <span className="mb-2 block text-sm font-medium text-slate-700">
                    Целевая сумма
                  </span>
                  <input
                    type="number"
                    min="0.01"
                    step="0.01"
                    value={target}
                    onChange={(event) => setTarget(event.target.value)}
                    placeholder="100000"
                    className="w-full rounded-2xl border border-slate-200 bg-white px-4 py-4 outline-none transition focus:border-blue-500 focus:ring-4 focus:ring-blue-100"
                    required
                  />
                </label>

                <div className="flex gap-3 md:col-span-2">
                  <button
                    type="submit"
                    className="inline-flex h-12 items-center rounded-full bg-slate-950 px-6 text-sm font-medium text-white hover:bg-slate-800"
                  >
                    Сохранить цель
                  </button>
                  <button
                    type="button"
                    onClick={resetForm}
                    className="inline-flex h-12 items-center rounded-full border border-slate-300 bg-white px-6 text-sm font-medium text-slate-700"
                  >
                    Отмена
                  </button>
                </div>
              </form>
            ) : null}

            {goals.length === 0 ? (
              <div className="rounded-[1.75rem] border border-dashed border-slate-200 bg-slate-50 px-8 py-14 text-center">
                <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-3xl bg-white text-blue-700 shadow-sm">
                  <Target className="h-8 w-8" />
                </div>
                <h2 className="mt-5 text-2xl font-semibold text-slate-950">
                  Пока нет целей
                </h2>
                <p className="mx-auto mt-3 max-w-lg text-slate-600">
                  Здесь будут появляться твои финансовые цели. Добавь первую,
                  чтобы начать отслеживать прогресс.
                </p>
              </div>
            ) : (
              <div className="space-y-4">
                {goals.map((goal) => {
                  const currentValue = Number.parseFloat(goal.current) || 0;
                  const targetValue = Number.parseFloat(goal.target) || 0;
                  const progress =
                    targetValue > 0
                      ? Math.min(100, (currentValue / targetValue) * 100)
                      : 0;

                  return (
                    <div
                      key={goal.id}
                      className="rounded-[1.75rem] border border-slate-200 bg-white p-6 shadow-sm"
                    >
                      <div className="flex items-start justify-between gap-4">
                        <div className="flex items-center gap-4">
                          <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-blue-50 text-blue-700">
                            <Target className="h-6 w-6" />
                          </div>
                          <div>
                            <h3 className="text-lg font-semibold text-slate-950">
                              {goal.title}
                            </h3>
                            <p className="mt-1 text-sm text-slate-500">
                              {currentValue.toLocaleString("ru-RU")} ₽ из{" "}
                              {targetValue.toLocaleString("ru-RU")} ₽
                            </p>
                          </div>
                        </div>

                        <button
                          onClick={() => removeGoal(goal.id)}
                          className="inline-flex h-10 w-10 items-center justify-center rounded-full border border-slate-200 text-slate-500 transition hover:bg-red-50 hover:text-red-600"
                          aria-label="Удалить цель"
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      </div>

                      <div className="mt-5">
                        <div className="mb-2 flex items-center justify-between text-sm">
                          <span className="text-slate-500">Прогресс</span>
                          <span className="font-medium text-slate-700">
                            {progress.toFixed(0)}%
                          </span>
                        </div>
                        <div className="h-3 overflow-hidden rounded-full bg-slate-100">
                          <div
                            className="h-full rounded-full bg-[linear-gradient(135deg,#0f3cc9_0%,#1d7cf2_100%)]"
                            style={{ width: `${progress}%` }}
                          />
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
