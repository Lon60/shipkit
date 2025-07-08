import { initTRPC } from '@trpc/server';
import { env } from '@/env';

const t = initTRPC.create();

export const router = t.router;
export const publicProcedure = t.procedure;

export const appRouter = router({
  config: publicProcedure.query(() => {
    return {
      apiBaseUrl: String(env.API_BASE_URL),
      appName: String(env.NEXT_PUBLIC_APP_NAME),
    };
  }),
});

export type AppRouter = typeof appRouter; 