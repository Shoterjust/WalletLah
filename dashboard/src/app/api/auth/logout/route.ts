import { backendFetch, proxyResponse } from "../../_lib/backend";

export async function POST() {
  const response = await backendFetch("/api/dashboard/auth/logout", {
    method: "POST",
  });
  return proxyResponse(response, { clearSession: true });
}
