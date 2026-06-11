import { backendFetch, proxyResponse } from "../../_lib/backend";

type RouteContext = {
  params: Promise<{ id: string }>;
};

export async function DELETE(_request: Request, context: RouteContext) {
  const { id } = await context.params;
  const response = await backendFetch(`/api/recurring-expenses/${encodeURIComponent(id)}`, {
    method: "DELETE",
  });
  return proxyResponse(response);
}
