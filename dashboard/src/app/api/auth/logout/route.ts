import { backendFetch, clearBackendSession, proxyResponse } from "../../_lib/backend";

export async function POST() {
  const response = await backendFetch("/api/dashboard/auth/logout", {
    method: "POST",
  });
  await clearBackendSession();
  return proxyResponse(response);
}
