import { createTRPCReact } from '@trpc/react-query';
import { httpBatchLink } from '@trpc/client';
import { notifyBackendStarting } from '@/lib/startup';
import type { AppRouter } from '@/server/api/trpc';

export const trpc = createTRPCReact<AppRouter>();

export const trpcClient = trpc.createClient({
  links: [
    httpBatchLink({
      url: '/trpc',
      fetch(url, options) {
        return fetch(url, options).then(async (res) => {
          if (res.status === 502 || res.status === 503) {
            notifyBackendStarting(res.status);
          }
          return res;
        }).catch((err) => {
          // Network failure: keep default behavior
          throw err;
        });
      },
    }),
  ],
}); 