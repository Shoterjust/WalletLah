import { backendFetch, backendSessionIdFrom, proxyResponse } from "../../_lib/backend";

export async function POST(request: Request) {
  try {
    const body = await request.text();
    const response = await backendFetch("/api/dashboard/auth/link-code", {
      method: "POST",
      body,
    });

    if (!response.ok) {
      return proxyResponse(response);
    }

    const backendSessionId = backendSessionIdFrom(response);
    if (!backendSessionId) {
      return Response.json(
        { message: "Backend accepted the code but did not return a dashboard session cookie." },
        { status: 502 }
      );
    }

    return proxyResponse(response, { backendSessionId });
  } catch (error) {
    console.error("Dashboard link-code proxy failed", error);
    return Response.json(
      { message: "Dashboard login proxy failed. Check Vercel function logs for /api/auth/link-code." },
      { status: 500 }
    );
  }
}
