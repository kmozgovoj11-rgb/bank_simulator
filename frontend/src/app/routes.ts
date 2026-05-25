import { createBrowserRouter } from "react-router-dom";
import Dashboard from "./components/Dashboard";
import Goals from "./components/Goals";
import Login from "./components/Login";
import Registration from "./components/Registration";
import Transfer from "./components/Transfer";

export const router = createBrowserRouter([
  {
    path: "/",
    Component: Login,
  },
  {
    path: "/login",
    Component: Login,
  },
  {
    path: "/registration",
    Component: Registration,
  },
  {
    path: "/dashboard",
    Component: Dashboard,
  },
  {
    path: "/transfer",
    Component: Transfer,
  },
  {
    path: "/goals",
    Component: Goals,
  },
]);
