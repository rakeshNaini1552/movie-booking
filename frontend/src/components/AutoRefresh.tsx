"use client";

import { useRouter } from "next/navigation";
import { useEffect } from "react";

type AutoRefreshProps = {
  // How long to wait between reloads, and how many times to try before giving up.
  intervalMs: number;
  attempts: number;
};

// Reloads the server data of the current page a few times. Used while a ticket is being issued:
// the booking is confirmed first and the ticket arrives a moment later through Kafka.
// The page stops rendering this component once the ticket exists, which stops the timer.
export default function AutoRefresh({ intervalMs, attempts }: AutoRefreshProps) {
  const router = useRouter();
  useEffect(() => {
    let remaining = attempts;
    const timer = setInterval(() => {
      remaining -= 1;
      router.refresh();
      if (remaining <= 0) clearInterval(timer);
    }, intervalMs);
    return () => clearInterval(timer);
  }, [router, intervalMs, attempts]);
  return null;
}
