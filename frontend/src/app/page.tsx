import { Suspense } from "react";
import ButtonLink from "@/components/ButtonLink";
import PageContainer from "@/components/PageContainer";
import NowShowing from "./now-showing";

const steps = [
  { title: "Pick a movie", text: "Browse what is on and choose a show time that suits you." },
  { title: "Choose your seats", text: "See the hall live and tap the seats you want." },
  { title: "Get your ticket", text: "Pay and your QR ticket is ready to scan at the door." },
];

export default function Home() {
  return (
    <PageContainer>
      <section className="animate-rise py-10 sm:py-16">
        <p className="text-brand-soft mb-4 text-xs font-semibold tracking-[0.2em] uppercase">
          Now booking
        </p>
        <h1 className="font-display max-w-3xl text-5xl leading-[1.05] font-extrabold tracking-tight text-balance sm:text-7xl">
          Your seat is <span className="text-brand-soft">waiting</span>.
        </h1>
        <p className="mt-6 max-w-xl text-lg text-zinc-300">
          Find a movie, pick the exact seats you want and walk in with a ticket on your phone.
        </p>
        <div className="mt-8 flex flex-wrap gap-3">
          <ButtonLink href="/movies" size="lg">
            Browse movies
          </ButtonLink>
          <ButtonLink href="/bookings" size="lg" variant="secondary">
            My bookings
          </ButtonLink>
        </div>
      </section>

      {/* Reads the session and calls the catalog, so it streams in behind a boundary. */}
      <Suspense fallback={null}>
        <NowShowing />
      </Suspense>

      <section aria-labelledby="how-it-works" className="mt-16">
        <h2 id="how-it-works" className="font-display text-2xl font-bold">
          How it works
        </h2>
        <ol className="mt-6 grid gap-4 sm:grid-cols-3">
          {steps.map((step, index) => (
            <li key={step.title} className="border-line bg-panel rounded-2xl border p-6">
              <span className="font-display text-brand-soft text-3xl font-extrabold">
                {String(index + 1).padStart(2, "0")}
              </span>
              <h3 className="mt-3 text-lg font-semibold">{step.title}</h3>
              <p className="mt-1 text-sm text-zinc-400">{step.text}</p>
            </li>
          ))}
        </ol>
      </section>
    </PageContainer>
  );
}
