export type Customer = {
  customerId: string;
  fullName: string;
  phone: string;
};

export type Account = {
  accountId: string;
  number: string;
  balance: string;
  currency: string;
  status: string;
  kind: string;
  interestRate: string | null;
  lastInterestAccrualAt: string | null;
  canAccrueInterest: boolean | null;
};

export type Transaction = {
  transactionId: string;
  type: string;
  amount: string;
  currency: string;
  status: string;
  description: string | null;
  timestamp: string;
  fromAccountNumber: string | null;
  toAccountNumber: string | null;
};

export type SessionResponse = {
  customer: Customer;
};

export type DashboardResponse = {
  customer: Customer;
  accounts: Account[];
  recentTransactions: Transaction[];
};

export type HistoryResponse = {
  accountNumber: string;
  transactions: Transaction[];
};

export type InterestAccrualResponse = {
  accountNumber: string;
  amount: string;
  newBalance: string;
  accruedAt: string;
  transaction: Transaction;
};

type RequestOptions = {
  method?: "GET" | "POST";
  body?: unknown;
};

async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const response = await fetch(path, {
    method: options.method ?? "GET",
    headers: {
      "Content-Type": "application/json",
    },
    body: options.body ? JSON.stringify(options.body) : undefined,
  });

  const text = await response.text();
  const data = text ? (JSON.parse(text) as Record<string, unknown>) : {};

  if (!response.ok) {
    const message =
      typeof data.error === "string" ? data.error : "Request failed";
    throw new Error(message);
  }

  return data as T;
}

export function registerCustomer(payload: {
  fullName: string;
  phone: string;
  password: string;
}) {
  return request<SessionResponse>("/api/auth/register", {
    method: "POST",
    body: payload,
  });
}

export function loginCustomer(payload: { phone: string; password: string }) {
  return request<SessionResponse>("/api/auth/login", {
    method: "POST",
    body: payload,
  });
}

export function fetchDashboard(customerId: string) {
  return request<DashboardResponse>(`/api/customers/${customerId}/dashboard`);
}

export function fetchAccountHistory(accountNumber: string) {
  return request<HistoryResponse>(`/api/accounts/${accountNumber}/history`);
}

export function createTransfer(payload: {
  customerId: string;
  fromAccountNumber: string;
  recipient: string;
  amount: string;
  description: string;
}) {
  return request<Transaction>("/api/transfers", {
    method: "POST",
    body: payload,
  });
}

export function createAdditionalAccount(payload: {
  customerId: string;
  accountKind: "SAVINGS" | "CREDIT";
}) {
  return request<Account>(`/api/customers/${payload.customerId}/accounts`, {
    method: "POST",
    body: { accountKind: payload.accountKind },
  });
}

export function deleteAdditionalAccount(payload: {
  customerId: string;
  accountNumber: string;
}) {
  return request<Account>(
    `/api/customers/${payload.customerId}/accounts/delete`,
    {
      method: "POST",
      body: { accountNumber: payload.accountNumber },
    },
  );
}

export function topUpStandardAccount(payload: {
  customerId: string;
  amount: string;
  description: string;
}) {
  return request<Transaction>(`/api/customers/${payload.customerId}/deposits`, {
    method: "POST",
    body: { amount: payload.amount, description: payload.description },
  });
}

export function fundSavingsAccount(payload: {
  customerId: string;
  accountNumber: string;
  amount: string;
  description: string;
}) {
  return request<Transaction>(
    `/api/customers/${payload.customerId}/savings-deposits`,
    {
      method: "POST",
      body: {
        accountNumber: payload.accountNumber,
        amount: payload.amount,
        description: payload.description,
      },
    },
  );
}

export function accrueInterest(payload: {
  customerId: string;
  accountNumber: string;
}) {
  return request<InterestAccrualResponse>(
    `/api/accounts/${payload.accountNumber}/accrue-interest`,
    {
      method: "POST",
      body: { customerId: payload.customerId },
    },
  );
}

export function formatMoney(amount: string, currency: string) {
  const value = Number.parseFloat(amount);
  if (!Number.isFinite(value)) {
    return `${amount} ${currency}`;
  }

  try {
    return new Intl.NumberFormat("ru-RU", {
      style: "currency",
      currency,
      maximumFractionDigits: 2,
    }).format(value);
  } catch {
    return `${value.toFixed(2)} ${currency}`;
  }
}

export function formatDate(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat("ru-RU", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(date);
}
