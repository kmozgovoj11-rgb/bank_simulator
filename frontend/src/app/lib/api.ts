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
    fromAccountNumber: string;
    toAccountNumber: string;
    amount: string;
    description: string;
  }) {
    return request<Transaction>("/api/transfers", {
      method: "POST",
      body: payload,
    });
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
  