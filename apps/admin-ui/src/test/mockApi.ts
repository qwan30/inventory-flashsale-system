export const TEST_SESSION = {
  accessToken: "test-access-token",
  accessTokenExpiresAt: "2026-03-16T08:00:00Z",
  username: "admin",
  displayName: "System Admin",
  role: "ADMIN" as const,
};

export function jsonResponse(body: unknown, status = 200) {
  return Promise.resolve(
    new Response(JSON.stringify(body), {
      status,
      headers: {
        "Content-Type": "application/json",
      },
    }),
  );
}
